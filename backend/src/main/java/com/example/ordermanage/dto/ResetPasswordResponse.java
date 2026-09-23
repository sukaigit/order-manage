package com.example.ordermanage.dto;

import lombok.Data;

@Data
public class ResetPasswordResponse {

    private Long id;
    private String defaultPassword;
    private Boolean firstLogin;
}
