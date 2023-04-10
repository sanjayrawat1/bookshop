package com.github.sanjayrawat1.bookshop.quoteservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.quoteservice.domain.Genre;
import com.github.sanjayrawat1.bookshop.quoteservice.domain.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteServiceApplicationTests {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void whenAllQuotesThenReturn() {
        webTestClient.get().uri("/quotes").exchange().expectStatus().is2xxSuccessful().expectBodyList(Quote.class);
    }

    @Test
    void whenRandomQuoteThenReturn() {
        webTestClient.get().uri("/quotes/random").exchange().expectStatus().is2xxSuccessful().expectBody(Quote.class);
    }

    @Test
    void whenRandomQuoteByGenreThenReturn() {
        webTestClient
            .get()
            .uri("/quotes/random/FANTASY")
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Quote.class)
            .value(quote -> assertThat(quote.genre()).isEqualTo(Genre.FANTASY));
    }
}
