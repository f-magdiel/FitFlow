package com.fitflow.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient usersRestClient(RestClient.Builder builder,
                               @Value("${fitflow.clients.users-svc-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    RestClient notifRestClient(RestClient.Builder builder,
                               @Value("${fitflow.clients.notif-svc-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
