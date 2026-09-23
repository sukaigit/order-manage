package com.example.ordermanage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordTest {

    private static final String ORIG_MD5 = "e10adc3949ba59abbe56e057f20f883e";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private User admin() {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", "admin"));
    }

    @AfterEach
    void restorePassword() {
        authService.resetFailCounts();
        User admin = admin();
        if (admin != null) {
            boolean dirty = !ORIG_MD5.equals(admin.getPassword())
                    || admin.getFirstLogin() == null || admin.getFirstLogin() != 0
                    || admin.getFailCount() == null || admin.getFailCount() != 0;
            if (dirty) {
                admin.setPassword(ORIG_MD5);
                admin.setFirstLogin(0);
                admin.setFailCount(0);
                userMapper.updateById(admin);
            }
        }
    }

    private String login() throws Exception {
        Map<String, String> cap = captchaService.create("ABCD");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "admin", "password", "123456",
                "captcha", "ABCD", "captcha_id", cap.get("captcha_id")));
        String raw = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(raw).get("data").get("token").asText();
    }

    @Test
    void weakPasswordReturns400() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"old_password\":\"123456\",\"new_password\":\"abcdefgh\",\"confirm_password\":\"abcdefgh\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码不符合安全规则"));
    }

    @Test
    void mismatchConfirmReturns400() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"old_password\":\"123456\",\"new_password\":\"NewPass@2026\",\"confirm_password\":\"Other@2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("两次密码不一致"));
    }

    @Test
    void sameAsCurrentReturns400() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"old_password\":\"123456\",\"new_password\":\"123456\",\"confirm_password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("新密码不能与当前密码相同"));
    }

    @Test
    void missingOldPasswordReturns400() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"old_password\":\"\",\"new_password\":\"NewPass@2026\",\"confirm_password\":\"NewPass@2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入当前密码"));
    }

    @Test
    void missingNewPasswordReturns400() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"old_password\":\"123456\",\"new_password\":\"\",\"confirm_password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写新密码"));
    }

    @Test
    void normalChangePasswordReturns200AndClearsFirstLogin() throws Exception {
        User admin = admin();
        admin.setFirstLogin(1);
        admin.setFailCount(3);
        userMapper.updateById(admin);

        String token = login();
        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"old_password\":\"123456\",\"new_password\":\"NewPass@2026\",\"confirm_password\":\"NewPass@2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        User after = admin();
        assertEquals(0, after.getFirstLogin(), "normal change must set first_login=0");
        assertEquals(0, after.getFailCount(), "normal change must reset fail_count");
    }

    @Test
    void forceChangeInvalidatesOldTokenAndClearsFirstLogin() throws Exception {
        User admin = admin();
        admin.setFirstLogin(1);
        userMapper.updateById(admin);

        String token = login();
        mockMvc.perform(put("/api/auth/password/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"new_password\":\"NewPass@2026\",\"confirm_password\":\"NewPass@2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(getMe(token)).andExpect(status().isUnauthorized());

        User after = admin();
        assertEquals(0, after.getFirstLogin(), "force change must set first_login=0");
        assertEquals(0, after.getFailCount(), "force change must reset fail_count");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getMe(String token) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/auth/me").header("Authorization", "Bearer " + token);
    }
}
