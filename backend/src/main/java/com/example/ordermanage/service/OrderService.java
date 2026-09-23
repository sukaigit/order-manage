package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.OrderListItem;
import com.example.ordermanage.dto.OrderSaveRequest;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderService {

    public static final String STATUS_PENDING = "待审核";
    public static final String STATUS_REJECTED = "已驳回";
    public static final String STATUS_FINISHED = "已完成";

    private final OrderMapper orderMapper;
    private final SupplierMapper supplierMapper;

    public OrderService(OrderMapper orderMapper, SupplierMapper supplierMapper) {
        this.orderMapper = orderMapper;
        this.supplierMapper = supplierMapper;
    }

    public PageResult<OrderListItem> page(String keyword, Long supplierId, String status,
                                          String startDate, String endDate, PageParam p) {
        p.validate();
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like("order_no", like).or().like("name", like));
        }
        if (supplierId != null) {
            wrapper.eq("supplier_id", supplierId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge("create_time", parseDate(startDate, false));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("create_time", parseDate(endDate, true));
        }
        wrapper.orderByAsc("id");
        IPage<Order> page = orderMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        return new PageResult<>(page.getTotal(), toListItems(page.getRecords()));
    }

    public OrderListItem create(OrderSaveRequest req) {
        validate(req);
        if (orderMapper.selectCount(new QueryWrapper<Order>().eq("order_no", req.getOrderNo().trim())) > 0) {
            throw new BizException(Err.CONFLICT, Err.ORDER_NO_EXISTS);
        }
        requireEnabledSupplier(req.getSupplierId());
        Order order = new Order();
        order.setOrderNo(req.getOrderNo().trim());
        order.setName(req.getName().trim());
        order.setAmount(req.getAmount());
        order.setSupplierId(req.getSupplierId());
        order.setRemark(req.getRemark());
        order.setStatus(STATUS_PENDING);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        return toListItem(order);
    }

    public OrderListItem update(Long id, OrderSaveRequest req) {
        Order order = require(id);
        if (STATUS_FINISHED.equals(order.getStatus())) {
            throw new BizException(Err.UNPROCESSABLE, Err.ORDER_FINISHED_EDIT);
        }
        validate(req);
        requireEnabledSupplier(req.getSupplierId());
        order.setName(req.getName().trim());
        order.setAmount(req.getAmount());
        order.setSupplierId(req.getSupplierId());
        order.setRemark(req.getRemark());
        if (STATUS_REJECTED.equals(order.getStatus())) {
            order.setStatus(STATUS_PENDING);
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return toListItem(order);
    }

    public void delete(Long id) {
        Order order = require(id);
        if (STATUS_FINISHED.equals(order.getStatus())) {
            throw new BizException(Err.UNPROCESSABLE, Err.ORDER_FINISHED_DELETE);
        }
        orderMapper.deleteById(order.getId());
    }

    public OrderListItem detail(Long id) {
        return toListItem(require(id));
    }

    public Order require(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(Err.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private void validate(OrderSaveRequest req) {
        if (!StringUtils.hasText(req.getOrderNo())) {
            throw new BizException(Err.BAD_REQUEST, "请输入订单编号");
        }
        if (!StringUtils.hasText(req.getName())) {
            throw new BizException(Err.BAD_REQUEST, "请输入订单名称");
        }
        if (req.getAmount() == null) {
            throw new BizException(Err.BAD_REQUEST, "请输入金额");
        }
        if (req.getAmount().signum() < 0) {
            throw new BizException(Err.BAD_REQUEST, Err.AMOUNT_NEGATIVE);
        }
        if (req.getSupplierId() == null) {
            throw new BizException(Err.BAD_REQUEST, "请选择供应商");
        }
    }

    private void requireEnabledSupplier(Long supplierId) {
        Supplier supplier = supplierMapper.selectById(supplierId);
        if (supplier == null || !"启用".equals(supplier.getStatus())) {
            throw new BizException(Err.UNPROCESSABLE, Err.SUPPLIER_DISABLED);
        }
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BizException(Err.BAD_REQUEST, "日期格式须为yyyy-MM-dd");
        }
    }

    private List<OrderListItem> toListItems(List<Order> orders) {
        Set<Long> supplierIds = orders.stream()
                .map(Order::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> supplierNames = supplierIds.isEmpty()
                ? Map.of()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName,
                                (a, b) -> a, java.util.LinkedHashMap::new));
        return orders.stream().map(o -> {
            OrderListItem item = toListItem(o);
            item.setSupplierName(supplierNames.get(o.getSupplierId()));
            return item;
        }).collect(Collectors.toList());
    }

    private OrderListItem toListItem(Order order) {
        OrderListItem item = new OrderListItem();
        item.setId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setName(order.getName());
        item.setAmount(order.getAmount());
        item.setSupplierId(order.getSupplierId());
        item.setStatus(order.getStatus());
        item.setRemark(order.getRemark());
        item.setCreateTime(order.getCreateTime());
        item.setUpdateTime(order.getUpdateTime());
        if (order.getSupplierId() != null) {
            Supplier supplier = supplierMapper.selectById(order.getSupplierId());
            item.setSupplierName(supplier != null ? supplier.getName() : null);
        }
        return item;
    }
}
