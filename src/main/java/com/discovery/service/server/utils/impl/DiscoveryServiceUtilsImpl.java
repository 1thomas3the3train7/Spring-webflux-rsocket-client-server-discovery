package com.discovery.service.server.utils.impl;

import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceType;
import com.discovery.service.server.utils.DiscoveryServiceUtils;

public class DiscoveryServiceUtilsImpl implements DiscoveryServiceUtils {
    public final static String SERVICE_GROUP = "SERVICE_GROUP_";

    public String buildServicesRedisKey(ServiceType serviceType) {
        return SERVICE_GROUP + serviceType.toString();
    }

    public String buildServiceUrl(ServiceInfo serviceInfo) {
        return "http://" + serviceInfo.getAddress() + ":" + serviceInfo.getPort();
    }
}
