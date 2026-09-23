# 设计指南 — Apple 风格

## 配色
- 主色: `#0066cc`
- 背景: `#f5f5f7`
- 卡片背景: `#ffffff`
- 边框: `#d2d2d7`
- 文字: `#1d1d1f`
- 次要文字: `#86868b`
- 危险: `#dc2626`
- 成功: `#1a8a3a`

## 字体
- `-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif`
- 基础字号: 14px
- 圆角: 8px (radius-lg), 12px (modal)

## 按钮
- 主要: `.btn-primary` — `#0066cc` 背景，白色文字
- 次要: `.btn-secondary` — 白色背景，边框
- 文字: `.btn-text` — 无边框，hover 变背景色
- 危险: `.btn-danger` — `#dc2626` 背景
- Pill 形状: `border-radius: 20px`

## 表单
- 标签使用 `.form-label`（字号13px，灰色）
- 输入框使用 `.form-input`（14px，圆角8px，边框 `#d2d2d7`）
- 必填字段加红色 `*`：`<span style="color:var(--color-danger)">*</span>`
- 占位：新增时显示「请选择」，编辑时隐藏

## 表格
- 使用 `.data-table` 类
- 背景交替: `:nth-child(even)`
- 标题和内容居中对齐（`.data-table th, .data-table td { text-align: center }`）
- 操作列使用 `display: flex; justify-content: center; flex-wrap: wrap`

## 弹窗
- 遮罩层: `.modal-overlay`（半透明黑，点空白关闭）
- 弹窗: `.modal`（最小宽480px，圆角12px，居中）
- 标题: `.modal-title`（18px，加粗）
- 底部: `.modal-footer`（右对齐，取消+确定按钮）
- 删除确认: SVG 图标 + scale-in 动画 + 「此操作不可撤销」

## 分页
- 支持 5/10/20/50 条/页 选择
- 智能省略号页码
- 显示范围：「显示第 X-Y 条，共 Z 条」

## 标签
- `.badge-active` — 绿色背景（启用/上架/已结算）
- `.badge-disabled` — 灰色背景（禁用/下架）
- `.badge-pending` — 待处理状态

## 筛选栏
- 使用 `.filter-bar` 类
- `display: flex; flex-wrap: wrap`
- 每个字段包裹在 `.form-group` 中
- 输入框/下拉统一宽度 180px

## Toast
- 通过 `$emit('toast', {msg, type})` 触发
- 类型：`success`/`warning`/`error`
- 2.5秒自动消失，stack 展示

## 导出
- 使用 XML Excel 格式（`.xls`），零依赖
- CSV BOM UTF-8 格式备选
- 文件命名：`{模块名}_{日期}.xls`

## 共享数据源
- 使用 JS store 模块（如 `src/store/deptStore.js`）
- 导出 `getDepts`/`addDept`/`updateDept`/`deleteDept` 方法
- 页面间通过 `import { getDepts } from '../store/deptStore.js'` 引用
