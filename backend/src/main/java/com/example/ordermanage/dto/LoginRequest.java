package com.example.ordermanage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginRequest {

    private String username;
    private String password;
    private String captcha;
    private String captchaId;
}
