package com.github.sanjayrawat1.bookshop.catalog.domain;

/**
 * Exception thrown when adding a book that already exists.
 *
 * @author Sanjay Singh Rawat
 */
public class BookAlreadyExistsException extends RuntimeException {

    public BookAlreadyExistsException(String isbn) {
        super("A book with ISBN " + isbn + " already exists.");
    }
}
