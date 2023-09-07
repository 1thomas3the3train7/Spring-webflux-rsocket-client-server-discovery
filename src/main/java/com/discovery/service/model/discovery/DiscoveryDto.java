package com.discovery.service.model.discovery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryDto {
    private ServiceInfo serviceInfo;
    private ServiceStatus serviceStatus;
}
