package ca.gbc.comp3095.orderservice;

import ca.gbc.comp3095.orderservice.stubs.InventoryClientStub;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the Order Service.
 * Uses TestContainers to start an ephemeral PostgreSQL container.
 * Uses RestAssured to test HTTP POST against the /api/order endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class OrderServiceApplicationTests {

    // Use TestContainers to spin up a real PostgreSQL instance for testing
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    // Injects the random port the test server is running on
    @LocalServerPort
    private Integer port;

    //Autowire the WireMockServer to get access to the WireMock runtime information
    @Autowired
    private WireMockServer wireMockServer;

    // Set up RestAssured with the server URL and random port
    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    static {
        postgreSQLContainer.start(); // Start the container before tests run
    }

    @Test
    void placeOrderTest() {
        // Sample request body to simulate placing an order
        String orderJson = """
                {
                  "skuCode": "samsung_tv_2025",
                  "price": 5000,
                  "quantity": 10
                }
                """;

        //Call the InventoryClientStub
        InventoryClientStub.stubInventoryCall("samsung_tv_2025", 10);

        /**
         * The following block:
         * - Sends a POST request to /api/order
         * - Sets content type as JSON
         * - Sends the orderJson in the request body
         * - Expects HTTP 201 Created as the response
         * - Extracts the response body as a string
         * - Asserts that the body equals "Order Placed Successfully"
         */
        var responseBodyString = RestAssured
                .given()                          // Start building the request
                .contentType("application/json")  // Set content type to JSON
                .body(orderJson)                  // Set the request payload
                .when()
                .post("/api/order")               // Specify endpoint and send POST
                .then()
                .log().all()                      // Log full request and response for debugging
                .statusCode(201)                  // Assert that response status is 201 (Created)
                .extract()
                .body().asString();               // Extract the body as a String

        // Validate that the response is what we expect
        assertThat(responseBodyString, Matchers.is("Successfully Placed Order"));
    }
}
