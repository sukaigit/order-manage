package com.example.ordermanage.controller;

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
import com.example.ordermanage.mapper.UserMapper;
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
class RolePermissionAssignTest {

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
    private UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempRoleIds = new ArrayList<>();
    private final List<Long> tempUserIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempUserIds) {
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
        for (Long id : tempRoleIds) {
            roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", id));
            roleFunctionMapper.delete(new QueryWrapper<RoleFunction>().eq("role_id", id));
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("role_id", id));
            roleMapper.deleteById(id);
        }
        tempRoleIds.clear();
        authService.resetFailCounts();
    }

    private String login(String username, String password) throws Exception {
        Map<String, String> cap = captchaService.create("ABCD");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username, "password", password,
                "captcha", "ABCD", "captcha_id", cap.get("captcha_id")));
        String raw = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(raw).get("data").get("token").asText();
    }

    private String adminToken() throws Exception {
        return login("admin", "123456");
    }

    private long createRole(String token, String name) throws Exception {
        String raw = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempRoleIds.add(id);
        return id;
    }

    @Test
    void assignFiveMenusRoundTrip() throws Exception {
        String token = adminToken();
        long roleId = createRole(token, "权限测试角色");

        List<Integer> menuIds = List.of(1, 2, 3, 6, 7);
        List<Integer> functionIds = List.of(1, 2, 8);
        String body = "{\"menu_ids\":[1,2,3,6,7],\"function_ids\":[1,2,8]}";

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role_id").value(roleId))
                .andExpect(jsonPath("$.data.menu_ids.length()").value(5))
                .andExpect(jsonPath("$.data.function_ids.length()").value(3));

        mockMvc.perform(get("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role_id").value(roleId))
                .andExpect(jsonPath("$.data.menu_ids[0]").value(1))
                .andExpect(jsonPath("$.data.menu_ids[1]").value(2))
                .andExpect(jsonPath("$.data.menu_ids[2]").value(3))
                .andExpect(jsonPath("$.data.menu_ids[3]").value(6))
                .andExpect(jsonPath("$.data.menu_ids[4]").value(7))
                .andExpect(jsonPath("$.data.function_ids.length()").value(3));

        org.junit.jupiter.api.Assertions.assertEquals(5,
                roleMenuMapper.selectCount(new QueryWrapper<RoleMenu>().eq("role_id", roleId)));
        org.junit.jupiter.api.Assertions.assertEquals(3,
                roleFunctionMapper.selectCount(new QueryWrapper<RoleFunction>().eq("role_id", roleId)));

        // full overwrite: shrink to 1 menu, old grants gone
        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menu_ids\":[10],\"function_ids\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menu_ids.length()").value(1))
                .andExpect(jsonPath("$.data.function_ids.length()").value(0));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                roleMenuMapper.selectCount(new QueryWrapper<RoleMenu>().eq("role_id", roleId)));
    }

    @Test
    void adminRolePermissionsImmutable() throws Exception {
        String token = adminToken();
        mockMvc.perform(put("/api/roles/1/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menu_ids\":[],\"function_ids\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("管理员角色权限不可修改"));
        org.junit.jupiter.api.Assertions.assertEquals(17,
                roleMenuMapper.selectCount(new QueryWrapper<RoleMenu>().eq("role_id", 1)));
        org.junit.jupiter.api.Assertions.assertEquals(32,
                roleFunctionMapper.selectCount(new QueryWrapper<RoleFunction>().eq("role_id", 1)));
    }

    @Test
    void adminRolePermissionsReadable() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/roles/1/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role_id").value(1))
                .andExpect(jsonPath("$.data.menu_ids.length()").value(17))
                .andExpect(jsonPath("$.data.function_ids.length()").value(32));
    }

    @Test
    void missingRoleReturns404() throws Exception {
        String token = adminToken();
        mockMvc.perform(get("/api/roles/999999/permissions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(put("/api/roles/999999/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menu_ids\":[],\"function_ids\":[]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void assignedMenusVisibleToRoleUserAfterLogin() throws Exception {
        String token = adminToken();
        long roleId = createRole(token, "菜单联动角色");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "username", "perm_link_user",
                "real_name", "联动用户",
                "role_id", roleId,
                "department_id", 1,
                "organization_id", 1));
        String raw = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long userId = objectMapper.readTree(raw).get("data").get("id").asLong();
        tempUserIds.add(userId);

        String userToken = login("perm_link_user", "Uu888888!");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menus.length()").value(0));

        mockMvc.perform(put("/api/roles/" + roleId + "/permissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"menu_ids\":[1,2,3,6,7],\"function_ids\":[1]}"))
                .andExpect(status().isOk());

        String userToken2 = login("perm_link_user", "Uu888888!");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + userToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menus.length()").value(3))
                .andExpect(jsonPath("$.data.menus[?(@.code=='MENU_WORKBENCH')]").exists())
                .andExpect(jsonPath("$.data.menus[?(@.code=='MENU_ORDER')].children[?(@.code=='MENU_ORDER_LIST')]").exists())
                .andExpect(jsonPath("$.data.menus[?(@.code=='MENU_SUPPLIER')].children[?(@.code=='MENU_SUPPLIER_LIST')]").exists())
                .andExpect(jsonPath("$.data.menus[?(@.code=='MENU_REPORT')]").doesNotExist());
    }
}
