package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.OrderAudit;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.OrderAuditMapper;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
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
class OrderAuditPassRejectTest {

    private static final String AUDITOR = "审核员丙";

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
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempOrderIds = new ArrayList<>();
    private final List<Long> tempAuditIds = new ArrayList<>();
    private Long tempUserId;

    @BeforeEach
    void setup() {
        authService.resetFailCounts();
        tempUserId = createTempUser("auditor_pr", AUDITOR, 2L);
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
        if (tempUserId != null) {
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", tempUserId));
            userMapper.deleteById(tempUserId);
            tempUserId = null;
        }
        authService.resetFailCounts();
    }

    private Long createTempUser(String username, String realName, Long roleId) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(AuthService.md5("123456"));
        u.setRealName(realName);
        u.setStatus("启用");
        u.setDepartmentId(1L);
        u.setOrganizationId(1L);
        userMapper.insert(u);
        UserRole link = new UserRole();
        link.setUserId(u.getId());
        link.setRoleId(roleId);
        userRoleMapper.insert(link);
        return u.getId();
    }

    private Long plantPending(String orderNo) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName("待审核订单");
        order.setAmount(new BigDecimal("100.00"));
        order.setSupplierId(1L);
        order.setStatus("待审核");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        tempOrderIds.add(order.getId());
        return order.getId();
    }

    private String login(String username) throws Exception {
        Map<String, String> cap = captchaService.create("ABCD");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", "123456",
                "captcha", "ABCD", "captcha_id", cap.get("captcha_id")));
        String raw = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(raw).get("data").get("token").asText();
    }

    private long auditCount(Long orderId) {
        return orderAuditMapper.selectCount(new QueryWrapper<OrderAudit>().eq("order_id", orderId));
    }

    @Test
    void passByAuditorFinishesOrderAndWritesAuditRow() throws Exception {
        String token = login("auditor_pr");
        Long id = plantPending("ORD-AUD-P1");

        mockMvc.perform(post("/api/order-audits/" + id + "/pass")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"同意通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.order_id").value(id))
                .andExpect(jsonPath("$.data.status").value("已完成"))
                .andExpect(jsonPath("$.data.audit.result").value("通过"))
                .andExpect(jsonPath("$.data.audit.auditor").value(AUDITOR))
                .andExpect(jsonPath("$.data.audit.opinion").value("同意通过"))
                .andExpect(jsonPath("$.data.audit.audit_time").isNotEmpty());

        Order saved = orderMapper.selectById(id);
        org.junit.jupiter.api.Assertions.assertEquals("已完成", saved.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(1L, auditCount(id));
        OrderAudit row = orderAuditMapper.selectOne(
                new QueryWrapper<OrderAudit>().eq("order_id", id));
        org.junit.jupiter.api.Assertions.assertEquals("通过", row.getResult());
        org.junit.jupiter.api.Assertions.assertEquals(AUDITOR, row.getAuditor());
        org.junit.jupiter.api.Assertions.assertEquals("同意通过", row.getOpinion());
        tempAuditIds.add(row.getId());
    }

    @Test
    void rejectByAuditorStoresOpinionAndRejectsOrder() throws Exception {
        String token = login("auditor_pr");
        Long id = plantPending("ORD-AUD-R1");

        mockMvc.perform(post("/api/order-audits/" + id + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"金额与合同不符，请修改\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_id").value(id))
                .andExpect(jsonPath("$.data.status").value("已驳回"))
                .andExpect(jsonPath("$.data.audit.result").value("驳回"))
                .andExpect(jsonPath("$.data.audit.auditor").value(AUDITOR))
                .andExpect(jsonPath("$.data.audit.opinion").value("金额与合同不符，请修改"))
                .andExpect(jsonPath("$.data.audit.audit_time").isNotEmpty());

        Order saved = orderMapper.selectById(id);
        org.junit.jupiter.api.Assertions.assertEquals("已驳回", saved.getStatus());
        OrderAudit row = orderAuditMapper.selectOne(
                new QueryWrapper<OrderAudit>().eq("order_id", id));
        org.junit.jupiter.api.Assertions.assertEquals("驳回", row.getResult());
        org.junit.jupiter.api.Assertions.assertEquals("金额与合同不符，请修改", row.getOpinion());
        tempAuditIds.add(row.getId());
    }

    @Test
    void rejectEmptyOpinionReturns400() throws Exception {
        String token = login("auditor_pr");
        Long id = plantPending("ORD-AUD-R2");

        mockMvc.perform(post("/api/order-audits/" + id + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("驳回时审核意见必填"));

        org.junit.jupiter.api.Assertions.assertEquals("待审核",
                orderMapper.selectById(id).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0L, auditCount(id));
    }

    @Test
    void secondAuditOnNonPendingOrderReturns422() throws Exception {
        String token = login("auditor_pr");
        Long id = plantPending("ORD-AUD-P2");

        mockMvc.perform(post("/api/order-audits/" + id + "/pass")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"同意\"}"))
                .andExpect(status().isOk());
        OrderAudit row = orderAuditMapper.selectOne(
                new QueryWrapper<OrderAudit>().eq("order_id", id));
        tempAuditIds.add(row.getId());

        mockMvc.perform(post("/api/order-audits/" + id + "/pass")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"再审\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("仅待审核订单可审核"));

        org.junit.jupiter.api.Assertions.assertEquals(1L, auditCount(id));
    }

    @Test
    void adminDirectCallReturns403() throws Exception {
        String adminToken = login("admin");
        Long id = plantPending("ORD-AUD-A1");

        mockMvc.perform(post("/api/order-audits/" + id + "/pass")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"管理员直接通过\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/order-audits/" + id + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"管理员直接驳回\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        org.junit.jupiter.api.Assertions.assertEquals("待审核",
                orderMapper.selectById(id).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0L, auditCount(id));
    }

    @Test
    void rejectOpinionOver255Returns400() throws Exception {
        String token = login("auditor_pr");
        Long id = plantPending("ORD-AUD-R3");
        String opinion = "驳".repeat(256);

        mockMvc.perform(post("/api/order-audits/" + id + "/reject")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("opinion", opinion))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        org.junit.jupiter.api.Assertions.assertEquals("待审核",
                orderMapper.selectById(id).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0L, auditCount(id));
    }
}
