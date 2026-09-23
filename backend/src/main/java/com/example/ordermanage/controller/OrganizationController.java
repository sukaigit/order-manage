package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.OrgTreeNode;
import com.example.ordermanage.dto.OrganizationSaveRequest;
import com.example.ordermanage.service.OrganizationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @RequirePerm("org:create")
    @GetMapping("/api/organizations")
    public Result<PageResult<OrgTreeNode>> tree(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(name = "short_name", required = false) String shortName,
            @RequestParam(required = false) String level,
            PageParam pageParam) {
        return Result.ok(organizationService.tree(code, name, shortName, level, pageParam));
    }

    @RequirePerm("org:create")
    @PostMapping("/api/organizations")
    public Result<OrgTreeNode> create(@RequestBody OrganizationSaveRequest request) {
        return Result.ok(organizationService.create(request));
    }

    @RequirePerm("org:update")
    @PutMapping("/api/organizations/{id}")
    public Result<OrgTreeNode> update(@PathVariable Long id,
                                      @RequestBody OrganizationSaveRequest request) {
        return Result.ok(organizationService.update(id, request));
    }

    @RequirePerm("org:delete")
    @DeleteMapping("/api/organizations/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        organizationService.delete(id);
        return Result.ok(null);
    }
}
