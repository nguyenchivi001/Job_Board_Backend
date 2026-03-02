package com.jobboard.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${auth-service.url}") String authUrl,
                               @Value("${job-service.url}") String jobUrl,
                               @Value("${application-service.url}") String appUrl,
                               @Value("${profile-service.url}") String profileUrl) {
        return builder.routes()
                .route("auth-service",        r -> r.path("/api/auth/**").uri(authUrl))
                .route("job-service",         r -> r.path("/api/jobs/**").uri(jobUrl))
                .route("application-service", r -> r.path("/api/applications/**").uri(appUrl))
                .route("profile-service",     r -> r.path("/api/profiles/**").uri(profileUrl))
                .build();
    }
}
