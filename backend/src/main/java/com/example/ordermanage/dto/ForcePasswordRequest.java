package com.example.ordermanage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForcePasswordRequest {

    private String newPassword;
    private String confirmPassword;
}
