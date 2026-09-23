package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.MenuFunctionItem;
import com.example.ordermanage.dto.MenuSaveRequest;
import com.example.ordermanage.dto.MenuTreeNode;
import com.example.ordermanage.entity.Function;
import com.example.ordermanage.entity.Menu;
import com.example.ordermanage.entity.MenuFunction;
import com.example.ordermanage.entity.RoleMenu;
import com.example.ordermanage.mapper.FunctionMapper;
import com.example.ordermanage.mapper.MenuFunctionMapper;
import com.example.ordermanage.mapper.MenuMapper;
import com.example.ordermanage.mapper.RoleMenuMapper;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MenuService {

    private final MenuMapper menuMapper;
    private final MenuFunctionMapper menuFunctionMapper;
    private final FunctionMapper functionMapper;
    private final RoleMenuMapper roleMenuMapper;

    public MenuService(MenuMapper menuMapper, MenuFunctionMapper menuFunctionMapper,
                       FunctionMapper functionMapper, RoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.functionMapper = functionMapper;
        this.menuFunctionMapper = menuFunctionMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    public PageResult<MenuTreeNode> page(String code, String name, String route, String menuType,
                                          PageParam p) {
        p.validate();
        List<Menu> all = menuMapper.selectList(new QueryWrapper<Menu>().orderByAsc("sort").orderByAsc("id"));
        Map<Long, Menu> byId = all.stream().collect(Collectors.toMap(Menu::getId, m -> m));
        Map<Long, List<Menu>> childrenOf = new HashMap<>();
        for (Menu m : all) {
            if (m.getParentId() != null) {
                childrenOf.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
            }
        }
        for (List<Menu> list : childrenOf.values()) {
            list.sort(Comparator.comparing(Menu::getSort).thenComparing(Menu::getId));
        }

        boolean hasFilter = StringUtils.hasText(code) || StringUtils.hasText(name)
                || StringUtils.hasText(route) || StringUtils.hasText(menuType);
        Set<Long> matched = new HashSet<>();
        for (Menu m : all) {
            if (!hasFilter || matches(m, code, name, route, menuType)) {
                matched.add(m.getId());
            }
        }
        if (matched.isEmpty()) {
            return new PageResult<>(0, List.of());
        }

        Set<Long> kept = new HashSet<>(matched);
        for (Long id : matched) {
            Long cur = id;
            while (cur != null && byId.containsKey(cur)) {
                kept.add(cur);
                cur = byId.get(cur).getParentId();
            }
        }
        Deque<Long> queue = new ArrayDeque<>(matched);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            for (Menu child : childrenOf.getOrDefault(id, List.of())) {
                if (kept.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }

        Map<Long, List<MenuFunctionItem>> functions = loadFunctions();
        List<Menu> roots = all.stream()
                .filter(m -> m.getParentId() == null && kept.contains(m.getId()))
                .sorted(Comparator.comparing(Menu::getSort).thenComparing(Menu::getId))
                .collect(Collectors.toList());
        long total = roots.size();
        int from = (int) ((p.getPage() - 1) * p.getPageSize());
        if (from >= roots.size()) {
            return new PageResult<>(total, List.of());
        }
        int to = Math.min(from + p.getPageSize(), roots.size());
        List<MenuTreeNode> pageRoots = new ArrayList<>();
        for (Menu root : roots.subList(from, to)) {
            pageRoots.add(buildNode(root, childrenOf, kept, functions));
        }
        return new PageResult<>(total, pageRoots);
    }

    public List<MenuTreeNode> fullTree() {
        List<Menu> all = menuMapper.selectList(new QueryWrapper<Menu>().orderByAsc("sort").orderByAsc("id"));
        Map<Long, List<Menu>> childrenOf = new HashMap<>();
        for (Menu m : all) {
            if (m.getParentId() != null) {
                childrenOf.computeIfAbsent(m.getParentId(), k -> new ArrayList<>()).add(m);
            }
        }
        for (List<Menu> list : childrenOf.values()) {
            list.sort(Comparator.comparing(Menu::getSort).thenComparing(Menu::getId));
        }
        Map<Long, List<MenuFunctionItem>> functions = loadFunctions();
        Set<Long> allIds = all.stream().map(Menu::getId).collect(Collectors.toSet());
        List<MenuTreeNode> roots = new ArrayList<>();
        all.stream()
                .filter(m -> m.getParentId() == null || !allIds.contains(m.getParentId()))
                .sorted(Comparator.comparing(Menu::getSort).thenComparing(Menu::getId))
                .forEach(m -> roots.add(buildNode(m, childrenOf, allIds, functions)));
        return roots;
    }

    private Map<Long, List<MenuFunctionItem>> loadFunctions() {
        List<MenuFunction> links = menuFunctionMapper.selectList(
                new QueryWrapper<MenuFunction>().orderByAsc("menu_id").orderByAsc("function_id"));
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> functionIds = links.stream().map(MenuFunction::getFunctionId)
                .distinct().collect(Collectors.toList());
        Map<Long, Function> byId = functionMapper.selectBatchIds(functionIds).stream()
                .collect(Collectors.toMap(Function::getId, f -> f));
        Map<Long, List<MenuFunctionItem>> result = new HashMap<>();
        for (MenuFunction link : links) {
            Function f = byId.get(link.getFunctionId());
            if (f == null) {
                continue;
            }
            MenuFunctionItem item = new MenuFunctionItem();
            item.setId(f.getId());
            item.setCode(f.getCode());
            item.setName(f.getName());
            item.setPerm(f.getPerm());
            result.computeIfAbsent(link.getMenuId(), k -> new ArrayList<>()).add(item);
        }
        return result;
    }

    private boolean matches(Menu m, String code, String name, String route, String menuType) {
        if (StringUtils.hasText(code) && (m.getCode() == null || !m.getCode().contains(code.trim()))) {
            return false;
        }
        if (StringUtils.hasText(name) && (m.getName() == null || !m.getName().contains(name.trim()))) {
            return false;
        }
        if (StringUtils.hasText(route) && (m.getRoute() == null || !m.getRoute().contains(route.trim()))) {
            return false;
        }
        if (StringUtils.hasText(menuType) && !menuType.equals(m.getMenuType())) {
            return false;
        }
        return true;
    }

    private MenuTreeNode buildNode(Menu menu, Map<Long, List<Menu>> childrenOf, Set<Long> kept,
                                   Map<Long, List<MenuFunctionItem>> functions) {
        MenuTreeNode node = new MenuTreeNode();
        node.setId(menu.getId());
        node.setCode(menu.getCode());
        node.setName(menu.getName());
        node.setParentId(menu.getParentId());
        node.setRoute(menu.getRoute());
        node.setMenuType(menu.getMenuType());
        node.setSort(menu.getSort());
        node.setRemark(menu.getRemark());
        node.setCreateTime(menu.getCreateTime());
        node.setUpdateTime(menu.getUpdateTime());
        node.setFunctions(functions.getOrDefault(menu.getId(), List.of()));
        for (Menu child : childrenOf.getOrDefault(menu.getId(), List.of())) {
            if (kept.contains(child.getId())) {
                node.getChildren().add(buildNode(child, childrenOf, kept, functions));
            }
        }
        return node;
    }

    public MenuTreeNode create(MenuSaveRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入菜单名称");
        }
        String code;
        if (StringUtils.hasText(req.getCode())) {
            code = req.getCode().trim();
            if (menuMapper.selectCount(new QueryWrapper<Menu>().eq("code", code)) > 0) {
                throw new BizException(Err.CONFLICT, Err.MENU_CODE_EXISTS);
            }
        } else {
            code = generateCode();
        }
        Menu menu = new Menu();
        if (req.getParentId() == null) {
            menu.setMenuType("level1");
        } else {
            Menu parent = menuMapper.selectById(req.getParentId());
            if (parent == null) {
                throw new BizException(Err.BAD_REQUEST, "父菜单不存在");
            }
            if (!"level1".equals(parent.getMenuType())) {
                throw new BizException(Err.BAD_REQUEST, "父菜单必须是一级菜单");
            }
            if (!StringUtils.hasText(req.getRoute())) {
                throw new BizException(Err.UNPROCESSABLE, Err.MENU_LEVEL2_ROUTE_REQUIRED);
            }
            menu.setMenuType("level2");
            menu.setParentId(req.getParentId());
        }
        menu.setCode(code);
        menu.setName(req.getName().trim());
        menu.setRoute(req.getRoute() == null ? "" : req.getRoute());
        menu.setSort(req.getSort() == null ? 0 : req.getSort());
        menu.setRemark(req.getRemark());
        menu.setCreateTime(LocalDateTime.now());
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.insert(menu);
        return toNode(menu);
    }

    public MenuTreeNode update(Long id, MenuSaveRequest req) {
        Menu menu = require(id);
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入菜单名称");
        }
        if ("level2".equals(menu.getMenuType()) && !StringUtils.hasText(req.getRoute())) {
            throw new BizException(Err.UNPROCESSABLE, Err.MENU_LEVEL2_ROUTE_REQUIRED);
        }
        menu.setName(req.getName().trim());
        menu.setRoute(req.getRoute() == null ? "" : req.getRoute());
        menu.setRemark(req.getRemark());
        if (req.getSort() != null) {
            menu.setSort(req.getSort());
        }
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menu);
        return toNode(menu);
    }

    @Transactional
    public void delete(Long id) {
        Menu menu = require(id);
        Long children = menuMapper.selectCount(new QueryWrapper<Menu>().eq("parent_id", id));
        if (children > 0) {
            throw new BizException(Err.UNPROCESSABLE, Err.MENU_HAS_CHILDREN);
        }
        menuFunctionMapper.delete(new QueryWrapper<MenuFunction>().eq("menu_id", id));
        roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("menu_id", id));
        menuMapper.deleteById(menu.getId());
    }

    public Menu require(Long id) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BizException(Err.NOT_FOUND, "菜单不存在");
        }
        return menu;
    }

    private String generateCode() {
        long n = menuMapper.selectCount(null) + 1;
        String code;
        do {
            code = String.format("MENU_%03d", n);
            n++;
        } while (menuMapper.selectCount(new QueryWrapper<Menu>().eq("code", code)) > 0);
        return code;
    }

    private MenuTreeNode toNode(Menu menu) {
        MenuTreeNode node = new MenuTreeNode();
        node.setId(menu.getId());
        node.setCode(menu.getCode());
        node.setName(menu.getName());
        node.setParentId(menu.getParentId());
        node.setRoute(menu.getRoute());
        node.setMenuType(menu.getMenuType());
        node.setSort(menu.getSort());
        node.setRemark(menu.getRemark());
        node.setCreateTime(menu.getCreateTime());
        node.setUpdateTime(menu.getUpdateTime());
        return node;
    }
}
