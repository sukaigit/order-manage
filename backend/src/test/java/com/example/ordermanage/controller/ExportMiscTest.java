package com.example.ordermanage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ExportMiscTest {

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
    private final List<Long> tempSupplierIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        authService.resetFailCounts();
        tempUserIds.add(createTempUser("exp_auditor", "导出审核员", 2L));
        tempUserIds.add(createTempUser("exp_user", "导出普通用户", 3L));
        tempSupplierIds.add(plantSupplier("SUP-EXP-1", "导出供应商甲", "启用"));
        plant("ORD-MEXP-1", " misc 甲单", 1L, OrderService.STATUS_PENDING,
                LocalDateTime.of(2026, 5, 1, 10, 0, 0));
        plant("ORD-MEXP-2", " misc 乙单", 2L, OrderService.STATUS_FINISHED,
                LocalDateTime.of(2026, 5, 2, 11, 0, 0));
        plant("ORD-MEXP-3", " misc 丙单", 1L, OrderService.STATUS_REJECTED,
                LocalDateTime.of(2026, 5, 3, 12, 0, 0));
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
        for (Long id : tempSupplierIds) {
            supplierMapper.deleteById(id);
        }
        tempSupplierIds.clear();
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

    private Long plantSupplier(String code, String name, String status) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setName(name);
        s.setContact("联系人");
        s.setPhone("13900000000");
        s.setAddress("地址");
        s.setStatus(status);
        s.setCreateTime(LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        s.setUpdateTime(LocalDateTime.of(2026, 5, 1, 9, 0, 0));
        supplierMapper.insert(s);
        return s.getId();
    }

    private void plant(String orderNo, String name, Long supplierId, String status,
                       LocalDateTime createTime) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName(name);
        order.setAmount(new BigDecimal("200.00"));
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

    @Test
    void supplierExportMatchesListHeadersAndRowCount() throws Exception {
        String token = login("admin");

        String listRaw = mockMvc.perform(get("/api/suppliers")
                        .param("keyword", "导出供应商")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long total = objectMapper.readTree(listRaw).get("data").get("total").asLong();
        assertEquals(1, total);

        MvcResult result = mockMvc.perform(get("/api/suppliers/export")
                        .param("keyword", "导出供应商")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        Matchers.matchesPattern("attachment; filename=suppliers_\\d{4}-\\d{2}-\\d{2}\\.xls")))
                .andExpect(header().string("Content-Type", "application/vnd.ms-excel"))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 0);
        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("供应商编号", header.getCell(0).getStringCellValue());
            assertEquals("名称", header.getCell(1).getStringCellValue());
            assertEquals("联系人", header.getCell(2).getStringCellValue());
            assertEquals("联系电话", header.getCell(3).getStringCellValue());
            assertEquals("地址", header.getCell(4).getStringCellValue());
            assertEquals("状态", header.getCell(5).getStringCellValue());
            assertEquals("创建时间", header.getCell(6).getStringCellValue());
            assertEquals(total, sheet.getLastRowNum());
            assertEquals("SUP-EXP-1", sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void reportExportMatchesDetailHeadersAndRowCount() throws Exception {
        String token = login("admin");

        String detailRaw = mockMvc.perform(get("/api/reports/detail")
                        .param("start_date", "2026-05-01")
                        .param("end_date", "2026-05-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long total = objectMapper.readTree(detailRaw).get("data").get("total").asLong();
        assertEquals(3, total);

        MvcResult result = mockMvc.perform(get("/api/reports/export")
                        .param("start_date", "2026-05-01")
                        .param("end_date", "2026-05-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        Matchers.matchesPattern("attachment; filename=reports_\\d{4}-\\d{2}-\\d{2}\\.xls")))
                .andExpect(header().string("Content-Type", "application/vnd.ms-excel"))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 0);
        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("订单编号", header.getCell(0).getStringCellValue());
            assertEquals("订单名称", header.getCell(1).getStringCellValue());
            assertEquals("金额", header.getCell(2).getStringCellValue());
            assertEquals("供应商", header.getCell(3).getStringCellValue());
            assertEquals("状态", header.getCell(4).getStringCellValue());
            assertEquals("创建时间", header.getCell(5).getStringCellValue());
            assertEquals(6, header.getLastCellNum(), "report export has no remark column");
            assertEquals(total, sheet.getLastRowNum());
            assertEquals("ORD-MEXP-1", sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }

    @Test
    void reportExportForbiddenForNonAdmin() throws Exception {
        String auditorToken = login("exp_auditor");
        mockMvc.perform(get("/api/reports/export")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        String userToken = login("exp_user");
        mockMvc.perform(get("/api/reports/export")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
