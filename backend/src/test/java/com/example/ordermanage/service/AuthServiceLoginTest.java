package com.example.ordermanage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
class AuthServiceLoginTest {

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
    void cleanup() {
        authService.resetFailCounts();
        User admin = admin();
        if (admin != null) {
            boolean dirty = !"启用".equals(admin.getStatus())
                    || admin.getFirstLogin() == null || admin.getFirstLogin() != 0
                    || admin.getFailCount() == null || admin.getFailCount() != 0;
            if (dirty) {
                admin.setStatus("启用");
                admin.setFirstLogin(0);
                admin.setFailCount(0);
                userMapper.updateById(admin);
            }
        }
    }

    private String loginBody(String username, String password, String captchaCode) {
        Map<String, String> cap = captchaCode == null
                ? captchaService.create("ABCD")
                : captchaService.create(captchaCode);
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "username", username == null ? "" : username,
                    "password", password == null ? "" : password,
                    "captcha", captchaCode == null ? "ABCD" : captchaCode,
                    "captcha_id", cap.get("captcha_id")));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String loginBodyWithId(String username, String password, String captcha, String captchaId) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "captcha", captcha,
                "captcha_id", captchaId));
    }

    @Test
    void missingCredentialsReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\",\"captcha\":\"A1B2\",\"captcha_id\":\"cap_x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请输入用户名和密码"));
    }

    @Test
    void missingCaptchaReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\",\"captcha\":\"\",\"captcha_id\":\"cap_x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("请输入验证码"));
    }

    @Test
    void wrongCaptchaReturns401() throws Exception {
        Map<String, String> cap = captchaService.create("A3K9");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBodyWithId("admin", "123456", "XXXX", cap.get("captcha_id"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("验证码错误"));
    }

    @Test
    void fourthWrongPasswordSaysOneChanceLeft() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("admin", "bad", "ABCD")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "bad", "ABCD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("密码错误，还剩 1 次机会"));
    }

    @Test
    void fifthWrongPasswordLocksAccountAndPersistsInDb() throws Exception {
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("admin", "bad", "ABCD")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "bad", "ABCD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("账户已锁定；密码错误次数过多，请联系系统管理员解锁"));

        User reloaded = admin();
        assertEquals(5, reloaded.getFailCount(), "fail_count must persist in DB after 5 failures");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "bad", "ABCD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("账户已锁定；密码错误次数过多，请联系系统管理员解锁"));
    }

    @Test
    void successResetsFailCountInDb() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "bad", "ABCD")))
                .andExpect(status().isUnauthorized());
        assertEquals(1, admin().getFailCount());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "123456", "ABCD")))
                .andExpect(status().isOk());
        assertEquals(0, admin().getFailCount(), "login success must reset fail_count in DB");
    }

    @Test
    void firstLoginUserGetsTrueInResponse() throws Exception {
        User admin = admin();
        admin.setFirstLogin(1);
        userMapper.updateById(admin);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "123456", "ABCD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_login").value(true));
    }

    @Test
    void disabledAccountReturns401() throws Exception {
        User admin = admin();
        admin.setStatus("停用");
        userMapper.updateById(admin);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "123456", "ABCD")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("账户已禁用，请联系系统管理员启用"));
    }

    @Test
    void successReturnsTokenMenusPermissions() throws Exception {
        String body = loginBody("admin", "123456", "ABCD");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.first_login").value(false))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.role_code").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.menus[?(@.code=='MENU_WORKBENCH')]").exists())
                .andExpect(jsonPath("$.data.permissions[?(@=='order:query')]").exists());

        String raw = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin", "123456", "ABCD")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(raw).get("data");
        if (data.get("password") != null) {
            throw new AssertionError("password must not appear in response");
        }
    }
}
