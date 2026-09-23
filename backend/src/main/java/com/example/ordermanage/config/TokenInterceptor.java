package com.example.ordermanage.config;

import com.example.ordermanage.common.Result;
import com.example.ordermanage.context.LoginUser;
import com.example.ordermanage.context.UserContext;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.UserMapper;
import com.example.ordermanage.service.PermissionQueryService;
import com.example.ordermanage.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final UserMapper userMapper;
    private final PermissionQueryService permissionQueryService;
    private final ObjectMapper objectMapper;

    public TokenInterceptor(TokenService tokenService, UserMapper userMapper,
                            PermissionQueryService permissionQueryService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userMapper = userMapper;
        this.permissionQueryService = permissionQueryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        Long userId = tokenService.resolve(token);
        if (userId == null) {
            writeUnauthorized(response);
            return false;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            writeUnauthorized(response);
            return false;
        }
        String roleCode = permissionQueryService.primaryRole(userId) != null
                ? permissionQueryService.primaryRole(userId).getCode() : null;
        UserContext.set(new LoginUser(userId, user.getUsername(), roleCode, token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(401, "未认证")));
    }
}
