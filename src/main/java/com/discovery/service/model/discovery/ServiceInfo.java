package com.discovery.service.model.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo {
    private String address;
    private int port;
    private int rSocketPort;
    private ServiceType serviceType;
    private List<ServiceInfo> serviceToConnected;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInfo that = (ServiceInfo) o;
        return port == that.port && rSocketPort == that.rSocketPort && Objects.equals(address, that.address) && serviceType == that.serviceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, rSocketPort, serviceType);
    }
}
