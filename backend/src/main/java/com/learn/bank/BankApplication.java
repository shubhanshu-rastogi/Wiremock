package com.learn.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the "real" banking backend.
 * Run it, then point the UI at http://localhost:8080.
 * Later you'll swap the UI to WireMock on :8081 and the app behaves the same.
 */
@SpringBootApplication
public class BankApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }
}
