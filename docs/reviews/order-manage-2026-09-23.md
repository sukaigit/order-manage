# 代码审查报告 — order-manage Phase 5 审查与归档（审查部分）

> 项目 `order-manage` · 分支 `feat/order-manage` · HEAD `d8c1554` · 审查日期 **2026-09-23**
> 审查方式：全自主执行（用户已授权）；所有命令输出为真实执行结果，未运行的工具如实标注，未伪造任何扫描结论。
> 范围纪律：仅在 `docs/reviews/` 下写入；未修改 `backend/`、`frontend/`、`sql/`、`openspec/`、`docs/test/`、`docs/plans/`、`docs/api/`、prototype、流程文档；未 commit。

---

## 1. 前置检查

### 1.1 git 状态

`git status --porcelain`（工作区）：

```
（空输出 — working tree clean）
```

`git status -sb`：

```
## feat/order-manage
```

`git log --oneline -3`：

```
d8c1554 test: order-manage 167 TC + 13 UAT + report, 156/167 pass, 171 backend green
c674e46 feat: slice8 frontend api integration from prototype to live rest
4dd0673 feat: slice9 excel exports operation log writer and smoke notes
```

`git rev-parse HEAD` → `d8c1554179d02eed2666522f1d7602f7fbce9bca`（与任务指定 HEAD 一致）。
`git rev-parse --show-toplevel` → `D:/workspace/ai/study/order-manage`（独立仓库，非外层 study 仓库）。

| 检查项 | 结果 |
|---|---|
| 工作区干净 | ✅ `git status --porcelain` 空 |
| HEAD = `d8c1554` | ✅ |
| 测试报告存在 | ✅ `docs/test/reports/order-manage-test-report.md`（167 TC / 171 后端全绿，见 §1.2） |
| 无未关闭 SEVERE/阻塞问题 | ✅ 见 §1.3 |

### 1.2 测试报告核验

`docs/test/reports/order-manage-test-report.md` 关键结论（引用）：

- 后端 **Tests run: 171, Failures: 0, Errors: 0, Skipped: 0**（35 个测试类，`mvn -q test` EXIT=0）
- 前端 `npm run build` EXIT=0
- 167 条 TC：✅ 156 / ⏳ 11（浏览器场景）；UAT 13 条全 ⏳
- 文档化 REST 端点 **57**；REQ→TC 覆盖 78/78

本次审查**未重跑** `mvn test`（报告已在 HEAD `d8c1554` 提交中给出权威 171 绿证据；抽查源码 `@Test` 标注计数 = **171**（35 文件），与 surefire 口径一致，交叉印证）。

### 1.3 已知问题（docs/test/issues.md，如实引用，不改状态）

| # | 严重度 | 问题 | 状态（原文） |
|---|---|---|---|
| 1 | 中 | `tb_user_role` 72 行孤儿数据（历史测试残留） | ✅ 已处理（已 DELETE 并复核；根因改进为后端待办） |
| 2 | 低 | 测试直连活库，无独立 test DB 隔离 | ✅ 无遗留（维持现状 + 收尾核查制度化） |
| 3 | 低 | 未安装 Playwright，11 TC + 13 UT 浏览器场景无法自动执行 | ⏳ 待浏览器执行 |
| 4 | 中 | REQ-NFR-001（50 并发）、REQ-NFR-005（2 万行 ≤2s）未压测 | ⏳ 待办 |
| 5 | 低 | REQ-RPT-005 跨月双分组未单测 | ⏳ 待浏览器执行 |

**判定：无 SEVERE/阻塞级开放问题**（issues.md 原文结论「无阻塞级遗留缺陷」）；⏳ 三项为已知待办，按门禁口径不阻塞，本报告结论中以「附注」保留，**不标记为已关闭**。

---

## 2. 自动扫描

### 2.1 semgrep

**工具可用**（非 TOOL_NOT_INSTALLED）：`semgrep --version` → `1.167.0`。

命令：`semgrep --config=auto --severity WARNING .`（在项目根执行）

真实输出摘要：

```
Scanning 219 files tracked by git with 722 Code rules
✅ Scan completed successfully.
 • Findings: 8 (8 blocking)
 • Rules run: 247
 • Targets scanned: 219
Ran 247 rules on 219 files: 8 findings.
```

8 条发现明细（`--json` 解析，真实 check_id）：

