package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
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
class UserStatusResetDeleteTest {

    private static final String DEFAULT_PASSWORD = "Uu888888!";
    private static final String DEFAULT_HASH = AuthService.md5(DEFAULT_PASSWORD);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempUserIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempUserIds) {
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
        authService.resetFailCounts();
        User admin = userMapper.selectOne(new QueryWrapper<User>().eq("username", "admin"));
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

    private Long createUser(String username, int failCount, int firstLogin) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(AuthService.md5("123456"));
        u.setRealName(username);
        u.setStatus("启用");
        u.setDepartmentId(1L);
        u.setOrganizationId(1L);
        u.setFirstLogin(firstLogin);
        u.setFailCount(failCount);
        userMapper.insert(u);
        tempUserIds.add(u.getId());
        UserRole link = new UserRole();
        link.setUserId(u.getId());
        link.setRoleId(3L);
        userRoleMapper.insert(link);
        return u.getId();
    }

    @Test
    void disableThenEnableUser() throws Exception {
        Long id = createUser("status_user", 0, 0);
        String token = login();

        mockMvc.perform(put("/api/users/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"停用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("停用"));
        org.junit.jupiter.api.Assertions.assertEquals("停用", userMapper.selectById(id).getStatus());

        mockMvc.perform(put("/api/users/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"启用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("启用"));
        org.junit.jupiter.api.Assertions.assertEquals("启用", userMapper.selectById(id).getStatus());
    }

    @Test
    void invalidStatusReturns400() throws Exception {
        Long id = createUser("badstatus_user", 0, 0);
        String token = login();
        mockMvc.perform(put("/api/users/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"锁定\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void disableAdminReturns422() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/users/1/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"停用\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("默认管理员 admin 不可停用"));
        org.junit.jupiter.api.Assertions.assertEquals("启用",
                userMapper.selectById(1L).getStatus());
    }

    @Test
    void statusMissingUserReturns404() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/users/999999/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"停用\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void resetPasswordRestoresDefaults() throws Exception {
        Long id = createUser("reset_user", 5, 0);
        String token = login();

        mockMvc.perform(put("/api/users/" + id + "/reset-password")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.default_password").value(DEFAULT_PASSWORD))
                .andExpect(jsonPath("$.data.first_login").value(true));

        User after = userMapper.selectById(id);
        org.junit.jupiter.api.Assertions.assertEquals(DEFAULT_HASH, after.getPassword());
        org.junit.jupiter.api.Assertions.assertEquals(1, after.getFirstLogin());
        org.junit.jupiter.api.Assertions.assertEquals(0, after.getFailCount());
    }

    @Test
    void resetMissingUserReturns404() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/users/999999/reset-password")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deleteUserCascadesUserRole() throws Exception {
        Long id = createUser("doomed_user", 0, 0);
        String token = login();
        mockMvc.perform(delete("/api/users/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(userMapper.selectById(id));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                userRoleMapper.selectCount(new QueryWrapper<UserRole>().eq("user_id", id)));
        tempUserIds.remove(id);
    }

    @Test
    void deleteAdminReturns422() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("默认管理员 admin 不可删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(userMapper.selectById(1L));
    }

    @Test
    void deleteMissingUserReturns404() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/users/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
