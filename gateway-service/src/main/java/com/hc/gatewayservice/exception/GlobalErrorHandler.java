package com.hc.gatewayservice.exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        if (ex instanceof ResponseStatusException rse) {
            exchange.getResponse().setStatusCode(rse.getStatusCode());

            if (rse.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return handleRateLimitExceeded(exchange);
            }
        }

        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorResponse = """
            {
                "error": "internal_server_error",
                "message": "An unexpected error occurred"
            }
            """;

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(errorResponse.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-Rate-Limit-Retry-After-Seconds", "60");
        exchange.getResponse().getHeaders().set("Retry-After", "60");

        String errorResponse = """
            {
                "error": "rate_limit_exceeded",
                "message": "Too many requests. Please try again later.",
                "retryAfter": 60,
                "level": "gateway"
            }
            """;

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(errorResponse.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
