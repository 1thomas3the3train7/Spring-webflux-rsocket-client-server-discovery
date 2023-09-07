package com.discovery.service.server.configuration;

import com.discovery.service.model.discovery.ServiceInfo;
import com.google.gson.reflect.TypeToken;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.util.List;

@Configuration
public class GsonConfig {
    public static Type listServiceInfo = new TypeToken<List<ServiceInfo>>(){}.getType();
}
