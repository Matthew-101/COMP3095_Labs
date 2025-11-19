package ca.gbc.comp3095.productservice;

import ca.gbc.comp3095.productservice.dto.ProductRequest;
import ca.gbc.comp3095.productservice.dto.ProductResponse;
import ca.gbc.comp3095.productservice.model.Product;
import ca.gbc.comp3095.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================================
 *  Integration Test: ProductService + Redis Cache
 *  -------------------------------------------------
 *  This test suite validates that our caching annotations actually behave
 *  the way we expect them to. Each test spins up a clean MongoDB and Redis
 *  instance using Testcontainers.
 *
 *  Covered behaviors (based on ProductServiceImpl):
 *   createProduct()  -> @CachePut(PRODUCT_CACHE, key = "#result.id()")
 *   getAllProducts() -> @Cacheable(PRODUCT_CACHE, key = "'ALL_PRODUCTS'")
 *   updateProduct()  -> @CachePut(PRODUCT_CACHE, key = "#result")
 *   deleteProduct()  -> @CacheEvict(PRODUCT_CACHE, key = "#productId")
 * ============================================================================
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProductServiceApplicationCacheTests {

    // ---------------------------------------------------------
    // STEP 1: Start ephemeral Redis and Mongo containers
    // ---------------------------------------------------------
    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.4.3"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(120));

    @Container
    @ServiceConnection(name = "mongodb")
    static MongoDBContainer mongodbContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0"))
            .withStartupTimeout(Duration.ofSeconds(120));

    // ---------------------------------------------------------
    // STEP 2: Inject Spring test utilities + beans
    // ---------------------------------------------------------
    @Autowired private MockMvc mockMvc;
    @Autowired private CacheManager cacheManager;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private ProductRepository productRepository;
    @MockitoSpyBean private ProductRepository productRepositorySpy; // allows us to verify cache hits vs DB hits

    @Autowired private RedisConnectionFactory redisConnectionFactory; // used to flush Redis before each test

    // Helper method to retrieve our configured cache
    private Cache productCache() {
        Cache c = cacheManager.getCache("PRODUCT_CACHE");
        assertNotNull(c, "PRODUCT_CACHE must exist and be configured correctly");
        return c;
    }

    // ---------------------------------------------------------
    // STEP 3: Reset state before each test
    // ---------------------------------------------------------
    @BeforeEach
    void setUp() {
        // Clear MongoDB data
        productRepository.deleteAll();

        // Flush Redis DB completely to avoid serializer mismatches
        try (var conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushDb();
        }

        // Clear the Spring CacheManager layer
        Cache cache = cacheManager.getCache("PRODUCT_CACHE");
        if (cache != null) cache.clear();

        // Reset spy counters so DB hit checks are accurate
        clearInvocations(productRepositorySpy);
    }

    // ---------------------------------------------------------
    // TEST 1: Verify @CachePut on createProduct()
    // ---------------------------------------------------------
    @Test
    void createProduct_cachesProductResponseUnderId() throws Exception {
        var req = new ProductRequest(null, "Samsung TV", "2025 Model", BigDecimal.valueOf(2000));

        // Perform POST -> should store Product in Mongo + cache the ProductResponse
        MvcResult result = mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        var body = objectMapper.readValue(result.getResponse().getContentAsString(), ProductResponse.class);
        assertNotNull(body.id(), "Response must include generated product ID");

        // Verify that the ProductResponse is now cached by its id
        ProductResponse cached = productCache().get(body.id(), ProductResponse.class);
        assertNotNull(cached, "ProductResponse should be cached under product ID");
        assertEquals("Samsung TV", cached.name());
        assertEquals(BigDecimal.valueOf(2000), cached.price());
    }

    // ---------------------------------------------------------
    // TEST 2: Verify @Cacheable on getAllProducts()
    // ---------------------------------------------------------
    @Test
    void getAllProducts_isCached_afterFirstCall_andSkipsRepository_onSecondCall() throws Exception {
        // Seed MongoDB with one product
        productRepository.save(Product.builder()
                .name("Text Book")
                .description("COMP3095")
                .price(BigDecimal.valueOf(100))
                .build());

        // First call should hit the repository (DB)
        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Text Book"));

        verify(productRepositorySpy, times(1)).findAll();
        clearInvocations(productRepositorySpy);

        // Second call should bypass DB entirely (read from Redis cache)
        mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Text Book"));

        verify(productRepositorySpy, times(0)).findAll();
    }

    // ---------------------------------------------------------
    // TEST 3: Verify @CachePut on updateProduct()
    // ---------------------------------------------------------
    @Test
    void updateProduct_cachesIdStringUnderIdKey() throws Exception {
        // Seed initial product
        var p = productRepository.save(Product.builder()
                .name("Laptop").description("Base").price(BigDecimal.valueOf(2000)).build());

        var update = new ProductRequest(p.getId(), "Gaming Laptop", "Pro", BigDecimal.valueOf(3000));

        // Perform PUT -> should update DB and cache id under its own key
        mockMvc.perform(put("/api/product/{id}", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNoContent());

        // Confirm that the ID was cached as a simple string value
        String cachedId = productCache().get(p.getId(), String.class);
        assertNotNull(cachedId, "Updated product ID should be cached as String");
        assertEquals(p.getId(), cachedId);
    }

    // ---------------------------------------------------------
    // TEST 4: Verify @CacheEvict on deleteProduct()
    // ---------------------------------------------------------
    @Test
    void deleteProduct_evictsSingleItemCacheEntry() throws Exception {
        // Seed DB with one product
        var p = productRepository.save(Product.builder()
                .name("Phone").description("Base").price(BigDecimal.valueOf(500)).build());

        // Preload cache manually to simulate an existing entry
        productCache().put(p.getId(), new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice()));
        assertNotNull(productCache().get(p.getId()), "Precondition: cache should contain product before deletion");

        // Perform DELETE -> should remove from DB and evict cache entry
        mockMvc.perform(delete("/api/product/{id}", p.getId()))
                .andExpect(status().isNoContent());

        assertNull(productCache().get(p.getId()), "Cache entry must be evicted after deletion");
    }
}
