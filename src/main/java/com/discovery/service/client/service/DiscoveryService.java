package com.discovery.service.client.service;

import com.discovery.service.model.discovery.DiscoveryDto;
import reactor.core.publisher.Mono;

public interface DiscoveryService {
    Mono<DiscoveryDto> register();

    Mono<DiscoveryDto> heartBeat();

    Mono<Void> deletedService(DiscoveryDto discoveryDto);

    Mono<DiscoveryDto> refreshConfig(DiscoveryDto discoveryDto);
}
