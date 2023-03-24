package com.github.sanjayrawat1.bookshop.edgeservice.config;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rate Limiter Configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
public class RateLimiterConfiguration {

    /**
     * The RequestRateLimiter filter relies on KeyResolver bean to determine which bucket to use for each request.
     * By default, it uses the currently authenticated user in Spring Security. If no user is defined, use default
     * key to apply rate-limiting to all unauthenticated requests.
     *
     * @return bucket key resolver.
     */
    @Bean
    public KeyResolver keyResolver() {
        return exchange -> exchange.getPrincipal().map(Principal::getName).defaultIfEmpty("anonymous");
    }
}
