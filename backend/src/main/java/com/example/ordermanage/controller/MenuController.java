package com.example.ordermanage.controller;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.common.Result;
import com.example.ordermanage.dto.MenuSaveRequest;
import com.example.ordermanage.dto.MenuTreeNode;
import com.example.ordermanage.service.MenuService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @RequirePerm("menu:create")
    @GetMapping("/api/menus")
    public Result<PageResult<MenuTreeNode>> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String route,
            @RequestParam(name = "menu_type", required = false) String menuType,
            PageParam pageParam) {
        return Result.ok(menuService.page(code, name, route, menuType, pageParam));
    }

    @RequirePerm("menu:create")
    @GetMapping("/api/menus/tree")
    public Result<List<MenuTreeNode>> tree() {
        return Result.ok(menuService.fullTree());
    }

    @RequirePerm("menu:create")
    @PostMapping("/api/menus")
    public Result<MenuTreeNode> create(@RequestBody MenuSaveRequest request) {
        return Result.ok(menuService.create(request));
    }

    @RequirePerm("menu:update")
    @PutMapping("/api/menus/{id}")
    public Result<MenuTreeNode> update(@PathVariable Long id, @RequestBody MenuSaveRequest request) {
        return Result.ok(menuService.update(id, request));
    }

    @RequirePerm("menu:delete")
    @DeleteMapping("/api/menus/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok(null);
    }
}
