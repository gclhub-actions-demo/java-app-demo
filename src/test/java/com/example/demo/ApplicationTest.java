package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

    @Test
    void mainStartsApplicationContext() {
        Application.main(new String[] {"--spring.main.web-application-type=none"});
    }

    @Test
    void startsApplicationContext() {
        try (ConfigurableApplicationContext context = new Application()
                .runApplication("--spring.main.web-application-type=none")) {
            assertTrue(context.isActive());
        }
    }
}
