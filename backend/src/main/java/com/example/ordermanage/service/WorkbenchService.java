package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.ordermanage.dto.WorkbenchStatsResponse;
import com.example.ordermanage.entity.Order;
import com.example.ordermanage.entity.Supplier;
import com.example.ordermanage.mapper.OrderMapper;
import com.example.ordermanage.mapper.SupplierMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkbenchService {

    private static final String SUPPLIER_ENABLED = "启用";
    private static final String WELCOME_TITLE = "欢迎使用订单管理系统";
    private static final String WELCOME_TIPS =
            "可前往「订单管理」维护订单，「订单审核」处理审核，「统计报表」查看经营分析。";

    private final OrderMapper orderMapper;
    private final SupplierMapper supplierMapper;

    public WorkbenchService(OrderMapper orderMapper, SupplierMapper supplierMapper) {
        this.orderMapper = orderMapper;
        this.supplierMapper = supplierMapper;
    }

    public WorkbenchStatsResponse stats() {
        long total = orderMapper.selectCount(null);
        long pending = countStatus(OrderService.STATUS_PENDING);
        long done = countStatus(OrderService.STATUS_FINISHED);
        long rejected = countStatus(OrderService.STATUS_REJECTED);
        long activeSuppliers = supplierMapper.selectCount(
                new QueryWrapper<Supplier>().eq("status", SUPPLIER_ENABLED));

        WorkbenchStatsResponse resp = new WorkbenchStatsResponse();
        resp.setTotalOrders(total);
        resp.setPendingOrders(pending);
        resp.setDoneOrders(done);
        resp.setRejectedOrders(rejected);
        resp.setActiveSuppliers(activeSuppliers);
        WorkbenchStatsResponse.Welcome welcome = new WorkbenchStatsResponse.Welcome();
        welcome.setTitle(WELCOME_TITLE);
        welcome.setPendingCount(pending);
        welcome.setRejectedCount(rejected);
        welcome.setTips(WELCOME_TIPS);
        resp.setWelcome(welcome);
        return resp;
    }

    private long countStatus(String status) {
        return orderMapper.selectCount(new QueryWrapper<Order>().eq("status", status));
    }
}
