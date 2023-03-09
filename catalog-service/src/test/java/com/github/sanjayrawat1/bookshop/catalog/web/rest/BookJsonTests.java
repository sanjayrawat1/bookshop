package com.github.sanjayrawat1.bookshop.catalog.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.sanjayrawat1.bookshop.catalog.domain.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

/**
 * Testing the JSON serialization with @JsonTest.
 * <p>
 * Using the @JsonTest annotation, you can test JSON serialization and deserialization for your domain objects. @JsonTest loads a Spring application context
 * and autoconfigures the JSON mappers for the specific library in use (by default, it's Jackson).
 * Furthermore, it configures the JacksonTester utility, which you can use to check that the JSON mapping works as expected,
 * relying on the JsonPath and JSONAssert library.
 * <p>
 * JsonPath provides expressions you can use to navigate a JSON object and extract data from it.
 * For example, if I wanted to get the isbn field from the Book object’s JSON representation, I could use the following JsonPath expression: @.isbn.
 *
 * @author Sanjay Singh Rawat
 */
@JsonTest
public class BookJsonTests {

    /**
     * Utility class to assert JSON serialization and deserialization.
     */
    @Autowired
    private JacksonTester<Book> json;

    @Test
    void testSerialization() throws Exception {
        var book = new Book(123L, "1234567890", "Title", "Author", 9.90, 2);
        // Verifying the parsing from Java to JSON, using the JsonPath format to navigate the JSON object.
        var jsonContent = json.write(book);
        assertThat(jsonContent).extractingJsonPathNumberValue("@.id").isEqualTo(book.id().intValue());
        assertThat(jsonContent).extractingJsonPathStringValue("@.isbn").isEqualTo(book.isbn());
        assertThat(jsonContent).extractingJsonPathStringValue("@.title").isEqualTo(book.title());
        assertThat(jsonContent).extractingJsonPathStringValue("@.author").isEqualTo(book.author());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.price").isEqualTo(book.price());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.version").isEqualTo(book.version());
    }

    @Test
    void testDeserialization() throws Exception {
        var content =
            """
                {
                    "id": 123,
                    "isbn": "1234567890",
                    "title": "Title",
                    "author": "Author",
                    "price": 9.90,
                    "version": 2
                }
                """;
        assertThat(json.parse(content)).usingRecursiveComparison().isEqualTo(new Book(123L, "1234567890", "Title", "Author", 9.90, 2));
    }
}
