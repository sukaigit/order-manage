package com.example.ordermanage.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.common.PasswordValidator;
import com.example.ordermanage.dto.LoginRequest;
import com.example.ordermanage.dto.LoginResponse;
import com.example.ordermanage.entity.User;
import com.example.ordermanage.mapper.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final int MAX_FAIL = 5;

    private final UserMapper userMapper;
    private final CaptchaService captchaService;
    private final TokenService tokenService;
    private final PermissionQueryService permissionQueryService;

    public AuthService(UserMapper userMapper, CaptchaService captchaService, TokenService tokenService,
                       PermissionQueryService permissionQueryService) {
        this.userMapper = userMapper;
        this.captchaService = captchaService;
        this.tokenService = tokenService;
        this.permissionQueryService = permissionQueryService;
    }

    public LoginResponse login(LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();
        String captcha = req.getCaptcha();
        String captchaId = req.getCaptchaId();

        if (username.isEmpty() || password.isEmpty()) {
            throw new BizException(Err.UNAUTHORIZED, "请输入用户名和密码");
        }
        if (!StringUtils.hasText(captcha)) {
            throw new BizException(Err.UNAUTHORIZED, "请输入验证码");
        }
        if (!captchaService.validate(captchaId, captcha)) {
            throw new BizException(Err.UNAUTHORIZED, "验证码错误");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new BizException(Err.UNAUTHORIZED, "用户名或密码错误");
        }

        int fail = user.getFailCount() == null ? 0 : user.getFailCount();
        if (fail >= MAX_FAIL) {
            throw new BizException(Err.UNAUTHORIZED, Err.PASSWORD_LOCKED);
        }
        if (!"启用".equals(user.getStatus())) {
            throw new BizException(Err.UNAUTHORIZED, "账户已禁用，请联系系统管理员启用");
        }

        if (!md5(password).equals(user.getPassword())) {
            int next = fail + 1;
            user.setFailCount(next);
            userMapper.updateById(user);
            if (next >= MAX_FAIL) {
                throw new BizException(Err.UNAUTHORIZED, Err.PASSWORD_LOCKED);
            }
            int left = MAX_FAIL - next;
            throw new BizException(Err.UNAUTHORIZED, "密码错误，还剩 " + left + " 次机会");
        }

        if (fail != 0) {
            user.setFailCount(0);
            userMapper.updateById(user);
        }
        return buildLoginResponse(user, tokenService.issue(user.getId()));
    }

    public LoginResponse buildLoginResponse(User user, String token) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setFirstLogin(user.getFirstLogin() != null && user.getFirstLogin() == 1);
        resp.setUser(permissionQueryService.userInfo(user.getId()));
        resp.setMenus(permissionQueryService.menus(user.getId()));
        resp.setPermissions(permissionQueryService.permissions(user.getId()));
        return resp;
    }

    public LoginResponse me(Long userId, String token) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(Err.UNAUTHORIZED, "未认证");
        }
        return buildLoginResponse(user, token);
    }

    public void logout(String token) {
        tokenService.remove(token);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        if (!org.springframework.util.StringUtils.hasText(oldPassword)) {
            throw new BizException(Err.BAD_REQUEST, "请输入当前密码");
        }
        if (!org.springframework.util.StringUtils.hasText(newPassword)) {
            throw new BizException(Err.BAD_REQUEST, "请填写新密码");
        }
        if (newPassword == null ? confirmPassword != null : !newPassword.equals(confirmPassword)) {
            throw new BizException(Err.BAD_REQUEST, "两次密码不一致");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BizException(Err.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        if (!PasswordValidator.isValid(newPassword)) {
            throw new BizException(Err.BAD_REQUEST, "密码不符合安全规则");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !md5(oldPassword).equals(user.getPassword())) {
            throw new BizException(Err.BAD_REQUEST, "当前密码错误");
        }
        updatePassword(user, newPassword);
    }

    public void forceChangePassword(Long userId, String newPassword, String confirmPassword, String currentToken) {
        validateNewPassword(newPassword, confirmPassword);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(Err.UNAUTHORIZED, "未认证");
        }
        updatePassword(user, newPassword);
        tokenService.remove(currentToken);
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (!org.springframework.util.StringUtils.hasText(newPassword)) {
            throw new BizException(Err.BAD_REQUEST, "请填写新密码");
        }
        if (!PasswordValidator.isValid(newPassword)) {
            throw new BizException(Err.BAD_REQUEST, "密码不符合安全规则");
        }
        if (newPassword == null ? confirmPassword != null : !newPassword.equals(confirmPassword)) {
            throw new BizException(Err.BAD_REQUEST, "两次密码不一致");
        }
    }

    private void updatePassword(User user, String newPassword) {
        user.setPassword(md5(newPassword));
        user.setFirstLogin(0);
        user.setFailCount(0);
        userMapper.updateById(user);
    }

    public void resetFailCounts() {
        userMapper.update(null, new UpdateWrapper<User>().set("fail_count", 0));
    }

    public static String md5(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
