package com.discovery.service.starter;


import com.discovery.service.client.configuration.ClientServiceConfig;
import com.discovery.service.client.controller.ConfigController;
import com.discovery.service.client.service.DiscoveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "discovery", name = "type", value = "client")
public class DiscoveryController extends ConfigController {
    public DiscoveryController(ClientServiceConfig clientServiceConfig, DiscoveryService discoveryService) {
        super(clientServiceConfig, discoveryService);
    }
}
