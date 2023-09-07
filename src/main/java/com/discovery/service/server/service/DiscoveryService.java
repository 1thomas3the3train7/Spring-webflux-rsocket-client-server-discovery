package com.discovery.service.server.service;

import com.discovery.service.model.discovery.DiscoveryDto;
import reactor.core.publisher.Mono;

public interface DiscoveryService {
    Mono<DiscoveryDto> registerService(DiscoveryDto discoveryDto);

    Mono<DiscoveryDto> heartbeatService(DiscoveryDto discoveryDto);
}
