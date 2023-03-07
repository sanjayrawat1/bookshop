package com.github.sanjayrawat1.bookshop.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// @ConfigurationPropertiesScan annotation loads configuration data beans in the spring context.
// Instead of making Spring scan the application context, searching for configuration data beans,
// you can directly specify which ones Spring should consider by using the @EnableConfigurationProperties annotation.
@ConfigurationPropertiesScan
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
