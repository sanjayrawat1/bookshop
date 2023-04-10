package com.github.sanjayrawat1.bookshop.quotefunction.functions;

import com.github.sanjayrawat1.bookshop.quotefunction.domain.Genre;
import com.github.sanjayrawat1.bookshop.quotefunction.domain.Quote;
import com.github.sanjayrawat1.bookshop.quotefunction.domain.QuoteService;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Sanjay Singh Rawat
 */
@Slf4j
@Configuration
public class QuoteFunctions {

    @Bean
    Supplier<Flux<Quote>> allQuotes(QuoteService quoteService) {
        return () -> {
            log.info("Getting all quotes");
            return quoteService.getAllQuotes().delaySequence(Duration.ofSeconds(1));
        };
    }

    @Bean
    Supplier<Mono<Quote>> randomQuote(QuoteService quoteService) {
        return () -> {
            log.info("Getting random quote");
            return quoteService.getRandomQuote();
        };
    }

    @Bean
    Function<Mono<Genre>, Mono<Quote>> genreQuote(QuoteService quoteService) {
        return mono ->
            mono.flatMap(genre -> {
                log.info("Getting quote for type {}", genre);
                return quoteService.getRandomQuoteByGenre(genre);
            });
    }

    @Bean
    Consumer<Quote> logQuote() {
        return quote -> log.info("Quote: '{}' by {}", quote.content(), quote.author());
    }
}
