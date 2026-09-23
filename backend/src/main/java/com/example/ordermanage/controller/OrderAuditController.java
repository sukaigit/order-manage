package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.OrderAuditActionResponse;
import com.example.ordermanage.dto.OrderAuditHistoryItem;
import com.example.ordermanage.dto.OrderAuditOpinionRequest;
import com.example.ordermanage.dto.OrderAuditPageResult;
import com.example.ordermanage.service.OrderAuditService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderAuditController {

    private final OrderAuditService orderAuditService;

    public OrderAuditController(OrderAuditService orderAuditService) {
        this.orderAuditService = orderAuditService;
    }

    @RequirePerm("order:query")
    @GetMapping("/api/order-audits")
    public Result<OrderAuditPageResult> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            PageParam pageParam) {
        return Result.ok(orderAuditService.page(keyword, status, pageParam));
    }

    @RequirePerm("audit:pass")
    @PostMapping("/api/order-audits/{id}/pass")
    public Result<OrderAuditActionResponse> pass(
            @PathVariable Long id,
            @RequestBody(required = false) OrderAuditOpinionRequest request) {
        return Result.ok(orderAuditService.pass(id, request));
    }

    @RequirePerm("audit:reject")
    @PostMapping("/api/order-audits/{id}/reject")
    public Result<OrderAuditActionResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) OrderAuditOpinionRequest request) {
        return Result.ok(orderAuditService.reject(id, request));
    }

    @RequirePerm("order:query")
    @GetMapping("/api/orders/{id}/audits")
    public Result<List<OrderAuditHistoryItem>> history(@PathVariable Long id) {
        return Result.ok(orderAuditService.history(id));
    }
}
