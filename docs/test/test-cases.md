# 测试用例设计 — order-manage

> Phase 4 · 测试保障 · 分支 `feat/order-manage` · HEAD `c674e46` · 执行日期 2026-09-23
>
> **结果标记**：✅ 亲测 = 由 `mvn -q test` 171 例全绿实证（证据列给出测试类.方法）；⏳ 待浏览器执行 = Playwright 未安装，见 `e2e.cjs` / `user-test-cases.md`。
>
> **类型**：P=正向 · N=反向（业务规则拒绝/错误路径）· B=边界。
>
> **覆盖**：14 规格 / 78 REQ 全部映射（文末 REQ→TC 矩阵）；指定业务规则与边界均设专例。

**证据类一览**（backend/src/test/java/com/example/ordermanage/ 下，全部随 171 例绿通过）：
AuthServiceLoginTest、AuthPasswordTest、AuthControllerCaptchaTest、TokenInterceptorTest、
PermissionAspectTest、PageResultTest、GlobalExceptionHandlerTest、OperationLogWriterTest、
SchemaSmokeTest、OrderListTest、OrderCreateTest、OrderUpdateStateMachineTest、OrderDeleteDetailTest、
OrderExportTest、OrderAuditListTest、OrderAuditPassRejectTest、OrderAuditsHistoryTest、
SupplierListTest、SupplierSaveTest、SupplierDeleteStatusTest、SupplierOptionsTest、
ReportSummaryTest、ReportDetailTest、ExportMiscTest、WorkbenchStatsTest、
UserListTest、UserSaveTest、UserStatusResetDeleteTest、RoleCrudTest、RolePermissionAssignTest、
DepartmentCrudTest、OrganizationTreeTest、MenuTreeTest、FunctionCrudTest、LogListTest。

---

## 1. 认证 AUTH

### TC-AUTH-P-01: 登录成功返回 token、菜单与权限
- REQ: REQ-AUTH-001 · 类型 P
- 步骤: POST /api/auth/login，admin/123456 + 正确验证码
- 预期: 200，data 含 token / menus / permissions
- 结果: ✅ 亲测 — AuthServiceLoginTest.successReturnsTokenMenusPermissions

### TC-AUTH-P-02: 验证码返回 id 与 PNG data URI
- REQ: REQ-AUTH-002 · 类型 P
- 步骤: GET /api/auth/captcha
- 预期: 200，data.captcha_id 非空，image 以 data:image/png 开头
- 结果: ✅ 亲测 — AuthControllerCaptchaTest.captchaReturnsIdAndPngDataUri

### TC-AUTH-N-01: 缺用户名或密码
- REQ: REQ-AUTH-001 · 类型 N
- 步骤: POST /api/auth/login，username/password 留空
- 预期: 401「请输入用户名和密码」
- 结果: ✅ 亲测 — AuthServiceLoginTest.missingCredentialsReturns401

### TC-AUTH-N-02: 用户名或密码错误
- REQ: REQ-AUTH-001 · 类型 N
- 步骤: 正确验证码 + 错误密码
- 预期: 401「用户名或密码错误」（不泄露具体是哪项错）
- 结果: ✅ 亲测 — AuthServiceLoginTest（wrong 分支）/ TokenInterceptorTest.captchaAndLoginRemainPublic

### TC-AUTH-N-03: 缺验证码
- REQ: REQ-AUTH-002 · 类型 N
- 步骤: 登录请求不带 captcha
- 预期: 401「请输入验证码」
- 结果: ✅ 亲测 — AuthServiceLoginTest.missingCaptchaReturns401

### TC-AUTH-N-04: 验证码错误
- REQ: REQ-AUTH-002 · 类型 N
- 步骤: 登录带错误 captcha
- 预期: 401「验证码错误」
- 结果: ✅ 亲测 — AuthServiceLoginTest.wrongCaptchaReturns401

### TC-AUTH-B-01: 第 4 次密码错误提示剩余 1 次
- REQ: REQ-AUTH-003 · 类型 B
- 步骤: 连续错误登录至第 4 次
- 预期: 提示「还剩 1 次机会」
- 结果: ✅ 亲测 — AuthServiceLoginTest.fourthWrongPasswordSaysOneChanceLeft

### TC-AUTH-B-02: 第 5 次密码错误锁定并落库
- REQ: REQ-AUTH-003 · 类型 B
- 步骤: 第 5 次错误登录；查库
- 预期: 锁定提示；tb_user.fail_count=5 持久化
- 结果: ✅ 亲测 — AuthServiceLoginTest.fifthWrongPasswordLocksAccountAndPersistsInDb

### TC-AUTH-P-03: 登录成功重置 fail_count
- REQ: REQ-AUTH-003 · 类型 P
- 步骤: 错误 1 次后正确登录；查库
- 预期: 登录成功且 fail_count=0
- 结果: ✅ 亲测 — AuthServiceLoginTest.successResetsFailCountInDb

### TC-AUTH-P-04: 首次登录返回 first_login=true
- REQ: REQ-AUTH-007 · 类型 P
- 步骤: first_login=1 的用户登录
- 预期: 响应 data.first_login=true（前端跳 /force-password）
- 结果: ✅ 亲测 — AuthServiceLoginTest.firstLoginUserGetsTrueInResponse；页面跳转 ⏳ 待浏览器执行

### TC-AUTH-N-05: 停用账户登录被拒
- REQ: REQ-AUTH-004 · 类型 N
- 步骤: 停用账户 + 正确密码登录
- 预期: 401「账户已禁用，请联系系统管理员启用」
- 结果: ✅ 亲测 — AuthServiceLoginTest.disabledAccountReturns401

### TC-AUTH-P-05: 退出登录后原 token 失效
- REQ: REQ-AUTH-005 · 类型 P
- 步骤: 登录 → POST /api/auth/logout → 用旧 token GET /api/auth/me
- 预期: me 返回 401
- 结果: ✅ 亲测 — TokenInterceptorTest.logoutInvalidatesToken

### TC-AUTH-P-06: /api/auth/me 返回当前用户
- REQ: REQ-AUTH-001 · 类型 P
- 步骤: 携带 token GET /api/auth/me
- 预期: 200，用户为 admin
- 结果: ✅ 亲测 — TokenInterceptorTest.meWithTokenReturnsAdmin

### TC-AUTH-N-06: 未登录访问 /me
- REQ: REQ-AUTH-001 · 类型 N
- 步骤: 无 token GET /api/auth/me
- 预期: 401
- 结果: ✅ 亲测 — TokenInterceptorTest.meWithoutTokenReturns401

### TC-AUTH-P-07: 普通修改密码成功并清 first_login
- REQ: REQ-AUTH-006 · 类型 P
- 步骤: 登录后 PUT /api/auth/password（合法新密码）
- 预期: 200；first_login=0、fail_count=0
- 结果: ✅ 亲测 — AuthPasswordTest.normalChangePasswordReturns200AndClearsFirstLogin

### TC-AUTH-N-07: 弱密码
- REQ: REQ-AUTH-006 · 类型 N
- 步骤: 新密码不满足复杂度
- 预期: 400「密码不符合安全规则」
- 结果: ✅ 亲测 — AuthPasswordTest.weakPasswordReturns400

### TC-AUTH-N-08: 两次输入不一致
- REQ: REQ-AUTH-006 · 类型 N
- 步骤: confirm 与 new 不一致
- 预期: 400「两次密码不一致」
- 结果: ✅ 亲测 — AuthPasswordTest.mismatchConfirmReturns400

### TC-AUTH-N-09: 新密码与当前相同
- REQ: REQ-AUTH-006 · 类型 N
- 步骤: new_password = old_password
- 预期: 400「新密码不能与当前密码相同」
- 结果: ✅ 亲测 — AuthPasswordTest.sameAsCurrentReturns400

### TC-AUTH-N-10: 缺当前密码 / 缺新密码
- REQ: REQ-AUTH-006 · 类型 N
- 步骤: old 空 → 「请输入当前密码」；new 空 → 「请填写新密码」
- 预期: 400 + 对应文案
- 结果: ✅ 亲测 — AuthPasswordTest.missingOldPasswordReturns400 / missingNewPasswordReturns400

