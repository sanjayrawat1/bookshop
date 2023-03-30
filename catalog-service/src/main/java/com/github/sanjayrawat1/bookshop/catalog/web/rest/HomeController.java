package com.github.sanjayrawat1.bookshop.catalog.web.rest;

import com.github.sanjayrawat1.bookshop.catalog.config.BookshopProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home Controller.
 *
 * @author Sanjay Singh Rawat
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HomeController {

    private final BookshopProperties bookshopProperties;

    @GetMapping("/")
    public String getGreeting() {
        log.info("Fetching the greeting message from the catalog");
        return bookshopProperties.getGreeting();
    }
}
