package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.example.ordermanage.service.OrderService;
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
class ReportDetailTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempOrderIds = new ArrayList<>();
    private final List<Long> tempUserIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        authService.resetFailCounts();
        tempUserIds.add(createTempUser("dtl_auditor", "明细审核员", 2L));
        tempUserIds.add(createTempUser("dtl_user", "明细普通用户", 3L));
        plant("ORD-DTL-1", "华东订单A", 1L, OrderService.STATUS_PENDING,
                new BigDecimal("100.00"), LocalDateTime.of(2026, 3, 15, 10, 0, 0));
        plant("ORD-DTL-2", "华东订单B", 1L, OrderService.STATUS_REJECTED,
                new BigDecimal("300.00"), LocalDateTime.of(2026, 3, 15, 11, 0, 0));
        plant("ORD-DTL-3", "南方订单C", 2L, OrderService.STATUS_FINISHED,
                new BigDecimal("600.00"), LocalDateTime.of(2026, 3, 15, 12, 0, 0));
        plant("ORD-DTL-4", "南方订单D", 2L, OrderService.STATUS_FINISHED,
                new BigDecimal("1000.00"), LocalDateTime.of(2026, 3, 15, 13, 0, 0));
    }

    @AfterEach
    void cleanup() {
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

    private void plant(String orderNo, String name, Long supplierId, String status,
                       BigDecimal amount, LocalDateTime createTime) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName(name);
        order.setAmount(amount);
        order.setSupplierId(supplierId);
        order.setStatus(status);
        order.setCreateTime(createTime);
        order.setUpdateTime(createTime);
        orderMapper.insert(order);
        tempOrderIds.add(order.getId());
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

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url);
    }

    @Test
    void detailReturnsOrderListFields() throws Exception {
        String adminToken = login("admin");
        mockMvc.perform(get("/api/reports/detail")
                        .param("start_date", "2026-03-15")
                        .param("end_date", "2026-03-15")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.list.length()").value(4))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-DTL-1"))
                .andExpect(jsonPath("$.data.list[0].name").value("华东订单A"))
                .andExpect(jsonPath("$.data.list[0].amount").value(100.00))
                .andExpect(jsonPath("$.data.list[0].supplier_name").value("华东原料供应商"))
                .andExpect(jsonPath("$.data.list[0].status").value("待审核"))
                .andExpect(jsonPath("$.data.list[0].create_time").isNotEmpty())
                .andExpect(jsonPath("$.data.list[3].order_no").value("ORD-DTL-4"));
    }

    @Test
    void detailPaginatesWithPageAndPageSize() throws Exception {
        String adminToken = login("admin");
        mockMvc.perform(get("/api/reports/detail")
                        .param("start_date", "2026-03-15")
                        .param("end_date", "2026-03-15")
                        .param("page", "2")
                        .param("pageSize", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-DTL-3"))
                .andExpect(jsonPath("$.data.list[1].order_no").value("ORD-DTL-4"));
    }

    @Test
    void detailAppliesSupplierFilter() throws Exception {
        String adminToken = login("admin");
        mockMvc.perform(get("/api/reports/detail")
                        .param("start_date", "2026-03-15")
                        .param("end_date", "2026-03-15")
                        .param("supplier_id", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].order_no").value("ORD-DTL-1"))
                .andExpect(jsonPath("$.data.list[0].supplier_name").value("华东原料供应商"))
                .andExpect(jsonPath("$.data.list[1].order_no").value("ORD-DTL-2"));
    }

    @Test
    void detailEmptyWindowReturnsEmptyList() throws Exception {
        String adminToken = login("admin");
        mockMvc.perform(get("/api/reports/detail")
                        .param("start_date", "2099-01-01")
                        .param("end_date", "2099-12-31")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(0));
    }

    @Test
    void detailForbiddenForNonAdmin() throws Exception {
        String auditorToken = login("dtl_auditor");
        mockMvc.perform(get("/api/reports/detail")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        String userToken = login("dtl_user");
        mockMvc.perform(get("/api/reports/detail")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
