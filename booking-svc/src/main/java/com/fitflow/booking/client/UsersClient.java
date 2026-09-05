package com.fitflow.booking.client;

import com.fitflow.booking.dto.client.UserProfileResponse;
import com.fitflow.booking.exception.ExternalServiceException;
import com.fitflow.booking.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class UsersClient {

    private final RestClient restClient;

    public UsersClient(@Qualifier("usersRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public UserProfileResponse validateUserExists(UUID userId) {
        try {
            return restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .body(UserProfileResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new UserNotFoundException(userId);
            }
            throw new ExternalServiceException("No se pudo validar el usuario en users-svc", ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("No se pudo validar el usuario en users-svc", ex);
        }
    }
}
