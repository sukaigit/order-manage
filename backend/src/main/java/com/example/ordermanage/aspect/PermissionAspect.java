package com.example.ordermanage.aspect;

import com.example.ordermanage.annotation.RequirePerm;
import com.example.ordermanage.common.BizException;
import com.example.ordermanage.common.Err;
import com.example.ordermanage.context.LoginUser;
import com.example.ordermanage.context.UserContext;
import com.example.ordermanage.service.PermissionQueryService;
import java.util.Set;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private static final Set<String> AUDITOR_ONLY_PERMS = Set.of("audit:pass", "audit:reject");
    private static final String ROLE_AUDITOR = "ROLE_AUDITOR";

    private final PermissionQueryService permissionQueryService;

    public PermissionAspect(PermissionQueryService permissionQueryService) {
        this.permissionQueryService = permissionQueryService;
    }

    @Before("@annotation(requirePerm)")
    public void check(RequirePerm requirePerm) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BizException(Err.UNAUTHORIZED, "未认证");
        }
        String perm = requirePerm.value();
        if (AUDITOR_ONLY_PERMS.contains(perm) && !ROLE_AUDITOR.equals(user.roleCode())) {
            throw new BizException(Err.FORBIDDEN, "无权限");
        }
        if (!permissionQueryService.hasPermission(user.userId(), perm)) {
            throw new BizException(Err.FORBIDDEN, "无权限");
        }
    }
}
