package com.github.sanjayrawat1.bookshop.order.event;

/**
 * DTO representing the event about orders being dispatched.
 *
 * @author Sanjay Singh Rawat
 */
public record OrderDispatchedMessage(Long orderId) {}
