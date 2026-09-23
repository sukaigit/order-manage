# 测试报告 — order-manage Phase 4 测试保障

> 项目 `order-manage` · 分支 `feat/order-manage` · HEAD `c674e46` · 报告日期 **2026-09-23**
> 执行方式：全自主（用户已授权），所有结果为真实执行输出；未运行的浏览器场景一律标 ⏳，无伪造 ✅。

---

## 1. 汇总数据

### 1.1 测试用例与覆盖

| 指标 | 数值 |
|---|---|
| 规格 / 需求 | 14 specs / **78 REQ**，全部映射 ≥1 TC（矩阵见 `test-cases.md` 附录） |
| 测试用例总数 | **167** 条 TC（含 BASE 2 条） |
| 正向 P / 反向 N / 边界 B | **87 / 70 / 10** |
| ✅ 亲测（API/单测实证） | **156** |
| ⏳ 待浏览器执行 | **11**（AUTH-B-03、ORDER-B-04、ORDER-N-15、SUP-N-09、USER-N-08、AUDIT-N-06、LOG-B-01、NFR-P-01、NFR-N-01、NFR-P-02、NFR-P-04） |
| 部分✅+部分⏳ | RPT-B-01（单月分组✅ / 跨月⏳）、NFR-P-03（构建侧✅ / 部署冒烟⏳） |
| 用户验收用例 UAT | **13** 条 UT，全部 ⏳（浏览器手工，Phase 4 自动阶段未执行） |
| E2E 脚本 | `e2e.cjs` 20 个场景，`--dry` 自检 **EXIT=0**（实跑需安装 Playwright，见脚本头注释） |
| 文档化 REST 端点 | **57**（auth 6 / workbench 1 / orders 7 / order-audits 3 / suppliers 7 / reports 3 / users 6 / roles 6 / departments 4 / organizations 4 / menus 5 / functions 4 / logs 1） |

### 1.2 指定业务规则（均设反向专例）

| 业务规则 | 结果 | 证据 |
|---|---|---|
| 仅待审核/已驳回可编辑，已完成不可编辑 | ✅（正反均断言） | OrderUpdateStateMachineTest：pendingOrderEdit… / rejectedOrderEdit… / finishedOrderEditReturns422「已完成订单不可编辑」 |
| 驳回意见必填 | ✅ | OrderAuditPassRejectTest.rejectEmptyOpinionReturns400（+超 255 字 400） |
| 报表仅 admin，非 admin 403 | ✅ | ReportSummaryTest.summaryForbiddenForNonAdmin、ReportDetailTest.detailForbiddenForNonAdmin、ExportMiscTest.reportExportForbiddenForNonAdmin |
| 仅审核员可审核（管理员也 403） | ✅ | PermissionAspectTest.adminAuditPassReturns403 + OrderAuditPassRejectTest.adminDirectCallReturns403 |
| 供应商停用不可选 | ✅ | OrderCreateTest.disabledSupplierReturns422 + SupplierOptionsTest.returnsOnlyEnabledSuppliers + SupplierDeleteStatusTest.statusDisabledSupplierExcludedFromOptions |
| 金额负值拒绝 | ✅（创建+编辑双路径） | OrderCreateTest.negativeAmountReturns400、OrderUpdateStateMachineTest.negativeAmountEditReturns400 |
| 编号/唯一键重复拒绝 | ✅ | 409：订单 duplicateOrderNoReturns409、供应商 createDuplicateCodeReturns409、用户 duplicateUsernameReturns409、部门 createDuplicateCodeReturns409、角色 createDuplicateCodeReturns409、菜单/功能 409（MenuTreeTest/FunctionCrudTest） |

### 1.3 边界值

| 边界 | 结果 |
|---|---|
| page=0 → 400 | ✅ PageResultTest.pageZeroRejectedWith400 |
| pageSize=0 / 101 → 400，缺省回填 1/10 | ✅ PageResultTest.pageSizeOutOfRangeRejectedWith400 + validPagePassesDefaults |
| pageSize=100 合法 | ✅（validate 上界断言：101 拒绝 ⇒ 100 落在合法域）；浏览器复核归入 E2E-ORDER-04c |
| pageSize=1 实际翻页 | ✅ OrderListTest.paginationKeepsFilters |
| 金额=0（合法下界） | ⏳ TC-ORDER-B-04 / E2E-ORDER-05（负值下界 ✅ 已断言） |
| 日志 pageSize 档位 {5,10,20,50}、7 非法 | ✅ LogListTest.defaultPageSizeIsFiveAndAllowedSizesAreValidated |
| 驳回意见 255/256 | ✅ OrderAuditPassRejectTest.rejectOpinionOver255Returns400 |
| 机构 5 级上限 | ✅ OrganizationTreeTest.createChildDerivesNextLevelAndBlocksLastLevel |
| 报表跨月分组 | 单月 ✅ / 跨月 ⏳（RPT-B-01） |

---

## 2. 执行记录（真实输出）

