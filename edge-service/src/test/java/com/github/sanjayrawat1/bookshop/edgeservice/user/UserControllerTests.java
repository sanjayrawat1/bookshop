package com.github.sanjayrawat1.bookshop.edgeservice.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.edgeservice.config.SecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Sanjay Singh Rawat
 */
@WebFluxTest(UserController.class)
@Import(SecurityConfiguration.class)
public class UserControllerTests {

    @Autowired
    private WebTestClient webClient;

    /**
     * A mock bean to skip the interaction with Keycloak when retrieving information about the Client registration.
     */
    @MockBean
    ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Test
    void whenNotAuthenticatedThen401() {
        webClient.get().uri("/user").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void whenAuthenticatedThenReturnUser() {
        var expectedUser = new User("sanjay", "Sanjay", "Rawat", List.of("employee", "customer"));

        webClient
            .mutateWith(configureMockOidcLogin(expectedUser))
            .get()
            .uri("/user")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(User.class)
            .value(user -> assertThat(user).isEqualTo(expectedUser));
    }

    /**
     * Defines an authentication context based on OIDC and uses the expected user and builds a mock ID Token.
     */
    private SecurityMockServerConfigurers.OidcLoginMutator configureMockOidcLogin(User expectedUser) {
        return SecurityMockServerConfigurers
            .mockOidcLogin()
            .idToken(builder -> {
                builder.claim(StandardClaimNames.PREFERRED_USERNAME, expectedUser.username());
                builder.claim(StandardClaimNames.GIVEN_NAME, expectedUser.firstName());
                builder.claim(StandardClaimNames.FAMILY_NAME, expectedUser.lastName());
            });
    }
}
