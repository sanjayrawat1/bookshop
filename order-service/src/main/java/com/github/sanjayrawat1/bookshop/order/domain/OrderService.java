package com.github.sanjayrawat1.bookshop.order.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Order Service.
 *
 * @author Sanjay Singh Rawat
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Mono<Order> submitOrder(String isbn, int quantity) {
        // 1. Mono.just() - Creates a Mono out of an Order object.
        // 2. flatMap(repo::save) - Saves the order object produced asynchronously by the previous step of the reactive stream into the database.
        return Mono.just(buildRejectedOrder(isbn, quantity)).flatMap(orderRepository::save);
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }
}
