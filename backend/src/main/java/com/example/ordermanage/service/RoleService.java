package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.RoleListItem;
import com.example.ordermanage.dto.RolePermissionsRequest;
import com.example.ordermanage.dto.RolePermissionsResponse;
import com.example.ordermanage.dto.RoleSaveRequest;
import com.example.ordermanage.entity.Role;
import com.example.ordermanage.entity.RoleFunction;
import com.example.ordermanage.entity.RoleMenu;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.RoleFunctionMapper;
import com.example.ordermanage.mapper.RoleMapper;
import com.example.ordermanage.mapper.RoleMenuMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RoleService {

    private static final Set<String> BUILTIN_CODES = Set.of("ROLE_ADMIN", "ROLE_AUDITOR", "ROLE_USER");

    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final RoleFunctionMapper roleFunctionMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(RoleMapper roleMapper, RoleMenuMapper roleMenuMapper,
                       RoleFunctionMapper roleFunctionMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleFunctionMapper = roleFunctionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public PageResult<RoleListItem> page(String keyword, PageParam p) {
        p.validate();
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like("code", like).or().like("name", like));
        }
        wrapper.orderByAsc("id");
        IPage<Role> page = roleMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        List<RoleListItem> items = page.getRecords().stream().map(this::toItem).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), items);
    }

    @OpLog(action = "新增角色", targetSpEL = "#root.result.code")
    public RoleListItem create(RoleSaveRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入角色名称");
        }
        String code;
        if (StringUtils.hasText(req.getCode())) {
            code = req.getCode().trim();
            if (roleMapper.selectCount(new QueryWrapper<Role>().eq("code", code)) > 0) {
                throw new BizException(Err.CONFLICT, Err.CODE_EXISTS);
            }
        } else {
            code = generateCode();
        }
        Role role = new Role();
        role.setCode(code);
        role.setName(req.getName().trim());
        role.setRemark(req.getRemark());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return toItem(role);
    }

    @OpLog(action = "编辑角色", targetSpEL = "#root.args[1].code")
    public RoleListItem update(Long id, RoleSaveRequest req) {
        Role role = require(id);
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入角色名称");
        }
        role.setName(req.getName().trim());
        role.setRemark(req.getRemark());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        return toItem(role);
    }

    @OpLog(action = "删除角色", targetSpEL = "#root.args[0]")
    public void delete(Long id) {
        Role role = require(id);
        Long refs = userRoleMapper.selectCount(new QueryWrapper<UserRole>().eq("role_id", id));
        if (refs > 0) {
            throw new BizException(Err.UNPROCESSABLE, Err.ROLE_REFERENCED);
        }
        roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", id));
        roleFunctionMapper.delete(new QueryWrapper<RoleFunction>().eq("role_id", id));
        roleMapper.deleteById(id);
    }

    public Role require(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(Err.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    public RoleListItem toItem(Role role) {
        RoleListItem item = new RoleListItem();
        item.setId(role.getId());
        item.setCode(role.getCode());
        item.setName(role.getName());
        item.setRemark(role.getRemark());
        item.setCreateTime(role.getCreateTime());
        item.setMenuCount(roleMenuMapper.selectCount(new QueryWrapper<RoleMenu>().eq("role_id", role.getId())));
        item.setFunctionCount(roleFunctionMapper.selectCount(
                new QueryWrapper<RoleFunction>().eq("role_id", role.getId())));
        item.setUserCount(userRoleMapper.selectCount(new QueryWrapper<UserRole>().eq("role_id", role.getId())));
        item.setBuiltin(BUILTIN_CODES.contains(role.getCode()));
        return item;
    }

    private String generateCode() {
        long n = roleMapper.selectCount(null) + 1;
        String code;
        do {
            code = String.format("ROLE_%03d", n);
            n++;
        } while (roleMapper.selectCount(new QueryWrapper<Role>().eq("code", code)) > 0);
        return code;
    }

    public RolePermissionsResponse permissions(Long roleId) {
        Role role = require(roleId);
        List<Long> menuIds = roleMenuMapper.selectList(new QueryWrapper<RoleMenu>()
                        .eq("role_id", roleId).orderByAsc("menu_id"))
                .stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
        List<Long> functionIds = roleFunctionMapper.selectList(new QueryWrapper<RoleFunction>()
                        .eq("role_id", roleId).orderByAsc("function_id"))
                .stream().map(RoleFunction::getFunctionId).collect(Collectors.toList());
        RolePermissionsResponse resp = new RolePermissionsResponse();
        resp.setRoleId(role.getId());
        resp.setMenuIds(menuIds);
        resp.setFunctionIds(functionIds);
        return resp;
    }

    @Transactional
    public RolePermissionsResponse assignPermissions(Long roleId, RolePermissionsRequest req) {
        Role role = require(roleId);
        if ("ROLE_ADMIN".equals(role.getCode())) {
            throw new BizException(Err.UNPROCESSABLE, Err.ROLE_ADMIN_PERMISSION_IMMUTABLE);
        }
        roleMenuMapper.delete(new QueryWrapper<RoleMenu>().eq("role_id", roleId));
        roleFunctionMapper.delete(new QueryWrapper<RoleFunction>().eq("role_id", roleId));
        List<Long> menuIds = req.getMenuIds() == null ? List.of() : req.getMenuIds();
        List<Long> functionIds = req.getFunctionIds() == null ? List.of() : req.getFunctionIds();
        LocalDateTime now = LocalDateTime.now();
        for (Long menuId : new ArrayList<>(menuIds)) {
            if (menuId == null) {
                continue;
            }
            RoleMenu rm = new RoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            rm.setCreateTime(now);
            rm.setUpdateTime(now);
            roleMenuMapper.insert(rm);
        }
        for (Long functionId : new ArrayList<>(functionIds)) {
            if (functionId == null) {
                continue;
            }
            RoleFunction rf = new RoleFunction();
            rf.setRoleId(roleId);
            rf.setFunctionId(functionId);
            rf.setCreateTime(now);
            rf.setUpdateTime(now);
            roleFunctionMapper.insert(rf);
        }
        return permissions(roleId);
    }
}
