package com.fitflow.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UsersSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsersSvcApplication.class, args);
	}

}
