package com.github.sanjayrawat1.bookshop.catalog.web.errors;

import com.github.sanjayrawat1.bookshop.catalog.domain.BookAlreadyExistsException;
import com.github.sanjayrawat1.bookshop.catalog.domain.BookNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (<a href="https://tools.ietf.org/html/rfc7807">RFC7807</a>).
 *
 * @author Sanjay Singh Rawat
 */
@ControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail bookNotFoundHandler(BookNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BookAlreadyExistsException.class)
    public ProblemDetail bookAlreadyExistsHandler(BookAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