| # | 文件:行 | 规则 | 审查判定 |
|---|---|---|---|
| 1 | `backend/.../aspect/OperationLogAspect.java:48` | `java.spring.security.audit.spel-injection.spel-injection` | **低风险 / 可接受**：SpEL 表达式来自 `@OpLog(targetSpEL=…)` 注解（编译期常量，如 `"#root.args[0].username"`），**非用户输入**；上下文仅 `StandardEvaluationContext` 取值拼字符串写日志。属 semgrep 模式告警，无实际注入面 |
| 2 | `backend/.../service/AuthService.java:168` | `java.lang.security.audit.crypto.use-of-md5.use-of-md5` | **已知设计约束**：密码 MD5 与种子 `e10adc3949ba59abbe56e057f20f883e`（admin/123456）及 Phase 0–4 既定方案一致；见 §4 安全清单「密码 MD5」项 **FAIL（已知）** |
| 3–5 | `docs/prototype/src/views/Layout.vue:10,14,19` | `javascript.vue...avoid-v-html` | **原型代码**（只读基线，不在交付前端内）；图标为前端常量映射，非用户可控 |
| 6–8 | `frontend/src/views/Layout.vue:10,15,20` | `javascript.vue...avoid-v-html` | **低风险 / 可接受**：`m.icon` 由 `buildMenus` 用前端本地 `ICONS[menu.code]` 常量表赋值（Layout.vue:44/69/74/76）；`tb_menu` 无 icon 列（init.sql CREATE 仅 code/name/route…），菜单管理无法写入 icon — **无用户可控数据进入 v-html**。建议后续改 `:class` 绑定，非阻塞 |

> 扫描范围说明：semgrep 按 git 跟踪文件扫描（219 files），跳过 `.semgrepignore` 41 项（node_modules/target/dist 等）。

### 2.2 gitleaks

**工具可用**（非 TOOL_NOT_INSTALLED）：`gitleaks version` → `8.30.1`。

命令：`gitleaks detect --source . -v`

真实输出摘要：

```
Finding:     "token": "eyJhbGciOiJIUzI1NiJ9..."
RuleID:      generic-api-key
File:        docs/api/接口定义.md
Line:        130
Commit:      069c6768fe0d49971f07e451a513952d680a6c0c
14 commits scanned. scanned ~1302275 bytes (1.30 MB) in 474ms
WRN leaks found: 1
```

**审查判定：误报（文档示例占位）**。该行位于 API 文档登录响应示例：`"token": "eyJhbGciOiJIUzI1NiJ9..."` — 为 JWT 头部截断占位符（`...` 截断，无签名/载荷），非真实密钥。实际实现 `TokenService` 使用 `UUID.randomUUID()` 内存令牌，**代码中不存在任何 JWT 密钥**。不修改文档（docs/api 只读），记录为已识别误报。

### 2.3 补充 grep 秘密扫描（回退/加固，已实际执行）

在 semgrep + gitleaks 之外额外执行（因 gitleaks 对中文路径文档告警需人工复核，且需覆盖未跟踪模式）：

| 模式 | 结果 |
|---|---|
| `password\s*=\s*['\"]` / `api_key` / `Bearer <长串>` / `-----BEGIN … PRIVATE KEY-----` / `secret=长值` | **0 真实命中**（仅 `Login.vue` 表单清空 `this.form.password = ''` 赋值，非秘密） |
| `TODO\|FIXME\|System.out.print\|printStackTrace` | **0 命中**（全仓库，排除 node_modules/target/dist） |
| `application.yml` 全文 | 仅 `jdbc:sqlite:${ORDER_DB:…}` 路径占位 + 端口；**无任何凭据** |
| `sql/init.sql` | 种子密码仅存 MD5 哈希 `e10adc…` 并注释明示「仅存哈希」——设计内种子凭据，非泄露 |
| Bearer 硬编码 | 前端仅 `localStorage.getItem('token')` 动态拼接（`request.js:12-13`），无硬编码 |

扫描覆盖对象：git 跟踪的 `backend/`、`frontend/`、`sql/`、`docs/`、`openspec/` 全部文本文件（约 267 文件 / ~23.7k 行，排除 node_modules/target/dist/.git）。

---

## 3. 六维质量审查

### 3.1 度量

