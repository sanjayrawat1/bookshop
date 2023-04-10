package com.github.sanjayrawat1.bookshop.quoteservice.domain;

import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Quote Service.
 *
 * @author Sanjay Singh Rawat
 */
@Service
public class QuoteService {

    private static final Random random = new Random();

    private static final List<Quote> quotes = List.of(
        new Quote("Content A", "Author A", Genre.ADVENTURE),
        new Quote("Content B", "Author B", Genre.ADVENTURE),
        new Quote("Content C", "Author C", Genre.FANTASY),
        new Quote("Content D", "Author D", Genre.FANTASY),
        new Quote("Content E", "Author E", Genre.SCIENCE_FICTION),
        new Quote("Content F", "Author F", Genre.SCIENCE_FICTION)
    );

    public Flux<Quote> getAllQuotes() {
        return Flux.fromIterable(quotes);
    }

    public Mono<Quote> getRandomQuote() {
        return Mono.just(quotes.get(random.nextInt(quotes.size() - 1)));
    }

    public Mono<Quote> getRandomQuoteByGenre(Genre genre) {
        var quotesForGenre = quotes.stream().filter(q -> q.genre().equals(genre)).toList();
        return Mono.just(quotesForGenre.get(random.nextInt(quotesForGenre.size() - 1)));
    }
}
