package com.github.sanjayrawat1.bookshop.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

/**
 * Security Configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchange -> exchange.pathMatchers("/management/**").permitAll().anyExchange().authenticated())
            // enables OAuth2 Resource Server support using the default configuration based on JWT (JWT authentication).
            .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt)
            // each request must include an Access Token, so there's no need to keep a session cache alive between requests. We want it to be stateless.
            .requestCache(requestCacheSpec -> requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()))
            // since the authentication strategy is stateless and doesn't involve a browser-based client, we can safely disable the CSRF protection.
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }
}
