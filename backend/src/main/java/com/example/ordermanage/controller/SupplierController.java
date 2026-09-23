package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.SupplierOption;
import com.example.ordermanage.dto.SupplierSaveRequest;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.service.SupplierService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @RequirePerm("supplier:query")
    @GetMapping("/api/suppliers")
    public Result<PageResult<Supplier>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            PageParam pageParam) {
        return Result.ok(supplierService.page(keyword, status, pageParam));
    }

    @RequirePerm("supplier:create")
    @PostMapping("/api/suppliers")
    public Result<Supplier> create(@RequestBody SupplierSaveRequest request) {
        return Result.ok(supplierService.create(request));
    }

    @RequirePerm("supplier:update")
    @PutMapping("/api/suppliers/{id}")
    public Result<Supplier> update(@PathVariable Long id,
                                   @RequestBody SupplierSaveRequest request) {
        return Result.ok(supplierService.update(id, request));
    }

    @RequirePerm("supplier:delete")
    @DeleteMapping("/api/suppliers/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return Result.ok(null);
    }

    @RequirePerm("supplier:update")
    @PutMapping("/api/suppliers/{id}/status")
    public Result<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                    @RequestBody SupplierSaveRequest request) {
        return Result.ok(supplierService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/api/suppliers/options")
    public Result<List<SupplierOption>> options() {
        return Result.ok(supplierService.options());
    }
}
