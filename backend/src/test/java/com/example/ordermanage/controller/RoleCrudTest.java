package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.Role;
import com.example.ordermanage.entity.RoleFunction;
import com.example.ordermanage.entity.RoleMenu;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.RoleFunctionMapper;
import com.example.ordermanage.mapper.RoleMapper;
import com.example.ordermanage.mapper.RoleMenuMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
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
class RoleCrudTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private RoleFunctionMapper roleFunctionMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempRoleIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempRoleIds) {
            roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", id));
            roleFunctionMapper.delete(new QueryWrapper<RoleFunction>().eq("role_id", id));
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("role_id", id));
            roleMapper.deleteById(id);
        }
        tempRoleIds.clear();
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
    void listReturnsSeedRolesWithCounts() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].code").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.data.list[0].name").value("管理员"))
                .andExpect(jsonPath("$.data.list[0].menu_count").value(17))
                .andExpect(jsonPath("$.data.list[0].function_count").value(32))
                .andExpect(jsonPath("$.data.list[0].user_count").value(1))
                .andExpect(jsonPath("$.data.list[0].builtin").value(true));
    }

    @Test
    void keywordFilterWorks() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/roles")
                        .param("keyword", "审核")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("ROLE_AUDITOR"));
    }

    @Test
    void createWithoutNameReturns400() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请输入角色名称"));
    }

    @Test
    void createAutoGeneratesCodeAndEmptyPermissions() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"财务员\",\"remark\":\"财务查询\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("财务员"))
                .andExpect(jsonPath("$.data.menu_count").value(0))
                .andExpect(jsonPath("$.data.function_count").value(0))
                .andExpect(jsonPath("$.data.user_count").value(0))
                .andExpect(jsonPath("$.data.builtin").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempRoleIds.add(id);
        String code = objectMapper.readTree(raw).get("data").get("code").asText();
        org.junit.jupiter.api.Assertions.assertNotNull(code);
        org.junit.jupiter.api.Assertions.assertFalse(code.isBlank());
    }

    @Test
    void createDuplicateCodeReturns409() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"冒名者\",\"code\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void updateIgnoresCodeAndRequiresName() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"临时角色\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempRoleIds.add(id);
        String originalCode = objectMapper.readTree(raw).get("data").get("code").asText();

        mockMvc.perform(put("/api/roles/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"code\":\"HACKED\",\"remark\":\"r\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入角色名称"));

        mockMvc.perform(put("/api/roles/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"临时角色改\",\"code\":\"HACKED\",\"remark\":\"改备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("临时角色改"))
                .andExpect(jsonPath("$.data.remark").value("改备注"))
                .andExpect(jsonPath("$.data.code").value(originalCode));
        org.junit.jupiter.api.Assertions.assertEquals(originalCode, roleMapper.selectById(id).getCode());
    }

    @Test
    void deleteUnreferencedRoleClearsGrants() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"待删角色\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempRoleIds.add(id);
        roleMenuMapper.insert(roleMenu(id, 1L));
        roleFunctionMapper.insert(roleFunction(id, 1L));

        mockMvc.perform(delete("/api/roles/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(roleMapper.selectById(id));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                roleMenuMapper.selectCount(new QueryWrapper<RoleMenu>().eq("role_id", id)));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                roleFunctionMapper.selectCount(new QueryWrapper<RoleFunction>().eq("role_id", id)));
        tempRoleIds.remove(id);
    }

    private RoleMenu roleMenu(Long roleId, Long menuId) {
        RoleMenu rm = new RoleMenu();
        rm.setRoleId(roleId);
        rm.setMenuId(menuId);
        return rm;
    }

    private RoleFunction roleFunction(Long roleId, Long functionId) {
        RoleFunction rf = new RoleFunction();
        rf.setRoleId(roleId);
        rf.setFunctionId(functionId);
        return rf;
    }

    @Test
    void deleteReferencedRoleReturns422() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/roles/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该角色已被用户引用，无法删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(roleMapper.selectById(1L));
    }

    @Test
    void deleteMissingRoleReturns404() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/roles/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void updateMissingRoleReturns404() throws Exception {
        String token = login();
        mockMvc.perform(put("/api/roles/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
