package com.example.ordermanage.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.OperationLog;
import com.example.ordermanage.mapper.OperationLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "order.oplog.enabled=true")
class OperationLogWriterTest {

    private static final long SEED_LOG_COUNT = 5L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OperationLogMapper operationLogMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void baseline() {
        authService.resetFailCounts();
        purgeAddedLogs();
    }

    @AfterEach
    void cleanup() {
        authService.resetFailCounts();
        purgeAddedLogs();
    }

    private void purgeAddedLogs() {
        operationLogMapper.delete(new QueryWrapper<OperationLog>().gt("id", SEED_LOG_COUNT));
    }

    private long countLogs() {
        return operationLogMapper.selectCount(null);
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
    void loginInsertsOperationLogRowAndAppearsInLogApi() throws Exception {
        long before = countLogs();

        String token = login();

        long after = countLogs();
        assertTrue(after > before,
                "login must increase tb_operation_log count, before=" + before + " after=" + after);

        OperationLog latest = operationLogMapper.selectOne(new QueryWrapper<OperationLog>()
                .gt("id", SEED_LOG_COUNT)
                .orderByDesc("id")
                .last("LIMIT 1"));
        assertTrue(latest != null, "new log row must exist");
        assertTrue("登录".equals(latest.getAction()), "action must be 登录 but was " + latest.getAction());
        assertTrue(latest.getUser() != null && !latest.getUser().isEmpty(), "operator must be set");
        assertTrue(latest.getIp() != null, "ip must be set");
        assertTrue(latest.getCreateTime() != null, "create_time must be set");

        mockMvc.perform(get("/api/logs")
                        .param("action", "登录")
                        .param("page", "1")
                        .param("pageSize", "50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.list[?(@.action=='登录')]").exists());
    }
}
