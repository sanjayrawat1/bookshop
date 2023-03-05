package com.github.sanjayrawat1.bookshop.catalog.domain;

/**
 * Exception thrown when a book cannot be found.
 *
 * @author Sanjay Singh Rawat
 */
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(String isbn) {
        super("The book with ISBN " + isbn + " was not found.");
    }
}
