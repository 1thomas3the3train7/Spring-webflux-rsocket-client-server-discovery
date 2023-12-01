package com.discovery.service.starter;

import com.discovery.service.server.service.DiscoveryService;
import com.discovery.service.server.service.impl.DiscoveryServiceImpl;
import com.discovery.service.server.utils.DiscoveryServiceUtils;
import com.discovery.service.server.utils.impl.DiscoveryServiceUtilsImpl;
import com.google.gson.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class ServerDiscoveryConfig {
    @Bean
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "server")
    public DiscoveryServiceUtils discoveryServiceUtils() {
        return new DiscoveryServiceUtilsImpl();
    }
    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "server")
    public WebClient webClient() {
        return WebClient.create();
    }

    @Bean
    @ConditionalOnMissingBean(Gson.class)
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "server")
    public Gson gson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        return gsonBuilder.create();
    }
    @Bean
    @ConditionalOnProperty(prefix = "discovery", name = "type", value = "server")
    public DiscoveryService discoveryService (DiscoveryServiceUtils discoveryServiceUtils,
                                              ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                                              Gson gson,
                                              WebClient webClient) {
        DiscoveryServiceImpl discoveryService = DiscoveryServiceImpl.builder()
                .discoveryServiceUtils(discoveryServiceUtils)
                .reactiveRedisTemplate(reactiveRedisTemplate)
                .gson(gson)
                .webClient(webClient)
                .build();
        return discoveryService;
    }
}
class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public JsonElement serialize(LocalDateTime dateTime, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(formatter.format(dateTime));
    }

    @Override
    public LocalDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return LocalDateTime.parse(jsonElement.getAsString(), formatter);
    }
}