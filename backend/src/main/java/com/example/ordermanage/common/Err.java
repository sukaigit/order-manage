package com.example.ordermanage.common;

/**
 * 业务错误码与中文文案常量（对齐 docs/api/接口定义.md「状态码与错误码」）。
 */
public final class Err {

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int UNPROCESSABLE = 422;

    public static final String PARAM_INVALID = "参数校验失败";
    public static final String PAGE_INVALID = "分页参数非法";
    public static final String AMOUNT_NEGATIVE = "金额不能为负数";
    public static final String NOT_LOGIN = "未登录或登录已失效";

    public static final String ORDER_FINISHED_EDIT = "已完成订单不可编辑";
    public static final String ORDER_FINISHED_DELETE = "已完成订单不可删除";
    public static final String AUDIT_ONLY_PENDING = "仅待审核订单可审核";
    public static final String AUDIT_REJECT_OPINION_REQUIRED = "驳回时审核意见必填";
    public static final String SUPPLIER_REFERENCED = "该供应商已被订单引用，无法删除";
    public static final String SUPPLIER_DISABLED = "该供应商已停用，不可选择";
    public static final String ADMIN_CANNOT_DISABLE = "默认管理员 admin 不可停用";
    public static final String ADMIN_CANNOT_DELETE = "默认管理员 admin 不可删除";
    public static final String ROLE_ADMIN_PERMISSION_IMMUTABLE = "管理员角色权限不可修改";
    public static final String ROLE_REFERENCED = "该角色已被用户引用，无法删除";
    public static final String DEPT_REFERENCED = "该部门已被用户引用，无法删除";
    public static final String ORG_HAS_CHILDREN = "该机构下有下级机构，无法删除";
    public static final String ORG_LAST_LEVEL = "已达最末级，不可新增下级";
    public static final String ORG_CODE_INVALID = "机构编号只能包含字母和数字";
    public static final String MENU_HAS_CHILDREN = "该菜单下存在子菜单，无法删除";
    public static final String MENU_LEVEL2_ROUTE_REQUIRED = "二级菜单必须配置路由";
    public static final String PASSWORD_LOCKED = "账户已锁定；密码错误次数过多，请联系系统管理员解锁";

    public static final String USERNAME_EXISTS = "用户名已存在";
    public static final String CODE_EXISTS = "编号已存在";
    public static final String DEPT_CODE_EXISTS = "部门编号已存在";
    public static final String ORG_CODE_EXISTS = "机构编号已存在";
    public static final String MENU_CODE_EXISTS = "菜单编号已存在";
    public static final String SUPPLIER_CODE_EXISTS = "供应商编号已存在";
    public static final String ORDER_NO_EXISTS = "订单编号已存在";
    public static final String PERM_EXISTS = "权限标识已存在";
    public static final String PERM_FORMAT_INVALID = "权限标识格式必须为域:操作";

    private Err() {
    }
}