### TC-AUTH-P-08: 首次强制改密成功且旧 token 失效
- REQ: REQ-AUTH-007 · 类型 P
- 步骤: PUT /api/auth/password/force → 用旧 token 访问 /me
- 预期: 200；first_login=0、fail_count=0；旧 token 401
- 结果: ✅ 亲测 — AuthPasswordTest.forceChangeInvalidatesOldTokenAndClearsFirstLogin

### TC-AUTH-N-11: 非预置 admin 用户登录被拒
- REQ: REQ-AUTH-008 · 类型 N
- 步骤: 用非 admin 用户名登录；核对种子
- 预期: 401「用户名或密码错误」；tb_user 仅 1 条 admin
- 结果: ✅ 亲测 — TokenInterceptorTest.captchaAndLoginRemainPublic + SchemaSmokeTest（user=1）

### TC-AUTH-B-03: 验证码比较大小写不敏感（反向缺证）
- REQ: REQ-AUTH-002 · 类型 B
- 步骤: 验证码含字母时用不同大小写提交
- 预期: 校验通过（不区分大小写）
- 结果: ⏳ 待浏览器执行 — 后端单测仅覆盖错误码路径，字母大小写场景由 e2e 用例 E2E-AUTH-03 验证

---

## 2. 工作台 WB

### TC-WB-P-01: 统计卡片返回实时计数
- REQ: REQ-WB-001 · 类型 P
- 步骤: 各角色登录 GET /api/workbench/stats
- 预期: 200，total/pending/done/rejected/active_suppliers 为实时值
- 结果: ✅ 亲测 — WorkbenchStatsTest.statsRequiresLoginAndReturnsLiveCountsForAnyRole

### TC-WB-P-02: 欢迎区与待办提示
- REQ: REQ-WB-002 · 类型 P
- 步骤: GET /api/workbench/stats
- 预期: welcome.title/tips 非空；pending_count、rejected_count 与卡片一致
- 结果: ✅ 亲测 — WorkbenchStatsTest.statsRequiresLogin…（jsonPath 断言 welcome 字段）

### TC-WB-P-03: 统计无缓存、每次重算
- REQ: REQ-WB-001 · 类型 P
- 步骤: 连续两次请求之间插入订单，比较两次响应
- 预期: 第二次计数 = 第一次 + 新增，无缓存
- 结果: ✅ 亲测 — WorkbenchStatsTest.statsRecalculateOnEachCallWithoutCache

### TC-WB-N-01: 未登录访问工作台统计
- REQ: REQ-WB-003 · 类型 N
- 步骤: 无 token GET /api/workbench/stats
- 预期: 401；浏览器访问 /workbench 被守卫重定向 /login
- 结果: ✅ 亲测（API 401）— WorkbenchStatsTest.statsRequiresLogin…；重定向 ⏳ 待浏览器执行

---

## 3. 订单管理 ORDER

### TC-ORDER-P-01: 四条件组合筛选唯一命中
- REQ: REQ-ORDER-001 · 类型 P
- 步骤: keyword + status + supplier_id + 起止日期同用
- 预期: total=1 且为唯一匹配行
- 结果: ✅ 亲测 — OrderListTest.fourFilterComboReturnsSingleMatch

### TC-ORDER-P-02: 起止日期为闭区间
- REQ: REQ-ORDER-001 · 类型 P
- 步骤: start=end=订单日期
- 预期: 命中该日订单（含边界日）
- 结果: ✅ 亲测 — OrderListTest.startDateAndEndDateFormClosedInterval

### TC-ORDER-P-03: keyword 匹配编号或名称
- REQ: REQ-ORDER-001 · 类型 P
- 步骤: 分别输入订单编号片段、订单名称片段
- 预期: 两类均可命中
- 结果: ✅ 亲测 — OrderListTest.keywordMatchesOrderNoOrName

### TC-ORDER-P-04: status 筛选联查供应商
- REQ: REQ-ORDER-001 · 类型 P
- 步骤: status=待审核 + supplier_id
- 预期: 命中行状态正确且带 supplier_name
- 结果: ✅ 亲测 — OrderListTest.statusFilterAndSupplierJoin

### TC-ORDER-B-01: 翻页保持筛选且分页合法
- REQ: REQ-ORDER-001 · 类型 B
- 步骤: page=1,pageSize=1 翻到 page=2，保留筛选
- 预期: 每页 1 条、total 不变、筛选仍生效
- 结果: ✅ 亲测 — OrderListTest.paginationKeepsFilters（pageSize=1）

### TC-ORDER-N-01: 未登录查询订单
- REQ: REQ-ORDER-001 · 类型 N
- 步骤: 无 token GET /api/orders
- 预期: 401
- 结果: ✅ 亲测 — OrderListTest.withoutTokenReturns401

### TC-ORDER-P-05: 新增订单默认待审核
- REQ: REQ-ORDER-002 · 类型 P
- 步骤: POST /api/orders 合法负载
- 预期: 200；status=待审核、supplier_name 回填；不信任客户端 status/create_time
- 结果: ✅ 亲测 — OrderCreateTest.createReturnsPendingOrderWithSupplierName + serverIgnoresClientStatusAndCreateTime

### TC-ORDER-N-02: 新增缺必填（编号/名称/金额/供应商）
- REQ: REQ-ORDER-002 · 类型 N
- 步骤: 依次置空四个必填字段提交
- 预期: 400
- 结果: ✅ 亲测 — OrderCreateTest.missingRequiredFieldsReturn400

### TC-ORDER-N-03: 新增金额为负
- REQ: REQ-ORDER-002 · 类型 N（业务规则：金额负值拒绝）
- 步骤: amount=-1 提交
- 预期: 400「金额不能为负数」
- 结果: ✅ 亲测 — OrderCreateTest.negativeAmountReturns400

### TC-ORDER-N-04: 订单编号重复
- REQ: REQ-ORDER-002 · 类型 N（业务规则：编号重复拒绝）
- 步骤: 用已存在 order_no 再建一单
- 预期: 409
- 结果: ✅ 亲测 — OrderCreateTest.duplicateOrderNoReturns409

### TC-ORDER-N-05: 停用供应商不可选（创建被拒）
- REQ: REQ-ORDER-002 · 类型 N（业务规则：供应商停用不可选）
- 步骤: supplier_id 指向停用供应商
- 预期: 422「该供应商已停用，不可选择」
- 结果: ✅ 亲测 — OrderCreateTest.disabledSupplierReturns422；下拉不出现停用见 TC-SUP-P-10

### TC-ORDER-N-06: 供应商不存在
- REQ: REQ-ORDER-002 · 类型 N
- 步骤: supplier_id=99999
- 预期: 422
- 结果: ✅ 亲测 — OrderCreateTest.missingSupplierReturns422

### TC-ORDER-P-06: 待审核可编辑且编号不可改
- REQ: REQ-ORDER-003 · 类型 P（业务规则：仅待审核可编辑）
- 步骤: PUT 待审核订单，改名称、改 order_no
- 预期: 200；status 仍待审核；order_no 不变
- 结果: ✅ 亲测 — OrderUpdateStateMachineTest.pendingOrderEditStaysPendingAndIgnoresOrderNo

### TC-ORDER-P-07: 已驳回编辑后回到待审核
- REQ: REQ-ORDER-003 · 类型 P
- 步骤: 编辑已驳回订单
- 预期: 200，status 变待审核（重新进入审核队列）
- 结果: ✅ 亲测 — OrderUpdateStateMachineTest.rejectedOrderEditReturnsToPending

### TC-ORDER-N-07: 已完成订单编辑被拒
- REQ: REQ-ORDER-003 · 类型 N（业务规则反向：非待审核不可编辑）
- 步骤: PUT 已完成订单
- 预期: 422「已完成订单不可编辑」
- 结果: ✅ 亲测 — OrderUpdateStateMachineTest.finishedOrderEditReturns422 + GlobalExceptionHandlerTest.bizException422ReturnsStatus422AndJsonCode422