| 指标 | 数值 | 取证方式 |
|---|---|---|
| 后端主代码 | **4,614** 行 Java / 148 个 `.java`（含测试） | `Get-ChildItem … Measure-Object` |
| 后端测试代码 | **5,668** 行 / **35** 测试类 / **171** `@Test` | 目录计数 + `@Test` grep = 171（与报告 surefire 一致） |
| 前端 src | **3,027** 行 / 31 文件 / **15** 视图 | 同上 |
| 仓库文本总量 | **~23,687** 行 / 267 文件 | 排除 node_modules/target/dist/.git |
| 数据库表 | **14** | `sql/init.sql` `CREATE TABLE` 计数 |
| REST 端点 | **57** | 测试报告 §1.1（文档化） |
| 规格 | 14 specs / **78 REQ** | `openspec/specs/**` `### REQ-` 计数 = 78 |
| 测试 | 167 TC / 171 后端单测全绿 | 测试报告 + 本次 `@Test` 复核 |

### 3.2 REQ 抽样交叉核对（24/78 = **30.8% ≥ 20% 门槛**）

抽样覆盖 auth / orders / audit / suppliers / reports / users / logs / roles / departments / functions / workbench / nfr 全部域，每条：规格要求 → 实现落点 → 证据。

| # | REQ | 规格要点 | 实现落点 | 证据 | 判定 |
|---|---|---|---|---|---|
| 1 | REQ-AUTH-001 | 登录必填/验证码/错误清空/首登跳转 | `AuthService.login` 39–83：空值 401、验证码前置校验、`md5` 比对、`buildLoginResponse` 带 `first_login` | AuthServiceLoginTest ×9；前端 Login.vue 清空+刷新 | ✅ |
| 2 | REQ-AUTH-002 | 4 位验证码、不区分大小写、缺失/错误提示 | `CaptchaService` 4 位、`equalsIgnoreCase`、`store.remove` 一次性消费、TTL 5min | AuthControllerCaptchaTest；AuthService 47–52 | ✅ |
| 3 | REQ-AUTH-003 | 连续 5 次锁定、剩 N 次提示、成功重置 | `AuthService` `MAX_FAIL=5`，59–76 锁定/剩余次数，78–81 重置 | AuthServiceLoginTest；`Err.PASSWORD_LOCKED` | ✅ |
| 4 | REQ-AUTH-004 | 停用账户不可登录 | `AuthService:63-65` 状态非「启用」拒绝 | 测试报告业务规则表 | ✅ |
| 5 | REQ-AUTH-006 | 改密：当前密码必填、8–20 位 ≥3 类、新旧不同、两次一致 | `changePassword` 109–130 + `PasswordValidator.isValid`（8-20、4 类字符 ≥3） | AuthPasswordTest ×7 | ✅ |
| 6 | REQ-AUTH-007 | 强制改密无需当前密码、成功后踢出 token | `forceChangePassword` 133–141：不校验旧密码，`tokenService.remove(currentToken)` | AuthPasswordTest；前端 1.5s 跳转 ⏳ 浏览器项已在 issues 登记 | ✅（后端） |
| 7 | REQ-ORDER-001 | 分页+关键词/供应商/状态/时间范围筛选、翻页保持 | `OrderService.page/queryWrapper` 44–79：like/or、eq、ge/le、orderByAsc | OrderListTest ×6（含 paginationKeepsFilters） | ✅ |
| 8 | REQ-ORDER-002 | 必填校验、状态默认待审核 | `create` 82–99：`validate` + 重复编号 409 + `STATUS_PENDING` | OrderCreateTest ×7 | ✅ |
| 9 | REQ-ORDER-003 | 仅待审核/已驳回可编辑；已完成 422；驳回编辑回待审核；金额非负 | `update` 102–119：finished→422、rejected→pending；`validate` 金额 | OrderUpdateStateMachineTest ×5（含 finishedOrderEditReturns422） | ✅ |
| 10 | REQ-ORDER-004 | 仅待审核/已驳回可删除；已完成拒绝；前端二次确认 | `delete` 122–127 + 状态守卫；`OrderList.vue:214 confirmDelete` | OrderDeleteDetailTest ×6 | ✅ |
| 11 | REQ-ORDER-005 | 详情按 ID；不存在 404 | `OrderService:137` `Err.NOT_FOUND「订单不存在」` | OrderDeleteDetailTest | ✅ |
| 12 | REQ-ORDER-006 | 按筛选导出 .xls | `OrderController` `@RequirePerm("order:export")` + ExcelExporter | OrderExportTest ×3 | ✅ |
| 13 | REQ-AUDIT-001 | 仅待审核可审核，非待审核拒绝 | `OrderAuditService` 守卫 + 测试「非待审核→422 仅待审核订单可审核」 | 测试报告 §3.3 状态机 | ✅ |
| 14 | REQ-AUDIT-002 | 通过→已完成（不经过已审核） | pass 分支置 `STATUS_FINISHED` | OrderAuditPassRejectTest | ✅ |
| 15 | REQ-AUDIT-003 | 驳回→已驳回+记意见；编辑后回待审核可再审 | reject 分支 + `update` rejected→pending；历史升序 | 测试报告 §3.3 | ✅ |
| 16 | REQ-AUDIT-004 | 仅审核员可审核（管理员也 403） | `PermissionAspect:18,34` `AUDITOR_ONLY_PERMS` 硬编码 `ROLE_AUDITOR` | `adminAuditPassReturns403` / `auditorAuditPassReturns200` | ✅ |
| 17 | REQ-AUDIT-005 | 驳回意见必填 ≤255；通过可空 | `OPINION_MAX_LENGTH=255`；rejectEmpty 400 / over255 400 | OrderAuditPassRejectTest | ✅ |
| 18 | REQ-SUP-004 | 被订单引用不可删 | `SupplierService.delete:102` `orderMapper.selectCount(supplier_id)` | SupplierDeleteStatusTest ×7 | ✅ |
| 19 | REQ-SUP-005 | 停用供应商不出现在选项 | `options():125` `.eq("status","启用")` | SupplierOptionsTest + disabledSupplierReturns422 | ✅ |
| 20 | REQ-RPT-001 | 仅管理员可访问报表（summary/detail/export） | `ReportService.requireAdmin()` 三入口 48/66/71 + export 另加 `@RequirePerm("report:export")` | summaryForbiddenForNonAdmin / detailForbidden / reportExportForbidden | ✅ |
| 21 | REQ-RPT-005 | 按自然月分组趋势 | `ReportService.monthlyTrend` 61 | 单月断言 ✅；**跨月 ⏳**（issues #5，未关闭） | ⚠️ 部分（已登记） |
| 22 | REQ-USER-005/007 | 重置为默认密码哈希存储；用户管理仅管理员 | `UserService:243` `md5(DEFAULT_PASSWORD)`；`@RequirePerm("user:*")` | UserStatusResetDeleteTest ×9；auditorUsersReturns403 | ✅ |
| 23 | REQ-LOG-001/004 | 默认 5 条/页、{5,10,20,50} 档；仅 log:view | LogListTest 默认档位+非法拒绝；`LogController:21 @RequirePerm("log:view")` | auditorGets403 | ✅ |
| 24 | REQ-NFR-001/005 | 50 并发 / 2 万行 ≤2s | **未压测**（issues #4 ⏳，种子 tb_order=0 无灌量） | — | ⏳ 未验证（已登记，不伪造） |

