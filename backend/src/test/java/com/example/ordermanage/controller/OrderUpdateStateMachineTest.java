package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class OrderUpdateStateMachineTest {

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

    private Long plant(String orderNo, String status) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName("状态机订单");
        order.setAmount(new BigDecimal("50.00"));
        order.setSupplierId(1L);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
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
    void pendingOrderEditStaysPendingAndIgnoresOrderNo() throws Exception {
        String token = login();
        Long id = plant("ORD-UP-PEND", "待审核");

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"HACKED-NO\",\"name\":\"改名后的待审核\","
                                + "\"amount\":200.00,\"supplier_id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order_no").value("ORD-UP-PEND"))
                .andExpect(jsonPath("$.data.name").value("改名后的待审核"))
                .andExpect(jsonPath("$.data.amount").value(200.00))
                .andExpect(jsonPath("$.data.status").value("待审核"))
                .andExpect(jsonPath("$.data.supplier_name").value("华东原料供应商"));

        Order saved = orderMapper.selectById(id);
        org.junit.jupiter.api.Assertions.assertEquals("ORD-UP-PEND", saved.getOrderNo());
        org.junit.jupiter.api.Assertions.assertEquals("待审核", saved.getStatus());
    }

    @Test
    void rejectedOrderEditReturnsToPending() throws Exception {
        String token = login();
        Long id = plant("ORD-UP-REJ", "已驳回");

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-UP-REJ\",\"name\":\"驳回后修改\","
                                + "\"amount\":300.00,\"supplier_id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("待审核"));

        org.junit.jupiter.api.Assertions.assertEquals("待审核",
                orderMapper.selectById(id).getStatus());
    }

    @Test
    void finishedOrderEditReturns422() throws Exception {
        String token = login();
        Long id = plant("ORD-UP-DONE", "已完成");

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-UP-DONE\",\"name\":\"完成单改名\","
                                + "\"amount\":1,\"supplier_id\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("已完成订单不可编辑"));

        org.junit.jupiter.api.Assertions.assertEquals("已完成",
                orderMapper.selectById(id).getStatus());
    }

    @Test
    void negativeAmountEditReturns400() throws Exception {
        String token = login();
        Long id = plant("ORD-UP-NEG", "待审核");

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-UP-NEG\",\"name\":\"负金额编辑\","
                                + "\"amount\":-5,\"supplier_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("金额不能为负数"));
    }

    @Test
    void missingOrderReturns404() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/orders/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"X\",\"name\":\"X\",\"amount\":1,\"supplier_id\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