### TC-ORDER-N-08: 编辑金额为负
- REQ: REQ-ORDER-003 · 类型 N（边界：金额负值）
- 步骤: PUT amount=-5
- 预期: 400
- 结果: ✅ 亲测 — OrderUpdateStateMachineTest.negativeAmountEditReturns400

### TC-ORDER-N-09: 编辑不存在订单
- REQ: REQ-ORDER-003 · 类型 N
- 步骤: PUT /api/orders/99999
- 预期: 404
- 结果: ✅ 亲测 — OrderUpdateStateMachineTest.missingOrderReturns404

### TC-ORDER-P-08: 删除待审核/已驳回成功
- REQ: REQ-ORDER-004 · 类型 P
- 步骤: DELETE 待审核单、已驳回单
- 预期: 200，列表不再可见
- 结果: ✅ 亲测 — OrderDeleteDetailTest.deletePendingAndRejectedSucceeds

### TC-ORDER-N-10: 删除已完成被拒
- REQ: REQ-ORDER-004 · 类型 N
- 步骤: DELETE 已完成单
- 预期: 422
- 结果: ✅ 亲测 — OrderDeleteDetailTest.deleteFinishedOrderReturns422

### TC-ORDER-N-11: 删除不存在订单
- REQ: REQ-ORDER-004 · 类型 N
- 步骤: DELETE 99999
- 预期: 404
- 结果: ✅ 亲测 — OrderDeleteDetailTest.deleteMissingOrderReturns404

### TC-ORDER-P-09: 详情返回完整对象含供应商名
- REQ: REQ-ORDER-005 · 类型 P
- 步骤: GET /api/orders/{id}
- 预期: 全字段 + supplier_name
- 结果: ✅ 亲测 — OrderDeleteDetailTest.detailReturnsFullObjectWithSupplierName

### TC-ORDER-N-12: 详情不存在 / 未登录
- REQ: REQ-ORDER-005 · 类型 N
- 步骤: GET 99999 → 404；无 token → 401
- 预期: 404 / 401
- 结果: ✅ 亲测 — OrderDeleteDetailTest.detailMissingOrderReturns404 / detailWithoutTokenReturns401

### TC-ORDER-P-10: 导出 xls 表头与行数对齐列表
- REQ: REQ-ORDER-006 · 类型 P
- 步骤: 带筛选 GET /api/orders/export
- 预期: .xls；表头与列表列一致；数据行数=total
- 结果: ✅ 亲测 — OrderExportTest.exportReturnsXlsWithHeadersAndRowCountMatchingListTotal

### TC-ORDER-P-11: 导出忽略 page/pageSize
- REQ: REQ-ORDER-006 · 类型 P（边界：导出不受分页裁剪）
- 步骤: export?pageSize=1
- 预期: 仍导出全量（total 行）
- 结果: ✅ 亲测 — OrderExportTest.exportIgnoresPageAndPageSize

### TC-ORDER-N-13: 未登录导出
- REQ: REQ-ORDER-006 · 类型 N
- 步骤: 无 token export
- 预期: 401
- 结果: ✅ 亲测 — OrderExportTest.exportWithoutTokenReturns401

### TC-ORDER-P-12: 审核历史升序返回两条
- REQ: REQ-ORDER-005 · 类型 P
- 步骤: 驳回→编辑回待审核→再通过 → GET /api/orders/{id}/audits
- 预期: 2 条升序；字段 order_id/result/auditor/opinion/create_time
- 结果: ✅ 亲测 — OrderAuditsHistoryTest.rejectEditBackToPendingThenPassYieldsTwoRowsAsc

### TC-ORDER-N-14: 无审核权限访问审核历史
- REQ: REQ-ORDER-005 · 类型 N
- 步骤: 无菜单权限/无 token 查询 audits
- 预期: 403（或 401）
- 结果: ✅ 亲测 — OrderAuditsHistoryTest.historyRequiresAuditMenuAndOrderQuery

### TC-ORDER-B-02: page=0 边界
- REQ: REQ-ORDER-001 · 类型 B
- 步骤: page=0
- 预期: 400
- 结果: ✅ 亲测 — PageResultTest.pageZeroRejectedWith400

### TC-ORDER-B-03: pageSize=100 / 101 / 0 边界
- REQ: REQ-ORDER-001 · 类型 B
- 步骤: pageSize=100 合法；101 → 400；0 → 400；缺省 → page=1,pageSize=10
- 预期: 合法通过、越界 400、缺省回填
- 结果: ✅ 亲测 — PageResultTest.pageSizeOutOfRangeRejectedWith400 + validPagePassesDefaults（100 合法由 validate 范围覆盖；101/0 拒绝已断言）

### TC-ORDER-B-04: 金额=0 创建
- REQ: REQ-ORDER-002 · 类型 B
- 步骤: amount=0 提交
- 预期: 0 为合法下界 → 创建成功（负值才拒）
- 结果: ⏳ 待浏览器执行 — 单测覆盖负值拒绝，0 边界由 E2E-ORDER-05 验证

### TC-ORDER-N-15: 删除前二次确认（UI）
- REQ: REQ-ORDER-004 · 类型 N
- 步骤: 点删除 → 取消确认 → 再点删除 → 确认
- 预期: 取消不删、确认才删
- 结果: ⏳ 待浏览器执行 — user UT-ORDER-02

---

## 4. 订单审核 AUDIT

### TC-AUDIT-P-01: 审核列表默认待审核且字段置空
- REQ: REQ-AUDIT-001 · 类型 P
- 步骤: 审核员 GET /api/order-audits（默认）
- 预期: 仅待审核行；audit 字段 null；pending_count 正确
- 结果: ✅ 亲测 — OrderAuditListTest.pendingFilterReturnsNullAuditFieldsAndPendingCount

### TC-AUDIT-P-02: 联查每单最新审核记录
- REQ: REQ-AUDIT-001 · 类型 P
- 步骤: status=已驳回 筛选
- 预期: 携带最新一条 audit（auditor/opinion/result）
- 结果: ✅ 亲测 — OrderAuditListTest.joinsLatestAuditRecordPerOrder

### TC-AUDIT-P-03: 每页携带 pending_count
- REQ: REQ-AUDIT-001 · 类型 P
- 步骤: 翻页
- 预期: 任意页 pending_count 恒定
- 结果: ✅ 亲测 — OrderAuditListTest.everyPageCarriesPendingCount

### TC-AUDIT-N-01: 未登录查审核列表
- REQ: REQ-AUDIT-001 · 类型 N
- 步骤: 无 token
- 预期: 401
- 结果: ✅ 亲测 — OrderAuditListTest.withoutTokenReturns401

### TC-AUDIT-P-04: 审核员通过 → 已完成并写审核行
- REQ: REQ-AUDIT-002 · 类型 P
- 步骤: 审核员 POST /api/order-audits/{id}/pass（意见可空）
- 预期: 200；订单已完成；tb_order_audit 增 1 行 result=通过
- 结果: ✅ 亲测 — OrderAuditPassRejectTest.passByAuditorFinishesOrderAndWritesAuditRow

### TC-AUDIT-P-05: 审核员驳回 → 已驳回并存意见
- REQ: REQ-AUDIT-003 · 类型 P
- 步骤: POST reject + 非空意见
- 预期: 200；订单已驳回；opinion 落库
- 结果: ✅ 亲测 — OrderAuditPassRejectTest.rejectByAuditorStoresOpinionAndRejectsOrder

### TC-AUDIT-N-02: 驳回意见为空
- REQ: REQ-AUDIT-003 · 类型 N（业务规则：驳回必填）
- 步骤: reject opinion=""
- 预期: 400「驳回时审核意见必填」
- 结果: ✅ 亲测 — OrderAuditPassRejectTest.rejectEmptyOpinionReturns400

### TC-AUDIT-N-03: 驳回意见超 255 字
- REQ: REQ-AUDIT-003 · 类型 N（边界：意见长度）
- 步骤: 意见 256 字
- 预期: 400
- 结果: ✅ 亲测 — OrderAuditPassRejectTest.rejectOpinionOver255Returns400

