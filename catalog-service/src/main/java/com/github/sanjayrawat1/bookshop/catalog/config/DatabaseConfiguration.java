package com.github.sanjayrawat1.bookshop.catalog.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Database configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
// Enables entity auditing in Spring Data JDBC
@EnableJdbcAuditing
public class DatabaseConfiguration {

    /**
     * Returns the currently authenticated user for auditing purposes.
     */
    @Bean
    AuditorAware<String> auditorAware() {
        return () ->
            Optional
                .ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
