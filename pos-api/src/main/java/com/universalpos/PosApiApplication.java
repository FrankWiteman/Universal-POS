package com.universalpos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * UniversalPOS — Enterprise Multi-Tenant Point-of-Sale System
 *
 * Entry point for the Spring Boot API server.
 * Inspired by Oracle Retail Xstore POS architecture.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PosApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosApiApplication.class, args);
    }
}
