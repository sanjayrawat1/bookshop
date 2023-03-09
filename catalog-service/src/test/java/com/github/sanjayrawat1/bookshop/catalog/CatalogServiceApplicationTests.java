package com.github.sanjayrawat1.bookshop.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.catalog.config.Constants;
import com.github.sanjayrawat1.bookshop.catalog.domain.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.SPRING_PROFILE_INTEGRATION_TEST)
class CatalogServiceApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void contextLoads() {}

    @Test
    void whenPostRequestThenBookCreated() {
        var expectedBook = Book.of("1231231231", "Title", "Author", 9.90);

        webTestClient
            .post()
            .uri("/books")
            .bodyValue(expectedBook)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .value(actualBook -> {
                assertThat(actualBook).isNotNull();
                assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
            });
    }

    @Test
    void whenGetRequestWithIdThenBookReturned() {
        var bookIsbn = "1231231230";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.90);
        Book expectedBook = webTestClient
            .post()
            .uri("/books")
            .bodyValue(bookToCreate)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();

        webTestClient
            .get()
            .uri("/books/{isbn}", bookIsbn)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Book.class)
            .value(actualBook -> {
                assertThat(actualBook).isNotNull();
                assertThat(actualBook.isbn()).isEqualTo(expectedBook.isbn());
            });
    }

    @Test
    void whenPutRequestThenBookUpdated() {
        var bookIsbn = "1231231232";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.90);
        Book createdBook = webTestClient
            .post()
            .uri("/books")
            .bodyValue(bookToCreate)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .value(book -> assertThat(book).isNotNull())
            .returnResult()
            .getResponseBody();

        var bookToUpdate = new Book(
            createdBook.id(),
            createdBook.isbn(),
            createdBook.title(),
            createdBook.author(),
            7.95,
            createdBook.createdDate(),
            createdBook.lastModifiedDate(),
            createdBook.version()
        );

        webTestClient
            .put()
            .uri("/books/{isbn}", bookIsbn)
            .bodyValue(bookToUpdate)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Book.class)
            .value(actualBook -> {
                assertThat(actualBook).isNotNull();
                assertThat(actualBook.price()).isEqualTo(bookToUpdate.price());
            });
    }

    @Test
    void whenDeleteRequestThenBookDeleted() {
        var bookIsbn = "1231231233";
        var bookToCreate = Book.of(bookIsbn, "Title", "Author", 9.90);

        webTestClient.post().uri("/books").bodyValue(bookToCreate).exchange().expectStatus().isCreated();

        webTestClient.delete().uri("/books/{isbn}", bookIsbn).exchange().expectStatus().isNoContent();

        webTestClient
            .get()
            .uri("/books/{isbn}", bookIsbn)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody(ProblemDetail.class)
            .value(problemDetail -> {
                assertThat(problemDetail).isNotNull();
                assertThat(problemDetail.getDetail()).isEqualTo("The book with ISBN " + bookIsbn + " was not found.");
            });
    }
}
