package com.example.ordermanage.context;

public record LoginUser(Long userId, String username, String roleCode, String token) {
}
