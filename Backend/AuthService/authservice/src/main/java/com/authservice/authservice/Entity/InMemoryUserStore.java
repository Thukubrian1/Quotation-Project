package com.authservice.authservice.Entity;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InMemoryUserStore {

    private final Map<String, String> users = Map.of(
            "Brian Thuku", "thukubrianngugi1@gmail.com"
    );

    public boolean validate(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }
}
