package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SupplierListTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;

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
    void listReturnsSeedSuppliers() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/suppliers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-001"))
                .andExpect(jsonPath("$.data.list[0].name").value("华东原料供应商"))
                .andExpect(jsonPath("$.data.list[0].contact").value("张三"))
                .andExpect(jsonPath("$.data.list[0].phone").value("13800000001"))
                .andExpect(jsonPath("$.data.list[0].address").value("上海市浦东新区"))
                .andExpect(jsonPath("$.data.list[0].status").value("启用"))
                .andExpect(jsonPath("$.data.list[0].create_time").exists())
                .andExpect(jsonPath("$.data.list[2].code").value("SUP-003"));
    }

    @Test
    void statusDisabledFilterReturnsOnlySup003() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/suppliers")
                        .param("status", "停用")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-003"))
                .andExpect(jsonPath("$.data.list[0].status").value("停用"));

        mockMvc.perform(get("/api/suppliers")
                        .param("status", "启用")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-001"))
                .andExpect(jsonPath("$.data.list[1].code").value("SUP-002"));
    }

    @Test
    void keywordMatchesNameOrContact() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/suppliers")
                        .param("keyword", "华东")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-001"));

        mockMvc.perform(get("/api/suppliers")
                        .param("keyword", "王五")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-003"));

        mockMvc.perform(get("/api/suppliers")
                        .param("keyword", "不存在的关键词")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    void keywordAndStatusCombineWithAnd() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/suppliers")
                        .param("keyword", "物流")
                        .param("status", "启用")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/suppliers")
                        .param("keyword", "物流")
                        .param("status", "停用")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-003"));
    }

    @Test
    void paginationKeepsFilters() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/suppliers")
                        .param("page", "2")
                        .param("pageSize", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-003"));

        mockMvc.perform(get("/api/suppliers")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .param("status", "启用")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("SUP-002"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isUnauthorized());
    }
}
