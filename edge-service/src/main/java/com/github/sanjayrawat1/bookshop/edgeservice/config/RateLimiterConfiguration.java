package com.github.sanjayrawat1.bookshop.edgeservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter Configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
public class RateLimiterConfiguration {

    /**
     * The RequestRateLimiter filter relies on KeyResolver bean to determine which bucket to use for each request.
     * By default, it uses the currently authenticated user in Spring Security. Until we add security, we will return
     * a constant value so that all requests will be mapped to the same bucket.
     *
     * @return bucket key resolver.
     */
    @Bean
    public KeyResolver keyResolver() {
        return exchange -> Mono.just("anonymous");
    }
}
