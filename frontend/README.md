# 管理系统原型模板

Vue 3 + Vite，Apple 风格设计。包含系统管理套件 + 订单业务页面（工作台/订单管理/订单审核/供应商管理/统计报表）。

> 本模板由 **OpenCode-标准化研发流程** Phase 1 使用：拷贝到项目 `docs/prototype/`，不要拷 `node_modules`（目标目录 `npm install` 自举）。

## 快速开始
```powershell
# 在 {PROJECT_ROOT} 下（以流程文档 {WORKFLOW_ROOT} 为源）
robocopy "$env:WORKFLOW_ROOT\prototype" ".\docs\prototype" /E /XD node_modules
Set-Location docs\prototype
npm install
npm run dev
```

## 目录结构
```
src/
├── assets/design.css        # Apple 风格 CSS（主色 #0066cc）
├── store/deptStore.js       # 共享数据示例
├── views/
│   ├── Login.vue            # 登录页（验证码/密码规则/锁定/首次改密）
│   ├── Layout.vue           # 侧边栏布局
│   ├── Workbench.vue        # 工作台（首页）
│   ├── OrderList.vue        # 订单管理
│   ├── OrderAuditList.vue   # 订单审核
│   ├── SupplierList.vue     # 供应商管理
│   ├── ReportHome.vue       # 统计报表
│   ├── Dashboard.vue        # 数据看板示例
│   ├── ExamplePage.vue      # CRUD 示例页（增删改查/筛选/分页/导入/导出）
│   ├── UserManage.vue       # 系统管理套件
│   ├── DepartmentManage.vue
│   ├── OrganizationManage.vue
│   ├── RoleManage.vue
│   ├── MenuManage.vue
│   ├── FuncManage.vue
│   ├── OperationLog.vue
│   └── ChangePassword.vue
├── router/index.js
└── ../design/design-guide.md  # 设计指南（拷贝后在 docs/prototype/design/）
```

## 创建新业务页面
1. 参考 `ExamplePage.vue` 的代码模式
2. 复制并在 router 添加路由
3. 修改字段名、接口数据、表单验证
4. 同步 `Layout.vue` 的 `topMenus`；业务页齐全后删除 `MENU_EXAMPLE` 与 `ExamplePage.vue`

## 首次接入必改（清除模板占位）
- 系统名：`Login.vue` / `ChangePassword.vue` / `Layout.vue` 标题
- `index.html` 的 `<title>`、`public/logo.svg`、`public/favicon.svg`
- 验证：全项目 grep 不应再出现旧占位名

## 技术栈
- Vue 3 (Options API)
- Vite
- Vue Router 4
- 无 UI 框架，纯 CSS
