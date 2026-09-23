package com.example.ordermanage.controller;

import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.OrderListItem;
import com.example.ordermanage.dto.ReportSummaryResponse;
import com.example.ordermanage.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/reports/summary")
    public Result<ReportSummaryResponse> summary(
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate,
            @RequestParam(value = "supplier_id", required = false) Long supplierId) {
        return Result.ok(reportService.summary(startDate, endDate, supplierId));
    }

    @GetMapping("/api/reports/detail")
    public Result<PageResult<OrderListItem>> detail(
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate,
            @RequestParam(value = "supplier_id", required = false) Long supplierId,
            PageParam pageParam) {
        return Result.ok(reportService.detail(startDate, endDate, supplierId, pageParam));
    }
}
