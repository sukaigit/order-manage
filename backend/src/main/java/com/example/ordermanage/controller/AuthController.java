package com.example.ordermanage.controller;

import com.example.ordermanage.common.Result;
import com.example.ordermanage.context.LoginUser;
import com.example.ordermanage.context.UserContext;
import com.example.ordermanage.dto.ForcePasswordRequest;
import com.example.ordermanage.dto.LoginRequest;
import com.example.ordermanage.dto.LoginResponse;
import com.example.ordermanage.dto.PasswordChangeRequest;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthService authService;

    public AuthController(CaptchaService captchaService, AuthService authService) {
        this.captchaService = captchaService;
        this.authService = authService;
    }

    @GetMapping("/api/auth/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(captchaService.create());
    }

    @PostMapping("/api/auth/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @GetMapping("/api/auth/me")
    public Result<LoginResponse> me() {
        LoginUser user = UserContext.get();
        return Result.ok(authService.me(user.userId(), user.token()));
    }

    @PostMapping("/api/auth/logout")
    public Result<Void> logout() {
        LoginUser user = UserContext.get();
        authService.logout(user.token());
        return Result.ok(null);
    }

    @PutMapping("/api/auth/password")
    public Result<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        LoginUser user = UserContext.get();
        authService.changePassword(user.userId(), request.getOldPassword(),
                request.getNewPassword(), request.getConfirmPassword());
        return Result.ok(null);
    }

    @PutMapping("/api/auth/password/force")
    public Result<Void> forceChangePassword(@RequestBody ForcePasswordRequest request) {
        LoginUser user = UserContext.get();
        authService.forceChangePassword(user.userId(), request.getNewPassword(),
                request.getConfirmPassword(), user.token());
        return Result.ok(null);
    }
}

