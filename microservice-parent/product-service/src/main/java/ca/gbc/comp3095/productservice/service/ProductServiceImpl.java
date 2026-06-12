package ca.gbc.comp3095.productservice.service;

import ca.gbc.comp3095.productservice.dto.ProductRequest;
import ca.gbc.comp3095.productservice.dto.ProductResponse;
import ca.gbc.comp3095.productservice.model.Product;
import ca.gbc.comp3095.productservice.respository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;
    private final CacheManager cacheManager;

    /**
     *  Our createProduct - caches the newly created product id into Redis 'after' saving out mongodb
     *  The cache key is the ID (primary key) of the newly created product
     */
    @Override
    @CachePut(value = "PRODUCT_CACHE",  key = "#result.id()")
    public ProductResponse createProduct(ProductRequest productRequest) {

        log.info("Creating a  new product {}",  productRequest);

        Product product = Product.builder()
                .name(productRequest.name())
                .description(productRequest.description())
                .price(productRequest.price())
                .build();


        productRepository.save(product);
        log.debug("Saved product {}",   product);


        /**

          // If you wanted to instead wanted to manually add to you cache you could use this code instead
          Cache productCache = cacheManager.getCache("PRODUCT_CACHE");
          productCache.put(product.getId(), product);
       **/

        return new ProductResponse(product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice());
    }

    @Override
    @Cacheable(value = "PRODUCT_CACHE", key = "'ALL_PRODUCTS'")
    public List<ProductResponse> getAllProducts() {

        log.debug("Returning a list of all products");

        List<Product> products = productRepository.findAll();

        return new ArrayList<>(products
                .stream()
                .map(this::mapToProductResponse)
                .toList());
    }

    private ProductResponse mapToProductResponse(Product product) {
        return new ProductResponse(product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice());
    }


    @Override
    @CachePut(value = "PRODUCT_CACHE", key = "#result" )
    public String updateProduct(String productId, ProductRequest productRequest) {

        log.debug("Update product with Id {}", productId);

        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(productId));

        Product product  = mongoTemplate.findOne(query, Product.class);

        if (product != null) {
             product.setName(productRequest.name());
             product.setDescription(productRequest.description());
             product.setPrice(productRequest.price());
             return productRepository.save(product).getId();
        }
        return productId;
    }

    @Override
    @CacheEvict(value = "PRODUCT_CACHE", key="#productId")
    public void deleteProduct(String productId) {
        log.debug("Deleting product with Id {}", productId);
        productRepository.deleteById(productId);
    }

}
