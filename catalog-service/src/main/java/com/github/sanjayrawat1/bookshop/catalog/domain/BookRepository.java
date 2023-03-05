package com.github.sanjayrawat1.bookshop.catalog.domain;

import java.util.Optional;

/**
 * The abstraction used by the domain layer to access {@link Book}.
 *
 * @author Sanjay Singh Rawat
 */
public interface BookRepository {
    Iterable<Book> findAll();

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Book save(Book book);

    void deleteByIsbn(String isbn);
}
