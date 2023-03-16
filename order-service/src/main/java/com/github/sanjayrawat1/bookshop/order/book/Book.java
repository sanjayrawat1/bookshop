package com.github.sanjayrawat1.bookshop.order.book;

/**
 * Book DTO.
 *
 * @author Sanjay Singh Rawat
 */
public record Book(String isbn, String title, String author, Double price) {}
