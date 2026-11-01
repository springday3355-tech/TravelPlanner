package com.example.travelplanner.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "MYSQL_INTEGRATION_TEST", matches = "true")
@SpringBootTest
@ActiveProfiles("prod")
class MySqlMigrationIntegrationTest {

    private static final Set<String> DOMAIN_TABLES = Set.of(
            "member",
            "place",
            "place_opening_hour",
            "itinerary",
            "itinerary_day",
            "itinerary_item",
            "itinerary_participant"
    );

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureMySql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> requiredEnvironmentVariable("MYSQL_TEST_URL"));
        registry.add("spring.datasource.username", () -> requiredEnvironmentVariable("MYSQL_TEST_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironmentVariable("MYSQL_TEST_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("jwt.secret", () -> "mysql-integration-test-secret-at-least-32-bytes");
    }

    @Test
    void startsTheApplicationAfterMigratingAnEmptyMySqlDatabase() throws Exception {
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.info().pending()).isEmpty();

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT table_name
                     FROM information_schema.tables
                     WHERE table_schema = DATABASE()
                     """);
             var resultSet = statement.executeQuery()) {
            var tables = new java.util.HashSet<String>();
            while (resultSet.next()) {
                tables.add(resultSet.getString("table_name"));
            }
            assertThat(tables).containsAll(DOMAIN_TABLES);
        }
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured for the MySQL integration test");
        }
        return value;
    }
}
