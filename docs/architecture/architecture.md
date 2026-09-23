# 订单管理系统 系统架构设计

## 1. 项目概述

- **项目名称**：订单管理系统（order-manage）
- **技术栈**：后端 Spring Boot + MyBatis-Plus + SQLite；前端 Vue 3（Options API）+ Vite + Vue Router 4 + plain JS store
- **部署方式**：单机部署（应用与数据库同一台服务器）
- **非功能约束**（对应 `openspec/specs/nonfunctional/spec.md`）：
  - 约 50 用户同时在线，系统保持可用
  - 页面加载、查询、报表响应时间 ≤ 2s
  - 兼容 Chrome、Edge、360 等 Chromium 系浏览器
  - 约 20000 条业务数据量级下仍满足 ≤ 2s 响应

## 2. 分层结构

### 后端（backend/，Spring Boot）

```
Controller（REST API，参数校验、鉴权入口）
  → Service（业务逻辑、事务、审核状态机）
    → Mapper（MyBatis-Plus，单表/条件查询）
      → SQLite DB（单文件，tb_ 前缀表）
```

- Controller 层统一返回响应体，处理分页参数与错误码。
- Service 层承载业务规则（如订单状态流转、权限校验、日志记录）。
- Mapper 层基于 MyBatis-Plus 的 `BaseMapper` 与条件构造器，不手写 SQL 为主。

### 前端（由 docs/prototype/ 改造为正式 frontend/）

```
Vue3 组件树（views/*.vue，Options API）
  → plain JS store（src/store/*.js，非 Pinia，共享数据）
    → fetch / axios API 层（统一 baseURL、token 注入）
      → 后端 REST API
```

- 原型已使用 plain JS store（`deptStore.js`、`orderStore.js`、`supplierStore.js`），正式版延续该模式，不引入 Pinia。

## 3. 模块划分

对应 `openspec/specs/` 下 14 个 spec 模块。

### 业务模块（5）

| 模块 | Spec | 路由 | 核心能力 |
|---|---|---|---|
| 工作台 workbench | workbench | `/workbench` | 统计卡片（订单总数/待审核/已完成/启用供应商）、待办欢迎卡 |
| 订单 orders | orders | `/orders/list` | 订单分页查询、新增、编辑、删除、详情、Excel 导出 |
| 订单审核 order-audit | order-audit | `/order-audit/list` | 待审核订单通过/驳回，记录审核痕迹 |
| 供应商 suppliers | suppliers | `/suppliers/list` | 供应商 CRUD、启停用、引用校验、Excel 导出 |
| 统计报表 reports | reports | `/reports/home` | 状态分布饼图、供应商金额柱状图、按月趋势折线图、明细导出 |

### 系统模块（8 + 1 非功能）

| 模块 | Spec | 路由 | 核心能力 |
|---|---|---|---|
| 认证 auth | auth | `/login`、`/force-password`、`/change-password` | 登录/验证码/锁定/退出/改密（普通+首次强制） |
| 用户 users | users | `/system/users` | 用户 CRUD、启停用、重置密码（仅管理员） |
| 角色 roles | roles | `/system/roles` | 角色 CRUD、菜单/功能权限分配 |
| 部门 departments | departments | `/system/departments` | 部门 CRUD（仅预置 DEPT-000 总部） |
| 机构 organizations | organizations | `/system/organizations` | 5 级机构树 CRUD |
| 菜单 menus | menus | `/system/menus` | 菜单树 CRUD（6 个一级 + 11 个二级） |
| 功能 functions | functions | `/system/functions` | 按钮级功能 CRUD（`域:操作` 权限标识） |
| 日志 logs | logs | `/system/logs` | 操作日志分页查询与筛选（log:view） |
| 非功能 nonfunctional | nonfunctional | — | 并发/响应/浏览器/部署/数据量基准 |

## 4. 前后端目录结构建议

```
order-manage/
├── backend/                      # Spring Boot 工程（Phase 3 新建）
│   └── src/main/java/.../controller|service|mapper|config|common
├── frontend/                     # 正式前端（由 docs/prototype/ 改造）
│   ├── src/
│   │   ├── views/                # 复用原型页面
│   │   ├── router/index.js       # 复用原型路由
│   │   ├── store/                # plain JS stores，mock 数据替换为 API 调用
│   │   └── api/                  # 新增：统一 API 封装层
│   └── ...
├── docs/prototype/               # Phase 1 原型（静态 mock 数据）
├── sql/init.sql                  # SQLite 初始化脚本
├── openspec/specs/               # 14 个需求 spec
└── docs/adr/                     # 架构决策记录
```

**关键约定**：Phase 3 将 `docs/prototype` 复制/改造为正式 `frontend/`，把 store 中的静态 mock 数据替换为对后端 REST API 的真实调用（filters → GET 参数，forms → POST/PUT），路由与页面结构保持一致。

## 5. 组件关系与数据流

