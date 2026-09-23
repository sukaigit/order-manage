package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.MenuFunction;
import com.example.ordermanage.entity.RoleFunction;
import com.example.ordermanage.mapper.FunctionMapper;
import com.example.ordermanage.mapper.MenuFunctionMapper;
import com.example.ordermanage.mapper.RoleFunctionMapper;
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
class FunctionCrudTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private FunctionMapper functionMapper;
    @Autowired
    private MenuFunctionMapper menuFunctionMapper;
    @Autowired
    private RoleFunctionMapper roleFunctionMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempFuncIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempFuncIds) {
            menuFunctionMapper.delete(new QueryWrapper<MenuFunction>().eq("function_id", id));
            roleFunctionMapper.delete(new QueryWrapper<RoleFunction>().eq("function_id", id));
            functionMapper.deleteById(id);
        }
        tempFuncIds.clear();
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

    private long track(String raw) throws Exception {
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempFuncIds.add(id);
        return id;
    }

    @Test
    void listReturnsSeedAndFilters() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/functions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(32))
                .andExpect(jsonPath("$.data.list[0].code").value("FUNC_ORDER_QUERY"))
                .andExpect(jsonPath("$.data.list[0].menu_id").value(3))
                .andExpect(jsonPath("$.data.list[0].menu_name").value("订单列表"))
                .andExpect(jsonPath("$.data.list[0].perm").value("order:query"))
                .andExpect(jsonPath("$.data.list[0].create_time").exists());

        mockMvc.perform(get("/api/functions")
                        .param("keyword", "订单")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5));

        mockMvc.perform(get("/api/functions")
                        .param("menu_id", "13")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].menu_name").value("部门管理"));

        mockMvc.perform(get("/api/functions")
                        .param("perm", "order:")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5));
    }

    @Test
    void createValidatesPermFormatAndDuplicates() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"无冒号\",\"menu_id\":3,\"perm\":\"noColon\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("权限标识格式必须为域:操作"));

        mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"双冒号\",\"menu_id\":3,\"perm\":\"a:b:c\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("权限标识格式必须为域:操作"));

        mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"menu_id\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入权限标识"));

        mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"perm\":\"order:query\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择所属菜单"));

        mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"重复权限\",\"menu_id\":3,\"perm\":\"order:query\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"menu_id\":999999,\"perm\":\"x:y\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("菜单不存在"));
    }

    @Test
    void createUpsertsMenuFunctionLink() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"部门导出\",\"menu_id\":13,"
                                + "\"perm\":\"dept:export\",\"remark\":\"导出部门\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("FUNC_DEPT_EXPORT"))
                .andExpect(jsonPath("$.data.menu_id").value(13))
                .andExpect(jsonPath("$.data.menu_name").value("部门管理"))
                .andExpect(jsonPath("$.data.perm").value("dept:export"))
                .andExpect(jsonPath("$.data.create_time").exists())
                .andReturn().getResponse().getContentAsString();
        long id = track(raw);

        org.junit.jupiter.api.Assertions.assertEquals(1, menuFunctionMapper.selectCount(
                new QueryWrapper<MenuFunction>().eq("function_id", id).eq("menu_id", 13)));
    }

    @Test
    void updateSyncsMenuFunctionAndKeepsCode() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"临时功能\",\"menu_id\":13,\"perm\":\"dept:import\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = track(raw);
        String originalCode = objectMapper.readTree(raw).get("data").get("code").asText();

        mockMvc.perform(put("/api/functions/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"临时功能改\",\"menu_id\":15,"
                                + "\"perm\":\"dept:import\",\"code\":\"HACKED\",\"remark\":\"r\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("临时功能改"))
                .andExpect(jsonPath("$.data.menu_id").value(15))
                .andExpect(jsonPath("$.data.menu_name").value("菜单管理"))
                .andExpect(jsonPath("$.data.code").value(originalCode));
        org.junit.jupiter.api.Assertions.assertEquals(originalCode,
                functionMapper.selectById(id).getCode());
        org.junit.jupiter.api.Assertions.assertEquals(0, menuFunctionMapper.selectCount(
                new QueryWrapper<MenuFunction>().eq("function_id", id).eq("menu_id", 13)));
        org.junit.jupiter.api.Assertions.assertEquals(1, menuFunctionMapper.selectCount(
                new QueryWrapper<MenuFunction>().eq("function_id", id).eq("menu_id", 15)));

        mockMvc.perform(put("/api/functions/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"menu_id\":15,\"perm\":\"badperm\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("权限标识格式必须为域:操作"));

        mockMvc.perform(put("/api/functions/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"menu_id\":15,\"perm\":\"order:query\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/functions/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"menu_id\":15,\"perm\":\"x:y\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCascadesMenuAndRoleLinks() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"待删功能\",\"menu_id\":13,\"perm\":\"dept:archive\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = track(raw);

        RoleFunction rf = new RoleFunction();
        rf.setRoleId(3L);
        rf.setFunctionId(id);
        roleFunctionMapper.insert(rf);

        mockMvc.perform(delete("/api/functions/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(functionMapper.selectById(id));
        org.junit.jupiter.api.Assertions.assertEquals(0, menuFunctionMapper.selectCount(
                new QueryWrapper<MenuFunction>().eq("function_id", id)));
        org.junit.jupiter.api.Assertions.assertEquals(0, roleFunctionMapper.selectCount(
                new QueryWrapper<RoleFunction>().eq("function_id", id)));
        tempFuncIds.remove(id);

        mockMvc.perform(delete("/api/functions/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
