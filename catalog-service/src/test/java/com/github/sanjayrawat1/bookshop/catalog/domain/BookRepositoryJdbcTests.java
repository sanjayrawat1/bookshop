package com.github.sanjayrawat1.bookshop.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.catalog.config.Constants;
import com.github.sanjayrawat1.bookshop.catalog.config.DatabaseConfiguration;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Sanjay Singh Rawat
 */
// Identifies a test class that focuses on Spring Data JDBC components.
// It makes each test method run in a transaction and rolls it back at the end, keeping the database clean.
@DataJdbcTest
// Imports the data configuration (needed to enable auditing).
@Import(DatabaseConfiguration.class)
// Disables the default behaviour of relying on an embedded test database since we want to use Testcontainers.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Enables the "integration" profile to load configuration from application-integration.yml
@ActiveProfiles(Constants.SPRING_PROFILE_INTEGRATION_TEST)
public class BookRepositoryJdbcTests {

    @Autowired
    private BookRepository bookRepository;

    /**
     * A lower-level object to interact with the database.
     * We can use it to set up the context for each test case instead of using the repository (the object under testing).
     */
    @Autowired
    private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void findAllBooks() {
        var book1 = Book.of("1234567890", "Title", "Author", 9.25, "Publisher");
        var book2 = Book.of("1234567891", "Another Title", "Another Author", 9.25, "Another Publisher");
        jdbcAggregateTemplate.insert(book1);
        jdbcAggregateTemplate.insert(book2);

        Iterable<Book> actualBooks = bookRepository.findAll();

        assertThat(
            StreamSupport
                .stream(actualBooks.spliterator(), true)
                .filter(book -> book.isbn().equals(book1.isbn()) || book.isbn().equals(book2.isbn()))
                .collect(Collectors.toList())
        )
            .hasSize(2);
    }

    @Test
    void findBookByIsbnWhenExisting() {
        var bookIsbn = "1234567890";
        var book = Book.of(bookIsbn, "Title", "Author", 12.90, "Publisher");
        // JdbcAggregateTemplate is used to prepare the data targeted by the test.
        jdbcAggregateTemplate.insert(book);

        Optional<Book> actualBook = bookRepository.findByIsbn(bookIsbn);

        assertThat(actualBook).isPresent();
        assertThat(actualBook.get().isbn()).isEqualTo(book.isbn());
    }

    @Test
    void findBookByIsbnWhenNotExisting() {
        Optional<Book> actualBook = bookRepository.findByIsbn("1234567890");
        assertThat(actualBook).isEmpty();
    }

    @Test
    void existsByIsbnWhenExisting() {
        var bookIsbn = "1234567890";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 12.12, "Publisher");
        jdbcAggregateTemplate.insert(bookToCreate);

        boolean existing = bookRepository.existsByIsbn(bookIsbn);

        assertThat(existing).isTrue();
    }

    @Test
    void existsByIsbnWhenNotExisting() {
        boolean existing = bookRepository.existsByIsbn("1234567890");
        assertThat(existing).isFalse();
    }

    @Test
    void deleteByIsbn() {
        var bookIsbn = "1234567890";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.25, "Publisher");
        var persistedBook = jdbcAggregateTemplate.insert(bookToCreate);

        bookRepository.deleteByIsbn(bookIsbn);

        assertThat(jdbcAggregateTemplate.findById(persistedBook.id(), Book.class)).isNull();
    }
}
