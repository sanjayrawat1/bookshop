package com.github.sanjayrawat1.bookshop.order.domain;

import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An Order.
 *
 * @author Sanjay Singh Rawat
 */
@Table("orders")
public record Order(
    // spotless:off
    @Id
    Long id,

    String bookIsbn,

    String bookName,

    Double bookPrice,

    Integer quantity,

    OrderStatus status,

    @CreatedDate
    Instant createdDate,

    @LastModifiedDate
    Instant lastModifiedDate,

    // @Version to provide a version number, which is essential for handling concurrent updates and using optimistic locking.
    @Version
    int version
    // spotless:on
) {
    public static Order of(String bookIsbn, String bookName, Double bookPrice, Integer quantity, OrderStatus status) {
        return new Order(null, bookIsbn, bookName, bookPrice, quantity, status, null, null, 0);
    }
}
