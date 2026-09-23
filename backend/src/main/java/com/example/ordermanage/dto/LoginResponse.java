package com.example.ordermanage.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private boolean firstLogin;
    private Map<String, Object> user;
    private List<Map<String, Object>> menus;
    private List<String> permissions;
}
