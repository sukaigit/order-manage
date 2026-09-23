package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PageResult;
import com.example.ordermanage.dto.LogListItem;
import com.example.ordermanage.entity.OperationLog;
import com.example.ordermanage.mapper.OperationLogMapper;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LogService {

    public static final int DEFAULT_PAGE_SIZE = 5;
    public static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(5, 10, 20, 50);

    private final OperationLogMapper operationLogMapper;

    public LogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public PageResult<LogListItem> page(String operator, String action, String target, String ip,
                                         String date, Integer page, Integer pageSize) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new BizException(Err.BAD_REQUEST, Err.PAGE_INVALID);
        }
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(operator)) {
            wrapper.like("user", operator.trim());
        }
        if (StringUtils.hasText(action)) {
            wrapper.like("action", action.trim());
        }
        if (StringUtils.hasText(target)) {
            wrapper.like("target", target.trim());
        }
        if (StringUtils.hasText(ip)) {
            wrapper.like("ip", ip.trim());
        }
        if (StringUtils.hasText(date)) {
            wrapper.apply("date(create_time) = {0}", date.trim());
        }
        wrapper.orderByAsc("id");
        IPage<OperationLog> result = operationLogMapper.selectPage(new Page<>(pageNo, size), wrapper);
        List<LogListItem> items = result.getRecords().stream().map(this::toItem)
                .collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), items);
    }

    public LogListItem toItem(OperationLog log) {
        LogListItem item = new LogListItem();
        item.setId(log.getId());
        item.setOperator(log.getUser());
        item.setAction(log.getAction());
        item.setTarget(log.getTarget());
        item.setIp(log.getIp());
        item.setCreateTime(log.getCreateTime());
        return item;
    }
}
