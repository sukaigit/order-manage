package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.mapper.DepartmentMapper;
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
class DepartmentCrudTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempDeptIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempDeptIds) {
            departmentMapper.deleteById(id);
        }
        tempDeptIds.clear();
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
    void listReturnsInitialSingleDepartment() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("DEPT-000"))
                .andExpect(jsonPath("$.data.list[0].name").value("总部"))
                .andExpect(jsonPath("$.data.list[0].remark").value("默认根部门"))
                .andExpect(jsonPath("$.data.list[0].create_time").exists());
    }

    @Test
    void keywordFilterWorks() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-101\",\"name\":\"采购部\",\"remark\":\"买买买\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DEPT-101"))
                .andExpect(jsonPath("$.data.name").value("采购部"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempDeptIds.add(id);

        mockMvc.perform(get("/api/departments")
                        .param("keyword", "采购")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("DEPT-101"));

        mockMvc.perform(get("/api/departments")
                        .param("keyword", "DEPT-")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/departments")
                        .param("keyword", "总部")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("DEPT-000"));
    }

    @Test
    void createMissingFieldsReturns400() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入部门编号"));

        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-200\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入部门名称"));
    }

    @Test
    void createDuplicateCodeReturns409() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-000\",\"name\":\"冒名部门\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("部门编号已存在"));
    }

    @Test
    void createThenDeleteUnreferencedSucceeds() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-301\",\"name\":\"临时部\",\"remark\":\"r\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DEPT-301"))
                .andExpect(jsonPath("$.data.create_time").exists())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempDeptIds.add(id);

        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(delete("/api/departments/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(departmentMapper.selectById(id));
        tempDeptIds.remove(id);
    }

    @Test
    void updateSavesNameAndRemarkAndRejectsDuplicateCode() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/departments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-401\",\"name\":\"待改部\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempDeptIds.add(id);

        mockMvc.perform(put("/api/departments/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-401\",\"name\":\"已改部\",\"remark\":\"改备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("已改部"))
                .andExpect(jsonPath("$.data.remark").value("改备注"))
                .andExpect(jsonPath("$.data.code").value("DEPT-401"));

        mockMvc.perform(put("/api/departments/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-000\",\"name\":\"已改部\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("部门编号已存在"));

        mockMvc.perform(put("/api/departments/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEPT-401\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入部门名称"));

        mockMvc.perform(put("/api/departments/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X1\",\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReferencedDepartmentReturns422() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/departments/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该部门已被用户引用，无法删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(departmentMapper.selectById(1L));
    }

    @Test
    void deleteMissingDepartmentReturns404() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/departments/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
