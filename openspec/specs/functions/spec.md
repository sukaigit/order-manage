## Purpose

功能（按钮）管理模块维护挂载在各菜单下的操作级功能权限，涵盖功能查询、新增、编辑、删除等操作。每个功能通过权限标识（`域:操作`）与所属菜单关联，是角色权限分配与按钮级控制的最小单元。

## Requirements

### REQ-FUNC-001: 功能列表查询（分页+筛选）

系统 SHALL 提供功能分页列表查询能力，列表字段包含功能编号、功能名称、所属菜单、权限标识、备注；并支持按功能编号/名称、所属菜单下拉、权限标识模糊搜索进行筛选。

#### Scenario: 组合条件查询返回匹配分页列表

- WHEN 用户以功能编号或名称关键词、指定所属菜单、权限标识关键词的组合条件查询功能列表
- THEN 系统返回同时满足全部筛选条件的分页功能列表，列表字段完整且分页信息正确

#### Scenario: 翻页保持筛选条件

- WHEN 用户在已应用筛选条件的功能列表中切换至下一页
- THEN 系统按相同筛选条件返回下一页数据，筛选条件保持不变

### REQ-FUNC-002: 新增功能

系统 SHALL 支持新增功能，必填字段为功能名称、所属菜单、权限标识；权限标识格式 MUST 为 `域:操作`（如 order:query）且全局唯一；功能编号 MUST 由权限标识生成并唯一。

#### Scenario: 校验通过创建成功

- WHEN 用户提交包含功能名称、所属菜单及格式合法且不重复的权限标识的新功能
- THEN 系统创建功能成功，创建时间为当前时间，功能挂载到所选菜单下

#### Scenario: 权限标识缺失或重复被拒绝

- WHEN 用户提交的新功能缺少权限标识，或权限标识与已有功能重复，或格式不含 `:`
- THEN 系统拒绝创建并返回校验错误信息

### REQ-FUNC-003: 编辑功能

系统 SHALL 允许修改功能名称、所属菜单、权限标识、备注；编辑时必填字段缺失、权限标识格式非法或与已有功能冲突 MUST 被拒绝；功能编号 MUST 不可修改。

#### Scenario: 编辑成功保存修改

- WHEN 用户修改功能名称或所属菜单并提交合法数据
- THEN 系统保存修改成功并返回更新后的功能信息，功能关联同步更新到新的所属菜单

#### Scenario: 必填校验失败被拒绝

- WHEN 用户编辑功能时清空功能名称、所属菜单或权限标识，或提交重复的权限标识
- THEN 系统拒绝保存并返回校验错误信息

### REQ-FUNC-004: 删除功能

系统 SHALL 允许删除功能，删除操作 MUST 经过用户二次确认，并 MUST 同步移除该功能的菜单关联（tb_menu_function）与角色功能授权。

#### Scenario: 功能删除成功且关联同步移除

- WHEN 用户对某功能确认执行删除
- THEN 系统删除该功能及其菜单关联与角色授权，后续查询不再返回该功能

#### Scenario: 未通过二次确认不执行删除

- WHEN 用户触发功能删除但在二次确认中未确认
- THEN 系统不执行删除，功能数据保持不变

### REQ-FUNC-005: 功能挂载与权限控制

每个功能 MUST 关联且仅关联一个所属菜单；功能的可见与可用 SHALL 由「菜单授权 + 角色功能权限」共同控制：角色无该菜单授权时菜单不展示，角色无该功能授权时对应按钮不展示、请求被拒绝。

#### Scenario: 功能随所属菜单与角色权限生效

- WHEN 角色被授予某菜单及该菜单下某功能
- THEN 该角色用户可见对应菜单并可操作对应按钮

#### Scenario: 角色无功能授权时按钮不可用

- WHEN 角色未被授予某功能（如普通用户无审核通过功能）
- THEN 该角色用户看不到对应按钮，或发起的请求被拒绝并返回权限错误提示

## 初始化数据

主要功能（与 sql/init.sql 中 tb_function 行一一对应，所属菜单与 tb_menu_function 一致）：

| 功能编号 | 功能名称 | 所属菜单 | 权限标识 |
|---|---|---|---|
| FUNC_ORDER_QUERY | 订单查询 | 订单列表 | order:query |
| FUNC_ORDER_CREATE | 订单新增 | 订单列表 | order:create |
| FUNC_ORDER_UPDATE | 订单编辑 | 订单列表 | order:update |
| FUNC_ORDER_DELETE | 订单删除 | 订单列表 | order:delete |
| FUNC_ORDER_EXPORT | 订单导出 | 订单列表 | order:export |
| FUNC_AUDIT_PASS | 审核通过 | 审核列表 | audit:pass |
| FUNC_AUDIT_REJECT | 审核驳回 | 审核列表 | audit:reject |
| FUNC_SUPPLIER_QUERY | 供应商查询 | 供应商列表 | supplier:query |
| FUNC_SUPPLIER_CREATE | 供应商新增 | 供应商列表 | supplier:create |
| FUNC_SUPPLIER_UPDATE | 供应商编辑 | 供应商列表 | supplier:update |
| FUNC_SUPPLIER_DELETE | 供应商删除 | 供应商列表 | supplier:delete |
| FUNC_SUPPLIER_EXPORT | 供应商导出 | 供应商列表 | supplier:export |
| FUNC_REPORT_EXPORT | 报表导出 | 报表首页 | report:export |
| FUNC_USER_CREATE | 用户新增 | 用户管理 | user:create |
| FUNC_USER_UPDATE | 用户编辑 | 用户管理 | user:update |
| FUNC_USER_DELETE | 用户删除 | 用户管理 | user:delete |
| FUNC_ROLE_CREATE | 角色新增 | 角色管理 | role:create |
| FUNC_ROLE_UPDATE | 角色编辑 | 角色管理 | role:update |
| FUNC_ROLE_DELETE | 角色删除 | 角色管理 | role:delete |
| FUNC_DEPT_CREATE | 部门新增 | 部门管理 | dept:create |
| FUNC_DEPT_UPDATE | 部门编辑 | 部门管理 | dept:update |
| FUNC_DEPT_DELETE | 部门删除 | 部门管理 | dept:delete |
| FUNC_ORG_CREATE | 机构新增 | 机构管理 | org:create |
| FUNC_ORG_UPDATE | 机构编辑 | 机构管理 | org:update |
| FUNC_ORG_DELETE | 机构删除 | 机构管理 | org:delete |
| FUNC_MENU_CREATE | 菜单新增 | 菜单管理 | menu:create |
| FUNC_MENU_UPDATE | 菜单编辑 | 菜单管理 | menu:update |
| FUNC_MENU_DELETE | 菜单删除 | 菜单管理 | menu:delete |
| FUNC_FUNC_CREATE | 功能新增 | 功能管理 | func:create |
| FUNC_FUNC_UPDATE | 功能编辑 | 功能管理 | func:update |
| FUNC_FUNC_DELETE | 功能删除 | 功能管理 | func:delete |
| FUNC_LOG_VIEW | 日志查看 | 操作日志 | log:view |
