package com.github.sanjayrawat1.bookshop.quoteservice.web.rest;

import com.github.sanjayrawat1.bookshop.quoteservice.domain.Genre;
import com.github.sanjayrawat1.bookshop.quoteservice.domain.Quote;
import com.github.sanjayrawat1.bookshop.quoteservice.domain.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Quote Controller.
 *
 * @author Sanjay Singh Rawat
 */
@RestController
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    @GetMapping("/quotes")
    public Flux<Quote> getAllQuotes() {
        return quoteService.getAllQuotes();
    }

    @GetMapping("/quotes/random")
    public Mono<Quote> getRandomQuote() {
        return quoteService.getRandomQuote();
    }

    @GetMapping("/quotes/random/{genre}")
    public Mono<Quote> getRandomQuote(@PathVariable Genre genre) {
        return quoteService.getRandomQuoteByGenre(genre);
    }
}
