package com.company.Integration.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * This code customizes the "cover page" or "header" of your Swagger UI webpage. 
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI integrationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("External API Integration Service")
                        .description("""
                                Spring Boot service that integrates third-party Weather and Currency APIs.
                                Weather responses are cached in Redis for 10 minutes, and every weather
                                call publishes an asynchronous audit event to Kafka.""")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Sudarshan GC")
                                .url("https://github.com/pablo483")
                                .email("sudarshangc580@gmail.com"))
                        .license(new License()
                                .name("MIT License")   // change to the license you pick
                                .url("https://opensource.org/licenses/MIT")));
    }
}