package com.example.travelplanner.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "MYSQL_INTEGRATION_TEST", matches = "true")
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

    @Test
    void migratesAnEmptyMySqlDatabaseAndValidatesTheSchema() throws Exception {
        String url = requiredEnvironmentVariable("MYSQL_TEST_URL");
        String username = requiredEnvironmentVariable("MYSQL_TEST_USERNAME");
        String password = requiredEnvironmentVariable("MYSQL_TEST_PASSWORD");

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .validateMigrationNaming(true)
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

        try (var connection = DriverManager.getConnection(url, username, password);
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

    private String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured for the MySQL integration test");
        }
        return value;
    }
}
