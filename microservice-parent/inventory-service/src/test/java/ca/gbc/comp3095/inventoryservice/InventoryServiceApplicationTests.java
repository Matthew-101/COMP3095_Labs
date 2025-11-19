package ca.gbc.comp3095.inventoryservice;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

// Tells Spring Boot to start the application with a random port for testing
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InventoryServiceApplicationTests {

    // Spins up a PostgreSQL container using Testcontainers library
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    // Injects the random port Spring Boot assigned for this test
    @LocalServerPort
    private Integer port;

    // Spring-injected utility to interact directly with the database
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        // Set the base URI and port for RestAssured so we can hit the running test server
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        /**
         * Spring Boot is smart enough to find and apply our migrations (main/resources/db) when the test starts —
         * as long as you include Flyway as a dependency and Flyway is enabled in your configuration
         * (which it is by default).
         */

        // Clear the table before each test to ensure a clean state
        jdbcTemplate.execute("DELETE FROM t_inventory;");

        // Insert test data into the inventory table
        jdbcTemplate.execute("INSERT INTO t_inventory (sku_code, quantity) VALUES ('SKU001', 200);");
        jdbcTemplate.execute("INSERT INTO t_inventory (sku_code, quantity) VALUES ('SKU002', 50);");
    }

    // Start the PostgreSQL container once for the test class
    static {
        postgreSQLContainer.start();
    }

    @Test
    void shouldReturnTrueWhenItemIsInStock() {
        // Sends a GET request to the /api/inventory endpoint with a valid SKU and quantity
        given()
                .queryParam("skuCode", "SKU001") // SKU that exists in DB
                .queryParam("quantity", 100)     // Quantity that is in stock
                .when()
                .get("/api/inventory")           // Endpoint to test
                .then()
                .log().all()                     // Logs the full response for debugging
                .statusCode(200)                 // Asserts that HTTP status is 200 OK
                .body(is("true"));               // Asserts the response body is the string "true"
    }

    @Test
    void shouldReturnFalseWhenItemDoesNotExist() {
        // Sends a GET request with an invalid SKU
        given()
                .queryParam("skuCode", "NON_EXISTENT_SKU") // SKU not in DB
                .queryParam("quantity", 100)               // Arbitrary quantity
                .when()
                .get("/api/inventory")
                .then()
                .log().all()
                .statusCode(200)
                .body(is("false"));                        // Should return "false" as item doesn't exist
    }
}
