package com.example.ordermanage.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class DemoController {

        @GetMapping("/demo/biz422")
        public Result<Void> biz422() {
            throw new BizException(422, "已完成订单不可编辑");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bizException422ReturnsStatus422AndJsonCode422() throws Exception {
        mockMvc.perform(get("/demo/biz422"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422))
                .andExpect(jsonPath("$.message").value("已完成订单不可编辑"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