**抽样结论**：22 条 ✅ 实现与规格一致且有测试/代码双证据；2 条 ⚠️/⏳（RPT-005 跨月、NFR 未压测）与 issues.md 登记状态一致，**未虚标通过**。覆盖 12/14 域（menus/organizations 未单列抽样，但同层 CRUD 由 MenuTreeTest×5 / OrganizationTreeTest×6 覆盖，报告已含）。

### 3.3 六维评价

| 维度 | 评级 | 说明 |
|---|---|---|
| **正确性** | 良好 | 171/171 单测全绿（源码 @Test 复核=171）；状态机/RBAC/分页边界/409 冲突均有反向专例；抽样 22/24 ✅。扣分：11 浏览器 TC + NFR 压测未执行（⏳ 已登记） |
| **可读性** | 良好 | 模块化 `controller/service/mapper/dto/entity` 清晰；命名一致（`requireXxx` 守卫模式）；0 TODO/FIXME/`System.out`/`printStackTrace`；中文错误文案统一 `Err` 常量。扣分：部分 Service 较长（OrderAuditService 235 行，可接受） |
| **架构** | 良好 | 单体分层 Controller→Service→Mapper→SQLite 与 architecture.md/plan 一致；`TokenInterceptor`（认证）与 `PermissionAspect`（授权）职责分离；`@ConditionalOnProperty(order.oplog.enabled)` 可关断日志切面；统一 `Result{code,message,data}` + Jackson SNAKE_CASE。扣分：Token 存内存 `ConcurrentHashMap`（重启失效、不支持多实例——单机部署 NFR-004 场景可接受，属已知权衡） |
| **安全** | 有已知短板 | 见 §4 清单：RBAC/验证码/SQLi 防护通过；**密码 MD5 为已知 FAIL（设计约束）**；gitleaks 1 条为文档占位误报；SpEL/v-html 经溯源为低风险 |
| **性能** | 未充分验证 | 分页统一 `PageParam.validate()`（1–100）防深翻页；`like` 走索引有限但 SQLite 单机量级可接受；**NFR-001/005 未压测 ⏳**（issues #4） |
| **品味** | 良好 | 遵守 AGENTS.md「简单至上/手术刀修改」：无投机抽象、无死代码标记；种子与文档注释诚实标注 MD5；测试报告如实区分 ✅/⏳ 不伪造。扣分：MD5 选型与 `StandardEvaluationContext` 用法略欠现代实践 |

