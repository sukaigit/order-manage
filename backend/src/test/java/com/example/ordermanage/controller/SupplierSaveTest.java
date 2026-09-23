package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.mapper.SupplierMapper;
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
class SupplierSaveTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempSupplierIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
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

    @Test
    void createMissingRequiredFieldsReturns400() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入供应商编号"));

        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-901\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入供应商名称"));

        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-901\",\"name\":\"测试供应商\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入联系人"));

        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-901\",\"name\":\"测试供应商\",\"contact\":\"赵六\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入联系电话"));
    }

    @Test
    void createDuplicateCodeReturns409() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-001\",\"name\":\"冒名供应商\",\"contact\":\"某\",\"phone\":\"1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("供应商编号已存在"));
    }

    @Test
    void createSucceedsWithStatusDefaultEnabled() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-901\",\"name\":\"西南运输供应商\",\"contact\":\"赵六\",\"phone\":\"13800000004\",\"address\":\"成都市武侯区\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SUP-901"))
                .andExpect(jsonPath("$.data.name").value("西南运输供应商"))
                .andExpect(jsonPath("$.data.contact").value("赵六"))
                .andExpect(jsonPath("$.data.phone").value("13800000004"))
                .andExpect(jsonPath("$.data.address").value("成都市武侯区"))
                .andExpect(jsonPath("$.data.status").value("启用"))
                .andExpect(jsonPath("$.data.create_time").exists())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempSupplierIds.add(id);
    }

    @Test
    void updateSavesBusinessFieldsButIgnoresCodeChange() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-902\",\"name\":\"待改供应商\",\"contact\":\"钱七\",\"phone\":\"13800000005\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempSupplierIds.add(id);

        mockMvc.perform(put("/api/suppliers/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-999\",\"name\":\"已改供应商\",\"contact\":\"孙八\",\"phone\":\"13800000006\",\"address\":\"杭州市西湖区\",\"status\":\"停用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.code").value("SUP-902"))
                .andExpect(jsonPath("$.data.name").value("已改供应商"))
                .andExpect(jsonPath("$.data.contact").value("孙八"))
                .andExpect(jsonPath("$.data.phone").value("13800000006"))
                .andExpect(jsonPath("$.data.address").value("杭州市西湖区"))
                .andExpect(jsonPath("$.data.status").value("停用"));

        mockMvc.perform(put("/api/suppliers/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-902\",\"name\":\"再改供应商\",\"contact\":\"孙八\",\"phone\":\"13800000006\",\"status\":\"启用\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("SUP-902"))
                .andExpect(jsonPath("$.data.name").value("再改供应商"))
                .andExpect(jsonPath("$.data.status").value("启用"));
    }

    @Test
    void updateMissingRequiredFieldsReturns400() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-903\",\"name\":\"校验供应商\",\"contact\":\"周九\",\"phone\":\"13800000007\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempSupplierIds.add(id);

        mockMvc.perform(put("/api/suppliers/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"SUP-903\",\"name\":\"\",\"contact\":\"周九\",\"phone\":\"13800000007\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入供应商名称"));

        mockMvc.perform(put("/api/suppliers/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"校验供应商\",\"contact\":\"周九\",\"phone\":\"13800000007\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入供应商编号"));
    }

    @Test
    void updateMissingSupplierReturns404() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/suppliers/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X1\",\"name\":\"x\",\"contact\":\"x\",\"phone\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