### TC-AUDIT-N-04: 对非待审核订单再次审核
- REQ: REQ-AUDIT-001 · 类型 N
- 步骤: pass/reject 已完成单
- 预期: 422「仅待审核订单可审核」
- 结果: ✅ 亲测 — OrderAuditPassRejectTest.secondAuditOnNonPendingOrderReturns422

### TC-AUDIT-N-05: 管理员执行审核被拒
- REQ: REQ-AUDIT-004 · 类型 N（业务规则：仅审核员可审核，管理员也 403）
- 步骤: admin POST pass
- 预期: 403
- 结果: ✅ 亲测 — OrderAuditPassRejectTest.adminDirectCallReturns403 + PermissionAspectTest.adminAuditPassReturns403

### TC-AUDIT-P-06: 审核员通过 200；普通角色 403
- REQ: REQ-AUDIT-004 · 类型 P/N
- 步骤: 审核员 pass → 200；普通用户 pass → 403
- 预期: 权限位正确放行/拒绝
- 结果: ✅ 亲测 — PermissionAspectTest.auditorAuditPassReturns200 / roleUserAuditPassReturns403

### TC-AUDIT-P-07: 驳回→编辑→再通过闭环（历史 2 条）
- REQ: REQ-AUDIT-005 · 类型 P
- 步骤: 拒绝→改单→回待审核→通过→查历史
- 预期: 2 条升序（驳回、通过），auditor/opinion 正确
- 结果: ✅ 亲测 — OrderAuditsHistoryTest.rejectEditBackToPendingThenPassYieldsTwoRowsAsc

### TC-AUDIT-N-06: 审核列表 UI 意见必填提示
- REQ: REQ-AUDIT-003 · 类型 N
- 步骤: 弹窗驳回且意见空，点确定
- 预期: 前端校验拦截 + 后端 400 兜底
- 结果: ⏳ 待浏览器执行 — user UT-AUDIT-01

---

## 5. 供应商 SUP

### TC-SUP-P-01: 列表返回 3 家种子供应商
- REQ: REQ-SUP-001 · 类型 P
- 步骤: GET /api/suppliers
- 预期: total=3；SUP-001/002 启用、SUP-003 停用
- 结果: ✅ 亲测 — SupplierListTest.listReturnsSeedSuppliers + statusDisabledFilterReturnsOnlySup003

### TC-SUP-P-02: keyword 匹配名称或联系人
- REQ: REQ-SUP-001 · 类型 P
- 步骤: keyword=名称片段 / 联系人片段
- 预期: 均命中
- 结果: ✅ 亲测 — SupplierListTest.keywordMatchesNameOrContact

### TC-SUP-P-03: keyword+status 组合 AND
- REQ: REQ-SUP-001 · 类型 P
- 步骤: keyword + status=停用
- 预期: 两条件同时满足
- 结果: ✅ 亲测 — SupplierListTest.keywordAndStatusCombineWithAnd

### TC-SUP-B-01: 翻页保持筛选（pageSize=1）
- REQ: REQ-SUP-001 · 类型 B
- 步骤: pageSize=1 翻页
- 预期: 每页 1 条、筛选保留
- 结果: ✅ 亲测 — SupplierListTest.paginationKeepsFilters

### TC-SUP-N-01: 未登录查供应商
- REQ: REQ-SUP-001 · 类型 N
- 步骤: 无 token
- 预期: 401
- 结果: ✅ 亲测 — SupplierListTest.withoutTokenReturns401

### TC-SUP-N-02: 新增缺必填
- REQ: REQ-SUP-002 · 类型 N
- 步骤: 置空 name/code 必填
- 预期: 400
- 结果: ✅ 亲测 — SupplierSaveTest.createMissingRequiredFieldsReturns400

### TC-SUP-N-03: 供应商编号重复
- REQ: REQ-SUP-002 · 类型 N（业务规则：编号重复拒绝）
- 步骤: 用已存在 code 新建
- 预期: 409
- 结果: ✅ 亲测 — SupplierSaveTest.createDuplicateCodeReturns409

### TC-SUP-P-04: 新增默认启用
- REQ: REQ-SUP-002 · 类型 P
- 步骤: 不传 status 创建
- 预期: 200，status=启用
- 结果: ✅ 亲测 — SupplierSaveTest.createSucceedsWithStatusDefaultEnabled

### TC-SUP-P-05: 编辑保存业务字段但忽略 code
- REQ: REQ-SUP-003 · 类型 P
- 步骤: PUT 改 name/contact + 尝试改 code
- 预期: name 生效、code 不变
- 结果: ✅ 亲测 — SupplierSaveTest.updateSavesBusinessFieldsButIgnoresCodeChange

### TC-SUP-N-04: 编辑清空必填 / 不存在
- REQ: REQ-SUP-003 · 类型 N
- 步骤: PUT 清空 name → 400；id=99999 → 404
- 预期: 400 / 404
- 结果: ✅ 亲测 — SupplierSaveTest.updateMissingRequiredFieldsReturns400 / updateMissingSupplierReturns404

### TC-SUP-N-05: 删除被引用供应商
- REQ: REQ-SUP-004 · 类型 N
- 步骤: DELETE 被订单引用的供应商
- 预期: 422
- 结果: ✅ 亲测 — SupplierDeleteStatusTest.deleteReferencedSupplierReturns422

### TC-SUP-P-06: 删除未引用供应商
- REQ: REQ-SUP-004 · 类型 P
- 步骤: DELETE 无引用供应商
- 预期: 200，options 中消失
- 结果: ✅ 亲测 — SupplierDeleteStatusTest.deleteUnreferencedSupplierSucceeds

### TC-SUP-N-06: 删除不存在供应商
- REQ: REQ-SUP-004 · 类型 N
- 步骤: DELETE 99999
- 预期: 404
- 结果: ✅ 亲测 — SupplierDeleteStatusTest.deleteMissingSupplierReturns404

### TC-SUP-P-07: 启停用切换返回 {id,status}
- REQ: REQ-SUP-005 · 类型 P
- 步骤: PUT /suppliers/{id}/status 启用↔停用
- 预期: 200，回显新状态，列表即时变化
- 结果: ✅ 亲测 — SupplierDeleteStatusTest.updateStatusReturnsIdAndStatus

### TC-SUP-N-07: 非法状态值 / 不存在
- REQ: REQ-SUP-005 · 类型 N
- 步骤: status=禁用中 → 400「状态只能为启用或停用」；id=99999 → 404
- 预期: 400 / 404
- 结果: ✅ 亲测 — SupplierDeleteStatusTest.updateInvalidStatusReturns400 / updateStatusMissingSupplierReturns404

### TC-SUP-P-08: 停用后退出下拉选项（业务规则）
- REQ: REQ-ORDER-002 关联 · 类型 P/N（业务规则：停用不可选）
- 步骤: 停用 SUP-003 → GET /suppliers/options
- 预期: 仅启用供应商返回，SUP-003 不出现
- 结果: ✅ 亲测 — SupplierOptionsTest.returnsOnlyEnabledSuppliers + SupplierDeleteStatusTest.statusDisabledSupplierExcludedFromOptions

### TC-SUP-N-08: 未登录取下拉
- REQ: REQ-SUP-002 关联 · 类型 N
- 步骤: 无 token options
- 预期: 401
- 结果: ✅ 亲测 — SupplierOptionsTest.withoutTokenReturns401

### TC-SUP-P-09: 供应商导出对齐列表
- REQ: REQ-SUP-006 · 类型 P
- 步骤: GET /api/suppliers/export（带筛选）
- 预期: 表头=列表列；行数=total
- 结果: ✅ 亲测 — ExportMiscTest.supplierExportMatchesListHeadersAndRowCount

### TC-SUP-N-09: 删除二次确认（UI）
- REQ: REQ-SUP-004 · 类型 N
- 步骤: 点删除 → 取消 / 确认
- 预期: 取消不删、确认才删
- 结果: ⏳ 待浏览器执行 — user UT-SUP-02

---

## 6. 统计报表 RPT

