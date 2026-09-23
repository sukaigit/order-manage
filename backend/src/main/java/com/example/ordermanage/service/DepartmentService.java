package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.DepartmentListItem;
import com.example.ordermanage.dto.DepartmentSaveRequest;
import com.example.ordermanage.entity.Department;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.DepartmentMapper;
import com.example.ordermanage.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public DepartmentService(DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    public PageResult<DepartmentListItem> page(String keyword, PageParam p) {
        p.validate();
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like("code", like).or().like("name", like));
        }
        wrapper.orderByAsc("id");
        IPage<Department> page = departmentMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        List<DepartmentListItem> items = page.getRecords().stream().map(this::toItem).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), items);
    }

    public DepartmentListItem create(DepartmentSaveRequest req) {
        validate(req);
        String code = req.getCode().trim();
        if (departmentMapper.selectCount(new QueryWrapper<Department>().eq("code", code)) > 0) {
            throw new BizException(Err.CONFLICT, Err.DEPT_CODE_EXISTS);
        }
        Department dept = new Department();
        dept.setCode(code);
        dept.setName(req.getName().trim());
        dept.setRemark(req.getRemark());
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        departmentMapper.insert(dept);
        return toItem(dept);
    }

    public DepartmentListItem update(Long id, DepartmentSaveRequest req) {
        Department dept = require(id);
        validate(req);
        String code = req.getCode().trim();
        if (departmentMapper.selectCount(new QueryWrapper<Department>()
                .eq("code", code).ne("id", id)) > 0) {
            throw new BizException(Err.CONFLICT, Err.DEPT_CODE_EXISTS);
        }
        dept.setCode(code);
        dept.setName(req.getName().trim());
        dept.setRemark(req.getRemark());
        dept.setUpdateTime(LocalDateTime.now());
        departmentMapper.updateById(dept);
        return toItem(dept);
    }

    public void delete(Long id) {
        Department dept = require(id);
        Long refs = userMapper.selectCount(new QueryWrapper<User>().eq("department_id", id));
        if (refs > 0) {
            throw new BizException(Err.UNPROCESSABLE, Err.DEPT_REFERENCED);
        }
        departmentMapper.deleteById(dept.getId());
    }

    public Department require(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BizException(Err.NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    private void validate(DepartmentSaveRequest req) {
        if (!StringUtils.hasText(req.getCode())) {
            throw new BizException(Err.BAD_REQUEST, "请输入部门编号");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入部门名称");
        }
    }

    public DepartmentListItem toItem(Department dept) {
        DepartmentListItem item = new DepartmentListItem();
        item.setId(dept.getId());
        item.setCode(dept.getCode());
        item.setName(dept.getName());
        item.setRemark(dept.getRemark());
        item.setCreateTime(dept.getCreateTime());
        return item;
    }
}
