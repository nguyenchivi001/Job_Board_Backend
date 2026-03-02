package com.jobboard.api_gateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/actuator"
    );

    private static final List<String> OPTIONAL_AUTH_PREFIXES = List.of(
            "/api/jobs",
            "/api/profiles"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // 1. Always public
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 2. Optional JWT — GET /api/jobs/**, GET /api/profiles/**
        if (HttpMethod.GET.equals(method) && isOptionalAuthPath(path)) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange); // anonymous GET → cho qua
            }
            // Có token → validate và forward headers
            return validateAndForward(exchange, chain, authHeader.substring(7));
        }

        // 3. Require JWT
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return sendUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        return validateAndForward(exchange, chain, authHeader.substring(7));
    }

    private Mono<Void> validateAndForward(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        if (!jwtUtil.validateToken(token)) {
            return sendUnauthorized(exchange, "Invalid or expired token");
        }

        return redisTemplate.hasKey("blacklist:" + token)
                .flatMap(blacklisted -> {
                    if (Boolean.TRUE.equals(blacklisted)) {
                        return sendUnauthorized(exchange, "Token has been revoked");
                    }

                    String userId = jwtUtil.extractUserId(token);
                    String role = jwtUtil.extractRole(token);

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isOptionalAuthPath(String path) {
        return OPTIONAL_AUTH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> sendUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        String body = """
                {"status":401,"error":"Unauthorized","message":"%s"}""".formatted(message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
