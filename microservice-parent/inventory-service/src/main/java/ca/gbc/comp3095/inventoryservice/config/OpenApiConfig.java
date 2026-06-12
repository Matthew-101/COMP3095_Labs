package ca.gbc.comp3095.inventoryservice.config;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {


    @Value("${inventory-service.version}")
    private String version;


    @Bean
    public OpenAPI inventoryServiceAPI(){

        return new OpenAPI()
                .info(new Info().title("inventory Service API")
                        .description("Inventory Service API - for determining available inventory of product to place orders")
                        .version(version)
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Inventory Service - COMP3095 Summer 2026 - Wiki Documentation")
                        .url("https://mycompany.sharepoint.ca/inventory-service/docs"));

    }

}
