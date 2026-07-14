package com.bulkimport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bulkImportOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Bulk Data Import & Validation System API")
                        .description("REST APIs for uploading, validating and processing CSV/Excel files.")
                        .version("1.0"));

    }

}