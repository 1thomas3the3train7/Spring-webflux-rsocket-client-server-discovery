package com.discovery.service.server.service.impl;

import com.discovery.service.model.discovery.DiscoveryDto;
import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceStatus;
import com.discovery.service.model.discovery.ServiceType;
import com.discovery.service.server.service.DiscoveryService;
import com.discovery.service.server.utils.DiscoveryServiceUtils;
import com.google.gson.Gson;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.discovery.service.server.configuration.GsonConfig.listServiceInfo;

@Slf4j
@Builder
public class DiscoveryServiceImpl implements DiscoveryService, CommandLineRunner {
    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final DiscoveryServiceUtils discoveryServiceUtils;
    private final Gson gson;

    public DiscoveryServiceImpl(WebClient webClient, ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                DiscoveryServiceUtils discoveryServiceUtils, Gson gson) {
        this.webClient = webClient;
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.discoveryServiceUtils = discoveryServiceUtils;
        this.gson = gson;
    }

    @Override
    public void run(String... args) throws Exception {
        Flux.interval(Duration.ofSeconds(5))
                .flatMap(v -> {
                    final ServiceType[] serviceTypes = ServiceType.values();
                    return Flux.fromArray(serviceTypes)
                            .parallel(4)
                            .runOn(Schedulers.parallel())
                            .flatMap(curr -> reactiveRedisTemplate.opsForValue()
                                    .get(discoveryServiceUtils.buildServicesRedisKey(curr))
                                    .map(s -> {
                                        List<ServiceInfo> services = gson.fromJson(s, listServiceInfo);
                                        if (services == null)
                                            services = List.of();
                                        return services;
                                    })
                                    .flatMapIterable(list -> list)
                                    .flatMap(serviceInfo -> webClient.post()
                                            .uri(discoveryServiceUtils.buildServiceUrl(serviceInfo) + "/discovery/heartbeat")
                                            .bodyValue(serviceInfo)
                                            .retrieve()
                                            .bodyToMono(DiscoveryDto.class)
                                            .map(discoveryDto -> {
                                                if (!Objects.equals(discoveryDto.getServiceInfo(), serviceInfo)) {
                                                    log.warn("SERVICE NOT EQUALS, REQUEST: {}, RESPONSE: {}", serviceInfo, discoveryDto.getServiceInfo());
                                                }
                                                return discoveryDto;
                                            })
                                            .onErrorResume(throwable -> {
                                                log.warn("ERROR IN HEARTBEAT SERVICE, REQUEST: {}", serviceInfo);
                                                if (throwable instanceof WebClientRequestException) {
                                                    final String redisKey = discoveryServiceUtils.buildServicesRedisKey(serviceInfo.getServiceType());
                                                    return reactiveRedisTemplate.opsForValue()
                                                            .get(redisKey)
                                                            .map(s -> {
                                                                List<ServiceInfo> services = gson.fromJson(s, listServiceInfo);
                                                                if (services == null)
                                                                    services = new ArrayList<>();
                                                                final boolean resRemove = services.remove(serviceInfo);
                                                                log.info("REMOVE SERVICE FROM REDIS,  RESULT: {}, KEY: {}, SERVICE: {}, LIST: {}",
                                                                        resRemove, redisKey, serviceInfo, services);
                                                                return services;
                                                            })
                                                            .flatMap(listServiceInfo -> reactiveRedisTemplate.opsForValue()
                                                                    .set(redisKey, gson.toJson(listServiceInfo)))
                                                            .flatMap(b -> {
                                                                if (!b)
                                                                    log.error("REDIS RETURN FALSE WHEN SET VALUE, KEY: {}, VALUE: {}", redisKey, listServiceInfo);
                                                                return sendToAllServiceMessageDeletedService(serviceInfo);
                                                            })
                                                            .thenReturn(DiscoveryDto.builder().build());
                                                }
                                                log.error(throwable.getMessage());
                                                return Mono.just(DiscoveryDto.builder().build());
                                            })));
                })
                .repeat()
                .subscribe();
    }

    public Mono<Void> sendToAllServiceMessageDeletedService(ServiceInfo serviceDeleted) {
        log.info("SERVICE DELETED {}", serviceDeleted);
        return Flux.fromArray(ServiceType.values())
                .filter(service -> !service.equals(ServiceType.DISCOVERY) && !service.equals(serviceDeleted.getServiceType()))
                .map(discoveryServiceUtils::buildServicesRedisKey)
                .flatMap(redisKey -> reactiveRedisTemplate.opsForValue()
                        .get(redisKey)
                        .map(string -> gson.<List<ServiceInfo>>fromJson(string, listServiceInfo))
                        .flatMapIterable(list -> list)
                        .flatMap(service -> webClient.post()
                                .uri(discoveryServiceUtils.buildServiceUrl(service) + "/discovery/heartbeat")
                                .bodyValue(DiscoveryDto.builder().serviceStatus(ServiceStatus.DELETE).serviceInfo(serviceDeleted).build())
                                .retrieve()
                                .bodyToMono(String.class)
                                .onErrorResume(throwable -> {
                                    log.warn("ERROR SEND MESSAGE ABOUT DELETED SERVICE, {}, {}", service, throwable.getMessage());
                                    return Mono.just(throwable.getMessage());
                                }))
                        .collectList())
                .then();
    }

