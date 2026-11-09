package com.example.financial_api.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DemoApplicationTests {
    @Test
    void contextLoads() {
        try (var context =
                new org.springframework.boot.builder.SpringApplicationBuilder(DemoApplication.class)
                        .run("--server.port=0")) {
            assertTrue(context.isActive());
        }
    }
}
