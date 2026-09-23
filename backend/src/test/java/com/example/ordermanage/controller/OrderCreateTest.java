package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.Order;
import com.example.ordermanage.mapper.OrderMapper;
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
class OrderCreateTest {

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

    private long createOrder(String token, String orderNo) throws Exception {
        String raw = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"" + orderNo + "\",\"name\":\"测试订单\","
                                + "\"amount\":100.00,\"supplier_id\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempOrderIds.add(id);
        return id;
    }

    @Test
    void createReturnsPendingOrderWithSupplierName() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-001\",\"name\":\"华东原料采购单\","
                                + "\"amount\":12500.00,\"supplier_id\":1,\"remark\":\"季度补货\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order_no").value("ORD-CR-001"))
                .andExpect(jsonPath("$.data.name").value("华东原料采购单"))
                .andExpect(jsonPath("$.data.amount").value(12500.00))
                .andExpect(jsonPath("$.data.supplier_id").value(1))
                .andExpect(jsonPath("$.data.supplier_name").value("华东原料供应商"))
                .andExpect(jsonPath("$.data.status").value("待审核"))
                .andExpect(jsonPath("$.data.remark").value("季度补货"))
                .andExpect(jsonPath("$.data.create_time").exists())
                .andReturn().getResponse().getContentAsString();
        tempOrderIds.add(objectMapper.readTree(raw).get("data").get("id").asLong());
    }

    @Test
    void missingRequiredFieldsReturn400() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"无编号\",\"amount\":1,\"supplier_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-M2\",\"amount\":1,\"supplier_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-M3\",\"name\":\"无金额\",\"supplier_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-M4\",\"name\":\"无供应商\",\"amount\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void negativeAmountReturns400() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-NEG\",\"name\":\"负金额\","
                                + "\"amount\":-1,\"supplier_id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("金额不能为负数"));
    }

    @Test
    void duplicateOrderNoReturns409() throws Exception {
        String token = login();
        createOrder(token, "ORD-CR-DUP");
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-DUP\",\"name\":\"重复编号\","
                                + "\"amount\":1,\"supplier_id\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void disabledSupplierReturns422() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-SUP\",\"name\":\"停用供应商\","
                                + "\"amount\":1,\"supplier_id\":3}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该供应商已停用，不可选择"));
    }

    @Test
    void missingSupplierReturns422() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-NSUP\",\"name\":\"不存在供应商\","
                                + "\"amount\":1,\"supplier_id\":999999}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该供应商已停用，不可选择"));
    }

    @Test
    void serverIgnoresClientStatusAndCreateTime() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-CR-SRV\",\"name\":\"服务端字段\","
                                + "\"amount\":1,\"supplier_id\":1,"
                                + "\"status\":\"已完成\",\"create_time\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("待审核"))
                .andExpect(jsonPath("$.data.create_time").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("2020"))))
                .andReturn().getResponse().getContentAsString();
        tempOrderIds.add(objectMapper.readTree(raw).get("data").get("id").asLong());
    }
}
