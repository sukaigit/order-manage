## Why

Phase 1 已完成 14 个 spec（78 REQ）、57 个 API 接口定义、14 表数据库设计与 Vue3 原型；Phase 3 需要一个可跟踪的 change 载体来承载实施任务分解与对操作日志落库等实施期新增需求的 delta，使 `openspec validate` 与后续 apply 能以 change 为单位推进订单管理系统从原型交付为可运行系统。

## What Changes

- 交付订单管理系统全栈实现：Spring Boot + MyBatis-Plus + SQLite 后端（57 端点）与由 `docs/prototype` 改造的 `frontend/` 前端。
- 新增操作日志落库能力（delta 见 `specs/logs/spec.md`）：关键写操作写入 `tb_operation_log`，支撑日志查询页运行时数据。
- 任务分解不重复维护，统一指向 `docs/plans/order-manage.md`（见 `tasks.md`）。

## Capabilities

### New Capabilities

（无 — 14 个能力 spec 已在 `openspec/specs/` 基线中）

### Modified Capabilities

- `logs`: 补充操作日志写入（落库）需求，使日志列表具备运行时数据来源。

## Impact

- 新增 `backend/`、`frontend/` 工程；只读依赖 `sql/init.sql` 与 `docs/` 设计文档；不修改既有 14 个主 spec。
