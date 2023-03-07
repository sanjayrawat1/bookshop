package com.github.sanjayrawat1.bookshop.catalog.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
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
