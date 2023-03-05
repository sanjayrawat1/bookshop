package com.github.sanjayrawat1.bookshop.catalog.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementing the use cases for the {@link Book} domain.
 *
 * @author Sanjay Singh Rawat
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public Iterable<Book> viewBookList() {
        return bookRepository.findAll();
    }

    public Book viewBookDetails(String isbn) {
        return bookRepository.findByIsbn(isbn).orElseThrow(() -> new BookNotFoundException(isbn));
    }

    public Book addBookToCatalog(Book book) {
        if (bookRepository.existsByIsbn(book.isbn())) {
            throw new BookAlreadyExistsException(book.isbn());
        }
        return bookRepository.save(book);
    }

    public void removeBookFromCatalog(String isbn) {
        bookRepository.deleteByIsbn(isbn);
    }

    public Book editBookDetails(String isbn, Book book) {
        return bookRepository
            .findByIsbn(isbn)
            .map(existingBook -> {
                var bookToUpdate = new Book(existingBook.isbn(), book.title(), book.author(), book.price());
                return bookRepository.save(bookToUpdate);
            })
            .orElseGet(() -> addBookToCatalog(book));
    }
}
