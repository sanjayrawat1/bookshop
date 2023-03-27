package com.github.sanjayrawat1.bookshop.edgeservice.config;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * @author Sanjay Singh Rawat
 */
@WebFluxTest
@Import(SecurityConfiguration.class)
public class SecurityConfigurationTests {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private BookshopProperties bookshopProperties;

    /**
     * A mock bean to skip the interaction with Keycloak when retrieving information about the Client registration.
     */
    @MockBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    void whenLogoutNotAuthenticatedAndNoCsrfTokenThen403() {
        webClient.post().uri("/logout").exchange().expectStatus().isForbidden();
    }

    @Test
    void whenLogoutAuthenticatedAndNoCsrfTokenThen403() {
        webClient.mutateWith(SecurityMockServerConfigurers.mockOidcLogin()).post().uri("/logout").exchange().expectStatus().isForbidden();
    }

    @Test
    void whenLogoutAuthenticatedAndWithCsrfTokenThen302() {
        when(clientRegistrationRepository.findByRegistrationId("test")).thenReturn(Mono.just(testClientRegistration()));

        webClient
            // uses a mock ID Token to authenticate the user
            .mutateWith(SecurityMockServerConfigurers.mockOidcLogin())
            // enhances the request to provide the required CSRF token
            .mutateWith(SecurityMockServerConfigurers.csrf())
            .post()
            .uri("/logout")
            .exchange()
            .expectStatus()
            .isFound();
    }

    /**
     * A mock ClientRegistration used by Spring Security to get the URLs to contact Keycloak.
     */
    private ClientRegistration testClientRegistration() {
        return ClientRegistration
            .withRegistrationId("test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("test")
            .authorizationUri("https://sso.bookshop.com/auth")
            .tokenUri("https://sso.bookshop.com/token")
            .redirectUri("https://bookshop.com")
            .build();
    }
}
