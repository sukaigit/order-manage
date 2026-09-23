package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageParam;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.context.LoginUser;
import com.example.ordermanage.context.UserContext;
import com.example.ordermanage.dto.MonthlyTrendItem;
import com.example.ordermanage.dto.OrderListItem;
import com.example.ordermanage.dto.ReportSummaryResponse;
import com.example.ordermanage.dto.StatusDistItem;
import com.example.ordermanage.dto.SupplierAmountItem;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReportService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final SupplierMapper supplierMapper;

    public ReportService(OrderService orderService, OrderMapper orderMapper,
                         SupplierMapper supplierMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.supplierMapper = supplierMapper;
    }

    public ReportSummaryResponse summary(String startDate, String endDate, Long supplierId) {
        requireAdmin();
        ReportSummaryResponse resp = new ReportSummaryResponse();
        long total = orderMapper.selectCount(filterWrapper(startDate, endDate, supplierId));
        resp.setTotal(total);
        if (total == 0) {
            resp.setStatusDist(List.of());
            resp.setSupplierAmount(List.of());
            resp.setMonthlyTrend(List.of());
            return resp;
        }
        resp.setStatusDist(statusDist(startDate, endDate, supplierId, total));
        resp.setSupplierAmount(supplierAmount(startDate, endDate, supplierId));
        resp.setMonthlyTrend(monthlyTrend(startDate, endDate, supplierId));
        return resp;
    }

    public PageResult<OrderListItem> detail(String startDate, String endDate, Long supplierId,
                                            PageParam p) {
        requireAdmin();
        return orderService.page(null, supplierId, null, startDate, endDate, p);
    }

    public List<OrderListItem> exportRows(String startDate, String endDate, Long supplierId) {
        requireAdmin();
        return orderService.listAll(null, supplierId, null, startDate, endDate);
    }

    private List<StatusDistItem> statusDist(String startDate, String endDate, Long supplierId,
                                            long total) {
        Map<String, Long> counts = orderMapper.selectMaps(
                        filterWrapper(startDate, endDate, supplierId)
                                .select("status, COUNT(*) AS cnt")
                                .groupBy("status"))
                .stream().collect(Collectors.toMap(
                        row -> (String) row.get("status"),
                        row -> ((Number) row.get("cnt")).longValue(),
                        (a, b) -> a));
        List<String> order = List.of(OrderService.STATUS_PENDING, OrderService.STATUS_REJECTED,
                OrderService.STATUS_FINISHED);
        List<StatusDistItem> items = new ArrayList<>();
        for (String status : order) {
            long count = counts.getOrDefault(status, 0L);
            StatusDistItem item = new StatusDistItem();
            item.setStatus(status);
            item.setCount(count);
            item.setPercent(BigDecimal.valueOf(Math.round(count * 1000.0 / total), 1));
            items.add(item);
        }
        return items;
    }

    private List<SupplierAmountItem> supplierAmount(String startDate, String endDate, Long supplierId) {
        List<Map<String, Object>> rows = orderMapper.selectMaps(
                filterWrapper(startDate, endDate, supplierId)
                        .select("supplier_id, SUM(amount) AS amt")
                        .groupBy("supplier_id")
                        .orderByDesc("amt"));
        Set<Long> supplierIds = rows.stream()
                .map(row -> ((Number) row.get("supplier_id")).longValue())
                .collect(Collectors.toSet());
        Map<Long, String> names = supplierIds.isEmpty()
                ? Map.of()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName,
                                (a, b) -> a, java.util.LinkedHashMap::new));
        List<SupplierAmountItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SupplierAmountItem item = new SupplierAmountItem();
            Long id = ((Number) row.get("supplier_id")).longValue();
            item.setSupplierId(id);
            item.setSupplierName(names.get(id));
            item.setAmount(toDecimal(row.get("amt")));
            items.add(item);
        }
        return items;
    }

    private List<MonthlyTrendItem> monthlyTrend(String startDate, String endDate, Long supplierId) {
        List<Map<String, Object>> rows = orderMapper.selectMaps(
                filterWrapper(startDate, endDate, supplierId)
                        .select("strftime('%Y-%m', create_time) AS month, COUNT(*) AS cnt, "
                                + "SUM(amount) AS amt")
                        .groupBy("strftime('%Y-%m', create_time)")
                        .orderByAsc("month"));
        List<MonthlyTrendItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            MonthlyTrendItem item = new MonthlyTrendItem();
            item.setMonth((String) row.get("month"));
            item.setCount(((Number) row.get("cnt")).longValue());
            item.setAmount(toDecimal(row.get("amt")));
            items.add(item);
        }
        return items;
    }

    private QueryWrapper<Order> filterWrapper(String startDate, String endDate, Long supplierId) {
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge("create_time", parseDate(startDate, false));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le("create_time", parseDate(endDate, true));
        }
        if (supplierId != null) {
            wrapper.eq("supplier_id", supplierId);
        }
        return wrapper;
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BizException(Err.BAD_REQUEST, "日期格式须为yyyy-MM-dd");
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String text = value.toString();
        BigDecimal decimal = new BigDecimal(text);
        return decimal.scale() < 1 ? decimal.setScale(1) : decimal;
    }

    private void requireAdmin() {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BizException(Err.UNAUTHORIZED, "未认证");
        }
        if (!ROLE_ADMIN.equals(user.roleCode())) {
            throw new BizException(Err.FORBIDDEN, "无权限");
        }
    }
}
