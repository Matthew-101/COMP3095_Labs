package ca.gbc.comp3095.productservice.config;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {


    @Value("${product-service.version}")
    private String version;


    @Bean
    public OpenAPI productServiceAPI(){

        return new OpenAPI()
                .info(new Info().title("Product Service API")
                        .description("Product Service API - For creating, reading, updating and deleting company products.")
                        .version(version)
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Product Service - COMP3095 Summer 2026 - Wiki Documentation")
                        .url("https://mycompany.sharepoint.ca/product-service/docs"));

    }

}
