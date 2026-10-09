package com.securetodo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SecureTodoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureTodoApplication.class, args);
    }
}

