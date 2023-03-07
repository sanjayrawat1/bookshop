package com.github.sanjayrawat1.bookshop.catalog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bookshop catalog service properties.
 * <p>
 * The @ConfigurationProperties beans are configured to listen to RefreshScopeRefreshedEvent events.
 * RefreshScopeRefreshedEvent events can be triggered after a new change is pushed to the configuration repository,
 * so that the client application reloads the context using the latest configuration data.
 * Spring Boot Actuator defines an /management/refresh endpoint that you can use to trigger the event manually.
 *
 * @author Sanjay Singh Rawat
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bookshop")
public class BookshopProperties {

    /**
     * A message to welcome users.
     */
    private String greeting;
}
