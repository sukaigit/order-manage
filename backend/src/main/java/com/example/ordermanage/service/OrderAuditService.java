package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.context.LoginUser;
import com.example.ordermanage.context.UserContext;
import com.example.ordermanage.dto.OrderAuditActionResponse;
import com.example.ordermanage.dto.OrderAuditHistoryItem;
import com.example.ordermanage.dto.OrderAuditListItem;
import com.example.ordermanage.dto.OrderAuditOpinionRequest;
import com.example.ordermanage.dto.OrderAuditPageResult;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.OrderAudit;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.OrderAuditMapper;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
import com.example.ordermanage.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderAuditService {

    public static final String RESULT_PASS = "通过";
    public static final String RESULT_REJECT = "驳回";
    private static final int OPINION_MAX_LENGTH = 255;
    private static final String AUDIT_MENU_CODE = "MENU_AUDIT";

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final OrderAuditMapper orderAuditMapper;
    private final SupplierMapper supplierMapper;
    private final UserMapper userMapper;
    private final PermissionQueryService permissionQueryService;

    public OrderAuditService(OrderService orderService, OrderMapper orderMapper,
                             OrderAuditMapper orderAuditMapper, SupplierMapper supplierMapper,
                             UserMapper userMapper, PermissionQueryService permissionQueryService) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.orderAuditMapper = orderAuditMapper;
        this.supplierMapper = supplierMapper;
        this.userMapper = userMapper;
        this.permissionQueryService = permissionQueryService;
    }

    public OrderAuditPageResult page(String keyword, String status, PageParam p) {
        p.validate();
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = keyword.trim();
            wrapper.and(w -> w.like("order_no", like).or().like("name", like));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        wrapper.orderByAsc("id");
        IPage<Order> page = orderMapper.selectPage(new Page<>(p.getPage(), p.getPageSize()), wrapper);
        long pendingCount = orderMapper.selectCount(
                new QueryWrapper<Order>().eq("status", OrderService.STATUS_PENDING));
        Map<Long, OrderAudit> latestAudits = latestAudits(page.getRecords());
        Map<Long, String> supplierNames = supplierNames(page.getRecords());
        List<OrderAuditListItem> items = page.getRecords().stream().map(order -> {
            OrderAuditListItem item = new OrderAuditListItem();
            item.setId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setName(order.getName());
            item.setAmount(order.getAmount());
            item.setSupplierId(order.getSupplierId());
            item.setSupplierName(supplierNames.get(order.getSupplierId()));
            item.setStatus(order.getStatus());
            item.setCreateTime(order.getCreateTime());
            OrderAudit latest = latestAudits.get(order.getId());
            if (latest != null) {
                item.setAuditor(latest.getAuditor());
                item.setAuditTime(latest.getCreateTime());
                item.setOpinion(latest.getOpinion());
            }
            item.setPendingCount(pendingCount);
            return item;
        }).collect(Collectors.toList());
        return new OrderAuditPageResult(page.getTotal(), items, pendingCount);
    }

    public OrderAuditActionResponse pass(Long id, OrderAuditOpinionRequest req) {
        Order order = requirePending(id);
        String opinion = req == null || req.getOpinion() == null ? "" : req.getOpinion();
        return doAudit(order, RESULT_PASS, opinion, OrderService.STATUS_FINISHED);
    }

    public OrderAuditActionResponse reject(Long id, OrderAuditOpinionRequest req) {
        Order order = requirePending(id);
        String opinion = req == null ? null : req.getOpinion();
        if (!StringUtils.hasText(opinion)) {
            throw new BizException(Err.BAD_REQUEST, Err.AUDIT_REJECT_OPINION_REQUIRED);
        }
        if (opinion.length() > OPINION_MAX_LENGTH) {
            throw new BizException(Err.BAD_REQUEST, "审核意见不能超过255字符");
        }
        return doAudit(order, RESULT_REJECT, opinion, OrderService.STATUS_REJECTED);
    }

    public List<OrderAuditHistoryItem> history(Long orderId) {
        requireAuditMenu();
        orderService.require(orderId);
        return orderAuditMapper.selectList(new QueryWrapper<OrderAudit>()
                        .eq("order_id", orderId)
                        .orderByAsc("create_time", "id"))
                .stream().map(audit -> {
                    OrderAuditHistoryItem item = new OrderAuditHistoryItem();
                    item.setId(audit.getId());
                    item.setOrderId(audit.getOrderId());
                    item.setResult(audit.getResult());
                    item.setAuditor(audit.getAuditor());
                    item.setOpinion(audit.getOpinion());
                    item.setCreateTime(audit.getCreateTime());
                    return item;
                }).collect(Collectors.toList());
    }

    private Order requirePending(Long id) {
        Order order = orderService.require(id);
        if (!OrderService.STATUS_PENDING.equals(order.getStatus())) {
            throw new BizException(Err.UNPROCESSABLE, Err.AUDIT_ONLY_PENDING);
        }
        return order;
    }

    private OrderAuditActionResponse doAudit(Order order, String result, String opinion,
                                             String newStatus) {
        LocalDateTime now = LocalDateTime.now();
        String auditor = resolveAuditor();
        OrderAudit row = new OrderAudit();
        row.setOrderId(order.getId());
        row.setResult(result);
        row.setAuditor(auditor);
        row.setOpinion(opinion);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        orderAuditMapper.insert(row);
        order.setStatus(newStatus);
        order.setUpdateTime(now);
        orderMapper.updateById(order);

        OrderAuditActionResponse.AuditInfo info = new OrderAuditActionResponse.AuditInfo();
        info.setResult(result);
        info.setAuditor(auditor);
        info.setOpinion(opinion);
        info.setAuditTime(now);
        OrderAuditActionResponse resp = new OrderAuditActionResponse();
        resp.setOrderId(order.getId());
        resp.setStatus(newStatus);
        resp.setAudit(info);
        return resp;
    }

    private String resolveAuditor() {
        LoginUser loginUser = UserContext.get();
        User user = userMapper.selectById(loginUser.userId());
        if (user != null && StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return loginUser.username();
    }

    private void requireAuditMenu() {
        LoginUser loginUser = UserContext.get();
        boolean hasAuditMenu = permissionQueryService.menus(loginUser.userId()).stream()
                .anyMatch(node -> AUDIT_MENU_CODE.equals(node.get("code")));
        if (!hasAuditMenu) {
            throw new BizException(Err.FORBIDDEN, "无权限");
        }
    }

    private Map<Long, OrderAudit> latestAudits(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderAuditMapper.selectList(new QueryWrapper<OrderAudit>().in("order_id", orderIds))
                .stream()
                .collect(Collectors.toMap(OrderAudit::getOrderId, a -> a,
                        OrderAuditService::keepLatest, LinkedHashMap::new));
    }

    private static OrderAudit keepLatest(OrderAudit a, OrderAudit b) {
        if (a.getCreateTime() == null) {
            return b;
        }
        if (b.getCreateTime() == null) {
            return a;
        }
        int cmp = a.getCreateTime().compareTo(b.getCreateTime());
        if (cmp != 0) {
            return cmp > 0 ? a : b;
        }
        if (a.getId() == null) {
            return b;
        }
        if (b.getId() == null) {
            return a;
        }
        return a.getId() >= b.getId() ? a : b;
    }

    private Map<Long, String> supplierNames(List<Order> orders) {
        Set<Long> supplierIds = orders.stream()
                .map(Order::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (supplierIds.isEmpty()) {
            return Map.of();
        }
        return supplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName,
                        (a, b) -> a, LinkedHashMap::new));
    }
}
