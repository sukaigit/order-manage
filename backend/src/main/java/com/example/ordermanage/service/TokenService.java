package com.example.ordermanage.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final ConcurrentHashMap<String, Long> tokens = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, userId);
        return token;
    }

    public Long resolve(String token) {
        if (token == null) {
            return null;
        }
        return tokens.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}
