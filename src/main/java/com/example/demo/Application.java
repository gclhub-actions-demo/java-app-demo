package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main application class for the Java App Demo.
 * This demonstrates a Spring Boot REST API for GitHub Actions CI/CD reference implementation.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        new Application().runApplication(args);
    }

    ConfigurableApplicationContext runApplication(String... args) {
        return SpringApplication.run(Application.class, args);
    }
}
