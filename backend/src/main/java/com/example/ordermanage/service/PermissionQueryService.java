package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.entity.Department;
import com.example.ordermanage.entity.Function;
import com.example.ordermanage.entity.Menu;
import com.example.ordermanage.entity.Role;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.DepartmentMapper;
import com.example.ordermanage.mapper.FunctionMapper;
import com.example.ordermanage.mapper.MenuMapper;
import com.example.ordermanage.mapper.OrganizationMapper;
import com.example.ordermanage.mapper.RoleFunctionMapper;
import com.example.ordermanage.mapper.RoleMapper;
import com.example.ordermanage.mapper.RoleMenuMapper;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
import com.example.ordermanage.entity.Organization;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PermissionQueryService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final RoleFunctionMapper roleFunctionMapper;
    private final FunctionMapper functionMapper;
    private final DepartmentMapper departmentMapper;
    private final OrganizationMapper organizationMapper;

    public PermissionQueryService(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                                  RoleMenuMapper roleMenuMapper, MenuMapper menuMapper,
                                  RoleFunctionMapper roleFunctionMapper, FunctionMapper functionMapper,
                                  DepartmentMapper departmentMapper, OrganizationMapper organizationMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuMapper = menuMapper;
        this.roleFunctionMapper = roleFunctionMapper;
        this.functionMapper = functionMapper;
        this.departmentMapper = departmentMapper;
        this.organizationMapper = organizationMapper;
    }

    public Role primaryRole(Long userId) {
        List<UserRole> links = userRoleMapper.selectList(
                new QueryWrapper<UserRole>().eq("user_id", userId).orderByAsc("id"));
        if (links.isEmpty()) {
            return null;
        }
        return roleMapper.selectById(links.get(0).getRoleId());
    }

    public List<Long> roleIds(Long userId) {
        return userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("user_id", userId))
                .stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }

    public List<Long> menuIdsForUser(Long userId) {
        List<Long> roles = roleIds(userId);
        if (roles.isEmpty()) {
            return List.of();
        }
        return roleMenuMapper.selectList(new QueryWrapper<com.example.ordermanage.entity.RoleMenu>()
                        .in("role_id", roles))
                .stream().map(com.example.ordermanage.entity.RoleMenu::getMenuId)
                .distinct().collect(Collectors.toList());
    }

    /** Menu tree filtered by user's roles (tb_role_menu). */
    public List<Map<String, Object>> menus(Long userId) {
        List<Long> allowed = menuIdsForUser(userId);
        if (allowed.isEmpty()) {
            return List.of();
        }
        List<Menu> menus = menuMapper.selectBatchIds(allowed);
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        for (Menu m : menus) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("code", m.getCode());
            node.put("name", m.getName());
            node.put("route", m.getRoute());
            node.put("menu_type", m.getMenuType());
            node.put("parent_id", m.getParentId());
            node.put("sort", m.getSort());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodes.put(m.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Menu m : menus) {
            Map<String, Object> node = nodes.get(m.getId());
            if (m.getParentId() == null || !nodes.containsKey(m.getParentId())) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodes.get(m.getParentId()).get("children");
                children.add(node);
            }
        }
        roots.sort(Comparator.comparingInt(n -> (Integer) n.getOrDefault("sort", 0)));
        return roots;
    }

    /** Function perms (tb_function.perm) granted via user's roles. */
    public List<String> permissions(Long userId) {
        List<Long> roles = roleIds(userId);
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> functionIds = roleFunctionMapper.selectList(
                        new QueryWrapper<com.example.ordermanage.entity.RoleFunction>().in("role_id", roles))
                .stream().map(com.example.ordermanage.entity.RoleFunction::getFunctionId)
                .distinct().collect(Collectors.toList());
        if (functionIds.isEmpty()) {
            return List.of();
        }
        return functionMapper.selectBatchIds(functionIds).stream()
                .map(Function::getPerm).sorted().collect(Collectors.toList());
    }

    public boolean hasPermission(Long userId, String perm) {
        return permissions(userId).contains(perm);
    }

    /** Login/me user payload without password. */
    public Map<String, Object> userInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Map.of();
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("real_name", user.getRealName());
        info.put("status", user.getStatus());
        info.put("department_id", user.getDepartmentId());
        info.put("organization_id", user.getOrganizationId());
        info.put("first_login", user.getFirstLogin() != null && user.getFirstLogin() == 1);
        info.put("fail_count", user.getFailCount() == null ? 0 : user.getFailCount());
        if (user.getDepartmentId() != null) {
            Department d = departmentMapper.selectById(user.getDepartmentId());
            info.put("department_name", d == null ? null : d.getName());
        } else {
            info.put("department_name", null);
        }
        if (user.getOrganizationId() != null) {
            Organization o = organizationMapper.selectById(user.getOrganizationId());
            info.put("organization_name", o == null ? null : o.getName());
        } else {
            info.put("organization_name", null);
        }
        Role role = primaryRole(userId);
        if (role != null) {
            info.put("role_id", role.getId());
            info.put("role_code", role.getCode());
            info.put("role_name", role.getName());
        } else {
            info.put("role_id", null);
            info.put("role_code", null);
            info.put("role_name", null);
        }
        return info;
    }
}