---

## 4. 安全审查（栈适配清单）

技术栈：Spring Boot 3.3 + MyBatis-Plus + SQLite + Vue3 + Vite proxy + MD5 密码 + 图形验证码 + Bearer token + PermissionAspect。

| # | 检查项 | 结果 | 证据 |
|---|---|---|---|
| 1 | **Bearer 认证** | **PASS** | `TokenInterceptor.preHandle`：解析 `Authorization: Bearer ` 前缀 → `tokenService.resolve` → 查用户 → 写入 `UserContext`；无效 401 JSON。`WebConfig` 拦截 `/api/**`，仅豁免 `/api/auth/captcha`、`/api/auth/login`。测试：`noToken/meWithoutToken` 401 ✅。注：**实现为内存 UUID 会话令牌，非 JWT 签名**（文档示例用 JWT 形态占位）；单机可接受，多实例部署需改造（记录为已知权衡） |
| 2 | **RBAC（PermissionAspect）** | **PASS** | `@RequirePerm` 覆盖 user/role/supplier/order/audit/menu/func/org/log/report:export；`PermissionAspect` 先查 `AUDITOR_ONLY_PERMS`（audit:pass/reject 仅 ROLE_AUDITOR）再查 `hasPermission`；管理员审核 403、审核员碰用户管理 403、非 admin 报表 403 — 测试报告 §3.2 八场景全绿 |
| 3 | **SQL 注入（MyBatis-Plus）** | **PASS** | 全部查询经 `QueryWrapper.eq/like/apply("… = {0}", …)` 参数化（`LogService:51` 占位符传参）；无 `#{}` 之外的拼接；唯一 `.last("LIMIT 1")` 为常量字面量（`UserService:175`），无用户输入进入 SQL。`${}` 全仓 grep：仅 `application.yml` 的 `${ORDER_DB:…}` 与 pom `${lombok.version}` 属性占位 |
| 4 | **图形验证码** | **PASS** | `CaptchaService`：4 位、大小写不敏感（`equalsIgnoreCase`）、一次性消费（`store.remove`）、TTL 5 分钟、过期清理；登录前置校验（验证码错误先于密码错误）；仅 captcha/login 两接口免 token |
| 5 | **密码 MD5** | **FAIL（已知，设计约束）** | `AuthService.md5()` 单次 MD5 无盐（semgrep 命中 #2）；种子 `e10adc3949ba59abbe56e057f20f883e`。缓解：前端/接口不明文回显（REQ-USER-005）；5 次锁定；改密强制 8-20 位 ≥3 类。**不虚标 PASS**；建议后续迭代迁 bcrypt/scrypt（需种子迁移方案，超出本次只读范围） |
| 6 | **报表仅 admin** | **PASS** | `ReportService.requireAdmin()` 校验 `ROLE_ADMIN`（48/66/71 三入口全覆盖）；export 另加 `@RequirePerm("report:export")` 双保险；3 个 Forbidden 测试实证 |
| 7 | **CORS / 代理** | **PASS** | 后端**无 CORS 配置**（grep `cors/allowCredentials/allowedOrigins/Access-Control` = 0 命中）→ 无 `*`+credentials 危险组合；前端开发态仅 Vite `server.proxy['/api'] → localhost:8080`（`vite.config.js`），生产同源反代，**不暴露跨域面** |
| 8 | **XSS（Vue 转义）** | **PASS（附注）** | 默认 `{{ }}` 文本插值转义；语义化标签无 `v-html` 用户数据（OrderList 等业务视图 0 处 `v-html`）。唯一 `v-html` 为 `Layout.vue` 菜单图标，数据源为前端常量 `ICONS[code]`（`tb_menu` 无 icon 列，管理端无法注入）— 低风险，建议后续改 class 绑定（非阻塞）。semgrep 命中 #3–8 已溯源 |
| 9 | **仓库无秘密** | **PASS（附注）** | gitleaks 1 条 = API 文档 JWT 截断示例占位（§2.2 误报）；补充 grep（私钥/Bearer 长串/api_key/硬编码 secret）0 真实命中；`application.yml` 无凭据；种子密码仅哈希+注释。**无真实秘密入库** |

