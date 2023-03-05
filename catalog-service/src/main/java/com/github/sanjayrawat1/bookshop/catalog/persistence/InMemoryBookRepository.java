package com.github.sanjayrawat1.bookshop.catalog.persistence;

import com.github.sanjayrawat1.bookshop.catalog.domain.Book;
import com.github.sanjayrawat1.bookshop.catalog.domain.BookRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory implementation of the {@link BookRepository} interface.
 *
 * @author Sanjay Singh Rawat
 */
@Repository
public class InMemoryBookRepository implements BookRepository {

    private static final Map<String, Book> books = new ConcurrentHashMap<>();

    @Override
    public Iterable<Book> findAll() {
        return books.values();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return existsByIsbn(isbn) ? Optional.of(books.get(isbn)) : Optional.empty();
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        return books.get(isbn) != null;
    }

    @Override
    public Book save(Book book) {
        return books.put(book.isbn(), book);
    }

    @Override
    public void deleteByIsbn(String isbn) {
        books.remove(isbn);
    }
}