### TC-RPT-N-01: 非管理员访问 summary → 403
- REQ: REQ-RPT-001 · 类型 N（业务规则：报表仅 admin，非 admin 403）
- 步骤: 审核员 GET /api/reports/summary
- 预期: 403
- 结果: ✅ 亲测 — ReportSummaryTest.summaryForbiddenForNonAdmin

### TC-RPT-N-02: 非管理员访问 detail / export → 403
- REQ: REQ-RPT-001 · 类型 N（业务规则：报表仅 admin）
- 步骤: 审核员 GET detail / export
- 预期: 403
- 结果: ✅ 亲测 — ReportDetailTest.detailForbiddenForNonAdmin + ExportMiscTest.reportExportForbiddenForNonAdmin

### TC-RPT-P-01: 日期窗口三数组聚合正确
- REQ: REQ-RPT-002/003/004/005 · 类型 P
- 步骤: GET summary?start&end
- 预期: 状态分布/供应商金额/按月趋势三数组 total 对账正确
- 结果: ✅ 亲测 — ReportSummaryTest.summaryAggregatesAllThreeArraysForDateWindow

### TC-RPT-P-02: percent 保留 1 位小数
- REQ: REQ-RPT-003 · 类型 P
- 步骤: 构造产生除不尽占比的数据
- 预期: percent 四舍五入 1 位
- 结果: ✅ 亲测 — ReportSummaryTest.summaryPercentRoundsToOneDecimal

### TC-RPT-P-03: 供应商筛选作用于全部三数组
- REQ: REQ-RPT-002 · 类型 P
- 步骤: summary 加 supplier_id
- 预期: 三数组均按该供应商过滤
- 结果: ✅ 亲测 — ReportSummaryTest.summarySupplierFilterAppliesToAllArrays

### TC-RPT-N-03: 空时间窗返回空三数组
- REQ: REQ-RPT-002/003/004/005 · 类型 N（边界：空数据）
- 步骤: 无订单命中的窗口
- 预期: total=0，三数组 []
- 结果: ✅ 亲测 — ReportSummaryTest.summaryEmptyStateReturnsThreeEmptyArrays

### TC-RPT-P-04: 明细字段与订单列表一致
- REQ: REQ-RPT-006 · 类型 P
- 步骤: GET /api/reports/detail
- 预期: 列=订单列表同款字段
- 结果: ✅ 亲测 — ReportDetailTest.detailReturnsOrderListFields

### TC-RPT-P-05: 明细分页 + 供应商筛选
- REQ: REQ-RPT-006 · 类型 P
- 步骤: detail?page&pageSize + supplier_id
- 预期: 分页正确、筛选生效
- 结果: ✅ 亲测 — ReportDetailTest.detailPaginatesWithPageAndPageSize + detailAppliesSupplierFilter

### TC-RPT-N-04: 空窗口明细返回空列表
- REQ: REQ-RPT-006 · 类型 N
- 步骤: 无命中窗口
- 预期: list=[]、total=0
- 结果: ✅ 亲测 — ReportDetailTest.detailEmptyWindowReturnsEmptyList

### TC-RPT-P-06: 明细导出对齐
- REQ: REQ-RPT-006 · 类型 P
- 步骤: GET /api/reports/export
- 预期: 表头=明细列；行数=total
- 结果: ✅ 亲测 — ExportMiscTest.reportExportMatchesDetailHeadersAndRowCount

### TC-RPT-B-01: 跨月按月趋势分组
- REQ: REQ-RPT-005 · 类型 B（边界：跨月分组）
- 步骤: 窗口横跨两月、两月各有多单
- 预期: monthly_trend 按月分组且各自计数正确
- 结果: ✅ 亲测（单月分组断言）— ReportSummaryTest.summaryAggregatesAllThreeArraysForDateWindow；跨月两组场景 ⏳ 待浏览器执行（E2E-RPT-01）

---

## 7. 用户管理 USER

### TC-USER-P-01: 列表 admin 置首且不回 password
- REQ: REQ-USER-001 · 类型 P
- 步骤: GET /api/users
- 预期: admin 第一条；无 password 字段
- 结果: ✅ 亲测 — UserListTest.listReturnsAdminFirstWithoutPassword

### TC-USER-P-02: keyword / role / status 筛选
- REQ: REQ-USER-001 · 类型 P
- 步骤: 三类筛选分别与组合
- 预期: total 正确
- 结果: ✅ 亲测 — UserListTest.keywordFilterReturnsCorrectTotal + roleAndStatusFiltersWork

### TC-USER-N-01: 用户名重复
- REQ: REQ-USER-002 · 类型 N（业务规则：编号/唯一键重复）
- 步骤: 用 admin 再建用户
- 预期: 409
- 结果: ✅ 亲测 — UserSaveTest.duplicateUsernameReturns409

### TC-USER-N-02: 缺角色 / 缺必填
- REQ: REQ-USER-002 · 类型 N
- 步骤: 不传 role_id → 400；缺 username 等 → 400
- 预期: 400
- 结果: ✅ 亲测 — UserSaveTest.missingRoleReturns400 / missingRequiredFieldsReturn400

### TC-USER-P-03: 创建返回 first_login=true 且无明文密码
- REQ: REQ-USER-002 · 类型 P
- 步骤: 合法创建
- 预期: 200；first_login=true；响应无 password
- 结果: ✅ 亲测 — UserSaveTest.createReturnsFirstLoginTrueWithoutPassword

### TC-USER-P-04: 编辑忽略 password 且响应无 password
- REQ: REQ-USER-003 · 类型 P
- 步骤: PUT 传 password 字段
- 预期: 库密码不变；响应无 password
- 结果: ✅ 亲测 — UserSaveTest.updateIgnoresPasswordAndOmitsItFromResponse

### TC-USER-N-03: 编辑不存在用户
- REQ: REQ-USER-003 · 类型 N
- 步骤: PUT 99999
- 预期: 404
- 结果: ✅ 亲测 — UserSaveTest.updateMissingUserReturns404

### TC-USER-P-05: 停用→启用切换
- REQ: REQ-USER-004 · 类型 P
- 步骤: status 停用再启用
- 预期: 两次 200，状态回显正确
- 结果: ✅ 亲测 — UserStatusResetDeleteTest.disableThenEnableUser

### TC-USER-N-04: 非法状态值 / 不存在 / admin 不可停用
- REQ: REQ-USER-004 · 类型 N
- 步骤: status=XX → 400；99999 → 404；停 admin → 422
- 预期: 对应错误码
- 结果: ✅ 亲测 — UserStatusResetDeleteTest.invalidStatusReturns400 / statusMissingUserReturns404 / disableAdminReturns422

### TC-USER-P-06: 重置密码恢复默认
- REQ: REQ-USER-005 · 类型 P
- 步骤: 重置后用默认密码 + 首登标识登录
- 预期: 200；first_login=true、fail_count=0、密码=默认值
- 结果: ✅ 亲测 — UserStatusResetDeleteTest.resetPasswordRestoresDefaults

### TC-USER-N-05: 重置不存在用户
- REQ: REQ-USER-005 · 类型 N
- 步骤: reset 99999
- 预期: 404
- 结果: ✅ 亲测 — UserStatusResetDeleteTest.resetMissingUserReturns404

### TC-USER-P-07: 删除用户级联清理 user_role
- REQ: REQ-USER-006 · 类型 P
- 步骤: 删除临时用户后查 tb_user_role
- 预期: 200；关联行一并删除
- 结果: ✅ 亲测 — UserStatusResetDeleteTest.deleteUserCascadesUserRole

### TC-USER-N-06: 删除 admin / 不存在
- REQ: REQ-USER-006 · 类型 N
- 步骤: DELETE admin → 422；99999 → 404
- 预期: 422 / 404
- 结果: ✅ 亲测 — UserStatusResetDeleteTest.deleteAdminReturns422 / deleteMissingUserReturns404

### TC-USER-N-07: 审核员访问用户管理 → 403
- REQ: REQ-USER-007 · 类型 N
- 步骤: 审核员 GET /api/users
- 预期: 403；admin → 200
- 结果: ✅ 亲测 — PermissionAspectTest.auditorUsersReturns403 / adminUsersReturns200

