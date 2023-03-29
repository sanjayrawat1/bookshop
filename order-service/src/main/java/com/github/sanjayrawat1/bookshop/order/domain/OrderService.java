package com.github.sanjayrawat1.bookshop.order.domain;

import com.github.sanjayrawat1.bookshop.order.book.Book;
import com.github.sanjayrawat1.bookshop.order.book.BookClient;
import com.github.sanjayrawat1.bookshop.order.event.OrderAcceptedMessage;
import com.github.sanjayrawat1.bookshop.order.event.OrderDispatchedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final BookClient bookClient;

    private final OrderRepository orderRepository;

    private final StreamBridge streamBridge;

    public Flux<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient
            .getBookByIsbn(isbn)
            .map(book -> buildAcceptedOrder(book, quantity))
            .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
            .flatMap(orderRepository::save)
            .doOnNext(this::publishOrderAcceptedEvent);
    }

    public static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(book.isbn(), book.title() + " - " + book.author(), book.price(), quantity, OrderStatus.ACCEPTED);
    }

    /**
     * The code we implemented updates the status of the specific order to be DISPATCHED, an operation that can be executed several times
     * with the same result. Since the operation is idempotent, the code is resilient to duplicates. A further optimization would be to
     * check for the status and skip the update operation if it's already dispatched.
     *
     * @param orderDispatchedMessage the order dispatched event payload.
     * @return the DISPATCHED order.
     */
    public Flux<Order> consumeOrderDispatchedMessageEvent(Flux<OrderDispatchedMessage> orderDispatchedMessage) {
        // Accepts a reactive stream of OrderDispatchedMessage objects as input.
        return orderDispatchedMessage
            // For each object emitted to the stream, it reads the related order from the database.
            .flatMap(message -> orderRepository.findById(message.orderId()))
            // Updates the order with the “dispatched” status.
            .map(this::buildDispatchedOrder)
            // Saves the updated order in the database.
            .flatMap(orderRepository::save);
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(
            existingOrder.id(),
            existingOrder.bookIsbn(),
            existingOrder.bookName(),
            existingOrder.bookPrice(),
            existingOrder.quantity(),
            OrderStatus.DISPATCHED,
            existingOrder.createdDate(),
            existingOrder.lastModifiedDate(),
            existingOrder.createdBy(),
            existingOrder.lastModifiedBy(),
            existingOrder.version()
        );
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) {
            return;
        }
        var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id {}", order.id());
        // explicitly sends a message to the acceptOrder-out-0 binding
        var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Result of sending data for order with id {} : {}", order.id(), result);
    }
}
