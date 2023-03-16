package com.github.sanjayrawat1.bookshop.order.book;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Catalog Service Client to interact with BookController.
 *
 * @author Sanjay Singh Rawat
 */
@Component
@RequiredArgsConstructor
public class BookClient {

    private static final String BOOKS_ROOT_API = "/books/";

    private final WebClient catalogClient;

    public Mono<Book> getBookByIsbn(String isbn) {
        return catalogClient.get().uri(BOOKS_ROOT_API + isbn).retrieve().bodyToMono(Book.class).timeout(Duration.ofSeconds(3), Mono.empty());
    }
}
