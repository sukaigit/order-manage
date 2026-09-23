package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.ExcelExporter;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.OrderListItem;
import com.example.ordermanage.dto.ReportSummaryResponse;
import com.example.ordermanage.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController {

    private static final List<String> EXPORT_HEADERS = List.of(
            "订单编号", "订单名称", "金额", "供应商", "状态", "创建时间");

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

    @RequirePerm("report:export")
    @OpLog(action = "导出", targetSpEL = "'报表'")
    @GetMapping("/api/reports/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate,
            @RequestParam(value = "supplier_id", required = false) Long supplierId) {
        List<OrderListItem> items = reportService.exportRows(startDate, endDate, supplierId);
        List<List<String>> rows = items.stream().map(item -> List.of(
                nullToEmpty(item.getOrderNo()),
                nullToEmpty(item.getName()),
                item.getAmount() == null ? "" : item.getAmount().toPlainString(),
                nullToEmpty(item.getSupplierName()),
                nullToEmpty(item.getStatus()),
                ExcelExporter.formatTime(item.getCreateTime()))).toList();
        byte[] body = ExcelExporter.export(EXPORT_HEADERS, rows);
        String filename = "reports_" + LocalDate.now() + ".xls";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(body);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
