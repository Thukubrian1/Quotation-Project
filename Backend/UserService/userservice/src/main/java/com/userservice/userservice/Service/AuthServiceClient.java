package com.userservice.userservice.Service;

import com.shared.sharedlib.Dtos.GenericResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${external.service.auth-service.base-url}")
    private String authServiceBaseUrl;

    public boolean validateToken(String token) {
        try {
            String url = authServiceBaseUrl + "/auth/validate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GenericResponse> response = restTemplate.postForEntity(
                    url, entity, GenericResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GenericResponse responseBody = response.getBody();
                if (responseBody.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.getData();
                    return Boolean.TRUE.equals(data.get("valid"));
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Error validating token with auth service", e);
            return false;
        }
    }

    public String login(String username, String password) {
        try {
            String url = authServiceBaseUrl + "/auth/login";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String requestBody = "username=" + username + "&password=" + password;
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<GenericResponse> response = restTemplate.postForEntity(
                    url, entity, GenericResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GenericResponse responseBody = response.getBody();
                if (responseBody.getData() instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.getData();
                    return (String) data.get("access_token");
                }
            }

            throw new RuntimeException("Failed to get token from auth service");

        } catch (Exception e) {
            log.error("Error during login with auth service", e);
            throw new RuntimeException("Login failed", e);
        }
    }
}