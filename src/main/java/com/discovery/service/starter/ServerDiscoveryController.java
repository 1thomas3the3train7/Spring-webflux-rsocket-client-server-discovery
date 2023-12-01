package com.discovery.service.starter;

import com.discovery.service.server.controller.DiscoveryController;
import com.discovery.service.server.service.DiscoveryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "discovery", name = "type", value = "server")
public class ServerDiscoveryController extends DiscoveryController {
    public ServerDiscoveryController(DiscoveryService discoveryService) {
        super(discoveryService);
    }
}
