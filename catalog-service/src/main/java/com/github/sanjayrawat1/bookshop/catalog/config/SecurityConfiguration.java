package com.github.sanjayrawat1.bookshop.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.GET, "/", "/books/**").permitAll().anyRequest().hasRole("employee"))
            // enables OAuth2 Resource Server support using the default configuration based on JWT (JWT authentication).
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            // each request must include an Access Token, so there's no need to keep a user session alive between requests. We want it to be stateless.
            .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // since the authentication strategy is stateless and doesn't involve a browser-based client, we can safely disable the CSRF protection.
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }

    /**
     * With this bean in place, Spring Security will associate a list of GrantedAuthority objects with each authenticated user,
     * and we can use them to define authorization policies.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // defines a converter to map claims to GrantedAuthority objects.
        var jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // applies the “ROLE_” prefix to each user role.
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        // extracts the list of roles from the roles claim.
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        // defines a strategy to convert a JWT. We’ll only customize how to build granted authorities out of it.
        var jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
