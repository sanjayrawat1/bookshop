package com.github.sanjayrawat1.bookshop.edgeservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

/**
 * Security configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    /**
     * The SecurityWebFilterChain bean is used to define and configure security filter policies for the application.
     */
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveClientRegistrationRepository clientRegistrationRepository) {
        return http
            .authorizeExchange(exchange ->
                exchange
                    .pathMatchers("/management/**")
                    .permitAll()
                    // allows unauthenticated access to the SPA static resources
                    .pathMatchers("/", "/*.css", "/*.js", "/favicon.ico")
                    .permitAll()
                    // allows unauthenticated read access to the books in the catalog
                    .pathMatchers(HttpMethod.GET, "/books/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            )
            // when an exception is thrown because a user is not authenticated, it replies with an HTTP 401 response.
            .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
            // enables user authentication with OAuth2/OpenID connect
            .oauth2Login(Customizer.withDefaults())
            .logout(logout -> logout.logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository)))
            // a filter with the only purpose of subscribing to the CsrfToken reactive stream and ensuring its value is extracted correctly
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new XorServerCsrfTokenRequestAttributeHandler()::handle)
            )
            .build();
    }

    private ServerLogoutSuccessHandler oidcLogoutSuccessHandler(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        var oidcLogoutSuccessHandler = new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        // After logging out from the OIDC Provider, Keycloak will redirect the user to the application base URL computed dynamically
        // from Spring (locally, it's http://localhost:9000)
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
        return oidcLogoutSuccessHandler;
    }

    /**
     * Uses a cookie-based strategy for exchanging CSRF tokens with the Angular frontend.
     */
    @Bean
    WebFilter csrfWebFilter() {
        return (exchange, chain) -> {
            exchange
                .getResponse()
                .beforeCommit(() ->
                    Mono.defer(() -> {
                        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
                        return csrfToken != null ? csrfToken.then() : Mono.empty();
                    })
                );
            return chain.filter(exchange);
        };
    }

    /**
     * Defines a repository to store Access Tokens in the web session.
     */
    @Bean
    ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }
}
