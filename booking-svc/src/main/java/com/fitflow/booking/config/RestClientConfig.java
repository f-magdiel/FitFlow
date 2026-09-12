package com.fitflow.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

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
        Duration timeout = Duration.ofSeconds(2);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        return builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
