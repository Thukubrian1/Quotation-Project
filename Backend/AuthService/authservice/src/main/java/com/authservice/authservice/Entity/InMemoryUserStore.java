package com.authservice.authservice.Entity;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryUserStore {

    private final Map<String, String> users = new HashMap<>();

    public InMemoryUserStore() {
        // Initialize with test users
        users.put("Brian Thuku", "thukubrianngugi1@gmail.com");
        users.put("admin", "admin123");
        users.put("service-payment", "service-password");
    }
    public boolean validate(String username, String password) {
        return users.containsKey(username) && users.get(username).equals(password);
    }

    public void addUser(String username, String password) {
        users.put(username, password);
    }
}
