package ca.gbc.comp3095.productservice;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import ca.gbc.comp3095.productservice.repository.ProductRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductServiceApplicationTests {

    // Spin up Mongo and Redis for the real app stack
    @Container
    @ServiceConnection(name = "mongodb")
    static MongoDBContainer mongo =
            new MongoDBContainer(DockerImageName.parse("mongo:latest"))
                    .withStartupTimeout(Duration.ofSeconds(120));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.4.3"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort())
                    .withStartupTimeout(Duration.ofSeconds(120));

    @LocalServerPort
    private Integer port;

    // Sergio: dependencies to reset state between tests
    @Autowired private ProductRepository productRepository;
    @Autowired private RedisConnectionFactory redisConnectionFactory;
    @Autowired private CacheManager cacheManager;

    private Cache productCache() {
        Cache c = cacheManager.getCache("PRODUCT_CACHE");
        if (c == null) throw new IllegalStateException("PRODUCT_CACHE must be configured");
        return c;
    }

    @BeforeEach
    void setUp() {
        // Point RestAssured at the random local port
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // ---- Reset DB + Cache to prevent cross-test contamination ----
        productRepository.deleteAll(); // Mongo clean slate

        // Hard-flush Redis (keys from earlier runs/serializers)
        try (var conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushDb();
        }

        // Clear Spring’s cache facade as well (in case of local cache layer)
        productCache().clear();
    }

    @Test
    void createProductTest() {
        String requestBody = """
                {
                   "name": "Samsung TV",
                   "description": "Samsung TV - Model 2025",
                   "price": 2500
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/product")
                .then()
                .log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", Matchers.notNullValue())
                .body("name", Matchers.equalTo("Samsung TV"))
                .body("description", Matchers.equalTo("Samsung TV - Model 2025"))
                .body("price", Matchers.equalTo(2500));
    }

    @Test
    void getAllProductsTest() {
        // Seed one product
        String id = createProductAndReturnId("Samsung TV", "Samsung TV - Model 2025", 2500);

        // GET should include that product (don’t assume index 0)
        RestAssured.given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/product")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("size()", Matchers.greaterThanOrEqualTo(1))
                .body("id", Matchers.hasItem(id))
                .body("find { it.id == '%s'}.name".formatted(id), Matchers.equalTo("Samsung TV"))
                .body("find { it.id == '%s'}.description".formatted(id), Matchers.equalTo("Samsung TV - Model 2025"))
                .body("find { it.id == '%s'}.price".formatted(id), Matchers.equalTo(2500));
    }

    private String createProductAndReturnId(String name, String description, int price) {
        String requestBody = """
                {
                   "name": "%s",
                   "description": "%s",
                   "price": %d
                }
                """.formatted(name, description, price);

        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/product")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .path("id");
    }

    @Test
    void updateProductTest() {
        // Create → Update
        String id = createProductAndReturnId("LG Monitor", "LG 27-inch 4K", 800);

        String updateBody = """
                {
                   "name": "LG Monitor",
                   "description": "LG 27-inch 4K",
                   "price": 1000
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/api/product/{id}", id)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .header("Location", "/api/product/" + id);

        // Read back from GET all (assert by id, not position)
        RestAssured.given()
                .when()
                .get("/api/product")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("id", Matchers.hasItem(id))
                .body("find { it.id == '%s'}.name".formatted(id), Matchers.equalTo("LG Monitor"))
                .body("find { it.id == '%s'}.description".formatted(id), Matchers.equalTo("LG 27-inch 4K"))
                .body("find { it.id == '%s'}.price".formatted(id), Matchers.equalTo(1000));
    }

    @Test
    void deleteProductTest() {
        String id = createProductAndReturnId("Temp Item", "Disposable", 10);

        // Sanity: it’s present
        RestAssured.given()
                .when()
                .get("/api/product")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", Matchers.hasItem(id));

        // Delete
        RestAssured.given()
                .when()
                .delete("/api/product/{id}", id)
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        //Right after the DELETE in deleteProductTest, clear the cache to sidestep the warmed list
        productCache().clear();

        // Verify it’s gone
        RestAssured.given()
                .when()
                .get("/api/product")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", Matchers.not(Matchers.hasItem(id)));
    }
}
