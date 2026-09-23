package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.Order;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
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
class SupplierDeleteStatusTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempSupplierIds = new ArrayList<>();
    private final List<Long> tempOrderIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempOrderIds) {
            orderMapper.deleteById(id);
        }
        tempOrderIds.clear();
        for (Long id : tempSupplierIds) {
            supplierMapper.deleteById(id);
        }
        tempSupplierIds.clear();
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

    private long createTempSupplier(String token, String code) throws Exception {
        String raw = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"临时供应商\",\"contact\":\"临某\",\"phone\":\"13900000000\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempSupplierIds.add(id);
        return id;
    }

    private void plantOrderReferencing(long supplierId) {
        Order order = new Order();
        order.setOrderNo("TEMP-" + System.nanoTime());
        order.setName("临时订单");
        order.setAmount(new BigDecimal("100.00"));
        order.setSupplierId(supplierId);
        order.setStatus("待审核");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        tempOrderIds.add(order.getId());
    }

    @Test
    void deleteReferencedSupplierReturns422() throws Exception {
        String token = login();
        plantOrderReferencing(1L);

        mockMvc.perform(delete("/api/suppliers/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该供应商已被订单引用，无法删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(supplierMapper.selectById(1L));
    }

    @Test
    void deleteUnreferencedSupplierSucceeds() throws Exception {
        String token = login();
        long id = createTempSupplier(token, "SUP-DEL");

        mockMvc.perform(delete("/api/suppliers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(supplierMapper.selectById(id));
        tempSupplierIds.remove(Long.valueOf(id));
    }

    @Test
    void deleteMissingSupplierReturns404() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/suppliers/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateStatusReturnsIdAndStatus() throws Exception {
        String token = login();
        long id = createTempSupplier(token, "SUP-STA");

        mockMvc.perform(put("/api/suppliers/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"停用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("停用"));
        org.junit.jupiter.api.Assertions.assertEquals("停用",
                supplierMapper.selectById(id).getStatus());

        mockMvc.perform(put("/api/suppliers/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"启用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("启用"));
        org.junit.jupiter.api.Assertions.assertEquals("启用",
                supplierMapper.selectById(id).getStatus());
    }

    @Test
    void updateInvalidStatusReturns400() throws Exception {
        String token = login();
        long id = createTempSupplier(token, "SUP-BAD");

        mockMvc.perform(put("/api/suppliers/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"锁定\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("状态只能为启用或停用"));
    }

    @Test
    void updateStatusMissingSupplierReturns404() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/suppliers/999999/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"停用\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void statusDisabledSupplierExcludedFromOptions() throws Exception {
        String token = login();
        long id = createTempSupplier(token, "SUP-OPT");

        mockMvc.perform(put("/api/suppliers/" + id + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"停用\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/suppliers/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
