package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.entity.Order;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class OrderExportTest {

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

    @BeforeEach
    void plant() {
        tempOrderIds.add(plant("ORD-EXP-001", "导出甲单", 1L, "待审核",
                LocalDateTime.of(2026, 4, 1, 10, 0, 0)));
        tempOrderIds.add(plant("ORD-EXP-002", "导出乙单", 2L, "已完成",
                LocalDateTime.of(2026, 4, 2, 11, 0, 0)));
        tempOrderIds.add(plant("ORD-EXP-003", "导出丙单", 1L, "已驳回",
                LocalDateTime.of(2026, 4, 3, 12, 0, 0)));
    }

    @AfterEach
    void cleanup() {
        for (Long id : tempOrderIds) {
            orderMapper.deleteById(id);
        }
        tempOrderIds.clear();
        authService.resetFailCounts();
    }

    private Long plant(String orderNo, String name, Long supplierId, String status,
                       LocalDateTime createTime) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setName(name);
        order.setAmount(new BigDecimal("100.00"));
        order.setSupplierId(supplierId);
        order.setStatus(status);
        order.setRemark("备注");
        order.setCreateTime(createTime);
        order.setUpdateTime(createTime);
        orderMapper.insert(order);
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
    void exportReturnsXlsWithHeadersAndRowCountMatchingListTotal() throws Exception {
        String token = login();

        String listRaw = mockMvc.perform(get("/api/orders")
                        .param("keyword", "导出")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long total = objectMapper.readTree(listRaw).get("data").get("total").asLong();
        assertEquals(3, total);

        MvcResult result = mockMvc.perform(get("/api/orders/export")
                        .param("keyword", "导出")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment;")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern(
                                "attachment; filename=orders_\\d{4}-\\d{2}-\\d{2}\\.xls")))
                .andExpect(header().string("Content-Type", "application/vnd.ms-excel"))
                .andReturn();

        byte[] bytes = result.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 0, "export body must not be empty");

        try (Workbook wb = new HSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("订单编号", header.getCell(0).getStringCellValue());
            assertEquals("订单名称", header.getCell(1).getStringCellValue());
            assertEquals("金额", header.getCell(2).getStringCellValue());
            assertEquals("供应商", header.getCell(3).getStringCellValue());
            assertEquals("状态", header.getCell(4).getStringCellValue());
            assertEquals("创建时间", header.getCell(5).getStringCellValue());
            assertEquals("备注", header.getCell(6).getStringCellValue());
            assertEquals(total, sheet.getLastRowNum(),
                    "POI data row count must equal list total");
            assertEquals("ORD-EXP-001", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("导出甲单", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("华东原料供应商", sheet.getRow(1).getCell(3).getStringCellValue());
        }
    }

    @Test
    void exportWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/orders/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportIgnoresPageAndPageSize() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/orders/export")
                        .param("keyword", "导出")
                        .param("page", "2")
                        .param("pageSize", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment;")));
    }
}
