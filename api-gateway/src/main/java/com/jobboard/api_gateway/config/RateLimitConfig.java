package com.jobboard.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter(
            @Value("${spring.cloud.gateway.redis-rate-limiter.replenish-rate}") int replenishRate,
            @Value("${spring.cloud.gateway.redis-rate-limiter.burst-capacity}") int burstCapacity,
            @Value("${spring.cloud.gateway.redis-rate-limiter.requested-tokens}") int requestedTokens) {
        return new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                   .getAddress().getHostAddress()
        );
    }
}
