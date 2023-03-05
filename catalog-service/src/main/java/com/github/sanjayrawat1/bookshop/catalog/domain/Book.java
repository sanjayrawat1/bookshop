package com.github.sanjayrawat1.bookshop.catalog.domain;

/**
 * A Book.
 *
 * @author Sanjay Singh Rawat
 */
public record Book(String isbn, String title, String author, Double price) {}
