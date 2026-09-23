package com.example.ordermanage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ordermanage.mapper.OrganizationMapper;
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
class OrganizationTreeTest {

    private static final String FULL_REQUIRED =
            "\"name\":\"临时机构\",\"short_name\":\"临时\",\"region\":\"北京市\",\"address\":\"某大街1号\"";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrganizationMapper organizationMapper;
    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> tempOrgIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempOrgIds) {
            organizationMapper.deleteById(id);
        }
        tempOrgIds.clear();
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
        tempOrgIds.add(id);
        return id;
    }

    @Test
    void listReturnsRootPaginatedWithNestedChildren() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/organizations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("HQ001"))
                .andExpect(jsonPath("$.data.list[0].level").value("hq"))
                .andExpect(jsonPath("$.data.list[0].parent_id").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].children.length()").value(3))
                .andExpect(jsonPath("$.data.list[0].children[0].code").value("BJ001"))
                .andExpect(jsonPath("$.data.list[0].children[1].code").value("SH001"))
                .andExpect(jsonPath("$.data.list[0].children[2].code").value("GZ001"))
                .andExpect(jsonPath("$.data.list[0].children[0].children[0].code").value("BJ011"))
                .andExpect(jsonPath(
                        "$.data.list[0].children[0].children[0].children[0].code").value("BJ0111"))
                .andExpect(jsonPath(
                        "$.data.list[0].children[0].children[0].children[0].level").value("sub2"));
    }

    @Test
    void filterKeepsAncestorPathToMatch() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/organizations")
                        .param("name", "朝阳")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("HQ001"))
                .andExpect(jsonPath("$.data.list[0].children.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].children[0].code").value("BJ001"))
                .andExpect(jsonPath("$.data.list[0].children[0].children[0].code").value("BJ011"))
                .andExpect(jsonPath("$.data.list[0].children[0].children[0].children[0].code")
                        .value("BJ0111"));

        mockMvc.perform(get("/api/organizations")
                        .param("code", "SH0")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].code").value("HQ001"))
                .andExpect(jsonPath("$.data.list[0].children.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].children[0].code").value("SH001"));

        mockMvc.perform(get("/api/organizations")
                        .param("level", "sub2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath(
                        "$.data.list[0].children[0].children[0].children[0].code").value("BJ0111"));

        mockMvc.perform(get("/api/organizations")
                        .param("name", "不存在的机构XYZ")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list.length()").value(0));
    }

    @Test
    void createTopLevelDerivesHqAndValidatesCode() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BAD-CODE\"," + FULL_REQUIRED + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("机构编号只能包含字母和数字"));

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HQ001\"," + FULL_REQUIRED + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HQ002\",\"name\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入机构简称"));

        String raw = mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"TOP001\",\"name\":\"第二总行\",\"short_name\":\"二总\","
                                + "\"region\":\"北京市\",\"address\":\"金融街2号\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("TOP001"))
                .andExpect(jsonPath("$.data.level").value("hq"))
                .andExpect(jsonPath("$.data.parent_id").doesNotExist())
                .andExpect(jsonPath("$.data.create_time").exists())
                .andReturn().getResponse().getContentAsString();
        track(raw);
    }

    @Test
    void createChildDerivesNextLevelAndBlocksLastLevel() throws Exception {
        String token = login();

        String raw = mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CHD001\",\"parent_id\":1," + FULL_REQUIRED + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value("branch1"))
                .andExpect(jsonPath("$.data.parent_id").value(1))
                .andReturn().getResponse().getContentAsString();
        track(raw);

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CHD002\",\"parent_id\":7," + FULL_REQUIRED + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("已达最末级，不可新增下级"));

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CHD003\",\"parent_id\":999999," + FULL_REQUIRED + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("上级机构不存在"));
    }

    @Test
    void updateIgnoresCodeAndLevelAndValidatesRequired() throws Exception {
        String token = login();
        String raw = mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"UPD001\"," + FULL_REQUIRED + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = track(raw);

        mockMvc.perform(put("/api/organizations/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"HACKED\",\"name\":\"改名了\",\"short_name\":\"改简称\","
                                + "\"region\":\"上海市\",\"address\":\"改地址\",\"remark\":\"rr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("改名了"))
                .andExpect(jsonPath("$.data.remark").value("rr"))
                .andExpect(jsonPath("$.data.code").value("UPD001"))
                .andExpect(jsonPath("$.data.level").value("hq"));
        org.junit.jupiter.api.Assertions.assertEquals("UPD001",
                organizationMapper.selectById(id).getCode());
        org.junit.jupiter.api.Assertions.assertEquals("hq",
                organizationMapper.selectById(id).getLevel());

        mockMvc.perform(put("/api/organizations/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"改名了\",\"short_name\":\"s\",\"region\":\"\",\"address\":\"a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入所在地区"));

        mockMvc.perform(put("/api/organizations/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"short_name\":\"s\",\"region\":\"r\",\"address\":\"a\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRejectsParentWithChildrenAndAllowsLeaf() throws Exception {
        String token = login();
        mockMvc.perform(delete("/api/organizations/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("该机构下有下级机构，无法删除"));
        org.junit.jupiter.api.Assertions.assertNotNull(organizationMapper.selectById(1L));

        String raw = mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"DEL001\"," + FULL_REQUIRED + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = track(raw);

        mockMvc.perform(delete("/api/organizations/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        org.junit.jupiter.api.Assertions.assertNull(organizationMapper.selectById(id));
        tempOrgIds.remove(id);

        mockMvc.perform(delete("/api/organizations/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
