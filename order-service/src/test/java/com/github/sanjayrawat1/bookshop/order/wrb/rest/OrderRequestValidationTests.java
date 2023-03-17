package com.github.sanjayrawat1.bookshop.order.wrb.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.order.web.rest.OrderRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Sanjay Singh Rawat
 */
public class OrderRequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenAllFieldsCorrectThenValidationSucceeds() {
        var orderRequest = new OrderRequest("1234567890", 1);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
        assertThat(violations).isEmpty();
    }

    @Test
    void whenIsbnNotDefinedThenValidationFails() {
        var orderRequest = new OrderRequest("", 1);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book ISBN must be defined.");
    }

    @Test
    void whenQuantityIsNotDefinedThenValidationFails() {
        var orderRequest = new OrderRequest("1234567890", null);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("The book quantity must be defined.");
    }

    @Test
    void whenQuantityIsLowerThanMinThenValidationFails() {
        var orderRequest = new OrderRequest("1234567890", 0);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("You must order at least {min} item.");
    }

    @Test
    void whenQuantityIsGreaterThanMaxThenValidationFails() {
        var orderRequest = new OrderRequest("1234567890", 7);
        Set<ConstraintViolation<OrderRequest>> violations = validator.validate(orderRequest);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("You cannot order more than {max} items.");
    }
}
