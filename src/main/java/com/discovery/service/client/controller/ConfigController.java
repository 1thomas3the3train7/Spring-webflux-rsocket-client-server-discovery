package com.discovery.service.client.controller;

import com.discovery.service.client.configuration.ClientServiceConfig;
import com.discovery.service.client.service.DiscoveryService;
import com.discovery.service.model.discovery.DiscoveryDto;
import com.discovery.service.model.discovery.ServiceStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

@RequestMapping("/discovery")
public abstract class ConfigController {
    private final ClientServiceConfig clientServiceConfig;
    private final DiscoveryService discoveryService;

    public ConfigController(ClientServiceConfig clientServiceConfig, DiscoveryService discoveryService) {
        this.clientServiceConfig = clientServiceConfig;
        this.discoveryService = discoveryService;
    }

    @PostMapping("/heartbeat")
    public Mono<DiscoveryDto> heartbeat(@RequestBody DiscoveryDto discoveryDto) {
        return Mono.fromCallable(() -> {
                    if (discoveryDto.getServiceStatus() != null && Objects.equals(discoveryDto.getServiceStatus(), ServiceStatus.DELETE)) {
                        discoveryService.deletedService(discoveryDto);
                    }
                    final DiscoveryDto disc = DiscoveryDto.builder()
                            .serviceStatus(ServiceStatus.HEARTBEAT)
                            .serviceInfo(clientServiceConfig.buildInfo())
                            .build();
                    return disc;
                })
                .subscribeOn(Schedulers.boundedElastic());


    }
}
