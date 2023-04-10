package com.github.sanjayrawat1.bookshop.quoteservice.domain;

/**
 * A Quote.
 *
 * @author Sanjay Singh Rawat
 */
public record Quote(String content, String author, Genre genre) {}
