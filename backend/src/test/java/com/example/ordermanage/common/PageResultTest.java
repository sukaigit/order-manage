package com.example.ordermanage.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pageResultKeysAreTotalAndList() throws Exception {
        PageResult<String> pageResult = new PageResult<>(2L, List.of("a", "b"));
        String json = objectMapper.writeValueAsString(pageResult);
        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("total"), "JSON must contain key: total");
        assertTrue(node.has("list"), "JSON must contain key: list");
        assertEquals(2L, node.get("total").asLong());
        assertEquals(2, node.get("list").size());
    }

    @Test
    void pageResultOfIPage() {
        Page<String> page = new Page<>(1, 10, 25);
        page.setRecords(List.of("x", "y"));
        PageResult<String> result = PageResult.of(page);
        assertEquals(25L, result.total());
        assertEquals(2, result.list().size());
    }

    @Test
    void pageZeroRejectedWith400() {
        PageParam param = new PageParam();
        param.setPage(0);
        BizException ex = assertThrows(BizException.class, param::validate);
        assertEquals(400, ex.getCode());
    }

    @Test
    void pageSizeOutOfRangeRejectedWith400() {
        PageParam tooBig = new PageParam();
        tooBig.setPageSize(101);
        BizException ex = assertThrows(BizException.class, tooBig::validate);
        assertEquals(400, ex.getCode());

        PageParam zero = new PageParam();
        zero.setPageSize(0);
        BizException ex2 = assertThrows(BizException.class, zero::validate);
        assertEquals(400, ex2.getCode());
    }

    @Test
    void validPagePassesDefaults() {
        PageParam param = new PageParam();
        param.validate();
        assertEquals(1, param.getPage());
        assertEquals(10, param.getPageSize());
    }
}
