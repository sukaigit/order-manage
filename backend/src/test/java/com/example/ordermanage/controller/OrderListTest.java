package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.Order;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderListTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempOrderIds = new ArrayList<>();

    @BeforeEach
    void plant() {
        tempOrderIds.add(plant("ORD-LIST-001", "华东采购单", 1L, "待审核",
                LocalDateTime.of(2026, 1, 15, 10, 0, 0)));
        tempOrderIds.add(plant("ORD-LIST-002", "南方包材单", 2L, "已完成",
                LocalDateTime.of(2026, 2, 10, 11, 0, 0)));
        tempOrderIds.add(plant("ORD-LIST-003", "华东原料单", 1L, "已驳回",
                LocalDateTime.of(2026, 3, 5, 9, 30, 0)));
        tempOrderIds.add(plant("ORD-LIST-004", "北方物流单", 3L, "待审核",
                LocalDateTime.of(2026, 1, 20, 14, 0, 0)));
    }

    @AfterEach
    void cleanup() {
        for (Long id : tempOrderIds) {
            orderMapper.deleteById(id);
        }
        tempOrderIds.clear();
        authService.resetFailCounts();
    }

    private Long plant(String orderNo, String name, Long supplierId, String status,
                       LocalDateTime createTime) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName(name);
        order.setAmount(new BigDecimal("100.00"));
        order.setSupplierId(supplierId);
        order.setStatus(status);
        order.setCreateTime(createTime);
        order.setUpdateTime(createTime);
        orderMapper.insert(order);
        return order.getId();
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
    void fourFilterComboReturnsSingleMatch() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders")
                        .param("keyword", "华东")
                        .param("supplier_id", "1")
                        .param("status", "待审核")
                        .param("start_date", "2026-01-01")
                        .param("end_date", "2026-01-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-001"))
                .andExpect(jsonPath("$.data.list[0].supplier_name").value("华东原料供应商"));
    }

    @Test
    void startDateAndEndDateFormClosedInterval() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders")
                        .param("start_date", "2026-01-15")
                        .param("end_date", "2026-01-15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-001"));

        mockMvc.perform(get("/api/orders")
                        .param("start_date", "2026-01-16")
                        .param("end_date", "2026-01-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-004"));
    }

    @Test
    void keywordMatchesOrderNoOrName() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders")
                        .param("keyword", "ORD-LIST-002")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("南方包材单"));

        mockMvc.perform(get("/api/orders")
                        .param("keyword", "物流")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-004"));
    }

    @Test
    void statusFilterAndSupplierJoin() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders")
                        .param("status", "待审核")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].supplier_name").value("华东原料供应商"))
                .andExpect(jsonPath("$.data.list[1].supplier_name").value("北方物流供应商"));

        mockMvc.perform(get("/api/orders")
                        .param("supplier_id", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-002"));
    }

    @Test
    void paginationKeepsFilters() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders")
                        .param("status", "待审核")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-004"))
                .andExpect(jsonPath("$.data.list[0].status").value("待审核"));

        mockMvc.perform(get("/api/orders")
                        .param("keyword", "华东")
                        .param("supplier_id", "1")
                        .param("status", "待审核")
                        .param("start_date", "2026-01-01")
                        .param("end_date", "2026-01-31")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-LIST-001"));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }
}
