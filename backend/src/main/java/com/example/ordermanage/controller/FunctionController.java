package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.FunctionListItem;
import com.example.ordermanage.dto.FunctionSaveRequest;
import com.example.ordermanage.service.FunctionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FunctionController {

    private final FunctionService functionService;

    public FunctionController(FunctionService functionService) {
        this.functionService = functionService;
    }

    @RequirePerm("func:create")
    @GetMapping("/api/functions")
    public Result<PageResult<FunctionListItem>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "menu_id", required = false) Long menuId,
            @RequestParam(required = false) String perm,
            PageParam pageParam) {
        return Result.ok(functionService.page(keyword, menuId, perm, pageParam));
    }

    @RequirePerm("func:create")
    @PostMapping("/api/functions")
    public Result<FunctionListItem> create(@RequestBody FunctionSaveRequest request) {
        return Result.ok(functionService.create(request));
    }

    @RequirePerm("func:update")
    @PutMapping("/api/functions/{id}")
    public Result<FunctionListItem> update(@PathVariable Long id,
                                           @RequestBody FunctionSaveRequest request) {
        return Result.ok(functionService.update(id, request));
    }

    @RequirePerm("func:delete")
    @DeleteMapping("/api/functions/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        functionService.delete(id);
        return Result.ok(null);
    }
}
