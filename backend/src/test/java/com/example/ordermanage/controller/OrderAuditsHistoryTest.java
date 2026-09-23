package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class OrderAuditsHistoryTest {

    private static final String AUDITOR = "审核员丁";

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
    private final List<Long> tempUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        authService.resetFailCounts();
        tempUserIds.add(createTempUser("auditor_h", AUDITOR, 2L));
        tempUserIds.add(createTempUser("user_h", "普通用户甲", 3L));
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
        for (Long id : tempUserIds) {
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
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
        order.setName("审核历史订单");
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

    private void trackAudits(Long orderId) {
        orderAuditMapper.selectList(new QueryWrapper<OrderAudit>().eq("order_id", orderId))
                .forEach(a -> tempAuditIds.add(a.getId()));
    }

    @Test
    void rejectEditBackToPendingThenPassYieldsTwoRowsAsc() throws Exception {
        String auditorToken = login("auditor_h");
        String adminToken = login("admin");
        Long id = plantPending("ORD-AUD-H1");

        mockMvc.perform(post("/api/order-audits/" + id + "/reject")
                        .header("Authorization", "Bearer " + auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"金额与合同不符\"}"))
                .andExpect(status().isOk());
        trackAudits(id);

        mockMvc.perform(put("/api/orders/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"order_no\":\"ORD-AUD-H1\",\"name\":\"修正后订单\","
                                + "\"amount\":200.00,\"supplier_id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("待审核"));

        mockMvc.perform(post("/api/order-audits/" + id + "/pass")
                        .header("Authorization", "Bearer " + auditorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"复核通过\"}"))
                .andExpect(status().isOk());
        trackAudits(id);

        mockMvc.perform(get("/api/orders/" + id + "/audits")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].order_id").value(id))
                .andExpect(jsonPath("$.data[0].result").value("驳回"))
                .andExpect(jsonPath("$.data[0].auditor").value(AUDITOR))
                .andExpect(jsonPath("$.data[0].opinion").value("金额与合同不符"))
                .andExpect(jsonPath("$.data[0].create_time").isNotEmpty())
                .andExpect(jsonPath("$.data[1].order_id").value(id))
                .andExpect(jsonPath("$.data[1].result").value("通过"))
                .andExpect(jsonPath("$.data[1].auditor").value(AUDITOR))
                .andExpect(jsonPath("$.data[1].opinion").value("复核通过"))
                .andExpect(jsonPath("$.data[1].create_time").isNotEmpty());
    }

    @Test
    void historyRequiresAuditMenuAndOrderQuery() throws Exception {
        String normalToken = login("user_h");
        String adminToken = login("admin");
        Long id = plantPending("ORD-AUD-H2");

        mockMvc.perform(get("/api/orders/" + id + "/audits")
                        .header("Authorization", "Bearer " + normalToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(get("/api/orders/" + id + "/audits")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
