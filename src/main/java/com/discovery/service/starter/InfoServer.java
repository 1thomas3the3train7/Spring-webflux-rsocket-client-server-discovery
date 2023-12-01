package com.discovery.service.starter;

import lombok.Getter;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

public class InfoServer implements ApplicationListener<WebServerInitializedEvent> {
    @Getter
    private int port;
    @Getter
    private String address;
    private ServerProperties serverProperties;
    public InfoServer(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
        this.address = serverProperties.getAddress().getHostAddress();
    }
}
