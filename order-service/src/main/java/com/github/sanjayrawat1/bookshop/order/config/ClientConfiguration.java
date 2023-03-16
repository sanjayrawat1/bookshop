package com.github.sanjayrawat1.bookshop.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Spring WebClient configuration.
 *
 * @author Sanjay Singh Rawat
 */
@Configuration
public class ClientConfiguration {

    @Bean
    WebClient catalogClient(WebClient.Builder builder, ClientProperties clientProperties) {
        return builder.baseUrl(clientProperties.catalogServiceUri().toString()).build();
    }
}
