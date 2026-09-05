package com.fitflow.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BookingSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingSvcApplication.class, args);
    }
}
