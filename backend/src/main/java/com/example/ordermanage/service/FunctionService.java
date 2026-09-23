package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.FunctionListItem;
import com.example.ordermanage.dto.FunctionSaveRequest;
import com.example.ordermanage.entity.Function;
import com.example.ordermanage.entity.Menu;
import com.example.ordermanage.entity.MenuFunction;
import com.example.ordermanage.entity.RoleFunction;
import com.example.ordermanage.mapper.FunctionMapper;
import com.example.ordermanage.mapper.MenuFunctionMapper;
import com.example.ordermanage.mapper.MenuMapper;
import com.example.ordermanage.mapper.RoleFunctionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class FunctionService {

    private static final Pattern PERM_PATTERN = Pattern.compile("^[^:]+:[^:]+$");

    private final FunctionMapper functionMapper;
    private final MenuMapper menuMapper;
    private final MenuFunctionMapper menuFunctionMapper;
    private final RoleFunctionMapper roleFunctionMapper;

    public FunctionService(FunctionMapper functionMapper, MenuMapper menuMapper,
                           MenuFunctionMapper menuFunctionMapper, RoleFunctionMapper roleFunctionMapper) {
        this.functionMapper = functionMapper;
        this.menuMapper = menuMapper;
        this.menuFunctionMapper = menuFunctionMapper;
        this.roleFunctionMapper = roleFunctionMapper;
    }

    public PageResult<FunctionListItem> page(String keyword, Long menuId, String perm, PageParam p) {
        p.validate();
        QueryWrapper<Function> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like("code", like).or().like("name", like));
        }
        if (menuId != null) {
            List<Long> functionIds = menuFunctionMapper.selectList(
                            new QueryWrapper<MenuFunction>().eq("menu_id", menuId))
                    .stream().map(MenuFunction::getFunctionId).collect(Collectors.toList());
            if (functionIds.isEmpty()) {
                return new PageResult<>(0, List.of());
            }
            wrapper.in("id", functionIds);
        }
        if (StringUtils.hasText(perm)) {
            wrapper.like("perm", perm.trim());
        }
        wrapper.orderByAsc("id");
        IPage<Function> page = functionMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        List<Long> ids = page.getRecords().stream().map(Function::getId).collect(Collectors.toList());
        Map<Long, String> menuNames = menuId != null
                ? Map.of(menuId, menuName(menuId))
                : loadMenuNames(page.getRecords());
        List<FunctionListItem> items = page.getRecords().stream()
                .map(f -> toItem(f, menuNames)).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), items);
    }

    private String menuName(Long menuId) {
        Menu menu = menuMapper.selectById(menuId);
        return menu == null ? null : menu.getName();
    }

    private Map<Long, String> loadMenuNames(List<Function> functions) {
        if (functions.isEmpty()) {
            return Map.of();
        }
        List<Long> functionIds = functions.stream().map(Function::getId).collect(Collectors.toList());
        List<MenuFunction> links = menuFunctionMapper.selectList(
                new QueryWrapper<MenuFunction>().in("function_id", functionIds));
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> menuIds = links.stream().map(MenuFunction::getMenuId).distinct()
                .collect(Collectors.toList());
        return menuMapper.selectBatchIds(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, Menu::getName));
    }

    public FunctionListItem create(FunctionSaveRequest req) {
        validate(req);
        String perm = req.getPerm().trim();
        if (functionMapper.selectCount(new QueryWrapper<Function>().eq("perm", perm)) > 0) {
            throw new BizException(Err.CONFLICT, Err.PERM_EXISTS);
        }
        Function function = new Function();
        function.setCode(generateCode(perm));
        function.setName(req.getName().trim());
        function.setPerm(perm);
        function.setRemark(req.getRemark());
        function.setCreateTime(LocalDateTime.now());
        function.setUpdateTime(LocalDateTime.now());
        functionMapper.insert(function);
        upsertMenuFunction(function.getId(), req.getMenuId());
        return toItem(function, loadMenuNames(List.of(function)));
    }

    public FunctionListItem update(Long id, FunctionSaveRequest req) {
        Function function = require(id);
        validate(req);
        String perm = req.getPerm().trim();
        if (functionMapper.selectCount(new QueryWrapper<Function>()
                .eq("perm", perm).ne("id", id)) > 0) {
            throw new BizException(Err.CONFLICT, Err.PERM_EXISTS);
        }
        function.setName(req.getName().trim());
        function.setPerm(perm);
        function.setRemark(req.getRemark());
        function.setUpdateTime(LocalDateTime.now());
        functionMapper.updateById(function);
        upsertMenuFunction(id, req.getMenuId());
        return toItem(function, loadMenuNames(List.of(function)));
    }

    @Transactional
    public void delete(Long id) {
        Function function = require(id);
        menuFunctionMapper.delete(new QueryWrapper<MenuFunction>().eq("function_id", id));
        roleFunctionMapper.delete(new QueryWrapper<RoleFunction>().eq("function_id", id));
        functionMapper.deleteById(function.getId());
    }

    public Function require(Long id) {
        Function function = functionMapper.selectById(id);
        if (function == null) {
            throw new BizException(Err.NOT_FOUND, "功能不存在");
        }
        return function;
    }

    private void validate(FunctionSaveRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入功能名称");
        }
        if (req.getMenuId() == null) {
            throw new BizException(Err.BAD_REQUEST, "请选择所属菜单");
        }
        if (menuMapper.selectById(req.getMenuId()) == null) {
            throw new BizException(Err.BAD_REQUEST, "菜单不存在");
        }
        if (!StringUtils.hasText(req.getPerm())) {
            throw new BizException(Err.BAD_REQUEST, "请输入权限标识");
        }
        if (!PERM_PATTERN.matcher(req.getPerm().trim()).matches()) {
            throw new BizException(Err.BAD_REQUEST, Err.PERM_FORMAT_INVALID);
        }
    }

    private void upsertMenuFunction(Long functionId, Long menuId) {
        menuFunctionMapper.delete(new QueryWrapper<MenuFunction>().eq("function_id", functionId));
        MenuFunction link = new MenuFunction();
        link.setMenuId(menuId);
        link.setFunctionId(functionId);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        menuFunctionMapper.insert(link);
    }

    private String generateCode(String perm) {
        String candidate = "FUNC_" + perm.replace(":", "_").toUpperCase();
        if (functionMapper.selectCount(new QueryWrapper<Function>().eq("code", candidate)) == 0) {
            return candidate;
        }
        long n = functionMapper.selectCount(null) + 1;
        String code;
        do {
            code = candidate + "_" + n;
            n++;
        } while (functionMapper.selectCount(new QueryWrapper<Function>().eq("code", code)) > 0);
        return code;
    }

    public FunctionListItem toItem(Function function, Map<Long, String> menuNames) {
        FunctionListItem item = new FunctionListItem();
        item.setId(function.getId());
        item.setCode(function.getCode());
        item.setName(function.getName());
        item.setPerm(function.getPerm());
        item.setRemark(function.getRemark());
        item.setCreateTime(function.getCreateTime());
        List<MenuFunction> links = menuFunctionMapper.selectList(
                new QueryWrapper<MenuFunction>().eq("function_id", function.getId()));
        if (!links.isEmpty()) {
            Long menuId = links.get(0).getMenuId();
            item.setMenuId(menuId);
            String name = menuNames.get(menuId);
            if (name == null) {
                Menu menu = menuMapper.selectById(menuId);
                name = menu == null ? null : menu.getName();
            }
            item.setMenuName(name);
        }
        return item;
    }
}
