package ca.gbc.comp3095.productservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

// @SpringBootTest: Starts a full Spring application context on a random available port for integration testing.
@SpringBootTest(webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

    // @ServiceConnection: Wires the MongoDb Testcontainer directly into Spring's auto-configuration,
    @ServiceConnection
    static MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:latest");

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    // Start the container once for the entire test class (static initializer runs when the class is loaded).
    static {
        mongoDbContainer.start();
    }

    // Build WebTestClient manually
    @BeforeEach
    void setUp() {
       webTestClient = WebTestClient.bindToServer()
               .baseUrl("http://localhost:" + port)
               .build();
    }


    private String createProductAndReturnId(String name, String description, int price){

        String requestBody = """
                {                  
                  "name": "%s",
                  "description": "%s",
                  "price":  %d               
                }                
                """.formatted(name, description, price);

        Map responseBody = webTestClient.post()
                .uri("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(responseBody).isNotNull();
        return responseBody.get("id").toString();

    }



    //BDD - Behavioural Driven Development - (Given, When, Then)
    //POST - CREATE TEST
    @Test
    void createProductTest(){

        String requestBody = """
                {                  
                  "name": "Widget",
                  "description": "Widget Description 2026",
                  "price":  2000               
                }                
                """;

        webTestClient.post()
                .uri("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .value( body -> {
                    assertThat(body.get("id")).isNotNull();
                    assertThat(body.get("name")).isEqualTo("Widget");
                    assertThat(body.get("description")).isEqualTo("Widget Description 2026");
                    assertThat(body.get("price")).isEqualTo(2000);
                });

    }


    //GET - READ TEST
    @Test
    void getProductsTest(){

        String requestBody = """
                {                  
                  "name": "Widget",
                  "description": "Widget Description 2026",
                  "price":  2000               
                }                
                """;

        webTestClient.post()
                .uri("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated();


        webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .value( list -> {
                    assertThat(list).isNotEmpty();
                    Map<String, Object> first = list.get(0);
                    assertThat(first.get("id")).isNotNull();
                    assertThat(first.get("name")).isEqualTo("Widget");
                    assertThat(first.get("description")).isEqualTo("Widget Description 2026");
                    assertThat(first.get("price")).isEqualTo(2000);
                });

    }

    //PUT - UPDATE TEST
    @Test
    void updateProduct_return204(){

        String updateBody = """
                {                  
                  "name": "Widget",
                  "description": "Widget Description 2026",
                  "price":  5000               
                }                
                """;

        String id = createProductAndReturnId("Widget", "Widget Description 2026", 2000);

        webTestClient.put()
                .uri("/api/product/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateBody)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().valueEquals("Location", "/api/product/" + id);

        webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .value( list -> {
                    boolean updated = list.stream()
                            .anyMatch(product -> product.get("id").equals(id)
                                    && "Widget".equals(product.get("name").toString())
                                    && "Widget Description 2026".equals(product.get("description").toString())
                                    && Integer.valueOf(5000).equals(product.get("price")));
                    assertThat(updated).isTrue();
                });

    }

    //DELETE - DELETION TEST
    @Test
    void deleteProduct_return204(){


        String id = createProductAndReturnId("Widget", "Widget Description 2026", 2000);

        webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .value( list -> {
                    boolean exists  = list.stream()
                            .anyMatch(product -> product.get("id").equals(id));
                    assertThat(exists).isTrue();
                });

        webTestClient.delete()
                .uri("/api/product/" + id)
                .exchange()
                .expectStatus().isNoContent();


        webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .value( list -> {
                    boolean exists  = list.stream()
                            .anyMatch(product -> product.get("id").equals(id));
                    assertThat(exists).isFalse();
                });

    }



}