---

## 5. 结论

### 判定：**PASS_WITH_NOTES**

理由：

1. 前置检查全过：工作区干净、HEAD=`d8c1554`、171 后端测试全绿（报告 + 源码 @Test=171 双重印证）、无 SEVERE/阻塞开放问题。
2. 自动扫描：semgrep 8 findings 已逐条溯源（6 条低风险可接受、1 条 MD5 为已知设计约束、3+3 v-html 图标为前端常量非用户可控）；gitleaks 1 条为文档占位误报；补充 grep 秘密扫描 0 真实命中。
3. 六维质量良好，REQ 抽样 24/78（30.8% ≥ 20%）跨 12 域核对：22 ✅ / 2 与已登记 ⏳ 一致，无虚标。
4. 安全清单 9 项：**7 PASS / 1 FAIL（密码 MD5，已知设计约束）/ 1 PASS 附注**；FAIL 不隐瞒，按已知债务记录。
5. 存在 3 项已登记 ⏳ 待办（Playwright 浏览器场景、NFR 压测、RPT 跨月）——如实保留开放状态，不阻塞本阶段门禁（与 issues.md「无阻塞级」结论一致），故非裸 PASS。

### 问题表

| # | 严重度 | 模块 | 问题 | 状态 |
|---|---|---|---|---|
| 1 | 中 | 认证 | 密码单次无盐 MD5（semgrep use-of-md5；与种子方案绑定） | ⏳ 已知债务（建议后续迁移 bcrypt + 种子迁移，不阻塞） |
| 2 | 中 | 非功能 | REQ-NFR-001（50 并发）/ REQ-NFR-005（2 万行）未压测 | ⏳ 待办（issues #4，原样保留） |
| 3 | 低 | 前端 E2E | Playwright 未安装，11 TC + 13 UAT 浏览器场景未执行 | ⏳ 待浏览器执行（issues #3，原样保留） |
| 4 | 低 | 报表 | REQ-RPT-005 跨月双分组未单测 | ⏳ 待浏览器执行（issues #5，原样保留） |
| 5 | 低 | 测试基建 | 测试直连活库，无独立 test DB | ✅ 无遗留（issues #2，维持现状+收尾核查） |
| 6 | 低 | 数据 | `tb_user_role` 72 行孤儿（历史残留） | ✅ 已处理（issues #1，已清理复核；根因级联清理为后端待办） |
| 7 | 低 | XSS | `Layout.vue` 3 处 `v-html` 渲染菜单图标（当前为前端常量，无用户可控输入） | ⏳ 建议改 `:class` 绑定（非阻塞加固） |
| 8 | 低 | 扫描 | gitleaks 命中 API 文档 JWT 截断示例（`docs/api/接口定义.md:130`） | ✅ 已识别为误报（占位符非真实密钥；文档只读未改动） |
| 9 | 低 | 会话 | Token 为内存 UUID，重启失效、不支持水平扩展 | ⏳ 已知权衡（单机 NFR-004 可接受；多实例需改造） |
| 10 | 低 | 可维护性 | `OperationLogAspect` 使用 `StandardEvaluationContext` 解析注解内 SpEL | ✅ 已评估（表达式为编译期常量，非用户输入，无实际注入面） |

**汇总：0 项 SEVERE/阻塞；1 项中（已知债务 MD5）+ 1 项中（⏳ 压测待办）；其余低。阶段审查通过（附注），可进入归档/提交环节（由编排者执行 commit）。**

---

*审查执行：OpenCode Phase 5（审查部分）· 2026-09-23 · 只读代码 + 仅写 `docs/reviews/`。*
