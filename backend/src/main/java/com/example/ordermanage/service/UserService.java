package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.annotation.OpLog;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.ResetPasswordResponse;
import com.example.ordermanage.dto.UserListItem;
import com.example.ordermanage.dto.UserQuery;
import com.example.ordermanage.dto.UserSaveRequest;
import com.example.ordermanage.entity.Department;
import com.example.ordermanage.entity.Organization;
import com.example.ordermanage.entity.Role;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.entity.UserRole;
import com.example.ordermanage.mapper.DepartmentMapper;
import com.example.ordermanage.mapper.OrganizationMapper;
import com.example.ordermanage.mapper.RoleMapper;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.mapper.UserRoleMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    public static final String DEFAULT_PASSWORD = "Uu888888!";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;
    private final OrganizationMapper organizationMapper;

    public UserService(UserMapper userMapper, UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                       DepartmentMapper departmentMapper, OrganizationMapper organizationMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.departmentMapper = departmentMapper;
        this.organizationMapper = organizationMapper;
    }

    public PageResult<UserListItem> page(UserQuery q, PageParam p) {
        p.validate();
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(q.getKeyword())) {
            String like = q.getKeyword().trim();
            wrapper.and(w -> w.like("username", like).or().like("real_name", like));
        }
        if (StringUtils.hasText(q.getStatus())) {
            wrapper.eq("status", q.getStatus());
        }
        if (q.getDepartmentId() != null) {
            wrapper.eq("department_id", q.getDepartmentId());
        }
        if (q.getOrganizationId() != null) {
            wrapper.eq("organization_id", q.getOrganizationId());
        }
        if (q.getRoleId() != null) {
            List<Long> userIds = userRoleMapper.selectList(new QueryWrapper<UserRole>().eq("role_id", q.getRoleId()))
                    .stream().map(UserRole::getUserId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return new PageResult<>(0, List.of());
            }
            wrapper.in("id", userIds);
        }
        wrapper.orderByAsc("id");
        IPage<User> page = userMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        List<UserListItem> items = page.getRecords().stream().map(this::toItem).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), items);
    }

    public UserListItem toItem(User user) {
        UserListItem item = new UserListItem();
        item.setId(user.getId());
        item.setUsername(user.getUsername());
        item.setRealName(user.getRealName());
        item.setStatus(user.getStatus());
        item.setFirstLogin(user.getFirstLogin() != null && user.getFirstLogin() == 1);
        int fail = user.getFailCount() == null ? 0 : user.getFailCount();
        item.setFailCount(fail);
        item.setLocked(fail >= 5);
        item.setRemark(user.getRemark());
        item.setCreateTime(user.getCreateTime());
        item.setDepartmentId(user.getDepartmentId());
        if (user.getDepartmentId() != null) {
            Department d = departmentMapper.selectById(user.getDepartmentId());
            item.setDepartmentName(d == null ? null : d.getName());
        }
        item.setOrganizationId(user.getOrganizationId());
        if (user.getOrganizationId() != null) {
            Organization o = organizationMapper.selectById(user.getOrganizationId());
            item.setOrganizationName(o == null ? null : o.getName());
        }
        List<UserRole> links = userRoleMapper.selectList(new QueryWrapper<UserRole>()
                .eq("user_id", user.getId()).orderByAsc("id"));
        if (!links.isEmpty()) {
            item.setRoleId(links.get(0).getRoleId());
            Role role = roleMapper.selectById(links.get(0).getRoleId());
            item.setRoleName(role == null ? null : role.getName());
        }
        return item;
    }

    public User require(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(Err.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @OpLog(action = "新增用户", targetSpEL = "#root.result.username")
    public UserListItem create(UserSaveRequest req) {
        validate(req);
        if (userMapper.selectCount(new QueryWrapper<User>().eq("username", req.getUsername().trim())) > 0) {
            throw new BizException(Err.CONFLICT, Err.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(AuthService.md5(DEFAULT_PASSWORD));
        user.setRealName(req.getRealName().trim());
        user.setStatus(StringUtils.hasText(req.getStatus()) ? req.getStatus() : "启用");
        user.setDepartmentId(req.getDepartmentId());
        user.setOrganizationId(req.getOrganizationId());
        user.setFirstLogin(1);
        user.setFailCount(0);
        user.setRemark(req.getRemark());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        UserRole link = new UserRole();
        link.setUserId(user.getId());
        link.setRoleId(req.getRoleId());
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        userRoleMapper.insert(link);
        return toItem(user);
    }

    @OpLog(action = "编辑用户", targetSpEL = "#root.args[1].username")
    public UserListItem update(Long id, UserSaveRequest req) {
        User user = require(id);
        validate(req);
        Long count = userMapper.selectCount(new QueryWrapper<User>()
                .eq("username", req.getUsername().trim()).ne("id", id));
        if (count > 0) {
            throw new BizException(Err.CONFLICT, Err.USERNAME_EXISTS);
        }
        user.setUsername(req.getUsername().trim());
        user.setRealName(req.getRealName().trim());
        user.setDepartmentId(req.getDepartmentId());
        user.setOrganizationId(req.getOrganizationId());
        if (StringUtils.hasText(req.getStatus())) {
            if (!"启用".equals(req.getStatus()) && !"停用".equals(req.getStatus())) {
                throw new BizException(Err.BAD_REQUEST, "状态只能为启用或停用");
            }
            user.setStatus(req.getStatus());
        }
        user.setRemark(req.getRemark());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        UserRole existing = userRoleMapper.selectOne(new QueryWrapper<UserRole>()
                .eq("user_id", id).orderByAsc("id").last("LIMIT 1"));
        if (existing != null) {
            if (!req.getRoleId().equals(existing.getRoleId())) {
                existing.setRoleId(req.getRoleId());
                existing.setUpdateTime(LocalDateTime.now());
                userRoleMapper.updateById(existing);
            }
        } else {
            UserRole link = new UserRole();
            link.setUserId(id);
            link.setRoleId(req.getRoleId());
            link.setCreateTime(LocalDateTime.now());
            link.setUpdateTime(LocalDateTime.now());
            userRoleMapper.insert(link);
        }
        return toItem(user);
    }

    private void validate(UserSaveRequest req) {
        if (!StringUtils.hasText(req.getUsername())) {
            throw new BizException(Err.BAD_REQUEST, "请输入用户名");
        }
        if (!StringUtils.hasText(req.getRealName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入姓名");
        }
        if (req.getRoleId() == null) {
            throw new BizException(Err.BAD_REQUEST, "请选择角色");
        }
        if (roleMapper.selectById(req.getRoleId()) == null) {
            throw new BizException(Err.BAD_REQUEST, "角色不存在");
        }
        if (req.getDepartmentId() == null) {
            throw new BizException(Err.BAD_REQUEST, "请选择部门");
        }
        if (departmentMapper.selectById(req.getDepartmentId()) == null) {
            throw new BizException(Err.BAD_REQUEST, "部门不存在");
        }
        if (req.getOrganizationId() == null) {
            throw new BizException(Err.BAD_REQUEST, "请选择机构");
        }
        if (organizationMapper.selectById(req.getOrganizationId()) == null) {
            throw new BizException(Err.BAD_REQUEST, "机构不存在");
        }
        if (StringUtils.hasText(req.getStatus())
                && !"启用".equals(req.getStatus()) && !"停用".equals(req.getStatus())) {
            throw new BizException(Err.BAD_REQUEST, "状态只能为启用或停用");
        }
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        User user = require(id);
        if (!"启用".equals(status) && !"停用".equals(status)) {
            throw new BizException(Err.BAD_REQUEST, "状态只能为启用或停用");
        }
        if ("admin".equals(user.getUsername()) && "停用".equals(status)) {
            throw new BizException(Err.UNPROCESSABLE, Err.ADMIN_CANNOT_DISABLE);
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("status", user.getStatus());
        return data;
    }

    public ResetPasswordResponse resetPassword(Long id) {
        User user = require(id);
        user.setPassword(AuthService.md5(DEFAULT_PASSWORD));
        user.setFirstLogin(1);
        user.setFailCount(0);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        ResetPasswordResponse resp = new ResetPasswordResponse();
        resp.setId(user.getId());
        resp.setDefaultPassword(DEFAULT_PASSWORD);
        resp.setFirstLogin(true);
        return resp;
    }

    @OpLog(action = "删除用户", targetSpEL = "#root.args[0]")
    public void delete(Long id) {
        User user = require(id);
        if ("admin".equals(user.getUsername())) {
            throw new BizException(Err.UNPROCESSABLE, Err.ADMIN_CANNOT_DELETE);
        }
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", id));
        userMapper.deleteById(id);
    }
}
