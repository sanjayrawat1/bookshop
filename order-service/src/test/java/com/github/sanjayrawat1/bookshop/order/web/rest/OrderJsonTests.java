package com.github.sanjayrawat1.bookshop.order.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.order.domain.Order;
import com.github.sanjayrawat1.bookshop.order.domain.OrderStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

/**
 * @author Sanjay Singh Rawat
 */
@JsonTest
public class OrderJsonTests {

    @Autowired
    private JacksonTester<Order> json;

    @Test
    void testSerialization() throws Exception {
        var order = new Order(987L, "1234567890", "Book Name", 9.90, 1, OrderStatus.ACCEPTED, Instant.now(), Instant.now(), "sanjay", "sanjay", 21);
        var jsonContent = json.write(order);

        assertThat(jsonContent).extractingJsonPathNumberValue("@.id").isEqualTo(order.id().intValue());
        assertThat(jsonContent).extractingJsonPathStringValue("@.bookIsbn").isEqualTo(order.bookIsbn());
        assertThat(jsonContent).extractingJsonPathStringValue("@.bookName").isEqualTo(order.bookName());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.bookPrice").isEqualTo(order.bookPrice());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity").isEqualTo(order.quantity());
        assertThat(jsonContent).extractingJsonPathStringValue("@.status").isEqualTo(order.status().toString());
        assertThat(jsonContent).extractingJsonPathStringValue("@.createdDate").isEqualTo(order.createdDate().toString());
        assertThat(jsonContent).extractingJsonPathStringValue("@.lastModifiedDate").isEqualTo(order.lastModifiedDate().toString());
        assertThat(jsonContent).extractingJsonPathStringValue("@.createdBy").isEqualTo(order.createdBy());
        assertThat(jsonContent).extractingJsonPathStringValue("@.lastModifiedBy").isEqualTo(order.lastModifiedBy());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.version").isEqualTo(order.version());
    }
}
