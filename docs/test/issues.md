# 问题清单 — order-manage Phase 4 测试保障

> 分支 `feat/order-manage` · HEAD `c674e46` · 2026-09-23
> 发现阶段：DB=测试数据核查 · UNIT=单元测试 · BUILD=构建验证 · BROWSER=待浏览器执行

| 模块 | 问题描述 | 发现阶段 | 修复方式 | 状态 |
|---|---|---|---|---|
| 用户-角色关联 | 活库 `tb_user_role` 存在 **72 行孤儿数据**（user_id=275/276/299/301/323… 指向不存在的 `tb_user`），种子基线为 1 行；系历史测试运行残留（部分测试创建临时用户后未级联清理 `tb_user_role`） | DB | Phase 4 执行 `DELETE FROM tb_user_role WHERE user_id NOT IN (SELECT id FROM tb_user);` 清理 72 行，复查 `tb_user_role=1` 与 `init.sql` 种子一致；根因建议：后端测试统一在 `@AfterEach` 级联清理（`UserSaveTest`/`RolePermissionAssignTest` 已有 cleanup，覆盖不全） | ✅ 已处理（数据已清理；根因改进为后端待办，不阻塞门禁） |
| 全局 | 测试对活库执行（`jdbc:sqlite` 真实文件），无独立 test DB 隔离；当前各测试类自带 `cleanup()` 且全绿后种子计数复核一致（14 表全对齐） | DB | 维持现状 + 收尾统一核查（见报告「测试数据清理」）；本阶段未改任何连接配置 | ✅ 无遗留 |
| 前端 E2E | 仓库未安装 Playwright（无 `frontend/node_modules/playwright`，根目录无 package.json），浏览器场景无法在本机执行 | BROWSER | `docs/test/e2e.cjs` 交付可运行脚本（`--dry` 自检通过，实跑步骤见脚本头注释）；相关 11 条 TC 标记 ⏳ 待浏览器执行 | ⏳ 待浏览器执行（不阻塞：API 层断言已由 171 单测覆盖） |
| 非功能 | REQ-NFR-001（50 并发）、REQ-NFR-005（2 万行 ≤2s）未执行压测；种子 `tb_order=0` 无灌量条件 | BROWSER | 标记待办，需压测工具 + 灌数脚本（超出 Phase 4 自动执行范围） | ⏳ 待办（见 TC-NFR-N-01/TC-NFR-P-04） |
| 报表 | REQ-RPT-005 按月趋势仅由单窗口测试覆盖「单月分组」断言，跨月双分组未单测 | UNIT | 单月断言已绿（`ReportSummaryTest.summaryAggregatesAllThreeArraysForDateWindow`）；跨月场景纳入 e2e.cjs `E2E-RPT-01` | ⏳ 待浏览器执行 |

**结论**：无阻塞级遗留缺陷；✅ 已处理 2 项，⏳ 待浏览器/待办 3 项（均不阻塞阶段门禁，已在测试报告「问题清单」给出影响与后续动作）。
