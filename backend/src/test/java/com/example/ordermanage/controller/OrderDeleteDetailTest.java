package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderDeleteDetailTest {

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

    @AfterEach
    void cleanup() {
        for (Long id : tempOrderIds) {
            orderMapper.deleteById(id);
        }
        tempOrderIds.clear();
        authService.resetFailCounts();
    }

    private Long plant(String orderNo, String status, Long supplierId) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName("删除详情订单");
        order.setAmount(new BigDecimal("88.00"));
        order.setSupplierId(supplierId);
        order.setStatus(status);
        order.setRemark("测试备注");
        order.setCreateTime(LocalDateTime.of(2026, 4, 1, 8, 0, 0));
        order.setUpdateTime(LocalDateTime.of(2026, 4, 1, 8, 0, 0));
        orderMapper.insert(order);
        tempOrderIds.add(order.getId());
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
    void deletePendingAndRejectedSucceeds() throws Exception {
        String token = login();
        Long pending = plant("ORD-DEL-P", "待审核", 1L);
        Long rejected = plant("ORD-DEL-R", "已驳回", 2L);

        mockMvc.perform(delete("/api/orders/" + pending)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(orderMapper.selectById(pending));
        tempOrderIds.remove(pending);

        mockMvc.perform(delete("/api/orders/" + rejected)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(orderMapper.selectById(rejected));
        tempOrderIds.remove(rejected);
    }

    @Test
    void deleteFinishedOrderReturns422() throws Exception {
        String token = login();
        Long id = plant("ORD-DEL-D", "已完成", 1L);

        mockMvc.perform(delete("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("已完成订单不可删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(orderMapper.selectById(id));
    }

    @Test
    void deleteMissingOrderReturns404() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/orders/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void detailReturnsFullObjectWithSupplierName() throws Exception {
        String token = login();
        Long id = plant("ORD-DET-001", "待审核", 2L);

        mockMvc.perform(get("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.order_no").value("ORD-DET-001"))
                .andExpect(jsonPath("$.data.name").value("删除详情订单"))
                .andExpect(jsonPath("$.data.amount").value(88.00))
                .andExpect(jsonPath("$.data.supplier_id").value(2))
                .andExpect(jsonPath("$.data.supplier_name").value("南方包装供应商"))
                .andExpect(jsonPath("$.data.status").value("待审核"))
                .andExpect(jsonPath("$.data.remark").value("测试备注"))
                .andExpect(jsonPath("$.data.create_time").exists())
                .andExpect(jsonPath("$.data.update_time").exists());
    }

    @Test
    void detailMissingOrderReturns404() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void detailWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isUnauthorized());
    }
}
