package com.discovery.service.starter;

import com.discovery.service.client.configuration.ClientServiceConfig;
import com.discovery.service.client.service.DiscoveryService;
import com.discovery.service.client.service.impl.DiscoveryServiceImpl;
import com.discovery.service.client.service.rsocket.RSocketClientRequester;
import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
public class ClientDiscoveryServiceConfig {
    @Bean
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "client")
    public InfoServer infoServer(ServerProperties serverProperties) {
        InfoServer infoServer = new InfoServer(serverProperties);
        return infoServer;
    }

    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "client")
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "client")
    public DiscoveryService discoveryService(WebClient webClient,
                                             ClientServiceConfig clientServiceConfig,
                                             List<RSocketClientRequester> rSocketClientRequesters,
                                             @Value("{discovery.client.url}") String url) {
        DiscoveryServiceImpl discoveryService = new DiscoveryServiceImpl(webClient,
                clientServiceConfig,
                rSocketClientRequesters,
                url);
        return discoveryService;
    }

    @Bean
    @ConditionalOnMissingBean(ClientServiceConfig.class)
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "client")
    public ClientServiceConfig clientServiceConfig(InfoServer infoServer,
                                                   @Value("{discovery.client.connected}") String connected,
                                                   @Value("{discovery.client.type}") String type) {

        ClientServiceConfig clientServiceConfig = new ClientServiceConfig() {
            @Override
            public void configInit() {
                this.address = infoServer.getAddress();
                this.port = infoServer.getPort();
                if (connected != null) {
                    this.serviceTypesToConnected = Arrays.stream(connected.split(",")).map(s -> {
                                try {
                                    ServiceType serviceType = ServiceType.valueOf(s);
                                    return serviceType;
                                } catch (Exception e) {
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .toList();
                } else {
                    this.serviceTypesToConnected = List.of();
                }
            }

            @Override
            public ServiceInfo buildInfo() {
                return ServiceInfo.builder()
                        .address(this.address)
                        .port(this.port)
                        .serviceType(ServiceType.valueOf(type))
                        .serviceToConnected(this.serviceTypesToConnected.stream().map(f -> ServiceInfo.builder().serviceType(f).build()).toList())
                        .build();
            }
        };

        return clientServiceConfig;
    }
}
