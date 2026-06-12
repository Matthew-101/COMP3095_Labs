package ca.gbc.comp3095.inventoryservice;

import ca.gbc.comp3095.inventoryservice.model.Inventory;
import ca.gbc.comp3095.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

// Full Spring context on a random port — exercises the real HTTP stack, Flyway, and JPA.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceApplicationTests {

    @LocalServerPort
    private int port;

    // Injected to seed test data — no POST endpoint exists on this service.
    @Autowired
    private InventoryRepository inventoryRepository;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        // Wipe between tests so each test owns its own state.
        inventoryRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    // --- isInStock: happy paths -----------------------------------------------

    @Test
    void isInStock_returns_true_when_stock_exceeds_requested_quantity() {
        inventoryRepository.save(new Inventory(null, "samsung_tv_2025", 100));

        webTestClient.get()
                .uri("/api/inventory?skuCode=samsung_tv_2025&quantity=50")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(true);
    }

    @Test
    void isInStock_returns_true_when_stock_exactly_matches_requested_quantity() {
        // existsBySkuCodeAndQuantityGreaterThanEqual includes the equal case.
        inventoryRepository.save(new Inventory(null, "iphone_15", 10));

        webTestClient.get()
                .uri("/api/inventory?skuCode=iphone_15&quantity=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(true);
    }

    // --- isInStock: sad paths -------------------------------------------------

    @Test
    void isInStock_returns_false_when_requested_quantity_exceeds_stock() {
        inventoryRepository.save(new Inventory(null, "macbook_pro_2025", 3));

        webTestClient.get()
                .uri("/api/inventory?skuCode=macbook_pro_2025&quantity=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(false);
    }

    @Test
    void isInStock_returns_false_when_skuCode_does_not_exist() {
        webTestClient.get()
                .uri("/api/inventory?skuCode=nonexistent_sku&quantity=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(false);
    }

}

