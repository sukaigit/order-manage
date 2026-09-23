package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.ExcelExporter;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.OrderListItem;
import com.example.ordermanage.dto.OrderSaveRequest;
import com.example.ordermanage.service.OrderService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private static final List<String> EXPORT_HEADERS = List.of(
            "订单编号", "订单名称", "金额", "供应商", "状态", "创建时间", "备注");

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @RequirePerm("order:query")
    @GetMapping("/api/orders")
    public Result<PageResult<OrderListItem>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "supplier_id", required = false) Long supplierId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate,
            PageParam pageParam) {
        return Result.ok(orderService.page(keyword, supplierId, status, startDate, endDate, pageParam));
    }

    @RequirePerm("order:export")
    @OpLog(action = "导出", targetSpEL = "'订单'")
    @GetMapping("/api/orders/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "supplier_id", required = false) Long supplierId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        List<OrderListItem> items = orderService.listAll(keyword, supplierId, status, startDate, endDate);
        List<List<String>> rows = items.stream().map(item -> List.of(
                nullToEmpty(item.getOrderNo()),
                nullToEmpty(item.getName()),
                item.getAmount() == null ? "" : item.getAmount().toPlainString(),
                nullToEmpty(item.getSupplierName()),
                nullToEmpty(item.getStatus()),
                ExcelExporter.formatTime(item.getCreateTime()),
                nullToEmpty(item.getRemark()))).toList();
        byte[] body = ExcelExporter.export(EXPORT_HEADERS, rows);
        String filename = "orders_" + LocalDate.now() + ".xls";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(body);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @RequirePerm("order:create")
    @PostMapping("/api/orders")
    public Result<OrderListItem> create(@RequestBody OrderSaveRequest request) {
        return Result.ok(orderService.create(request));
    }

    @RequirePerm("order:update")
    @PutMapping("/api/orders/{id}")
    public Result<OrderListItem> update(@PathVariable Long id,
                                        @RequestBody OrderSaveRequest request) {
        return Result.ok(orderService.update(id, request));
    }

    @RequirePerm("order:delete")
    @DeleteMapping("/api/orders/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.ok(null);
    }

    @RequirePerm("order:query")
    @GetMapping("/api/orders/{id}")
    public Result<OrderListItem> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }
}
