package com.example.ordermanage.aspect;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.annotation.RequirePerm;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
class PermissionAspectTest {

    @TestConfiguration
    static class DemoConfig {

        @Bean
        DemoPermController demoPermController() {
            return new DemoPermController();
        }
    }

    @RestController
    static class DemoPermController {

        @RequirePerm("audit:pass")
        @PostMapping("/api/order-audits/{id}/pass")
        public Map<String, String> auditPass(@PathVariable Long id) {
            return Map.of("ok", "true");
        }
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
    private UserRoleMapper userRoleMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempUserIds = new ArrayList<>();

    @BeforeEach
    void createTempUsers() {
        authService.resetFailCounts();
        tempUserIds.add(createUser("user01", 3L));
        tempUserIds.add(createUser("auditor01", 2L));
    }

    @AfterEach
    void cleanup() {
        for (Long id : tempUserIds) {
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
        authService.resetFailCounts();
    }

    private Long createUser(String username, Long roleId) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(AuthService.md5("123456"));
        u.setRealName(username);
        u.setStatus("启用");
        u.setDepartmentId(1L);
        u.setOrganizationId(1L);
        userMapper.insert(u);
        UserRole link = new UserRole();
        link.setUserId(u.getId());
        link.setRoleId(roleId);
        userRoleMapper.insert(link);
        return u.getId();
    }

    private String login(String username) throws Exception {
        Map<String, String> cap = captchaService.create("ABCD");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", "123456",
                "captcha", "ABCD", "captcha_id", cap.get("captcha_id")));
        String raw = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(raw).get("data").get("token").asText();
    }

    @Test
    void roleUserAuditPassReturns403() throws Exception {
        String token = login("user01");
        mockMvc.perform(post("/api/order-audits/1/pass")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAuditPassReturns403() throws Exception {
        String token = login("admin");
        mockMvc.perform(post("/api/order-audits/1/pass")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorAuditPassReturns200() throws Exception {
        String token = login("auditor01");
        mockMvc.perform(post("/api/order-audits/1/pass")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void auditorUsersReturns403() throws Exception {
        String token = login("auditor01");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUsersReturns200() throws Exception {
        String token = login("admin");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }
}
