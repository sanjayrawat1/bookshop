package com.github.sanjayrawat1.bookshop.edgeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration.
 *
 * @author Sanjay Singh Rawat
 */
@EnableWebFluxSecurity
public class SecurityConfiguration {

    /**
     * The SecurityWebFilterChain bean is used to define and configure security filter policies for the application.
     */
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            // enables user authentication with OAuth2/OpenID connect
            .oauth2Login(Customizer.withDefaults())
            .build();
    }
}
