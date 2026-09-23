package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.RoleListItem;
import com.example.ordermanage.dto.RolePermissionsRequest;
import com.example.ordermanage.dto.RolePermissionsResponse;
import com.example.ordermanage.dto.RoleSaveRequest;
import com.example.ordermanage.service.RoleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @RequirePerm("role:create")
    @GetMapping("/api/roles")
    public Result<PageResult<RoleListItem>> list(
            @RequestParam(required = false) String keyword,
            PageParam pageParam) {
        return Result.ok(roleService.page(keyword, pageParam));
    }

    @RequirePerm("role:create")
    @PostMapping("/api/roles")
    public Result<RoleListItem> create(@RequestBody RoleSaveRequest request) {
        return Result.ok(roleService.create(request));
    }

    @RequirePerm("role:update")
    @PutMapping("/api/roles/{id}")
    public Result<RoleListItem> update(@PathVariable Long id, @RequestBody RoleSaveRequest request) {
        return Result.ok(roleService.update(id, request));
    }

    @RequirePerm("role:delete")
    @DeleteMapping("/api/roles/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok(null);
    }

    @RequirePerm("role:update")
    @GetMapping("/api/roles/{id}/permissions")
    public Result<RolePermissionsResponse> permissions(@PathVariable Long id) {
        return Result.ok(roleService.permissions(id));
    }

    @RequirePerm("role:update")
    @PutMapping("/api/roles/{id}/permissions")
    public Result<RolePermissionsResponse> assignPermissions(@PathVariable Long id,
                                                             @RequestBody RolePermissionsRequest request) {
        return Result.ok(roleService.assignPermissions(id, request));
    }
}
