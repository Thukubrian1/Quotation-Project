package com.authservice.authservice.Service;

import org.springframework.stereotype.Service;

@Service
public interface AuthService {

    String login(String userName, String userEmail);
}
