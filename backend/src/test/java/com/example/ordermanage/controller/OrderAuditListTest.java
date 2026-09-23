package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.OrderAudit;
import com.example.ordermanage.mapper.OrderAuditMapper;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
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
class OrderAuditListTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderAuditMapper orderAuditMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempOrderIds = new ArrayList<>();
    private final List<Long> tempAuditIds = new ArrayList<>();

    @BeforeEach
    void plant() {
        tempOrderIds.add(plant("ORD-AUD-L1", "待审核采购单", "待审核"));
        tempOrderIds.add(plant("ORD-AUD-L2", "待审核包材单", "待审核"));
        Long doneId = plant("ORD-AUD-L3", "已完成物流单", "已完成");
        tempOrderIds.add(doneId);
        // 插入顺序与 create_time 盪序，验证按最大 create_time 取最新一条
        tempAuditIds.add(plantAudit(doneId, "通过", "审核员乙", "同意",
                LocalDateTime.of(2026, 1, 3, 10, 0, 0)));
        tempAuditIds.add(plantAudit(doneId, "驳回", "审核员甲", "金额与合同不符",
                LocalDateTime.of(2026, 1, 2, 10, 0, 0)));
        Long rejectedId = plant("ORD-AUD-L4", "已驳回原料单", "已驳回");
        tempOrderIds.add(rejectedId);
        tempAuditIds.add(plantAudit(rejectedId, "驳回", "审核员甲", "需修改后重提",
                LocalDateTime.of(2026, 1, 1, 9, 0, 0)));
    }

    @AfterEach
    void cleanup() {
        for (Long id : tempAuditIds) {
            orderAuditMapper.deleteById(id);
        }
        tempAuditIds.clear();
        for (Long id : tempOrderIds) {
            orderMapper.deleteById(id);
        }
        tempOrderIds.clear();
        authService.resetFailCounts();
    }

    private Long plant(String orderNo, String name, String status) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName(name);
        order.setAmount(new BigDecimal("100.00"));
        order.setSupplierId(1L);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        return order.getId();
    }

    private Long plantAudit(Long orderId, String result, String auditor, String opinion,
                            LocalDateTime createTime) {
        OrderAudit audit = new OrderAudit();
        audit.setOrderId(orderId);
        audit.setResult(result);
        audit.setAuditor(auditor);
        audit.setOpinion(opinion);
        audit.setCreateTime(createTime);
        audit.setUpdateTime(createTime);
        orderAuditMapper.insert(audit);
        return audit.getId();
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
    void pendingFilterReturnsNullAuditFieldsAndPendingCount() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/order-audits")
                        .param("status", "待审核")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-AUD-L1"))
                .andExpect(jsonPath("$.data.list[0].status").value("待审核"))
                .andExpect(jsonPath("$.data.list[0].auditor").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.list[0].audit_time").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.list[0].opinion").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.pending_count").value(2))
                .andExpect(jsonPath("$.data.list[0].pending_count").value(2));
    }

    @Test
    void joinsLatestAuditRecordPerOrder() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/order-audits")
                        .param("status", "已完成")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-AUD-L3"))
                .andExpect(jsonPath("$.data.list[0].supplier_name").value("华东原料供应商"))
                .andExpect(jsonPath("$.data.list[0].auditor").value("审核员乙"))
                .andExpect(jsonPath("$.data.list[0].opinion").value("同意"))
                .andExpect(jsonPath("$.data.list[0].audit_time").isNotEmpty());

        mockMvc.perform(get("/api/order-audits")
                        .param("status", "已驳回")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-AUD-L4"))
                .andExpect(jsonPath("$.data.list[0].auditor").value("审核员甲"))
                .andExpect(jsonPath("$.data.list[0].opinion").value("需修改后重提"));
    }

    @Test
    void everyPageCarriesPendingCount() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/order-audits")
                        .param("status", "待审核")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-AUD-L2"))
                .andExpect(jsonPath("$.data.pending_count").value(2));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/order-audits"))
                .andExpect(status().isUnauthorized());
    }
}
