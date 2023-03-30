package com.github.sanjayrawat1.bookshop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sanjayrawat1.bookshop.order.book.Book;
import com.github.sanjayrawat1.bookshop.order.book.BookClient;
import com.github.sanjayrawat1.bookshop.order.domain.Order;
import com.github.sanjayrawat1.bookshop.order.domain.OrderStatus;
import com.github.sanjayrawat1.bookshop.order.event.OrderAcceptedMessage;
import com.github.sanjayrawat1.bookshop.order.web.rest.OrderRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceApplicationTests {

    // Customer and employee
    private static KeycloakToken sanjayToken;
    // Customer
    private static KeycloakToken anupToken;

    /**
     * Defines a Keycloak container for testing.
     */
    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:21.0.1")
        .withRealmImportFile("test-realm-config.json");

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.2"));

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutputDestination output;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BookClient bookClient;

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloakContainer.getAuthServerUrl() + "realms/Bookshop");
    }

    private static String r2dbcUrl() {
        return String.format(
            "r2dbc:postgresql://%s:%s/%s",
            postgresql.getHost(),
            postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgresql.getDatabaseName()
        );
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
    void whenGetOwnOrdersThenReturn() throws IOException {
        String bookIsbn = "1234567893";
        Book book = new Book(bookIsbn, "Title", "Author", 9.90);
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
        OrderRequest orderRequest = new OrderRequest(bookIsbn, 1);

        Order expectedOrder = webTestClient
            .post()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();
        assertThat(expectedOrder).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class)).isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient
            .get()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Order.class)
            .value(orders -> assertThat(orders.stream().filter(order -> order.bookIsbn().equals(bookIsbn)).findAny()).isNotEmpty());
    }

    @Test
    void whenGetOrdersForAnotherUserThenNotReturned() throws IOException {
        String bookIsbn = "1234567899";
        Book book = new Book(bookIsbn, "Title", "Author", 9.90);
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
        OrderRequest orderRequest = new OrderRequest(bookIsbn, 1);

        Order orderByAnup = webTestClient
            .post()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();
        assertThat(orderByAnup).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class)).isEqualTo(new OrderAcceptedMessage(orderByAnup.id()));

        Order orderBySanjay = webTestClient
            .post()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(sanjayToken.accessToken()))
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();
        assertThat(orderBySanjay).isNotNull();
        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class)).isEqualTo(new OrderAcceptedMessage(orderBySanjay.id()));

        webTestClient
            .get()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBodyList(Order.class)
            .value(orders -> {
                List<Long> orderIds = orders.stream().map(Order::id).collect(Collectors.toList());
                assertThat(orderIds).contains(orderByAnup.id());
                assertThat(orderIds).doesNotContain(orderBySanjay.id());
            });
    }

    @Test
    void whenPostRequestAndBookExistsThenOrderAccepted() throws IOException {
        String bookIsbn = "1234567899";
        Book book = new Book(bookIsbn, "Title", "Author", 9.90);
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.just(book));
        OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

        Order createdOrder = webTestClient
            .post()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
        assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);

        assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class)).isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
    }

    @Test
    void whenPostRequestAndBookNotExistsThenOrderRejected() {
        String bookIsbn = "1234567894";
        given(bookClient.getBookByIsbn(bookIsbn)).willReturn(Mono.empty());
        OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

        Order createdOrder = webTestClient
            .post()
            .uri("/orders")
            .headers(headers -> headers.setBearerAuth(anupToken.accessToken()))
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
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
