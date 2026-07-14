package com.bulkimport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@Configuration
public class ValidationConfig {

    @Bean
    Validator validator() {

        return Validation
                .buildDefaultValidatorFactory()
                .getValidator();

    }

}