    @Override
    public Mono<DiscoveryDto> registerService(DiscoveryDto discoveryDto) {
        log.info("START REGISTER SERVICE {}", discoveryDto);
        final ServiceInfo serviceRequest = discoveryDto.getServiceInfo();
        final String redisKey = discoveryServiceUtils.buildServicesRedisKey(serviceRequest.getServiceType());
        return reactiveRedisTemplate.opsForValue()
                .get(redisKey)
                .map(s -> {
                    List<ServiceInfo> list = gson.fromJson(s, listServiceInfo);
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    if (list.contains(serviceRequest)) {
                        list.remove(serviceRequest);
                        log.warn("SERVICE ALREADY REGISTER {}", serviceRequest);
                    }
                    list.add(serviceRequest);
                    return list;
                })
                .flatMap(list -> reactiveRedisTemplate.opsForValue().set(redisKey, gson.toJson(list)))
                .flatMapIterable(b -> discoveryDto.getServiceInfo().getServiceToConnected() == null ? new ArrayList<>() : discoveryDto.getServiceInfo().getServiceToConnected())
                .flatMap(serviceInfo -> reactiveRedisTemplate.opsForValue().get(discoveryServiceUtils.buildServicesRedisKey(serviceInfo.getServiceType())))
                .map(s -> gson.<List<ServiceInfo>>fromJson(s, listServiceInfo))
                .collectList()
                .map(lists -> {
                    final List<ServiceInfo> servicesToConnect = new ArrayList<>();
                    lists.forEach(servicesToConnect::addAll);
                    serviceRequest.setServiceToConnected(servicesToConnect);
                    return DiscoveryDto.builder()
                            .serviceInfo(serviceRequest)
                            .serviceStatus(ServiceStatus.REGISTER)
                            .build();
                });
    }

    @Override
    public Mono<DiscoveryDto> heartbeatService(DiscoveryDto discoveryDto) {
        log.info("HEARTBEAT {}: {}", discoveryDto.getServiceInfo().getServiceType(), discoveryDto);
        final ServiceInfo serviceRequest = discoveryDto.getServiceInfo();
        final String redisKey = discoveryServiceUtils.buildServicesRedisKey(serviceRequest.getServiceType());
        return reactiveRedisTemplate.opsForValue()
                .get(redisKey)
                .map(s -> {
                    List<ServiceInfo> servicesFromRedis = gson.fromJson(s, listServiceInfo);
                    if (servicesFromRedis == null || servicesFromRedis.isEmpty()) {
                        servicesFromRedis = new ArrayList<>();
                        log.warn("SERVICES IN REDIS IS null, request: {}, redisKey: {}", discoveryDto, redisKey);
                    }
                    if (!servicesFromRedis.contains(serviceRequest)) {
                        log.warn("SERVICE: {} NOT CONTAIN IN REDIS SERVICE LIST: {}", discoveryDto, servicesFromRedis);
                    }
                    servicesFromRedis.remove(serviceRequest);
                    servicesFromRedis.add(serviceRequest);
                    return servicesFromRedis;
                })
                .flatMap(list -> reactiveRedisTemplate.opsForValue().set(redisKey, gson.toJson(list)))
                .flatMapIterable(b -> discoveryDto.getServiceInfo().getServiceToConnected() == null ? new ArrayList<>() : discoveryDto.getServiceInfo().getServiceToConnected())
                .flatMap(serviceInfo -> reactiveRedisTemplate.opsForValue().get(discoveryServiceUtils.buildServicesRedisKey(serviceInfo.getServiceType())))
                .map(s -> gson.<List<ServiceInfo>>fromJson(s, listServiceInfo))
                .collectList()
                .map(list -> {
                    final List<ServiceInfo> servicsToConnect = new ArrayList<>();
                    list.forEach(servicsToConnect::addAll);
                    serviceRequest.setServiceToConnected(servicsToConnect);
                    return DiscoveryDto.builder()
                            .serviceStatus(ServiceStatus.HEARTBEAT)
                            .serviceInfo(serviceRequest)
                            .build();
                });
    }
}