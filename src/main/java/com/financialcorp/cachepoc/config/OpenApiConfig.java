package com.financialcorp.cachepoc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cachePocOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinancialCorp Cache POC")
                        .version("0.1.0")
                        .description("Accounts API with Redis write-through cache, plus cache admin flush/drop operations."));
    }
}
