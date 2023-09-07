package com.discovery.service.client.service.impl;

import com.discovery.service.client.configuration.ClientServiceConfig;
import com.discovery.service.client.service.DiscoveryService;
import com.discovery.service.client.service.rsocket.RSocketClientRequester;
import com.discovery.service.model.discovery.DiscoveryDto;
import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceStatus;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@Builder
public class DiscoveryServiceImpl implements DiscoveryService, CommandLineRunner {
    private final WebClient webClient;
    private final ClientServiceConfig discoveryServiceConfig;
    private final List<RSocketClientRequester> rSocketClientRequesters;
    private final String DISCOVERY_URL;

    public DiscoveryServiceImpl(WebClient webClient, ClientServiceConfig discoveryServiceConfig, List<RSocketClientRequester> rSocketClientRequesters, String DISCOVERY_URL) {
        this.webClient = webClient;
        this.discoveryServiceConfig = discoveryServiceConfig;
        this.rSocketClientRequesters = rSocketClientRequesters;
        this.DISCOVERY_URL = DISCOVERY_URL;
    }

    @Override
    public Mono<DiscoveryDto> register() {
        final DiscoveryDto discoveryDto = DiscoveryDto.builder()
                .serviceStatus(ServiceStatus.REGISTER)
                .serviceInfo(discoveryServiceConfig.buildInfo())
                .build();
        return webClient
                .post()
                .uri(DISCOVERY_URL + "/register")
                .body(Mono.just(discoveryDto), DiscoveryDto.class)
                .retrieve()
                .bodyToMono(DiscoveryDto.class)
                .flatMap(disc -> Flux.fromIterable(rSocketClientRequesters)
                        .flatMap(rSocketClientRequester -> {
                            rSocketClientRequester.buildLoadBalanceTargets(disc.getServiceInfo().getServiceToConnected());
                            return rSocketClientRequester.refreshRequester();
                        })
                        .collectList()
                        .map(list -> disc))
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
                .doOnError(throwable -> {
                    log.info("ERROR REGISTER SERVICE: {}", throwable.getMessage());
                });
    }

    @Override
    public Mono<DiscoveryDto> heartBeat() {
        final DiscoveryDto discoveryDto = DiscoveryDto.builder()
                .serviceStatus(ServiceStatus.HEARTBEAT)
                .serviceInfo(discoveryServiceConfig.buildInfo())
                .build();
        return webClient
                .post()
                .uri(DISCOVERY_URL + "/heartbeat")
                .bodyValue(discoveryDto)
                .retrieve()
                .bodyToMono(DiscoveryDto.class)
                .flatMap(disc -> Flux.fromIterable(rSocketClientRequesters)
                        .flatMap(rSocketClientRequester -> rSocketClientRequester.compareAndRebuild(disc.getServiceInfo().getServiceToConnected()))
                        .collectList()
                        .map(v -> disc));
    }

    @Override
    public Mono<Void> deletedService(DiscoveryDto discoveryDto) {
        final ServiceInfo requestService = discoveryDto.getServiceInfo();
        if (Objects.equals(discoveryDto.getServiceStatus(), ServiceStatus.DELETE)) {
            return Flux.fromIterable(rSocketClientRequesters.stream().filter(req -> Objects.equals(req.getServiceType(), requestService.getServiceType())).toList())
                    .flatMap(rSocketClientRequester -> {
                        rSocketClientRequester.deleteLoadBalanceTarget(requestService);
                        return rSocketClientRequester.refreshRequester();
                    })
                    .collectList()
                    .then();
        }
        return Mono.empty();
    }

    @Override
    public Mono<DiscoveryDto> refreshConfig(DiscoveryDto discoveryDto) {
        return null;
    }


    @Override
    public void run(String... args) throws Exception {
        this.register().subscribe(s -> log.info("REGISTER SERVICE {}", s));
        Flux.interval(Duration.ofSeconds(5))
                .flatMap(v -> heartBeat())
                .onErrorResume(throwable -> {
                    log.error("ERROR HEARTBEAT, MESSAGE: {}", throwable.getMessage());
                    return Mono.just(DiscoveryDto.builder().build());
                })
                .repeat()
                .subscribe();
    }
}
