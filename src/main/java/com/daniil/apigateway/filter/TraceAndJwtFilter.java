package com.daniil.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class TraceAndJwtFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
        exchange.getAttributes().put("traceId", traceId);

        log.info("[{}] Incoming request: {} {}", traceId,
                exchange.getRequest().getMethod(), exchange.getRequest().getURI());

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            exchange.mutate()
                    .request(r -> r.headers(h -> h.set(HttpHeaders.AUTHORIZATION, authHeader)))
                    .build();
        }

        return chain.filter(exchange)
                .doOnSuccess(v -> log.info("[{}] Response: {}", traceId, exchange.getResponse().getStatusCode()))
                .doOnError(err -> log.error("[{}] Error: {}", traceId, err.getMessage()));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
