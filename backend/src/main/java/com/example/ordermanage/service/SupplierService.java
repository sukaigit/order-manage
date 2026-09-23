package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.SupplierOption;
import com.example.ordermanage.dto.SupplierSaveRequest;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final OrderMapper orderMapper;

    public SupplierService(SupplierMapper supplierMapper, OrderMapper orderMapper) {
        this.supplierMapper = supplierMapper;
        this.orderMapper = orderMapper;
    }

    public PageResult<Supplier> page(String keyword, String status, PageParam p) {
        p.validate();
        QueryWrapper<Supplier> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like("name", like).or().like("contact", like));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByAsc("id");
        IPage<Supplier> page = supplierMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public Supplier create(SupplierSaveRequest req) {
        validate(req);
        String code = req.getCode().trim();
        if (supplierMapper.selectCount(new QueryWrapper<Supplier>().eq("code", code)) > 0) {
            throw new BizException(Err.CONFLICT, Err.SUPPLIER_CODE_EXISTS);
        }
        Supplier supplier = new Supplier();
        supplier.setCode(code);
        supplier.setName(req.getName().trim());
        supplier.setContact(req.getContact().trim());
        supplier.setPhone(req.getPhone().trim());
        supplier.setAddress(req.getAddress());
        supplier.setStatus("启用");
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.insert(supplier);
        return supplier;
    }

    public Supplier update(Long id, SupplierSaveRequest req) {
        Supplier supplier = require(id);
        validate(req);
        supplier.setName(req.getName().trim());
        supplier.setContact(req.getContact().trim());
        supplier.setPhone(req.getPhone().trim());
        supplier.setAddress(req.getAddress());
        if (StringUtils.hasText(req.getStatus())) {
            if (!"启用".equals(req.getStatus()) && !"停用".equals(req.getStatus())) {
                throw new BizException(Err.BAD_REQUEST, "状态只能为启用或停用");
            }
            supplier.setStatus(req.getStatus());
        }
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(supplier);
        return supplier;
    }

    public void delete(Long id) {
        Supplier supplier = require(id);
        Long refs = orderMapper.selectCount(new QueryWrapper<Order>().eq("supplier_id", id));
        if (refs > 0) {
            throw new BizException(Err.UNPROCESSABLE, Err.SUPPLIER_REFERENCED);
        }
        supplierMapper.deleteById(supplier.getId());
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        Supplier supplier = require(id);
        if (!"启用".equals(status) && !"停用".equals(status)) {
            throw new BizException(Err.BAD_REQUEST, "状态只能为启用或停用");
        }
        supplier.setStatus(status);
        supplier.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(supplier);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", supplier.getId());
        data.put("status", supplier.getStatus());
        return data;
    }

    public List<SupplierOption> options() {
        return supplierMapper.selectList(new QueryWrapper<Supplier>()
                        .eq("status", "启用").orderByAsc("id"))
                .stream().map(s -> {
                    SupplierOption option = new SupplierOption();
                    option.setId(s.getId());
                    option.setCode(s.getCode());
                    option.setName(s.getName());
                    return option;
                }).collect(Collectors.toList());
    }

    public Supplier require(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BizException(Err.NOT_FOUND, "供应商不存在");
        }
        return supplier;
    }

    private void validate(SupplierSaveRequest req) {
        if (!StringUtils.hasText(req.getCode())) {
            throw new BizException(Err.BAD_REQUEST, "请输入供应商编号");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入供应商名称");
        }
        if (!StringUtils.hasText(req.getContact())) {
            throw new BizException(Err.BAD_REQUEST, "请输入联系人");
        }
        if (!StringUtils.hasText(req.getPhone())) {
            throw new BizException(Err.BAD_REQUEST, "请输入联系电话");
        }
    }
}
