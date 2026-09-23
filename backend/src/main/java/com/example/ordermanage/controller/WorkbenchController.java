package com.example.ordermanage.controller;

import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.WorkbenchStatsResponse;
import com.example.ordermanage.service.WorkbenchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    public WorkbenchController(WorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/api/workbench/stats")
    public Result<WorkbenchStatsResponse> stats() {
        return Result.ok(workbenchService.stats());
    }
}