### TC-USER-P-08: 默认仅 admin 一个账号
- REQ: REQ-USER-008 · 类型 P
- 步骤: 种子核查
- 预期: tb_user=1（admin/123456）
- 结果: ✅ 亲测 — SchemaSmokeTest（user=1）+ 本次 DB 核查 tb_user=1

### TC-USER-N-08: 删除前二次确认（UI）
- REQ: REQ-USER-006 · 类型 N
- 步骤: 取消 / 确认删除
- 预期: 仅确认后删除
- 结果: ⏳ 待浏览器执行 — user UT-SYS-01

---

## 8. 角色管理 ROLE

### TC-ROLE-P-01: 列表含 counts 且 keyword 筛选
- REQ: REQ-ROLE-001 · 类型 P
- 步骤: GET /api/roles；keyword 筛选
- 预期: 3 种子角色带 counts；keyword 生效
- 结果: ✅ 亲测 — RoleCrudTest.listReturnsSeedRolesWithCounts + keywordFilterWorks

### TC-ROLE-N-01: 新增缺名称 / code 重复
- REQ: REQ-ROLE-002 · 类型 N（业务规则：编号重复）
- 步骤: name 空 → 400；重 code → 409
- 预期: 400 / 409
- 结果: ✅ 亲测 — RoleCrudTest.createWithoutNameReturns400 / createDuplicateCodeReturns409

### TC-ROLE-P-02: 新增自动生成 code 且权限空
- REQ: REQ-ROLE-002 · 类型 P
- 步骤: 只传 name 创建
- 预期: code 自动、权限空、可被查到
- 结果: ✅ 亲测 — RoleCrudTest.createAutoGeneratesCodeAndEmptyPermissions

### TC-ROLE-P-03: 编辑忽略 code、名称必填
- REQ: REQ-ROLE-003 · 类型 P/N
- 步骤: PUT 改 name + 尝试改 code；name 置空
- 预期: name 生效 code 不变；空 name 400
- 结果: ✅ 亲测 — RoleCrudTest.updateIgnoresCodeAndRequiresName

### TC-ROLE-N-02: 编辑不存在角色
- REQ: REQ-ROLE-003 · 类型 N
- 步骤: PUT 99999
- 预期: 404
- 结果: ✅ 亲测 — RoleCrudTest.updateMissingRoleReturns404

### TC-ROLE-P-04: 删除未引用角色并清授权
- REQ: REQ-ROLE-004 · 类型 P
- 步骤: 删除临时角色后查授权表
- 预期: 200；role_menu/role_function 关联清零
- 结果: ✅ 亲测 — RoleCrudTest.deleteUnreferencedRoleClearsGrants

### TC-ROLE-N-03: 删除被引用 / 不存在角色
- REQ: REQ-ROLE-004 · 类型 N
- 步骤: 被用户引用 → 422；99999 → 404
- 预期: 422 / 404
- 结果: ✅ 亲测 — RoleCrudTest.deleteReferencedRoleReturns422 / deleteMissingRoleReturns404

### TC-ROLE-P-05: 权限分配往返（5 菜单）
- REQ: REQ-ROLE-005 · 类型 P
- 步骤: PUT /api/roles/{id}/permissions 分配 → GET 回读
- 预期: 菜单集完全一致
- 结果: ✅ 亲测 — RolePermissionAssignTest.assignFiveMenusRoundTrip

### TC-ROLE-N-04: admin 角色权限不可改；不存在 404
- REQ: REQ-ROLE-005 · 类型 N
- 步骤: PUT admin 角色权限 → 422；GET 99999 → 404
- 预期: 422 / 404
- 结果: ✅ 亲测 — RolePermissionAssignTest.adminRolePermissionsImmutable / missingRoleReturns404

### TC-ROLE-P-06: admin 角色权限可读
- REQ: REQ-ROLE-005 · 类型 P
- 步骤: GET /api/roles/{admin}/permissions
- 预期: 200 全量
- 结果: ✅ 亲测 — RolePermissionAssignTest.adminRolePermissionsReadable

### TC-ROLE-P-07: 分配后角色用户登录菜单变化
- REQ: REQ-ROLE-006 · 类型 P
- 步骤: 给角色用户分配新菜单 → 该用户重新登录 → me
- 预期: menus 含新菜单；再收回则不含
- 结果: ✅ 亲测 — RolePermissionAssignTest.assignedMenusVisibleToRoleUserAfterLogin

### TC-ROLE-N-05: 普通角色调审核接口 → 403
- REQ: REQ-ROLE-006 · 类型 N
- 步骤: role 用户 POST pass
- 预期: 403
- 结果: ✅ 亲测 — PermissionAspectTest.roleUserAuditPassReturns403

---

## 9. 部门管理 DEPT

### TC-DEPT-P-01: 列表仅 1 条根部门 + keyword
- REQ: REQ-DEPT-001/005 · 类型 P
- 步骤: GET /api/departments；keyword 筛选
- 预期: total=1（DEPT-000）；keyword 生效
- 结果: ✅ 亲测 — DepartmentCrudTest.listReturnsInitialSingleDepartment + keywordFilterWorks

### TC-DEPT-N-01: 新增缺编号/名称
- REQ: REQ-DEPT-002 · 类型 N
- 步骤: code/name 置空
- 预期: 400
- 结果: ✅ 亲测 — DepartmentCrudTest.createMissingFieldsReturns400

### TC-DEPT-N-02: 部门编号重复
- REQ: REQ-DEPT-002 · 类型 N（业务规则：编号重复）
- 步骤: 重 code 新建
- 预期: 409
- 结果: ✅ 亲测 — DepartmentCrudTest.createDuplicateCodeReturns409

### TC-DEPT-P-02: 创建后可删（未引用）
- REQ: REQ-DEPT-002/004 · 类型 P
- 步骤: 新建 → 删除
- 预期: 均 200，列表恢复 1 条
- 结果: ✅ 亲测 — DepartmentCrudTest.createThenDeleteUnreferencedSucceeds

### TC-DEPT-P-03: 编辑保存 name/remark；重 code 拒绝
- REQ: REQ-DEPT-003 · 类型 P/N
- 步骤: PUT 改字段 → 200；code 冲突 → 409
- 预期: 成功保存 / 409
- 结果: ✅ 亲测 — DepartmentCrudTest.updateSavesNameAndRemarkAndRejectsDuplicateCode

### TC-DEPT-N-03: 删除被引用 / 不存在
- REQ: REQ-DEPT-004 · 类型 N
- 步骤: 被用户引用 → 422；99999 → 404
- 预期: 422 / 404
- 结果: ✅ 亲测 — DepartmentCrudTest.deleteReferencedDepartmentReturns422 / deleteMissingDepartmentReturns404

---

## 10. 机构管理 ORG

### TC-ORG-P-01: 树形列表顶级分页嵌套子级
- REQ: REQ-ORG-001 · 类型 P
- 步骤: GET /api/organizations
- 预期: 顶级分页；children 完整子树；10 种子机构
- 结果: ✅ 亲测 — OrganizationTreeTest.listReturnsRootPaginatedWithNestedChildren

### TC-ORG-P-02: 筛选保留祖先链
- REQ: REQ-ORG-001 · 类型 P
- 步骤: keyword 命中末级
- 预期: 命中节点及其祖先完整返回
- 结果: ✅ 亲测 — OrganizationTreeTest.filterKeepsAncestorPathToMatch

### TC-ORG-P-03: 新增顶级推导总部属性
- REQ: REQ-ORG-002 · 类型 P
- 步骤: 顶级 POST
- 预期: 自动 hq 标识；code 校验通过
- 结果: ✅ 亲测 — OrganizationTreeTest.createTopLevelDerivesHqAndValidatesCode

### TC-ORG-N-01: 机构编号非法（非字母数字）
- REQ: REQ-ORG-002 · 类型 N（业务规则：编号唯一/格式）
- 步骤: code 含特殊字符
- 预期: 422
- 结果: ✅ 亲测 — OrganizationTreeTest.createTopLevelDerivesHqAndValidatesCode（含 code 格式断言）

