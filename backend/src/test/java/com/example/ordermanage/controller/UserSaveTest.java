package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
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
class UserSaveTest {

    private static final String DEFAULT_HASH;

    static {
        DEFAULT_HASH = AuthService.md5("Uu888888!");
    }

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

    private final List<Long> tempUserIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempUserIds) {
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
        authService.resetFailCounts();
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

    private long createViaApi(String token, String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "real_name", "测试用户",
                "role_id", 3,
                "department_id", 1,
                "organization_id", 1));
        String raw = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempUserIds.add(id);
        return id;
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        String token = login();
        createViaApi(token, "dup_user01");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dup_user01",
                "real_name", "另一个",
                "role_id", 3,
                "department_id", 1,
                "organization_id", 1));
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void missingRoleReturns400() throws Exception {
        String token = login();
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "norole_user",
                "real_name", "无角色",
                "role_id", 999999,
                "department_id", 1,
                "organization_id", 1));
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createReturnsFirstLoginTrueWithoutPassword() throws Exception {
        String token = login();
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "newbie01",
                "real_name", "新用户",
                "role_id", 3,
                "department_id", 1,
                "organization_id", 1,
                "remark", "初始备注"));
        String raw = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("newbie01"))
                .andExpect(jsonPath("$.data.first_login").value(true))
                .andExpect(jsonPath("$.data.status").value("启用"))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(raw.contains("\"password\""),
                "create response must not contain password: " + raw);

        JsonNode data = objectMapper.readTree(raw).get("data");
        long id = data.get("id").asLong();
        tempUserIds.add(id);
        User saved = userMapper.selectById(id);
        org.junit.jupiter.api.Assertions.assertEquals(DEFAULT_HASH, saved.getPassword(),
                "password must be MD5 of default Uu888888!");
        org.junit.jupiter.api.Assertions.assertEquals(1, saved.getFirstLogin());
    }

    @Test
    void updateIgnoresPasswordAndOmitsItFromResponse() throws Exception {
        String token = login();
        long id = createViaApi(token, "editor01");
        String before = userMapper.selectById(id).getPassword();

        String body = "{\"username\":\"editor01\",\"real_name\":\"改名了\",\"role_id\":2,"
                + "\"department_id\":1,\"organization_id\":1,\"password\":\"Hacked@123\",\"status\":\"启用\"}";
        String raw = mockMvc.perform(put("/api/users/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.real_name").value("改名了"))
                .andExpect(jsonPath("$.data.role_id").value(2))
                .andExpect(jsonPath("$.data.role_name").value("审核员"))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(raw.contains("\"password\""),
                "update response must not contain password: " + raw);
        org.junit.jupiter.api.Assertions.assertEquals(before, userMapper.selectById(id).getPassword(),
                "PUT must ignore password field");
    }

    @Test
    void updateMissingUserReturns404() throws Exception {
        String token = login();
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "ghost",
                "real_name", "幽灵",
                "role_id", 3,
                "department_id", 1,
                "organization_id", 1));
        mockMvc.perform(put("/api/users/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void missingRequiredFieldsReturn400() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
