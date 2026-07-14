package com.bulkimport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BulkDataImportAndValidationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BulkDataImportAndValidationSystemApplication.class,args);
        System.out.println("Application Started");

    }

}