| # | 命令 | 结果 | 关键输出 |
|---|---|---|---|
| 1 | `cd backend && mvn -q test` | **EXIT=0** | 35 个测试类、**Tests run: 171, Failures: 0, Errors: 0, Skipped: 0**（逐份 surefire 报告求和 = 171，全部 `Failures: 0, Errors: 0`） |
| 2 | `cd frontend && npm run build` | **EXIT=0** | Vite 构建成功；`frontend/dist/index.html` 存在（dist 已被 gitignore） |
| 3 | `node --check docs/test/e2e.cjs` | **EXIT=0** | 语法通过 |
| 4 | `node docs/test/e2e.cjs --dry` | **EXIT=0** | 打印 20 个场景清单，`Total: 20 cases. Status: pending` |
| 5 | DB 种子核查（清理前） | 发现问题 | `tb_user_role=73`（种子=1）→ 72 行孤儿 |
| 6 | DB 清理 | **deleted_orphans 72** | `DELETE FROM tb_user_role WHERE user_id NOT IN (SELECT id FROM tb_user)` |
| 7 | DB 种子核查（清理后） | **14 表全部=种子** | 见 §4.1 |
| 8 | `git rev-parse --short HEAD` / `git status --porcelain` | c674e46 / 仅 `?? docs/test/` | 未改 `backend/`、`frontend/`、`sql/`、`docs/prototype/`、OpenCode 流程文档；**未 commit** |
| 9 | 前端 mock 残留扫描 | 0 命中 | `grep ORD2026\|mock\|Mock\|MOCK` 无匹配；无 `src/store/` 目录；15 视图（Dashboard/ExamplePage 已删） |

> 说明：171 = surefire 权威计数；源码 `@Test` 标注扫描多计 2 处系 `@TestConfiguration` 前缀误匹配，以 surefire 为准。

---

## 3. 关键验证点

### 3.1 页面结构覆盖（路由 ↔ 视图 ↔ API 模块）

| 路由 | 视图 | API 模块 | 覆盖 |
|---|---|---|---|
| `/login` | Login.vue | auth.js（captcha/login/me/logout/password×2） | ✅ TC-AUTH 22 条 |
| `/force-password`、`/change-password` | ChangePassword.vue | auth.js（change/force） | ✅ TC-AUTH-P-07/P-08；跳转交互 ⏳ |
| `/`（Layout） | Layout.vue | 侧边栏按 `session.menus` 动态构建（RBAC） | ✅ TC-ROLE-P-07 |
| `/workbench` | Workbench.vue | workbench.js（stats） | ✅ TC-WB |
| `/orders/list` | OrderList.vue | orders.js（list/create/update/delete/detail/export/audits） | ✅ TC-ORDER（31 条） |
| `/order-audit/list` | OrderAuditList.vue | audits.js（list/pass/reject） | ✅ TC-AUDIT（13 条） |
| `/suppliers/list` | SupplierList.vue | suppliers.js（CRUD/status/export/options） | ✅ TC-SUP（19 条） |
| `/reports/home` | ReportHome.vue | reports.js（summary/detail/export） | ✅ TC-RPT（11 条） |
| `/system/users` | UserManage.vue | system.js users 段（6） | ✅ TC-USER（16 条） |
| `/system/roles` | RoleManage.vue | system.js roles 段（6，含 permissions） | ✅ TC-ROLE（12 条） |
| `/system/departments` | DepartmentManage.vue | system.js departments（4） | ✅ TC-DEPT（6 条） |
| `/system/organizations` | OrganizationManage.vue | system.js organizations（4） | ✅ TC-ORG（8 条） |
| `/system/menus` | MenuManage.vue | system.js menus（5，含 tree） | ✅ TC-MENU（5 条） |
| `/system/functions` | FuncManage.vue | system.js functions（4） | ✅ TC-FUNC（6 条） |
| `/system/logs` | OperationLog.vue | system.js logs（1） | ✅ TC-LOG（7 条） |

**15/15 路由 → 视图 → API 模块全链路对齐，无孤儿页面、无 mock 残留、无死路由。** 57 端点均有 TC 触达（含 3 个导出端点 ×3 组断言）。

### 3.2 权限矩阵（RBAC 实测）

| 场景 | 结果 | 证据 |
|---|---|---|
| 非 admin → 报表 summary/detail/export | 403 ✅ | 3 个 Forbidden 测试 |
| 管理员 → 审核 pass | 403 ✅ | PermissionAspectTest.adminAuditPassReturns403 |
| 审核员 → 审核 pass | 200 ✅ | auditorAuditPassReturns200 |
| 审核员 → 用户管理 / 日志 | 403 ✅ | auditorUsersReturns403、LogListTest.auditorGets403 |
| admin → 用户管理 | 200 ✅ | adminUsersReturns200 |
| 普通角色 → 审核 pass | 403 ✅ | roleUserAuditPassReturns403 |
| 无 token → 受保护接口 | 401 ✅ | TokenInterceptorTest.noToken/meWithoutToken + 各列表 withoutTokenReturns401 |
| 分配菜单 → 角色用户重登菜单变化 | ✅ | RolePermissionAssignTest.assignedMenusVisibleToRoleUserAfterLogin |

