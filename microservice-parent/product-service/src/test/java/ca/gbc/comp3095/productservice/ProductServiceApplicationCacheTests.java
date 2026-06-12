package ca.gbc.comp3095.productservice;

import ca.gbc.comp3095.productservice.dto.ProductResponse;
import ca.gbc.comp3095.productservice.respository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductServiceApplicationCacheTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    @ServiceConnection
    static MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:latest");

    //Redis
    //No dedicated testcontainer needed for Redis module testing - GenericContainers serves our needs
    static GenericContainer<?> redisContainer = new GenericContainer("redis:latest")
            .withExposedPorts(6379)
            .withCommand("redis-server","--requirepass", "password");


    static {
        mongoDbContainer.start();
        redisContainer.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "password");
    }


    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        //reset the state of our database (prior to test) to a pristine state
        productRepository.deleteAll();

        //reset the state of our cache (prior to test) to a pristine state
        Cache cache = cacheManager.getCache("PRODUCT_CACHE");
        if(cache != null)
            cache.clear();

    }


    // Helper method only
    private Map<?, ?> createProduct(String name, String description, int price){

        String requestBody = """
                {                  
                  "name": "%s",
                  "description": "%s",
                  "price":  %d               
                }                
                """.formatted(name, description, price);

        return webTestClient.post()
                .uri("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult()
                .getResponseBody();

    }


    @Test
    void createProduct_shouldPopulateCacheWithProductId(){

        Map<?, ?> response   = createProduct("Samsung TV", "Samsung TV 2026", 2000);
        assertThat(response).isNotNull();
        String productId = (String) response.get("id");

        Cache cache = cacheManager.getCache("PRODUCT_CACHE");
        assertThat(cache).isNotNull();

        ProductResponse cachedProduct = cache.get(productId, ProductResponse.class);
        assertThat(cachedProduct).isNotNull();

        assertThat(cachedProduct.name()).isEqualTo("Samsung TV");

    }

    @Test
    void getAllProducts_shouldServeFromCacheOnSubsequentCall(){
        Map<?, ?> response  = createProduct("LG Monitor", "LG Monitor2026", 1000);

        // GET - "ALL_PRODUCTS" cache miss -> hit MongoDB -> cache the response (List with 1 Item)
        List<Map> firstResult = webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();

        assertThat(firstResult).hasSize(1);

        Cache cache = cacheManager.getCache("PRODUCT_CACHE");
        assertThat(cache).isNotNull();
        assertThat(cache.get("ALL_PRODUCTS", List.class)).isNotNull();

        Map<?, ?> secondResponse  = createProduct("iphone", "iphone 15 - 2026", 1500);

        // GET - "ALL_PRODUCTS" cache HIT -> does not hit MongoDB -> cache the response (List with 1 Item) - NOT 2
        List<Map> secondResult = webTestClient.get()
                .uri("/api/product")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Map.class)
                .returnResult()
                .getResponseBody();


        assertThat(secondResult)
                .hasSize(1);

        assertThat(secondResult.get(0).get("name")).isEqualTo("LG Monitor");


    }

    @Test
    void updateProduct_shouldRefreshCacheEntry(){
        Map<?, ?> response  = createProduct("LG Monitor", "LG Monitor2026", 1000);
        String productId =  (String) response.get("id");

        Cache cache = cacheManager.getCache("PRODUCT_CACHE");
        assertThat(cache).isNotNull();
        assertThat(cache.get(productId, ProductResponse.class)).isNotNull();

        // @CachePut(key = "#result" ) stores the returned String Id in the cache
        webTestClient.put()
                .uri("/api/product/" + productId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                  {
                   "name": "Updated Name",
                   "description": "Updated Description",
                    "price": 500                                                    
                  }
                  """)
                .exchange()
                .expectStatus().isNoContent();

        //After the cached value for this key is the String Id (not a ProductResponse)
        assertThat(cache.get(productId, String.class)).isNotNull();

    }


    @Test
    void deleteProduct_shouldEvictCacheEntry(){
        Map<?, ?> response  = createProduct("LG Monitor", "LG Monitor2026", 1000);
        String productId =  (String) response.get("id");

        Cache cache = cacheManager.getCache("PRODUCT_CACHE");
        assertThat(cache).isNotNull();
        assertThat(cache.get(productId, ProductResponse.class)).isNotNull();


        webTestClient.delete()
                .uri("/api/product/" + productId)
                .exchange()
                .expectStatus().isNoContent();

        // Assert that the product has been evicted from our cache
        assertThat(cache.get(productId, String.class)).isNull();

    }


}
