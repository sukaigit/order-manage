package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.UserMapper;
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
class UserListTest {

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
        User admin = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>().eq("username", "admin"));
        if (admin != null && (admin.getFailCount() == null || admin.getFailCount() != 0)) {
            admin.setFailCount(0);
            userMapper.updateById(admin);
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

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String uri) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(uri);
    }

    private User createUser(String username, String realName) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(AuthService.md5("123456"));
        u.setRealName(realName);
        u.setStatus("启用");
        u.setDepartmentId(1L);
        u.setOrganizationId(1L);
        u.setFirstLogin(0);
        u.setFailCount(0);
        userMapper.insert(u);
        tempUserIds.add(u.getId());
        return u;
    }

    @Test
    void listReturnsAdminFirstWithoutPassword() throws Exception {
        String token = login();
        String raw = mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].username").value("admin"))
                .andExpect(jsonPath("$.data.list[0].locked").value(false))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(raw.contains("\"password\""),
                "user list JSON must not contain password key: " + raw);
    }

    @Test
    void keywordFilterReturnsCorrectTotal() throws Exception {
        createUser("kw_alice", "关键字甲");
        createUser("kw_bob", "关键字乙");
        createUser("other_zoe", "无关用户");
        String token = login();

        mockMvc.perform(get("/api/users")
                        .param("keyword", "kw_")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/users")
                        .param("keyword", "kw_alice")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("kw_alice"));

        mockMvc.perform(get("/api/users")
                        .param("keyword", "关键字甲")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void roleAndStatusFiltersWork() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/users")
                        .param("role_id", "1")
                        .param("status", "启用")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].username").value("admin"))
                .andExpect(jsonPath("$.data.list[0].role_id").value(1))
                .andExpect(jsonPath("$.data.list[0].role_name").value("管理员"))
                .andExpect(jsonPath("$.data.list[0].department_name").value("总部"))
                .andExpect(jsonPath("$.data.list[0].organization_name").value("总行"));
    }
}
