package com.example.ordermanage.service;

import com.example.ordermanage.context.LoginUser;
import com.example.ordermanage.context.UserContext;
import com.example.ordermanage.entity.OperationLog;
import com.example.ordermanage.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogWriter {

    private final OperationLogMapper operationLogMapper;

    public OperationLogWriter(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    public void write(String operator, String action, String target) {
        OperationLog log = new OperationLog();
        log.setUser(operator == null || operator.isEmpty() ? "系统" : operator);
        log.setAction(action);
        log.setTarget(target);
        log.setIp(resolveIp());
        log.setCreateTime(LocalDateTime.now());
        log.setUpdateTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    public static String currentOperator() {
        LoginUser user = UserContext.get();
        return user == null ? null : user.username();
    }

    public static String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "";
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
            return request.getRemoteAddr() == null ? "" : request.getRemoteAddr();
        } catch (Exception e) {
            return "";
        }
    }
}
