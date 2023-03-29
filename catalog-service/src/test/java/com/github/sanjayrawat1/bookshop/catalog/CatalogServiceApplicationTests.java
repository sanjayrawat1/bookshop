package com.github.sanjayrawat1.bookshop.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.sanjayrawat1.bookshop.catalog.config.Constants;
import com.github.sanjayrawat1.bookshop.catalog.domain.Book;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.SPRING_PROFILE_INTEGRATION_TEST)
// Activates automatic startup and cleanup of test containers.
@Testcontainers
class CatalogServiceApplicationTests {

    // Customer and employee
    private static KeycloakToken sanjayToken;
    // Customer
    private static KeycloakToken anupToken;

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Defines a Keycloak container for testing.
     */
    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:21.0.1")
        .withRealmImportFile("test-realm-config.json");

    /**
     * Overwrites the Keycloak Issuer URI configuration to point to the test Keycloak instance.
     */
    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloakContainer.getAuthServerUrl() + "realms/Bookshop");
    }

    @BeforeAll
    static void generateAccessTokens() {
        WebClient webClient = WebClient
            .builder()
            .baseUrl(keycloakContainer.getAuthServerUrl() + "realms/Bookshop/protocol/openid-connect/token")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .build();

        sanjayToken = authenticateWith("sanjay", "password", webClient);
        anupToken = authenticateWith("anup", "password", webClient);
    }

    @Test
    void whenPostRequestThenBookCreated() {
        var expectedBook = Book.of("1231231231", "Title", "Author", 9.90, "Publisher");

        webTestClient
            .post()
            .uri("/books")
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .bodyValue(expectedBook)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .value(actualBook -> {
                assertThat(actualBook).isNotNull();
                assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
            });
    }

    @Test
    void whenPostRequestUnauthorizedThen403() {
        var expectedBook = Book.of("1231231231", "Title", "Author", 9.90, "Publisher");

        webTestClient
            .post()
            .uri("/books")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .bodyValue(expectedBook)
            .exchange()
            .expectStatus()
            .isForbidden();
    }

    @Test
    void whenPostRequestUnauthenticatedThen401() {
        var expectedBook = Book.of("1231231231", "Title", "Author", 9.90, "Publisher");

        webTestClient.post().uri("/books").bodyValue(expectedBook).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void whenGetRequestWithIdThenBookReturned() {
        var bookIsbn = "1231231230";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.90, "Publisher");
        Book expectedBook = webTestClient
            .post()
            .uri("/books")
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .bodyValue(bookToCreate)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();

        webTestClient
            .get()
            .uri("/books/{isbn}", bookIsbn)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Book.class)
            .value(actualBook -> {
                assertThat(actualBook).isNotNull();
                assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
            });
    }

    @Test
    void whenPutRequestThenBookUpdated() {
        var bookIsbn = "1231231232";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.90, "Publisher");
        Book createdBook = webTestClient
            .post()
            .uri("/books")
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .bodyValue(bookToCreate)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();

        var bookToUpdate = new Book(
            createdBook.id(),
            createdBook.isbn(),
            createdBook.title(),
            createdBook.author(),
            7.95,
            createdBook.publisher(),
            createdBook.createdDate(),
            createdBook.lastModifiedDate(),
            createdBook.version()
        );

        webTestClient
            .put()
            .uri("/books/{isbn}", bookIsbn)
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .bodyValue(bookToUpdate)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Book.class)
            .value(actualBook -> {
                assertThat(actualBook).isNotNull();
                assertThat(actualBook.price()).isEqualTo(bookToUpdate.price());
            });
    }

    @Test
    void whenDeleteRequestThenBookDeleted() {
        var bookIsbn = "1231231233";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.90, "Publisher");

        webTestClient
            .post()
            .uri("/books")
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .bodyValue(bookToCreate)
            .exchange()
            .expectStatus()
            .isCreated();

        webTestClient
            .delete()
            .uri("/books/{isbn}", bookIsbn)
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .exchange()
            .expectStatus()
            .isNoContent();

        webTestClient
            .get()
            .uri("/books/{isbn}", bookIsbn)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody(ProblemDetail.class)
            .value(problemDetail -> {
                assertThat(problemDetail).isNotNull();
                assertThat(problemDetail.getDetail()).isEqualTo("The book with ISBN " + bookIsbn + " was not found.");
            });
    }

    private static KeycloakToken authenticateWith(String username, String password, WebClient webClient) {
        return webClient
            .post()
            // uses the Password Grant flow to authenticate with Keycloak directly.
            .body(BodyInserters.fromFormData("grant_type", "password").with("client_id", "bookshop-test").with("username", username).with("password", password))
            .retrieve()
            .bodyToMono(KeycloakToken.class)
            .block();
    }

    private record KeycloakToken(String accessToken) {
        // instructs Jackson to use this constructor when deserializing JSON into KeycloakToken objects.
        @JsonCreator
        private KeycloakToken(@JsonProperty("access_token") final String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
