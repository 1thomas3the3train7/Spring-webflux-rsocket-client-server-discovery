package com.discovery.service.client.configuration;

import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceType;
import jakarta.annotation.PostConstruct;

import java.util.List;

public abstract class ClientServiceConfig {
    protected String address;
    protected int port;
    protected int rSocketPort;
    protected List<ServiceType> serviceTypesToConnected;


    @PostConstruct
    public abstract void configInit();

    public abstract ServiceInfo buildInfo();
}
