package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.OperationLog;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.OperationLogMapper;
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
class LogListTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempLogIds = new ArrayList<>();
    private final List<Long> tempUserIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempLogIds) {
            operationLogMapper.deleteById(id);
        }
        tempLogIds.clear();
        for (Long id : tempUserIds) {
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
        authService.resetFailCounts();
    }

    private String login(String username, String password) throws Exception {
        Map<String, String> cap = captchaService.create("ABCD");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", password,
                "captcha", "ABCD", "captcha_id", cap.get("captcha_id")));
        String raw = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(raw).get("data").get("token").asText();
    }

    private OperationLog insertTempLog(String user, String action, String target, String ip,
                                       String createTime) {
        OperationLog log = new OperationLog();
        log.setUser(user);
        log.setAction(action);
        log.setTarget(target);
        log.setIp(ip);
        log.setCreateTime(java.time.LocalDateTime.parse(createTime.replace(' ', 'T')));
        log.setUpdateTime(java.time.LocalDateTime.parse(createTime.replace(' ', 'T')));
        operationLogMapper.insert(log);
        tempLogIds.add(log.getId());
        return log;
    }

    @Test
    void defaultPageSizeIsFiveAndAllowedSizesAreValidated() throws Exception {
        insertTempLog("admin", "临时操作", "临时目标", "10.0.0.1", "2026-09-05 08:00:00");
        String token = login("admin", "123456");

        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.list.length()").value(5))
                .andExpect(jsonPath("$.data.list[0].operator").value("admin"))
                .andExpect(jsonPath("$.data.list[0].action").value("登录"))
                .andExpect(jsonPath("$.data.list[0].target").value("系统"))
                .andExpect(jsonPath("$.data.list[0].ip").value("192.168.1.10"))
                .andExpect(jsonPath("$.data.list[0].create_time").exists());

        mockMvc.perform(get("/api/logs")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(6));

        mockMvc.perform(get("/api/logs")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(6));

        mockMvc.perform(get("/api/logs")
                        .param("pageSize", "50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list.length()").value(6));

        mockMvc.perform(get("/api/logs")
                        .param("pageSize", "7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("分页参数非法"));
    }

    @Test
    void fuzzyFiltersAndDateExactCombineWithAnd() throws Exception {
        String token = login("admin", "123456");

        mockMvc.perform(get("/api/logs")
                        .param("operator", "admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4));

        mockMvc.perform(get("/api/logs")
                        .param("action", "新增")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].action").value("新增订单"));

        mockMvc.perform(get("/api/logs")
                        .param("target", "ORD20260905")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/logs")
                        .param("ip", "192.168.1.1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].ip").value("192.168.1.10"));

        mockMvc.perform(get("/api/logs")
                        .param("date", "2026-09-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/logs")
                        .param("date", "2026-09-01")
                        .param("operator", "admin")
                        .param("ip", "192.168.1.1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].action").value("登录"));
    }

    @Test
    void noMatchReturnsEmptyList() throws Exception {
        String token = login("admin", "123456");
        mockMvc.perform(get("/api/logs")
                        .param("operator", "不存在的操作人XYZ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(0));
    }

    @Test
    void auditorGets403() throws Exception {
        String adminToken = login("admin", "123456");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "username", "log_auditor",
                "real_name", "日志审核员",
                "role_id", 2,
                "department_id", 1,
                "organization_id", 1));
        String raw = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempUserIds.add(userId);

        String auditorToken = login("log_auditor", "Uu888888!");
        mockMvc.perform(get("/api/logs")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
