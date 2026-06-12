package ca.gbc.comp3095.productservice.respository;

import ca.gbc.comp3095.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {


}