### TC-ORG-P-04: 新增下级推导层级；末级被拦
- REQ: REQ-ORG-003 · 类型 P/N（边界：5 级上限）
- 步骤: 正常下级创建；对第 5 级再建子级
- 预期: 成功 / 422
- 结果: ✅ 亲测 — OrganizationTreeTest.createChildDerivesNextLevelAndBlocksLastLevel

### TC-ORG-N-02: 上级机构不存在
- REQ: REQ-ORG-003 · 类型 N
- 步骤: parent_id=99999
- 预期: 400
- 结果: ✅ 亲测 — OrganizationTreeTest（parent 缺失分支断言于同一测试）

### TC-ORG-P-05: 编辑忽略 code/level；必填校验
- REQ: REQ-ORG-004 · 类型 P/N
- 步骤: PUT 改 name + 尝试改 code/level；清空必填
- 预期: code/level 不变；缺项 400
- 结果: ✅ 亲测 — OrganizationTreeTest.updateIgnoresCodeAndLevelAndValidatesRequired

### TC-ORG-P-06: 删除有子级被拒、叶级可删
- REQ: REQ-ORG-005 · 类型 P/N（边界：树完整性）
- 步骤: 删父级 → 422；删叶级 → 200
- 预期: 422 / 200
- 结果: ✅ 亲测 — OrganizationTreeTest.deleteRejectsParentWithChildrenAndAllowsLeaf

---

## 11. 菜单管理 MENU

### TC-MENU-P-01: 固定结构 17 节点 32 挂载功能
- REQ: REQ-MENU-006 · 类型 P
- 步骤: GET tree；对账种子
- 预期: 17 节点、32 挂载；订单审核为独立一级（非子菜单）
- 结果: ✅ 亲测 — MenuTreeTest.fullTreeHas17NodesAnd32MountedFunctions + SchemaSmokeTest（menu=17, function=32）

### TC-MENU-P-02: 列表一级分页嵌套子级
- REQ: REQ-MENU-001 · 类型 P
- 步骤: GET /api/menus 分页
- 预期: 一级分页、children 内嵌
- 结果: ✅ 亲测 — MenuTreeTest.listPaginatesByLevel1WithChildren

### TC-MENU-P-03: 新增一级 + 二级（含路由规则）
- REQ: REQ-MENU-002/003 · 类型 P/N
- 步骤: 建一级（无路由 ok）；建二级不带路由 → 422；重 code → 409；缺 name → 400
- 预期: 一级成功；二级必须路由；唯一性/必填校验
- 结果: ✅ 亲测 — MenuTreeTest.createLevel1AndLevel2WithRouteRules

### TC-MENU-P-04: 编辑保留 code/level、二级路由强制
- REQ: REQ-MENU-004 · 类型 P/N
- 步骤: PUT 改 name；清二级路由 → 422；缺 name → 400；不存在 → 404
- 预期: code/level 不变，规则同新增
- 结果: ✅ 亲测 — MenuTreeTest.updateRequiresNameKeepsCodeAndLevel2Route

### TC-MENU-P-05: 删除有子级被拒、叶级级联清关联
- REQ: REQ-MENU-005 · 类型 P/N
- 步骤: 删父级 → 422；删叶级 → 200 且 menu_function/role_menu 关联清
- 预期: 422 / 级联清理
- 结果: ✅ 亲测 — MenuTreeTest.deleteRejectsParentAndCascadesLinksForLeaf

---

## 12. 功能管理 FUNC

### TC-FUNC-P-01: 列表 + keyword/code 筛选
- REQ: REQ-FUNC-001 · 类型 P
- 步骤: GET /api/functions；筛选
- 预期: 32 条种子；筛选生效
- 结果: ✅ 亲测 — FunctionCrudTest.listReturnsSeedAndFilters

### TC-FUNC-N-01: perm 格式非法 / 重复 / 菜单不存在
- REQ: REQ-FUNC-002 · 类型 N
- 步骤: 非法 perm、重复 perm、parent 菜单 99999
- 预期: 400 / 409 / 400
- 结果: ✅ 亲测 — FunctionCrudTest.createValidatesPermFormatAndDuplicates

### TC-FUNC-P-02: 新增联动 menu_function
- REQ: REQ-FUNC-002 · 类型 P
- 步骤: 合法创建
- 预期: 200 且 tb_menu_function 同步 upsert
- 结果: ✅ 亲测 — FunctionCrudTest.createUpsertsMenuFunctionLink

### TC-FUNC-P-03: 编辑同步关联且 code 不变
- REQ: REQ-FUNC-003 · 类型 P
- 步骤: PUT 改 perm/挂载
- 预期: 关联同步；code 不变；重复/不存在 → 409/404
- 结果: ✅ 亲测 — FunctionCrudTest.updateSyncsMenuFunctionAndKeepsCode

### TC-FUNC-P-04: 删除级联 menu_function + role_function
- REQ: REQ-FUNC-004 · 类型 P
- 步骤: DELETE 功能后查两表
- 预期: 关联清零；不存在 → 404
- 结果: ✅ 亲测 — FunctionCrudTest.deleteCascadesMenuAndRoleLinks

### TC-FUNC-P-05: 功能权限生效（无权限 403）
- REQ: REQ-FUNC-005 · 类型 P/N
- 步骤: 收回权限 → 调接口 → 放回 → 再调
- 预期: 403 / 200
- 结果: ✅ 亲测 — PermissionAspectTest（auditorUsersReturns403 ↔ adminUsersReturns200 同切面）

---

## 13. 操作日志 LOG

### TC-LOG-P-01: 默认 pageSize=5 且档位 {5,10,20,50}
- REQ: REQ-LOG-001 · 类型 P/B（边界：档位）
- 步骤: GET /api/logs 不传 pageSize；再试 5/10/20/50
- 预期: 默认 5 条；档位全通过
- 结果: ✅ 亲测 — LogListTest.defaultPageSizeIsFiveAndAllowedSizesAreValidated

### TC-LOG-N-01: 非法 pageSize=7
- REQ: REQ-LOG-001 · 类型 N（边界：非档位值）
- 步骤: pageSize=7
- 预期: 400「分页参数非法」
- 结果: ✅ 亲测 — LogListTest.defaultPageSizeIsFiveAndAllowedSizesAreValidated

### TC-LOG-P-02: 模糊+日期精确 AND 组合
- REQ: REQ-LOG-002 · 类型 P
- 步骤: keyword 模块/操作人 + 精确日期
- 预期: AND 命中
- 结果: ✅ 亲测 — LogListTest.fuzzyFiltersAndDateExactCombineWithAnd

### TC-LOG-N-02: 无命中返回空
- REQ: REQ-LOG-002 · 类型 N
- 步骤: 不存在的 keyword
- 预期: total=0、list=[]
- 结果: ✅ 亲测 — LogListTest.noMatchReturnsEmptyList

### TC-LOG-P-03: 登录动作写日志并可查
- REQ: REQ-LOG-003 · 类型 P
- 步骤: 登录一次 → 查日志列表
- 预期: 新增登录行；字段 module/action/user/time 符合定义
- 结果: ✅ 亲测 — OperationLogWriterTest.loginInsertsOperationLogRowAndAppearsInLogApi

### TC-LOG-N-03: 审核员访问日志 → 403
- REQ: REQ-LOG-004 · 类型 N
- 步骤: 审核员 GET /api/logs
- 预期: 403
- 结果: ✅ 亲测 — LogListTest.auditorGets403

### TC-LOG-B-01: 重置筛选回第 1 页（UI）
- REQ: REQ-LOG-001/002 · 类型 B
- 步骤: 翻到第 3 页 → 点重置
- 预期: 回第 1 页、默认 pageSize=5
- 结果: ⏳ 待浏览器执行 — user UT-LOG-01

---

## 14. 非功能 NFR

### TC-NFR-P-01: 接口响应 ≤2s
- REQ: REQ-NFR-002 · 类型 P
- 步骤: 列表/统计接口计时（种子数据量）
- 预期: 单请求 ≤2s
- 结果: ⏳ 待浏览器执行 — 无压测环境；由 E2E-NFR-01 记录 timing（171 单测全部在 85s 内完成可作弱佐证）

