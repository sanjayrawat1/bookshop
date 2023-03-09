package com.github.sanjayrawat1.bookshop.catalog.demo;

import com.github.sanjayrawat1.bookshop.catalog.config.Constants;
import com.github.sanjayrawat1.bookshop.catalog.domain.Book;
import com.github.sanjayrawat1.bookshop.catalog.domain.BookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Instead of using profiles as feature flags, a more scalable and structured approach is defining custom properties to configure functionality,
 * and relying on annotations such as @ConditionalOnProperty and @ConditionalOnCloudPlatform to control when certain beans should be loaded into
 * the Spring application context. That’s one of the foundations of Spring Boot auto-configuration.
 * <p>
 * For example, you could define a polar.testdata.enabled custom property and
 * use the @ConditionalOnProperty(name = "polar.testdata .enabled", havingValue = "true") annotation on the BookDataLoader class.
 *
 * @author Sanjay Singh Rawat
 */
@Component
@RequiredArgsConstructor
@Profile(Constants.SPRING_PROFILE_TEST_DATA)
public class BookDataLoader {

    private final BookRepository bookRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void loadBookTestData() {
        bookRepository.deleteAll();
        var book1 = Book.of("1234567891", "Northern Lights", "Lyra Silverstar", 9.90, "Publisher 1");
        var book2 = Book.of("1234567892", "Polar Journey", "Iorek Polarson", 12.90, "Publisher 2");
        bookRepository.saveAll(List.of(book1, book2));
    }
}
