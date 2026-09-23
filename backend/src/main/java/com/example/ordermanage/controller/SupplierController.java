package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.ExcelExporter;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.SupplierOption;
import com.example.ordermanage.dto.SupplierSaveRequest;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.service.SupplierService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
public class SupplierController {

    private static final List<String> EXPORT_HEADERS = List.of(
            "供应商编号", "名称", "联系人", "联系电话", "地址", "状态", "创建时间");

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

    @RequirePerm("supplier:export")
    @OpLog(action = "导出", targetSpEL = "'供应商'")
    @GetMapping("/api/suppliers/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        List<Supplier> items = supplierService.listAll(keyword, status);
        List<List<String>> rows = items.stream().map(s -> List.of(
                nullToEmpty(s.getCode()),
                nullToEmpty(s.getName()),
                nullToEmpty(s.getContact()),
                nullToEmpty(s.getPhone()),
                nullToEmpty(s.getAddress()),
                nullToEmpty(s.getStatus()),
                ExcelExporter.formatTime(s.getCreateTime()))).toList();
        byte[] body = ExcelExporter.export(EXPORT_HEADERS, rows);
        String filename = "suppliers_" + LocalDate.now() + ".xls";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(body);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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
