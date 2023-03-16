package com.github.sanjayrawat1.bookshop.order.config;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Sanjay Singh Rawat
 */
@ConfigurationProperties(prefix = "bookshop")
public record ClientProperties(@NotNull URI catalogServiceUri) {}
