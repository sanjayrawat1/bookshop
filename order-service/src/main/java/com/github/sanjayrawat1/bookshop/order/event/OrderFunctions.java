package com.github.sanjayrawat1.bookshop.order.event;

import com.github.sanjayrawat1.bookshop.order.domain.OrderService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

/**
 * @author Sanjay Singh Rawat
 */
@Slf4j
@Configuration
public class OrderFunctions {

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return orderDispatchedMessage ->
            // For each dispatched message, it updates the related order in the database.
            orderService
                .consumeOrderDispatchedMessageEvent(orderDispatchedMessage)
                // For each order updated in the database, it logs a message.
                .doOnNext(order -> log.info("The order with id {} is dispatched", order.id()))
                // Subscribes to the reactive stream in order to activate it. Without a subscriber, no data flows through the stream.
                .subscribe();
    }
}
