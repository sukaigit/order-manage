package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
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
class WorkbenchStatsTest {

    private static final String TITLE = "欢迎使用订单管理系统";
    private static final String TIPS =
            "可前往「订单管理」维护订单，「订单审核」处理审核，「统计报表」查看经营分析。";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private SupplierMapper supplierMapper;
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
        tempUserIds.add(createTempUser("wb_user", "工作台普通用户", 3L));
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

    private Long plant(String orderNo, String status) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName("工作台订单");
        order.setAmount(new BigDecimal("100.00"));
        order.setSupplierId(1L);
        order.setStatus(status);
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

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url);
    }

    private long countStatus(String status) {
        return orderMapper.selectCount(new QueryWrapper<Order>().eq("status", status));
    }

    @Test
    void statsRequiresLoginAndReturnsLiveCountsForAnyRole() throws Exception {
        long baseTotal = orderMapper.selectCount(null);
        long basePending = countStatus(OrderService.STATUS_PENDING);
        long baseDone = countStatus(OrderService.STATUS_FINISHED);
        long baseRejected = countStatus(OrderService.STATUS_REJECTED);
        long enabledSuppliers = supplierMapper.selectCount(
                new QueryWrapper<Supplier>().eq("status", "启用"));

        plant("ORD-WB-P1", OrderService.STATUS_PENDING);
        plant("ORD-WB-P2", OrderService.STATUS_PENDING);
        plant("ORD-WB-R1", OrderService.STATUS_REJECTED);
        plant("ORD-WB-D1", OrderService.STATUS_FINISHED);

        mockMvc.perform(get("/api/workbench/stats"))
                .andExpect(status().isUnauthorized());

        String token = login("wb_user");
        mockMvc.perform(get("/api/workbench/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total_orders").value((int) (baseTotal + 4)))
                .andExpect(jsonPath("$.data.pending_orders").value((int) (basePending + 2)))
                .andExpect(jsonPath("$.data.done_orders").value((int) (baseDone + 1)))
                .andExpect(jsonPath("$.data.rejected_orders").value((int) (baseRejected + 1)))
                .andExpect(jsonPath("$.data.active_suppliers").value((int) enabledSuppliers))
                .andExpect(jsonPath("$.data.welcome.title").value(TITLE))
                .andExpect(jsonPath("$.data.welcome.pending_count").value((int) (basePending + 2)))
                .andExpect(jsonPath("$.data.welcome.rejected_count").value((int) (baseRejected + 1)))
                .andExpect(jsonPath("$.data.welcome.tips").value(TIPS));
    }

    @Test
    void statsRecalculateOnEachCallWithoutCache() throws Exception {
        long baseTotal = orderMapper.selectCount(null);
        long basePending = countStatus(OrderService.STATUS_PENDING);
        String token = login("wb_user");

        mockMvc.perform(get("/api/workbench/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_orders").value((int) baseTotal))
                .andExpect(jsonPath("$.data.pending_orders").value((int) basePending));

        plant("ORD-WB-LIVE", OrderService.STATUS_PENDING);

        mockMvc.perform(get("/api/workbench/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_orders").value((int) (baseTotal + 1)))
                .andExpect(jsonPath("$.data.pending_orders").value((int) (basePending + 1)))
                .andExpect(jsonPath("$.data.welcome.pending_count").value((int) (basePending + 1)));
    }
}
