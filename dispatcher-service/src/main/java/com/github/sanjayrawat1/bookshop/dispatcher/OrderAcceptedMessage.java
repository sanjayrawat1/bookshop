package com.github.sanjayrawat1.bookshop.dispatcher;

/**
 * DTO representing the event about orders being accepted.
 *
 * @author Sanjay Singh Rawat
 */
public record OrderAcceptedMessage(Long orderId) {}
