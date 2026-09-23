package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.ResetPasswordResponse;
import com.example.ordermanage.dto.UserListItem;
import com.example.ordermanage.dto.UserQuery;
import com.example.ordermanage.dto.UserSaveRequest;
import com.example.ordermanage.dto.UserStatusRequest;
import com.example.ordermanage.service.UserService;
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
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @RequirePerm("user:create")
    @GetMapping("/api/users")
    public Result<PageResult<UserListItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "role_id", required = false) Long roleId,
            @RequestParam(required = false) String status,
            @RequestParam(name = "department_id", required = false) Long departmentId,
            @RequestParam(name = "organization_id", required = false) Long organizationId,
            PageParam pageParam) {
        UserQuery query = new UserQuery(keyword, roleId, status, departmentId, organizationId);
        return Result.ok(userService.page(query, pageParam));
    }

    @RequirePerm("user:create")
    @PostMapping("/api/users")
    public Result<UserListItem> create(@RequestBody UserSaveRequest request) {
        return Result.ok(userService.create(request));
    }

    @RequirePerm("user:update")
    @PutMapping("/api/users/{id}")
    public Result<UserListItem> update(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        return Result.ok(userService.update(id, request));
    }

    @RequirePerm("user:update")
    @PutMapping("/api/users/{id}/status")
    public Result<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                    @RequestBody UserStatusRequest request) {
        return Result.ok(userService.updateStatus(id, request.getStatus()));
    }

    @RequirePerm("user:update")
    @PutMapping("/api/users/{id}/reset-password")
    public Result<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        return Result.ok(userService.resetPassword(id));
    }

    @RequirePerm("user:delete")
    @DeleteMapping("/api/users/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok(null);
    }
}
