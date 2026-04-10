package com.bolas.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BolasApplication {

    public static void main(String[] args) {
        SpringApplication.run(BolasApplication.class, args);
    }
}
