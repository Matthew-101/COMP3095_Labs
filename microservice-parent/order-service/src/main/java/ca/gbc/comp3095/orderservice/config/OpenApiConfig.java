package ca.gbc.comp3095.orderservice.config;


import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {


    @Value("${order-service.version}")
    private String version;


    @Bean
    public OpenAPI orderServiceAPI(){

        return new OpenAPI()
                .info(new Info().title("Order Service API")
                        .description("Order Service API - for placing an order (of line items). Validates inventory")
                        .version(version)
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Order Service - COMP3095 Summer 2026 - Wiki Documentation")
                        .url("https://mycompany.sharepoint.ca/order-service/docs"));

    }

}
