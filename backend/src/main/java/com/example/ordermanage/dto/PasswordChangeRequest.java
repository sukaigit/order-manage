package com.example.ordermanage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordChangeRequest {

    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
