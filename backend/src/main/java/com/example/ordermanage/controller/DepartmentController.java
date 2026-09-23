package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.DepartmentListItem;
import com.example.ordermanage.dto.DepartmentSaveRequest;
import com.example.ordermanage.service.DepartmentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @RequirePerm("dept:create")
    @GetMapping("/api/departments")
    public Result<PageResult<DepartmentListItem>> list(
            @RequestParam(required = false) String keyword,
            PageParam pageParam) {
        return Result.ok(departmentService.page(keyword, pageParam));
    }

    @RequirePerm("dept:create")
    @PostMapping("/api/departments")
    public Result<DepartmentListItem> create(@RequestBody DepartmentSaveRequest request) {
        return Result.ok(departmentService.create(request));
    }

    @RequirePerm("dept:update")
    @PutMapping("/api/departments/{id}")
    public Result<DepartmentListItem> update(@PathVariable Long id,
                                             @RequestBody DepartmentSaveRequest request) {
        return Result.ok(departmentService.update(id, request));
    }

    @RequirePerm("dept:delete")
    @DeleteMapping("/api/departments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.ok(null);
    }
}
