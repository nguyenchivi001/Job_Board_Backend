package com.jobboard.api_gateway.config;

import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteDefinitionLocator customRouteDefinitionLocator() {
        return () -> Flux.fromIterable(List.of(
                route("auth-service",        "http://localhost:8081", "/api/auth/**"),
                route("job-service",         "http://localhost:8082", "/api/jobs/**"),
                route("application-service", "http://localhost:8083", "/api/applications/**"),
                route("profile-service",     "http://localhost:8084", "/api/profiles/**")
        ));
    }

    private RouteDefinition route(String id, String uri, String path) {
        RouteDefinition def = new RouteDefinition();
        def.setId(id);
        def.setUri(URI.create(uri));
        def.setPredicates(List.of(new PredicateDefinition("Path=" + path)));
        return def;
    }
}
