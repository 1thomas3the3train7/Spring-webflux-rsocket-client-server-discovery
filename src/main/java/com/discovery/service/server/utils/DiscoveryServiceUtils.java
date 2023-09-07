package com.discovery.service.server.utils;

import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceType;

public interface DiscoveryServiceUtils {
    String buildServicesRedisKey(ServiceType serviceType);
    String buildServiceUrl(ServiceInfo serviceInfo);
}
