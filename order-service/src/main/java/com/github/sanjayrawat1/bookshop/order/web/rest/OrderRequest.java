package com.github.sanjayrawat1.bookshop.order.web.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author Sanjay Singh Rawat
 */
public record OrderRequest(
    // spotless:off

    @NotBlank(message = "The book ISBN must be defined.")
    String isbn,

    @NotNull(message = "The book quantity must be defined.")
    @Min(value = 1, message = "You must order at least {min} item.")
    @Max(value = 5, message = "You cannot oder more than {max} items.")
    Integer quantity
    // spotless:on
) {}
