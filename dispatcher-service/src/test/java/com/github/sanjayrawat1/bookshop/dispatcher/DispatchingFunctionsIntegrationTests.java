package com.github.sanjayrawat1.bookshop.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.test.FunctionalSpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Integration tests for a function composition.
 *
 * @author Sanjay Singh Rawat
 */
@FunctionalSpringBootTest
@Disabled("These tests are only necessary when using the functions alone (no bindings)")
public class DispatchingFunctionsIntegrationTests {

    /**
     * All the functions managed by the framework are available through the FunctionCatalog, an object that acts as a function registry.
     * When the framework serves the function, it doesn't only contain the implementation; it's enhanced with extra features offered by Spring Cloud
     * Function, like transparent type conversion and function composition.
     */
    @Autowired
    private FunctionCatalog catalog;

    @Test
    void packOrder() {
        Function<OrderAcceptedMessage, Long> pack = catalog.lookup(Function.class, "pack");
        long orderId = 121;
        assertThat(pack.apply(new OrderAcceptedMessage(orderId))).isEqualTo(orderId);
    }

    @Test
    void labelOrder() {
        Function<Flux<Long>, Flux<OrderDispatchedMessage>> label = catalog.lookup(Function.class, "label");
        Flux<Long> orderId = Flux.just(121L);
        StepVerifier
            .create(label.apply(orderId))
            .expectNextMatches(dispatchedOrder -> dispatchedOrder.equals(new OrderDispatchedMessage(121L)))
            .verifyComplete();
    }

    @Test
    void packAndLabelOrder() {
        // gets the composed function from the FunctionCatalog
        Function<OrderAcceptedMessage, Flux<OrderDispatchedMessage>> packAndLabel = catalog.lookup(Function.class, "pack|label");
        long orderId = 121;
        StepVerifier
            .create(packAndLabel.apply(new OrderAcceptedMessage(orderId)))
            .expectNextMatches(dispatchedOrder -> dispatchedOrder.equals(new OrderDispatchedMessage(orderId)))
            .verifyComplete();
    }
}
