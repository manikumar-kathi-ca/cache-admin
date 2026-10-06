package com.financialcorp.cachepoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CachePocApplication {

    public static void main(String[] args) {
        SpringApplication.run(CachePocApplication.class, args);
    }
}
