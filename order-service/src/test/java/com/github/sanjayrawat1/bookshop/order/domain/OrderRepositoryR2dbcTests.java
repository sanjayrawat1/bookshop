package com.github.sanjayrawat1.bookshop.order.domain;

import com.github.sanjayrawat1.bookshop.order.config.DatabaseConfiguration;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

/**
 * @author Sanjay Singh Rawat
 */
// Identifies a test class that focuses on R2DBC components.
@DataR2dbcTest
// Imports R2DBC configuration needed to enable auditing.
@Import(DatabaseConfiguration.class)
// Activates automatic startup and cleanup of test containers.
@Testcontainers
public class OrderRepositoryR2dbcTests {

    // Identifies a PostgreSQL container for testing.
    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.2"));

    @Autowired
    private OrderRepository orderRepository;

    // Overwrite R2DBC and Flyway configuration to point to the test PostgreSQL instance.
    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderRepositoryR2dbcTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    // Builds and R2DBC connection string, because Testcontainers doesn't provide one out of the box as it does for JDBC
    private static String r2dbcUrl() {
        return String.format(
            "r2dbc:postgresql://%s:%s/%s",
            postgresql.getHost(),
            postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            postgresql.getDatabaseName()
        );
    }

    @Test
    void createRejectedOrder() {
        var rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3);
        StepVerifier.create(orderRepository.save(rejectedOrder)).expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED)).verifyComplete();
    }

    @Test
    void findOrderByIdWhenNotExisting() {
        StepVerifier.create(orderRepository.findById(987L)).expectNextCount(0).verifyComplete();
    }

    @Test
    void whenCreateOrderNotAuthenticatedThenNoAuditMetadata() {
        var rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3);
        StepVerifier
            .create(orderRepository.save(rejectedOrder))
            .expectNextMatches(order -> Objects.isNull(order.createdBy()) && Objects.isNull(order.lastModifiedBy()))
            .verifyComplete();
    }

    @Test
    @WithMockUser("sanjay")
    void whenCreateOrderAuthenticatedThenAuditMetadata() {
        var rejectedOrder = OrderService.buildRejectedOrder("1234567890", 3);
        StepVerifier
            .create(orderRepository.save(rejectedOrder))
            .expectNextMatches(order -> order.createdBy().equals("sanjay") && order.lastModifiedBy().equals("sanjay"))
            .verifyComplete();
    }
}
