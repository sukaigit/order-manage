package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.MenuFunction;
import com.example.ordermanage.entity.RoleMenu;
import com.example.ordermanage.mapper.MenuFunctionMapper;
import com.example.ordermanage.mapper.MenuMapper;
import com.example.ordermanage.mapper.RoleMenuMapper;
import com.example.ordermanage.service.AuthService;
import com.example.ordermanage.service.CaptchaService;
import com.fasterxml.jackson.databind.JsonNode;
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
class MenuTreeTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private MenuFunctionMapper menuFunctionMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempMenuIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempMenuIds) {
            menuFunctionMapper.delete(new QueryWrapper<MenuFunction>().eq("menu_id", id));
            roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("menu_id", id));
        }
        for (Long id : tempMenuIds) {
            menuMapper.deleteById(id);
        }
        tempMenuIds.clear();
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
        tempMenuIds.add(id);
        return id;
    }

    private int countNodes(JsonNode node) {
        int n = 1;
        for (JsonNode c : node.path("children")) {
            n += countNodes(c);
        }
        return n;
    }

    private int countFunctions(JsonNode node) {
        int n = node.path("functions").size();
        for (JsonNode c : node.path("children")) {
            n += countFunctions(c);
        }
        return n;
    }

    @Test
    void fullTreeHas17NodesAnd32MountedFunctions() throws Exception {
        String token = login();
        String raw = mockMvc.perform(get("/api/menus/tree")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(raw).get("data");
        org.junit.jupiter.api.Assertions.assertTrue(data.isArray());
        org.junit.jupiter.api.Assertions.assertEquals(6, data.size());

        int nodes = 0;
        int functions = 0;
        for (JsonNode root : data) {
            nodes += countNodes(root);
            functions += countFunctions(root);
        }
        org.junit.jupiter.api.Assertions.assertEquals(17, nodes,
                "menu tree must contain exactly 17 nodes");
        org.junit.jupiter.api.Assertions.assertEquals(32, functions,
                "menu tree must mount exactly 32 functions");

        JsonNode system = null;
        JsonNode order = null;
        for (JsonNode root : data) {
            if ("MENU_SYSTEM".equals(root.path("code").asText())) {
                system = root;
            }
            if ("MENU_ORDER".equals(root.path("code").asText())) {
                order = root;
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(system);
        org.junit.jupiter.api.Assertions.assertEquals(7, system.path("children").size());
        org.junit.jupiter.api.Assertions.assertNotNull(order);
        org.junit.jupiter.api.Assertions.assertEquals(1, order.path("children").size());
        JsonNode orderList = order.path("children").get(0);
        org.junit.jupiter.api.Assertions.assertEquals("MENU_ORDER_LIST",
                orderList.path("code").asText());
        org.junit.jupiter.api.Assertions.assertEquals(5, orderList.path("functions").size());
        org.junit.jupiter.api.Assertions.assertEquals("order:query",
                orderList.path("functions").get(0).path("perm").asText());
    }

    @Test
    void listPaginatesByLevel1WithChildren() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/menus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.list[0].code").value("MENU_WORKBENCH"))
                .andExpect(jsonPath("$.data.list[0].route").value("/workbench"))
                .andExpect(jsonPath("$.data.list[0].menu_type").value("level1"))
                .andExpect(jsonPath("$.data.list[1].code").value("MENU_ORDER"))
                .andExpect(jsonPath("$.data.list[1].children[0].code").value("MENU_ORDER_LIST"))
                .andExpect(jsonPath("$.data.list[1].children[0].menu_type").value("level2"));

        mockMvc.perform(get("/api/menus")
                        .param("menu_type", "level2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.list[?(@.code=='MENU_WORKBENCH')]").doesNotExist())
                .andExpect(jsonPath("$.data.list[?(@.code=='MENU_SYSTEM')].children.length()")
                        .value(7));

        mockMvc.perform(get("/api/menus")
                        .param("code", "MENU_ORDER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("MENU_ORDER"))
                .andExpect(jsonPath("$.data.list[0].children[0].code").value("MENU_ORDER_LIST"));

        mockMvc.perform(get("/api/menus")
                        .param("route", "/system/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("MENU_SYSTEM"))
                .andExpect(jsonPath("$.data.list[0].children[0].code").value("MENU_USERS"));
    }

    @Test
    void createLevel1AndLevel2WithRouteRules() throws Exception {
        String token = login();

        String raw = mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"打印管理\",\"code\":\"MENU_PRINT\",\"route\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("MENU_PRINT"))
                .andExpect(jsonPath("$.data.menu_type").value("level1"))
                .andExpect(jsonPath("$.data.parent_id").doesNotExist())
                .andExpect(jsonPath("$.data.create_time").exists())
                .andReturn().getResponse().getContentAsString();
        long rootId = track(raw);

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"子菜单无路由\",\"parent_id\":" + rootId + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("二级菜单必须配置路由"));

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入菜单名称"));

        mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"重复编号\",\"code\":\"MENU_ORDER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        String rawChild = mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"打印子页\",\"parent_id\":" + rootId
                                + ",\"route\":\"/system/print\",\"sort\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.menu_type").value("level2"))
                .andExpect(jsonPath("$.data.parent_id").value(rootId))
                .andExpect(jsonPath("$.data.route").value("/system/print"))
                .andExpect(jsonPath("$.data.sort").value(1))
                .andReturn().getResponse().getContentAsString();
        track(rawChild);

        mockMvc.perform(get("/api/menus")
                        .param("code", "MENU_PRINT")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].children.length()").value(1));
    }

    @Test
    void updateRequiresNameKeepsCodeAndLevel2Route() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").exists())
                .andReturn().getResponse().getContentAsString();
        long id = track(raw);
        String originalCode = objectMapper.readTree(raw).get("data").get("code").asText();

        mockMvc.perform(put("/api/menus/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"code\":\"HACKED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入菜单名称"));

        mockMvc.perform(put("/api/menus/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"更新后\",\"code\":\"HACKED\",\"route\":\"/x\",\"remark\":\"rr\",\"sort\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("更新后"))
                .andExpect(jsonPath("$.data.remark").value("rr"))
                .andExpect(jsonPath("$.data.sort").value(9))
                .andExpect(jsonPath("$.data.code").value(originalCode));
        org.junit.jupiter.api.Assertions.assertEquals(originalCode,
                menuMapper.selectById(id).getCode());

        String raw2 = mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"二级更新\",\"parent_id\":10,\"route\":\"/tmp/x\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long childId = track(raw2);

        mockMvc.perform(put("/api/menus/" + childId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"二级更新\",\"route\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("二级菜单必须配置路由"));

        mockMvc.perform(put("/api/menus/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRejectsParentAndCascadesLinksForLeaf() throws Exception {
        String token = login();

        String rawRoot = mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"待删父菜单\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long rootId = track(rawRoot);

        String rawChild = mockMvc.perform(post("/api/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"子菜单\",\"parent_id\":" + rootId
                                + ",\"route\":\"/tmp/child\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long childId = track(rawChild);

        mockMvc.perform(delete("/api/menus/" + rootId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该菜单下存在子菜单，无法删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(menuMapper.selectById(rootId));

        mockMvc.perform(delete("/api/menus/" + childId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        tempMenuIds.remove(childId);

        MenuFunction mf = new MenuFunction();
        mf.setMenuId(rootId);
        mf.setFunctionId(1L);
        menuFunctionMapper.insert(mf);
        RoleMenu rm = new RoleMenu();
        rm.setRoleId(3L);
        rm.setMenuId(rootId);
        roleMenuMapper.insert(rm);

        mockMvc.perform(delete("/api/menus/" + rootId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(menuMapper.selectById(rootId));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                menuFunctionMapper.selectCount(
                        new QueryWrapper<MenuFunction>().eq("menu_id", rootId)));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                roleMenuMapper.selectCount(new QueryWrapper<RoleMenu>().eq("menu_id", rootId)));
        tempMenuIds.remove(rootId);

        mockMvc.perform(delete("/api/menus/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