### 3.3 订单状态机（实测）

`待审核 --编辑--> 待审核` ✅ · `已驳回 --编辑--> 待审核` ✅ · `待审核 --通过--> 已完成` ✅ · `待审核 --驳回--> 已驳回` ✅ · `已完成 --编辑/删除--> 422` ✅ · `非待审核 --审核--> 422「仅待审核订单可审核」` ✅ · 驳回→编辑→再通过 ⇒ 历史 2 条升序 ✅。

### 3.4 种子基线（与 `sql/init.sql` 对齐）

role=3 · menu=17 · function=32 · menu_function=32 · role_function=46 · role_menu=29 · user=1 · **user_role=1（已清理）** · operation_log=5 · supplier=3 · **order=0** · **order_audit=0** · department=1 · organization=10 —— **14/14 一致**。

---

## 4. 测试数据清理

### 4.1 清理执行（本阶段已做）

1. **发现**：核查时 `tb_user_role=73`，种子为 1 行 → 定位 **72 行孤儿**（user_id 275/276/299/301/323/324/325… 指向不存在的 `tb_user`），系历史测试运行残留。
2. **清理 SQL**（仅数据，未改任何配置/代码）：
   ```sql
   DELETE FROM tb_user_role WHERE user_id NOT IN (SELECT id FROM tb_user);
   ```
   执行输出：`deleted_orphans 72` → `user_role_now 1`。
3. **收尾核查**（清理后）：14 张表逐表 count 与种子完全一致（§3.4）；`tb_order=0`、`tb_order_audit=0` —— 业务表无测试残留行（各测试类 `@AfterEach cleanup()` 生效）。
4. **后续建议**（记入 `issues.md`，不阻塞门禁）：后端测试统一在 cleanup 中级联清 `tb_user_role`（`UserSaveTest`/`PermissionAspectTest` 已有，覆盖不全）；每次 `mvn test` 后可复跑上述 DELETE + 14 表 count 校验。

### 4.2 流程纪律

- 本阶段仅新增 `docs/test/**` 五个交付物；`git status` 仅 `?? docs/test/`。
- 未修改 `backend/`、`frontend/`、`sql/init.sql`、`docs/prototype/`、`OpenCode-标准化研发流程.md`；未改 DB 连接配置；**未 commit**（交由编排者）。
- 未访问计划文件（全程未读写 Prometheus 计划）。

---

## 5. 问题清单

| # | 严重度 | 模块 | 问题 | 影响 | 状态 |
|---|---|---|---|---|---|
| 1 | 中 | 用户-角色关联 | `tb_user_role` 72 行孤儿数据（历史测试残留） | 种子基线漂移、统计失真风险 | ✅ 已处理（已删除并复核 =1；根因改进为后端待办） |
| 2 | 低 | 测试基建 | 测试直连活库（sqlite 文件），无独立 test DB | 依赖各测试类 cleanup 纪律；本阶段核查 14 表全对齐 | ✅ 无遗留（维持现状 + 收尾核查制度化） |
| 3 | 低 | 前端 E2E | 仓库未安装 Playwright，浏览器场景无法自动执行 | 11 条 TC + 13 条 UT 标 ⏳ | ⏳ 待浏览器执行（`e2e.cjs` 就绪，`--dry` EXIT=0；安装步骤见脚本头） |
| 4 | 中 | 非功能 | REQ-NFR-001（50 并发）、REQ-NFR-005（2 万行 ≤2s）未压测 | 非功能验收缺口 | ⏳ 待办（需压测工具+灌数，超出 Phase 4 自动范围） |
| 5 | 低 | 报表 | REQ-RPT-005 跨月双分组未单测（单月断言 ✅） | 趋势图跨月边界缺证 | ⏳ 待浏览器执行（E2E-RPT-01） |

**无阻塞级缺陷；阶段门禁可判定通过（附 3/4/5 为已知待办，均不阻塞）。**

---

## 6. 结论与交付物

- **后端**：171/171 全绿（0 失败 0 错误 0 跳过）—— **MVN_EXIT=0**。
- **前端**：生产构建成功 —— **NPM_EXIT=0**，`dist/index.html` 生成。
- **覆盖**：78/78 REQ 映射 TC；57 端点全部触达；P/N/B = 87/70/10；✅ 156 / ⏳ 11（如实标记）。
- **数据**：活库已恢复 `init.sql` 种子状态（14/14 表），清理步骤可复现（§4.1）。
- **交付物**（均在 `docs/test/` 下，未提交）：
  1. `docs/test/test-cases.md` — 167 条 TC + REQ→TC 覆盖矩阵
  2. `docs/test/issues.md` — 问题表（含孤儿数据发现与处置）
  3. `docs/test/user-test-cases.md` — 13 条 UAT（步骤+预期）
  4. `docs/test/e2e.cjs` — 20 场景 E2E（`--dry` EXIT=0；实跑装 Playwright 后 `node docs/test/e2e.cjs`）
  5. `docs/test/reports/order-manage-test-report.md` — 本报告