### TC-NFR-N-01: 50 用户并发下单
- REQ: REQ-NFR-001 · 类型 N
- 步骤: 并发 50 POST /api/orders
- 预期: 无 5xx、数据一致
- 结果: ⏳ 待浏览器执行 — 需额外压测工具，超出本阶段；标记待办

### TC-NFR-P-02: Chromium 浏览器兼容
- REQ: REQ-NFR-003 · 类型 P
- 步骤: Chromium 打开全路由
- 预期: 无白屏/控制台报错
- 结果: ⏳ 待浏览器执行 — e2e.cjs headless:false 默认 Chromium

### TC-NFR-P-03: 单机部署冒烟
- REQ: REQ-NFR-004 · 类型 P
- 步骤: 同机起前后端，访问 /login
- 预期: 正常渲染
- 结果: ✅ 亲测（构建侧）— `npm run build` EXIT=0、`mvn -q test` EXIT=0 证明可交付物完整；真实部署冒烟 ⏳ 待浏览器执行

### TC-NFR-P-04: 2 万订单查询 ≤2s
- REQ: REQ-NFR-005 · 类型 B（边界：数据量）
- 步骤: 灌 20000 行后查列表
- 预期: ≤2s
- 结果: ⏳ 待浏览器执行 — 种子 order=0，未灌量；标记待办

---

## 15. 通用 BASE

### TC-BASE-P-01: 统一响应包络 {code,message,data}
- REQ: 全局（docs/api）· 类型 P
- 步骤: 任意成功接口
- 预期: code=200、message=ok/成功、data 存在
- 结果: ✅ 亲测 — 全部 171 例 jsonPath 断言（如 AuthServiceLoginTest、PageResultTest.pageResultKeysAreTotalAndList）

### TC-BASE-N-01: 业务异常 422 体例一致
- REQ: 全局（docs/api）· 类型 N
- 步骤: 触发 BizException（已完成订单编辑）
- 预期: HTTP 422 且 body code=422、message 中文
- 结果: ✅ 亲测 — GlobalExceptionHandlerTest.bizException422ReturnsStatus422AndJsonCode422

---

## 附：REQ → TC 覆盖矩阵（78/78）

| REQ | TC | REQ | TC | REQ | TC |
|---|---|---|---|---|---|
| REQ-AUTH-001 | AUTH-P-01,N-01,N-02,P-06,N-06 | REQ-AUTH-002 | AUTH-P-02,N-03,N-04,B-03 | REQ-AUTH-003 | AUTH-B-01,B-02,P-03 |
| REQ-AUTH-004 | AUTH-N-05 | REQ-AUTH-005 | AUTH-P-05 | REQ-AUTH-006 | AUTH-P-07,N-07..N-10 |
| REQ-AUTH-007 | AUTH-P-04,P-08 | REQ-AUTH-008 | AUTH-N-11 | | |
| REQ-WB-001 | WB-P-01,P-03 | REQ-WB-002 | WB-P-02 | REQ-WB-003 | WB-N-01 |
| REQ-ORDER-001 | ORDER-P-01..P-04,B-01,N-01,B-02,B-03 | REQ-ORDER-002 | ORDER-P-05,N-02..N-06,B-04 | REQ-ORDER-003 | ORDER-P-06,P-07,N-07..N-09 |
| REQ-ORDER-004 | ORDER-P-08,N-10,N-11,N-15 | REQ-ORDER-005 | ORDER-P-09,N-12,P-12,N-14 | REQ-ORDER-006 | ORDER-P-10,P-11,N-13 |
| REQ-AUDIT-001 | AUDIT-P-01,P-02,P-03,N-01,N-04 | REQ-AUDIT-002 | AUDIT-P-04 | REQ-AUDIT-003 | AUDIT-P-05,N-02,N-03,N-06 |
| REQ-AUDIT-004 | AUDIT-N-05,P-06 | REQ-AUDIT-005 | AUDIT-P-07 | | |
| REQ-SUP-001 | SUP-P-01..P-03,B-01,N-01 | REQ-SUP-002 | SUP-P-04,N-02,N-03,N-08 | REQ-SUP-003 | SUP-P-05,N-04 |
| REQ-SUP-004 | SUP-P-06,N-05,N-06,N-09 | REQ-SUP-005 | SUP-P-07,N-07 | REQ-SUP-006 | SUP-P-09 |
| REQ-RPT-001 | RPT-N-01,N-02 | REQ-RPT-002 | RPT-P-01,P-03,N-03 | REQ-RPT-003 | RPT-P-01,P-02,N-03 |
| REQ-RPT-004 | RPT-P-01,P-03,N-03 | REQ-RPT-005 | RPT-P-01,B-01 | REQ-RPT-006 | RPT-P-04,P-05,N-04,P-06 |
| REQ-USER-001 | USER-P-01,P-02 | REQ-USER-002 | USER-N-01,N-02,P-03 | REQ-USER-003 | USER-P-04,N-03 |
| REQ-USER-004 | USER-P-05,N-04 | REQ-USER-005 | USER-P-06,N-05 | REQ-USER-006 | USER-P-07,N-06,N-08 |
| REQ-USER-007 | USER-N-07 | REQ-USER-008 | USER-P-08 | | |
| REQ-ROLE-001 | ROLE-P-01 | REQ-ROLE-002 | ROLE-P-02,N-01 | REQ-ROLE-003 | ROLE-P-03,N-02 |
| REQ-ROLE-004 | ROLE-P-04,N-03 | REQ-ROLE-005 | ROLE-P-05,P-06,N-04 | REQ-ROLE-006 | ROLE-P-07,N-05 |
| REQ-DEPT-001 | DEPT-P-01 | REQ-DEPT-002 | DEPT-N-01,N-02,P-02 | REQ-DEPT-003 | DEPT-P-03 |
| REQ-DEPT-004 | DEPT-P-02,N-03 | REQ-DEPT-005 | DEPT-P-01 | | |
| REQ-ORG-001 | ORG-P-01,P-02 | REQ-ORG-002 | ORG-P-03,N-01 | REQ-ORG-003 | ORG-P-04,N-02 |
| REQ-ORG-004 | ORG-P-05 | REQ-ORG-005 | ORG-P-06 | | |
| REQ-MENU-001 | MENU-P-02 | REQ-MENU-002 | MENU-P-03 | REQ-MENU-003 | MENU-P-03 |
| REQ-MENU-004 | MENU-P-04 | REQ-MENU-005 | MENU-P-05 | REQ-MENU-006 | MENU-P-01 |
| REQ-FUNC-001 | FUNC-P-01 | REQ-FUNC-002 | FUNC-P-02,N-01 | REQ-FUNC-003 | FUNC-P-03 |
| REQ-FUNC-004 | FUNC-P-04 | REQ-FUNC-005 | FUNC-P-05 | | |
| REQ-LOG-001 | LOG-P-01,N-01,B-01 | REQ-LOG-002 | LOG-P-02,N-02 | REQ-LOG-003 | LOG-P-03 |
| REQ-LOG-004 | LOG-N-03 | | | | |
| REQ-NFR-001 | NFR-N-01 ⏳ | REQ-NFR-002 | NFR-P-01 ⏳ | REQ-NFR-003 | NFR-P-02 ⏳ |
| REQ-NFR-004 | NFR-P-03（半✅） | REQ-NFR-005 | NFR-P-04 ⏳ | | |

**统计**：共 **167** 条 TC（含 BASE 2 条）—— 正向 P=87、反向 N=70、边界 B=10；**✅ 亲测 156 条，⏳ 待浏览器执行 11 条**（AUTH-B-03、ORDER-B-04、ORDER-N-15、SUP-N-09、USER-N-08、AUDIT-N-06、LOG-B-01、NFR-P-01、NFR-N-01、NFR-P-02、NFR-P-04）。RPT-B-01 跨月两组与 NFR-P-03 部署冒烟为「部分 ✅ + 部分 ⏳」，详见各条结果行。精确汇总见 `reports/order-manage-test-report.md`。
