package com.discovery.service.client.service.rsocket;

import com.discovery.service.exception.BadServiceTypeException;
import com.discovery.service.model.discovery.ServiceInfo;
import com.discovery.service.model.discovery.ServiceType;
import com.google.gson.Gson;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
public abstract class RSocketClientRequester {
    protected ServiceType serviceType = null;
    protected List<LoadbalanceTarget> loadbalanceTargets = new ArrayList<>();
    protected final Gson gson;
    protected Mono<RSocketRequester> rSocketRequester;

    public RSocketClientRequester(Gson gson, ServiceType serviceType) {
        this.gson = gson;
        this.serviceType = serviceType;
    }

    public Mono<Void> refreshRequester() {
        log.info("REFRESH RSOCKET REQUESTER, TARGETS: {}", fromLoadBalanceTarget());
        rSocketRequester = Mono.fromCallable(() -> RSocketRequester.builder()
                        .rsocketConnector(r -> r.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))))
                        .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                        .transports(Mono.just(this.loadbalanceTargets), new RoundRobinLoadbalanceStrategy()))
                .subscribeOn(Schedulers.boundedElastic());

        return rSocketRequester
                .then();
    }

    ;

    public void buildLoadBalanceTargets(List<ServiceInfo> services) {
        log.info("BUILD LOAD BALANCE TARGETS, {}", services.stream().filter(s -> Objects.equals(this.serviceType, s.getServiceType())).toList());
        if (this.serviceType == null)
            throw new BadServiceTypeException("SERVICE TYPE == null");
        this.loadbalanceTargets = services.stream()
                .filter(s -> Objects.equals(this.serviceType, s.getServiceType()))
                .map(s -> LoadbalanceTarget.from(gson.toJson(s), TcpClientTransport.create(s.getAddress(), s.getRSocketPort())))
                .toList();
    }

    public void deleteLoadBalanceTarget(ServiceInfo serviceInfo) {
        log.info("START DELETE LOAD BALANCE TARGET: {}, DELETED SERVICE: {}", fromLoadBalanceTarget(), serviceInfo);
        this.loadbalanceTargets = this.loadbalanceTargets.stream()
                .filter(loadbalanceTarget -> {
                    final ServiceInfo curr = gson.fromJson(loadbalanceTarget.getKey(), ServiceInfo.class);
                    return !serviceInfo.equals(curr);
                })
                .toList();
        log.info("RESULT BALANCE TARGET: {}", fromLoadBalanceTarget());
    }

    public List<ServiceInfo> fromLoadBalanceTarget() {
        if (this.loadbalanceTargets == null)
            return null;
        return this.loadbalanceTargets.stream()
                .map(loadbalanceTarget -> gson.fromJson(loadbalanceTarget.getKey(), ServiceInfo.class))
                .toList();
    }

    public Mono<Void> compareAndRebuild(List<ServiceInfo> services) {
        final var currList = fromLoadBalanceTarget();
        final var listFromDiscovery = services.stream()
                .filter(s -> Objects.equals(s.getServiceType(), this.serviceType))
                .toList();
        if (listFromDiscovery.size() > currList.size()) {
            buildLoadBalanceTargets(listFromDiscovery);
            return refreshRequester();
        } else {
            for (ServiceInfo serviceInfo : currList) {
                if (!listFromDiscovery.contains(serviceInfo)) {
                    buildLoadBalanceTargets(listFromDiscovery);
                    break;
                }
            }
        }

        return Mono.empty();
    }
}
