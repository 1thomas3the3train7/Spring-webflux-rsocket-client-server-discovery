package com.discovery.service.server.controller;

import com.discovery.service.model.discovery.DiscoveryDto;
import com.discovery.service.server.service.DiscoveryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public abstract class DiscoveryController {
    protected final DiscoveryService discoveryService;

    public DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @PostMapping("/register")
    public Mono<DiscoveryDto> register(@RequestBody DiscoveryDto discoveryDto) {
        return discoveryService.registerService(discoveryDto);
    }

    ;

    @PostMapping("/heartbeat")
    public Mono<DiscoveryDto> heartbeat(@RequestBody DiscoveryDto discoveryDto) {
        return discoveryService.heartbeatService(discoveryDto);
    }
}
