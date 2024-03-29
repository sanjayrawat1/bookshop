package com.github.sanjayrawat1.bookshop.dispatcher.web.rest;

import com.github.sanjayrawat1.bookshop.dispatcher.config.BookshopProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Sanjay Singh Rawat
 */
@RestController
@RequiredArgsConstructor
public class HomeController {

    private final BookshopProperties bookshopProperties;

    @GetMapping("/")
    public String getGreeting() {
        return bookshopProperties.getGreeting();
    }
}
