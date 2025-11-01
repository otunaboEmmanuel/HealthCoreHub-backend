package com.hc.gatewayservice.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class PreserveContentTypeGatewayFilterFactory
        extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);

            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                // Preserve the original content-type
                return chain.filter(exchange);
            }

            return chain.filter(exchange);
        };
    }
}
