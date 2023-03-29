package com.github.sanjayrawat1.bookshop.catalog.web.rest;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sanjayrawat1.bookshop.catalog.config.SecurityConfiguration;
import com.github.sanjayrawat1.bookshop.catalog.domain.Book;
import com.github.sanjayrawat1.bookshop.catalog.domain.BookNotFoundException;
import com.github.sanjayrawat1.bookshop.catalog.domain.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
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
@Import(SecurityConfiguration.class)
public class BookControllerMvcTests {

    private static final String ROLE_EMPLOYEE = "ROLE_employee";
    private static final String ROLE_CUSTOMER = "ROLE_customer";

    /**
     * Utility class to test the web layer in a mock environment.
     * <p>
     * MockMvc is a utility class that lets you test web endpoints without loading a server like Tomcat.
     */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Adds mock of BookService to the Spring application context.
     * <p>
     * Mocks created with the @MockBean annotation are different from standard mocks (for example, those created with Mockito)
     * since the class is not only mocked, but the mock is also included in the application context.
     * Whenever the context is asked to autowire that bean, it automatically injects the mock rather than the actual implementation.
     */
    @MockBean
    private BookService bookService;

    /**
     * Mocks the JwtDecoder so that the application doesn't try to call Keycloak and get the public keys for decoding the Access Token.
     */
    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void whenGetBookExistingAndAuthenticatedThenShouldReturn200() throws Exception {
        var isbn = "1234567890";
        var expectedBook = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        given(bookService.viewBookDetails(isbn)).willReturn(expectedBook);
        mockMvc.perform(get("/books/{isbn}", isbn).with(jwt())).andExpect(status().isOk());
    }

    @Test
    void whenGetBookExistingAndNotAuthenticatedThenShouldReturn200() throws Exception {
        var isbn = "1234567890";
        var expectedBook = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        given(bookService.viewBookDetails(isbn)).willReturn(expectedBook);
        mockMvc.perform(get("/books/{isbn}", isbn)).andExpect(status().isOk());
    }

    @Test
    void whenGetBookNotExistingAndAuthenticatedThenShouldReturn404() throws Exception {
        var isbn = "1234567890";
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
        mockMvc.perform(get("/books/{isbn}", isbn).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void whenGetBookNotExistingAndNotAuthenticatedThenShouldReturn404() throws Exception {
        var isbn = "1234567890";
        given(bookService.viewBookDetails(isbn)).willThrow(BookNotFoundException.class);
        mockMvc.perform(get("/books/{isbn}", isbn)).andExpect(status().isNotFound());
    }

    @Test
    void whenDeleteBookWithEmployeeRoleThenShouldReturn204() throws Exception {
        var isbn = "1234567890";
        mockMvc.perform(delete("/books/{isbn}", isbn).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_EMPLOYEE)))).andExpect(status().isNoContent());
    }

    @Test
    void whenDeleteBookWithCustomerRoleThenShouldReturn403() throws Exception {
        var isbn = "1234567890";
        mockMvc.perform(delete("/books/{isbn}", isbn).with(jwt().authorities(new SimpleGrantedAuthority(ROLE_CUSTOMER)))).andExpect(status().isForbidden());
    }

    @Test
    void whenDeleteBookNotAuthenticatedThenShouldReturn401() throws Exception {
        var isbn = "1234567890";
        mockMvc.perform(delete("/books/{isbn}", isbn)).andExpect(status().isUnauthorized());
    }

    @Test
    void whenPostBookWithEmployeeRoleThenShouldReturn201() throws Exception {
        var isbn = "1234567890";
        var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
        mockMvc
            .perform(
                post("/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookToCreate))
                    .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_EMPLOYEE)))
            )
            .andExpect(status().isCreated());
    }

    @Test
    void whenPostBookWithCustomerRoleThenShouldReturn403() throws Exception {
        var isbn = "1234567890";
        var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
        mockMvc
            .perform(
                post("/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookToCreate))
                    .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_CUSTOMER)))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void whenPostBookAndNotAuthenticatedThenShouldReturn403() throws Exception {
        var isbn = "1234567890";
        var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        mockMvc
            .perform(post("/books").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bookToCreate)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void whenPutBookWithEmployeeRoleThenShouldReturn200() throws Exception {
        var isbn = "1234567890";
        var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
        mockMvc
            .perform(
                put("/books/{isbn}", isbn)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookToCreate))
                    .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_EMPLOYEE)))
            )
            .andExpect(status().isOk());
    }

    @Test
    void whenPutBookWithCustomerRoleThenShouldReturn403() throws Exception {
        var isbn = "1234567890";
        var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        given(bookService.addBookToCatalog(bookToCreate)).willReturn(bookToCreate);
        mockMvc
            .perform(
                put("/books/{isbn}", isbn)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(bookToCreate))
                    .with(jwt().authorities(new SimpleGrantedAuthority(ROLE_CUSTOMER)))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void whenPutBookAndNotAuthenticatedThenShouldReturn401() throws Exception {
        var isbn = "1234567890";
        var bookToCreate = Book.of(isbn, "Title", "Author", 9.90, "Publisher");
        mockMvc
            .perform(put("/books/{isbn}", isbn).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(bookToCreate)))
            .andExpect(status().isUnauthorized());
    }
}
