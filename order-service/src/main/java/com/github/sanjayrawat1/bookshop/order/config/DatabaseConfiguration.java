package com.github.sanjayrawat1.bookshop.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

/**
 * Database configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
@EnableR2dbcAuditing
public class DatabaseConfiguration {

    @Bean
    ReactiveAuditorAware<String> auditorAware() {
        return () ->
            ReactiveSecurityContextHolder
                .getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
