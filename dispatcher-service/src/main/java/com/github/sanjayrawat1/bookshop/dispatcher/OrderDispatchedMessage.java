package com.github.sanjayrawat1.bookshop.dispatcher;

/**
 * DTO representing the event about orders being dispatched.
 *
 * @author Sanjay Singh Rawat
 */
public record OrderDispatchedMessage(Long orderId) {}
