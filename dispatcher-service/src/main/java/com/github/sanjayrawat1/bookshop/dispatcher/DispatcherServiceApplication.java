package com.github.sanjayrawat1.bookshop.dispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DispatcherServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatcherServiceApplication.class, args);
    }
}
