package com.github.sanjayrawat1.bookshop.catalog.web.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.sanjayrawat1.bookshop.catalog.domain.BookNotFoundException;
import com.github.sanjayrawat1.bookshop.catalog.domain.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice Integration tests for BookController.
 * <p>
 * Slice tests run against an application context containing only the parts of the configuration requested by that application slice.
 * In the case of collaborating beans outside the slice, such as the BookService class, we use mocks.
 *
 * @WebMvcTest(BookController.class) : Identifies a test class that focuses on Spring MVC components, explicitly targeting BookController
 *
 * @author Sanjay Singh Rawat
 */
@WebMvcTest(BookController.class)
public class BookControllerMvcTests {

    /**
     * Utility class to test the web layer in a mock environment.
     * <p>
     * MockMvc is a utility class that lets you test web endpoints without loading a server like Tomcat.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * Adds mock of BookService to the Spring application context.
     * <p>
     * Mocks created with the @MockBean annotation are different from standard mocks (for example, those created with Mockito)
     * since the class is not only mocked, but the mock is also included in the application context.
     * Whenever the context is asked to autowire that bean, it automatically injects the mock rather than the actual implementation.
     */
    @MockBean
    private BookService bookService;

    @Test
    void whenGetBookNotExistingThenShouldReturn404() throws Exception {
        String isbn = "73737313940";
        // Defines the expected behaviour for the BookService mock bean.
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
        // MockMvc is used to perform an HTTP GET request and verify the result.
        mockMvc.perform(get("/books/{isbn}", isbn)).andExpect(status().isNotFound());
    }
}
