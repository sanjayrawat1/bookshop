package com.github.sanjayrawat1.bookshop.order.book;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

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
        return catalogClient
            .get()
            .uri(BOOKS_ROOT_API + isbn)
            .retrieve()
            .bodyToMono(Book.class)
            .timeout(Duration.ofSeconds(3), Mono.empty())
            // return an empty object when a 404 response is received.
            .onErrorResume(WebClientResponseException.NotFound.class, exception -> Mono.empty())
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
            // if an error happens after the 3 retries, catch the exception and return an empty object.
            .onErrorResume(Exception.class, exception -> Mono.empty());
    }
}