1. **登录**：用户访问 `/login` → 输入用户名/密码/验证码 → 后端校验（错误计数、锁定、账户状态）→ 返回会话 token；`firstLogin=true` 则跳转 `/force-password`。
2. **会话/token**：前端保存 token，API 层统一注入请求头；退出登录清除 token 并回登录页。
3. **菜单权限加载**：登录后按用户角色从 `tb_role_menu`、`tb_role_function` 加载可见菜单树与功能权限 → 渲染侧边栏；无权限菜单不展示。
4. **业务页 CRUD**：页面筛选条件映射为 GET 查询参数，表单提交映射为 POST/PUT；Service 层做业务校验与权限校验，Mapper 落库 SQLite。
5. **审核流**：普通用户新建订单（状态=待审核）→ 审核员在审核列表执行通过（→已完成）或驳回（→已驳回，必填意见，写 `tb_order_audit`）→ 已驳回订单编辑保存后自动回待审核，可再次审核。
6. **报表聚合**：管理员在报表首页设置时间范围+供应商筛选 → 后端按状态/供应商/自然月聚合订单 → 返回四类统计数据与明细 → 前端渲染图表并支持 .xls 导出。

## 6. 技术选型依据

| 选型 | 依据 |
|---|---|
| Spring Boot | 成熟生态、自动配置、单机 jar 部署简单；与 MyBatis-Plus 集成顺畅 |
| MyBatis-Plus | XML 少、条件构造器适合 CRUD 密集型管理后台；`BaseMapper` 覆盖常见单表操作 |
| SQLite | 嵌入式单文件、零运维（无独立数据库服务进程），契合「单机部署」NFR；约 2 万条数据 + 50 并发下查询性能足够；备份即拷贝单文件 |
| Vue 3 Options API | 原型（docs/prototype）已用 Options API 完成全部页面，延续可最大程度复用组件与路由，降低迁移成本；纯 CSS 无 UI 框架依赖 |
| plain JS store | 原型 store 为普通模块导出函数（非 Pinia），正式版保持一致，避免无谓的依赖引入 |

## 7. 权限模型（RBAC）

### 关联表（与 `sql/init.sql` 一致）

- `tb_user_role`：用户 ↔ 角色
- `tb_role_menu`：角色 ↔ 菜单（控制侧边栏可见性）
- `tb_role_function`：角色 ↔ 功能（控制按钮/接口级权限，`域:操作` 标识）
- 辅助：`tb_menu_function`（功能挂载菜单）、`tb_role`、`tb_menu`、`tb_function`

### 三角色矩阵（与 init.sql INSERT 及 roles/spec.md 对齐）

| 角色 | 菜单权限 | 功能权限 |
|---|---|---|
| 管理员 ROLE_ADMIN | 全部 17 项菜单（工作台、订单管理+订单列表、订单审核+审核列表、供应商管理+供应商列表、统计报表+报表首页、系统管理+7 个二级菜单） | 全部 32 项功能 |
| 审核员 ROLE_AUDITOR | 7 项：工作台、订单管理、订单列表、订单审核、审核列表、供应商管理、供应商列表（无统计报表、无系统管理） | 4 项：订单查询 `order:query`、审核通过 `audit:pass`、审核驳回 `audit:reject`、供应商查询 `supplier:query` |
| 普通用户 ROLE_USER | 5 项：工作台、订单管理、订单列表、供应商管理、供应商列表（无订单审核、无统计报表、无系统管理） | 10 项：订单增/删/改/查/导出 + 供应商增/删/改/查/导出（无审核功能、无系统管理功能） |

## 8. 关键决策

- **ADR-001**：采用 Spring Boot + MyBatis-Plus + Vue3 + SQLite 单体单机架构 —— 详见 [ADR-001](../adr/ADR-001.md)。

## 9. 数据库设计摘要

- **存储**：SQLite 单文件（如 `data/order-manage.db`），`sql/init.sql` 幂等建表 + 初始化数据
- **命名**：表前缀 `tb_`，主键 `id` 自增，字段下划线命名，时间字段 `create_time` / `update_time`
- **13 张表**（以 `sql/init.sql` 的 CREATE TABLE 为准）：

| # | 表名 | 用途 |
|---|---|---|
| 1 | `tb_user` | 用户账号 |
| 2 | `tb_role` | 角色 |
| 3 | `tb_user_role` | 用户-角色关联 |
| 4 | `tb_department` | 部门 |
| 5 | `tb_organization` | 机构（5 级树） |
| 6 | `tb_menu` | 菜单（树） |
| 7 | `tb_function` | 功能（按钮权限） |
| 8 | `tb_menu_function` | 菜单-功能关联 |
| 9 | `tb_role_menu` | 角色-菜单授权 |
| 10 | `tb_role_function` | 角色-功能授权 |
| 11 | `tb_supplier` | 供应商 |
| 12 | `tb_order` | 订单（状态：待审核/已驳回/已完成） |
| 13 | `tb_order_audit` | 订单审核记录 |

详细字段定义见 `docs/database/数据库设计.md`（Phase 2 ③ 输出）。

---

# 系统架构 / 接口设计 / 数据库设计
详见 docs/architecture/architecture.md / docs/api/接口定义.md / docs/database/数据库设计.md
