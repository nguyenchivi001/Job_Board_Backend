package com.jobboard.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${auth-service.url}") String authUrl,
                               @Value("${job-service.url}") String jobUrl,
                               @Value("${application-service.url}") String appUrl,
                               @Value("${profile-service.url}") String profileUrl,
                               RedisRateLimiter rateLimiter,
                               KeyResolver keyResolver) {

        Function<GatewayFilterSpec, UriSpec> rateLimit = f -> f.requestRateLimiter(c -> {
            c.setRateLimiter(rateLimiter);
            c.setKeyResolver(keyResolver);
        });

        return builder.routes()
                .route("auth-service",        r -> r.path("/api/auth/**").filters(rateLimit).uri(authUrl))
                .route("job-service",         r -> r.path("/api/jobs/**").filters(rateLimit).uri(jobUrl))
                .route("application-service", r -> r.path("/api/applications/**").filters(rateLimit).uri(appUrl))
                .route("profile-service",     r -> r.path("/api/profiles/**").filters(rateLimit).uri(profileUrl))
                .build();
    }
}
