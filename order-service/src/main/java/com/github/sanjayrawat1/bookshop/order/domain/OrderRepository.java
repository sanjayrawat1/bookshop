package com.github.sanjayrawat1.bookshop.order.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Order Repository.
 *
 * @author Sanjay Singh Rawat
 */
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Flux<Order> findAllByCreatedBy(String userId);
}
