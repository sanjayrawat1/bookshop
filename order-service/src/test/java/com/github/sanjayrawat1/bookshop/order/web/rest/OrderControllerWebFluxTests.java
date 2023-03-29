package com.github.sanjayrawat1.bookshop.order.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.github.sanjayrawat1.bookshop.order.config.SecurityConfiguration;
import com.github.sanjayrawat1.bookshop.order.domain.Order;
import com.github.sanjayrawat1.bookshop.order.domain.OrderService;
import com.github.sanjayrawat1.bookshop.order.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * @author Sanjay Singh Rawat
 */
@WebFluxTest(OrderController.class)
@Import(SecurityConfiguration.class)
public class OrderControllerWebFluxTests {

    private static final String ROLE_CUSTOMER = "ROLE_customer";

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private OrderService orderService;

    /**
     * Mocks the ReactiveJwtDecoder so that the application doesn't try to call Keycloak and get the public key for decoding the Access Token.
     */
    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void whenBookNotAvailableThenRejectOrder() {
        var orderRequest = new OrderRequest("1234567890", 3);
        var expectedOrder = OrderService.buildRejectedOrder(orderRequest.isbn(), orderRequest.quantity());

        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity())).willReturn(Mono.just(expectedOrder));

        testClient
            // mutates the HTTP request with a mock, JWT-formatted Access Token for a user with the "customer" role.
            .mutateWith(SecurityMockServerConfigurers.mockJwt().authorities(new SimpleGrantedAuthority(ROLE_CUSTOMER)))
            .post()
            .uri("/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .value(actualOrder -> {
                assertThat(actualOrder).isNotNull();
                assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
            });
    }
}
