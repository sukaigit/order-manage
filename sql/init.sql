-- ============================================================
-- 订单管理系统 初始化数据 (SQLite)
-- 表名 tb_ 前缀；主键 id；字段下划线；时间字段 create_time / update_time
-- 幂等：CREATE TABLE IF NOT EXISTS；重复执行 INSERT 前可先清库
-- ============================================================

-- ---------------- 用户 / 角色 ----------------
CREATE TABLE IF NOT EXISTS tb_user (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL, -- 存储哈希，不明文展示
  real_name VARCHAR(64),
  status VARCHAR(8) NOT NULL DEFAULT '启用', -- 启用 / 停用
  department_id INTEGER,
  organization_id INTEGER,
  first_login INTEGER NOT NULL DEFAULT 0, -- 1=首次登录需强制改密（REQ-AUTH-007）
  fail_count INTEGER NOT NULL DEFAULT 0, -- 连续密码错误次数，>=5 锁定（REQ-AUTH-003）
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_role (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_user_role (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  role_id INTEGER NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------- 部门 / 机构 ----------------
CREATE TABLE IF NOT EXISTS tb_department (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_organization (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  short_name VARCHAR(64),
  level VARCHAR(16) NOT NULL, -- hq/branch1/branch2/sub1/sub2
  parent_id INTEGER,
  contact VARCHAR(64),
  phone VARCHAR(32),
  region VARCHAR(128),
  address VARCHAR(255),
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------- 菜单 / 功能 ----------------
CREATE TABLE IF NOT EXISTS tb_menu (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  parent_id INTEGER,
  route VARCHAR(128),
  menu_type VARCHAR(8) NOT NULL, -- level1 / level2
  sort INTEGER DEFAULT 0,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_function (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  perm VARCHAR(64) NOT NULL UNIQUE, -- 域:操作
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_menu_function (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  menu_id INTEGER NOT NULL,
  function_id INTEGER NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------- 角色-菜单 / 角色-功能 授权 ----------------
CREATE TABLE IF NOT EXISTS tb_role_menu (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  role_id INTEGER NOT NULL,
  menu_id INTEGER NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_role_function (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  role_id INTEGER NOT NULL,
  function_id INTEGER NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------- 业务表 ----------------
CREATE TABLE IF NOT EXISTS tb_supplier (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  contact VARCHAR(64),
  phone VARCHAR(32),
  address VARCHAR(255),
  status VARCHAR(8) NOT NULL DEFAULT '启用', -- 启用 / 停用
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_order (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  name VARCHAR(128) NOT NULL,
  amount REAL NOT NULL CHECK (amount >= 0), -- 金额非负
  supplier_id INTEGER,
  status VARCHAR(8) NOT NULL DEFAULT '待审核', -- 待审核 / 已驳回 / 已完成
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_order_audit (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  order_id INTEGER NOT NULL,
  result VARCHAR(8) NOT NULL, -- 通过 / 驳回
  auditor VARCHAR(64),
  opinion VARCHAR(255), -- 驳回必填，通过可空
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------- 操作日志（扩展表） ----------------
-- 在 architecture.md 13 表基线之上新增，支撑 logs spec 持久化
-- 列对齐 OperationLog.vue：操作人/操作类型/操作目标/IP/时间（时间->create_time）
CREATE TABLE IF NOT EXISTS tb_operation_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user VARCHAR(64) NOT NULL, -- 操作人
  action VARCHAR(64) NOT NULL, -- 操作类型
  target VARCHAR(128), -- 操作目标
  ip VARCHAR(64), -- IP
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP, -- 时间
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------- 索引（列表/筛选查询） ----------------
CREATE INDEX IF NOT EXISTS idx_user_status ON tb_user(status);
CREATE INDEX IF NOT EXISTS idx_user_department_id ON tb_user(department_id);
CREATE INDEX IF NOT EXISTS idx_user_organization_id ON tb_user(organization_id);
CREATE INDEX IF NOT EXISTS idx_user_role_user_id ON tb_user_role(user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role_id ON tb_user_role(role_id);
CREATE INDEX IF NOT EXISTS idx_organization_parent_id ON tb_organization(parent_id);
CREATE INDEX IF NOT EXISTS idx_organization_level ON tb_organization(level);
CREATE INDEX IF NOT EXISTS idx_menu_parent_id ON tb_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_menu_menu_type ON tb_menu(menu_type);
CREATE INDEX IF NOT EXISTS idx_menu_function_menu_id ON tb_menu_function(menu_id);
CREATE INDEX IF NOT EXISTS idx_menu_function_function_id ON tb_menu_function(function_id);
CREATE INDEX IF NOT EXISTS idx_role_menu_role_id ON tb_role_menu(role_id);
CREATE INDEX IF NOT EXISTS idx_role_menu_menu_id ON tb_role_menu(menu_id);
CREATE INDEX IF NOT EXISTS idx_role_function_role_id ON tb_role_function(role_id);
CREATE INDEX IF NOT EXISTS idx_role_function_function_id ON tb_role_function(function_id);
CREATE INDEX IF NOT EXISTS idx_supplier_status ON tb_supplier(status);
CREATE INDEX IF NOT EXISTS idx_order_status ON tb_order(status);
CREATE INDEX IF NOT EXISTS idx_order_supplier_id ON tb_order(supplier_id);
CREATE INDEX IF NOT EXISTS idx_order_create_time ON tb_order(create_time);
CREATE INDEX IF NOT EXISTS idx_order_audit_order_id ON tb_order_audit(order_id);
CREATE INDEX IF NOT EXISTS idx_operation_log_user ON tb_operation_log(user);
CREATE INDEX IF NOT EXISTS idx_operation_log_action ON tb_operation_log(action);
CREATE INDEX IF NOT EXISTS idx_operation_log_create_time ON tb_operation_log(create_time);

-- ============================================================
-- 初始化数据
-- ============================================================

-- 角色（3 行）：管理员 / 审核员 / 普通用户
INSERT INTO tb_role (id, code, name, remark, create_time, update_time) VALUES
(1, 'ROLE_ADMIN', '管理员', '拥有全部菜单与全部功能权限', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'ROLE_AUDITOR', '审核员', '工作台、订单审核、订单管理（查询）、供应商管理（查询）', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'ROLE_USER', '普通用户', '工作台、订单管理（增删改查导出）、供应商管理（增删改查导出）；无审核、无统计报表', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 部门（1 行）：默认根部门，业务部门运行时自建
INSERT INTO tb_department (id, code, name, remark, create_time, update_time) VALUES
(1, 'DEPT-000', '总部', '默认根部门', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 机构（10 行）：沿用模板 5 级结构（总行/一级分行/二级分行/一级支行/二级支行）
INSERT INTO tb_organization (id, code, name, short_name, level, parent_id, contact, phone, region, address, remark, create_time, update_time) VALUES
(1, 'HQ001', '总行', '总行', 'hq', NULL, '王总', '010-88888888', '北京市西城区', '金融街1号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'BJ001', '北京分行', '北京分行', 'branch1', 1, '张三', '010-66660001', '北京市朝阳区', '建国路88号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'SH001', '上海分行', '上海分行', 'branch1', 1, '李四', '021-66660002', '上海市浦东新区', '陆家嘴环路100号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'GZ001', '广州分行', '广州分行', 'branch1', 1, '', '', '广州市天河区', '珠江新城华夏路10号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 'BJ011', '朝阳支行', '朝阳支行', 'sub1', 2, '赵六', '010-88880001', '北京市朝阳区', '朝阳门外大街200号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'BJ021', '海淀支行', '海淀支行', 'sub1', 2, '', '', '北京市海淀区', '中关村大街50号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'BJ0111', '东城二级支行', '东城支行', 'sub2', 5, '', '', '北京市东城区', '王府井大街300号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 'SH011', '浦东支行', '浦东支行', 'sub1', 3, '孙七', '021-88880002', '上海市浦东新区', '张江路500号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 'SH021', '静安支行', '静安支行', 'sub1', 3, '', '', '上海市静安区', '南京西路600号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 'GZ011', '天河支行', '天河支行', 'sub1', 4, '', '', '广州市天河区', '天河路700号', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 用户（1 行）：默认管理员 admin
-- password: 123456 （MD5: e10adc3949ba59abbe56e057f20f883e，仅存哈希）
INSERT INTO tb_user (id, username, password, real_name, status, department_id, organization_id, first_login, fail_count, remark, create_time, update_time) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '启用', 1, 1, 0, 0, '默认管理员', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 用户-角色关联（1 行）：admin -> 管理员
INSERT INTO tb_user_role (id, user_id, role_id, create_time, update_time) VALUES
(1, 1, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 菜单（17 行）：6 个一级 + 11 个二级（订单审核为独立一级）
INSERT INTO tb_menu (id, code, name, parent_id, route, menu_type, sort, remark, create_time, update_time) VALUES
(1, 'MENU_WORKBENCH', '工作台', NULL, '/workbench', 'level1', 1, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'MENU_ORDER', '订单管理', NULL, '', 'level1', 2, '目录', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'MENU_ORDER_LIST', '订单列表', 2, '/orders/list', 'level2', 1, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'MENU_AUDIT', '订单审核', NULL, '', 'level1', 3, '独立一级目录', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 'MENU_AUDIT_LIST', '审核列表', 4, '/order-audit/list', 'level2', 1, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'MENU_SUPPLIER', '供应商管理', NULL, '', 'level1', 4, '目录', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'MENU_SUPPLIER_LIST', '供应商列表', 6, '/suppliers/list', 'level2', 1, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 'MENU_REPORT', '统计报表', NULL, '', 'level1', 5, '目录', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 'MENU_REPORT_HOME', '报表首页', 8, '/reports/home', 'level2', 1, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 'MENU_SYSTEM', '系统管理', NULL, '', 'level1', 6, '目录', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 'MENU_USERS', '用户管理', 10, '/system/users', 'level2', 1, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 'MENU_ROLES', '角色管理', 10, '/system/roles', 'level2', 2, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 'MENU_DEPTS', '部门管理', 10, '/system/departments', 'level2', 3, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 'MENU_ORGS', '机构管理', 10, '/system/organizations', 'level2', 4, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 'MENU_MENUS', '菜单管理', 10, '/system/menus', 'level2', 5, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 'MENU_FUNCS', '功能管理', 10, '/system/functions', 'level2', 6, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 'MENU_LOGS', '操作日志', 10, '/system/logs', 'level2', 7, '', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 功能（32 行）：订单/审核/供应商/报表/系统管理按钮
INSERT INTO tb_function (id, code, name, perm, remark, create_time, update_time) VALUES
(1, 'FUNC_ORDER_QUERY', '订单查询', 'order:query', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'FUNC_ORDER_CREATE', '订单新增', 'order:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'FUNC_ORDER_UPDATE', '订单编辑', 'order:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 'FUNC_ORDER_DELETE', '订单删除', 'order:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 'FUNC_ORDER_EXPORT', '订单导出', 'order:export', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 'FUNC_AUDIT_PASS', '审核通过', 'audit:pass', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 'FUNC_AUDIT_REJECT', '审核驳回', 'audit:reject', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 'FUNC_SUPPLIER_QUERY', '供应商查询', 'supplier:query', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 'FUNC_SUPPLIER_CREATE', '供应商新增', 'supplier:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 'FUNC_SUPPLIER_UPDATE', '供应商编辑', 'supplier:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 'FUNC_SUPPLIER_DELETE', '供应商删除', 'supplier:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 'FUNC_SUPPLIER_EXPORT', '供应商导出', 'supplier:export', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 'FUNC_REPORT_EXPORT', '报表导出', 'report:export', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 'FUNC_USER_CREATE', '用户新增', 'user:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 'FUNC_USER_UPDATE', '用户编辑', 'user:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 'FUNC_USER_DELETE', '用户删除', 'user:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 'FUNC_ROLE_CREATE', '角色新增', 'role:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(18, 'FUNC_ROLE_UPDATE', '角色编辑', 'role:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(19, 'FUNC_ROLE_DELETE', '角色删除', 'role:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(20, 'FUNC_DEPT_CREATE', '部门新增', 'dept:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(21, 'FUNC_DEPT_UPDATE', '部门编辑', 'dept:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(22, 'FUNC_DEPT_DELETE', '部门删除', 'dept:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(23, 'FUNC_ORG_CREATE', '机构新增', 'org:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(24, 'FUNC_ORG_UPDATE', '机构编辑', 'org:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(25, 'FUNC_ORG_DELETE', '机构删除', 'org:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(26, 'FUNC_MENU_CREATE', '菜单新增', 'menu:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(27, 'FUNC_MENU_UPDATE', '菜单编辑', 'menu:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(28, 'FUNC_MENU_DELETE', '菜单删除', 'menu:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(29, 'FUNC_FUNC_CREATE', '功能新增', 'func:create', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(30, 'FUNC_FUNC_UPDATE', '功能编辑', 'func:update', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(31, 'FUNC_FUNC_DELETE', '功能删除', 'func:delete', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(32, 'FUNC_LOG_VIEW', '日志查看', 'log:view', '', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 菜单-功能关联（32 行）：每个功能挂一个菜单
INSERT INTO tb_menu_function (id, menu_id, function_id, create_time, update_time) VALUES
(1, 3, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 3, 2, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 3, 3, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 3, 4, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 3, 5, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 5, 6, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 5, 7, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 7, 8, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 7, 9, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 7, 10, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 7, 11, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 7, 12, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 9, 13, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 11, 14, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 11, 15, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 11, 16, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 12, 17, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(18, 12, 18, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(19, 12, 19, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(20, 13, 20, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(21, 13, 21, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(22, 13, 22, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(23, 14, 23, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(24, 14, 24, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(25, 14, 25, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(26, 15, 26, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(27, 15, 27, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(28, 15, 28, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(29, 16, 29, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(30, 16, 30, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(31, 16, 31, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(32, 17, 32, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 角色-菜单授权：管理员=全部(17)，审核员=7，普通用户=5
INSERT INTO tb_role_menu (id, role_id, menu_id, create_time, update_time) VALUES
(1, 1, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 1, 2, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 1, 3, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 1, 4, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 1, 5, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 1, 6, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 1, 7, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 1, 8, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 1, 9, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 1, 10, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 1, 11, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 1, 12, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 1, 13, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 1, 14, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 1, 15, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 1, 16, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 1, 17, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(18, 2, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(19, 2, 2, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(20, 2, 3, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(21, 2, 4, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(22, 2, 5, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(23, 2, 6, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(24, 2, 7, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(25, 3, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(26, 3, 2, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(27, 3, 3, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(28, 3, 6, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(29, 3, 7, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 角色-功能授权：管理员=全部(32)，审核员=4（订单查询/审核通过/审核驳回/供应商查询），普通用户=10（订单+供应商 增删改查导出）
INSERT INTO tb_role_function (id, role_id, function_id, create_time, update_time) VALUES
(1, 1, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 1, 2, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 1, 3, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(4, 1, 4, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(5, 1, 5, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(6, 1, 6, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(7, 1, 7, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(8, 1, 8, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(9, 1, 9, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(10, 1, 10, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(11, 1, 11, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(12, 1, 12, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(13, 1, 13, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(14, 1, 14, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(15, 1, 15, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(16, 1, 16, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(17, 1, 17, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(18, 1, 18, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(19, 1, 19, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(20, 1, 20, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(21, 1, 21, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(22, 1, 22, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(23, 1, 23, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(24, 1, 24, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(25, 1, 25, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(26, 1, 26, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(27, 1, 27, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(28, 1, 28, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(29, 1, 29, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(30, 1, 30, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(31, 1, 31, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(32, 1, 32, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(33, 2, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(34, 2, 6, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(35, 2, 7, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(36, 2, 8, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(37, 3, 1, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(38, 3, 2, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(39, 3, 3, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(40, 3, 4, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(41, 3, 5, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(42, 3, 8, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(43, 3, 9, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(44, 3, 10, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(45, 3, 11, '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(46, 3, 12, '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 供应商（3 行）：来自 suppliers spec 初始化数据
INSERT INTO tb_supplier (id, code, name, contact, phone, address, status, create_time, update_time) VALUES
(1, 'SUP-001', '华东原料供应商', '张三', '13800000001', '上海市浦东新区', '启用', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(2, 'SUP-002', '南方包装供应商', '李四', '13800000002', '广州市天河区', '启用', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
(3, 'SUP-003', '北方物流供应商', '王五', '13800000003', '北京市朝阳区', '停用', '2026-01-01 00:00:00', '2026-01-01 00:00:00');

-- 操作日志（5 行样例）：对齐 OperationLog.vue 原型列，供日志页初始展示
INSERT INTO tb_operation_log (id, user, action, target, ip, create_time, update_time) VALUES
(1, 'admin', '登录', '系统', '192.168.1.10', '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
(2, 'admin', '新增订单', 'ORD20260905', '192.168.1.20', '2026-09-01 09:15:00', '2026-09-01 09:15:00'),
(3, '审核员', '审核通过', 'ORD20260905', '192.168.1.30', '2026-09-02 09:15:00', '2026-09-02 09:15:00'),
(4, 'admin', '导出报表', '订单明细报表', '192.168.1.40', '2026-09-03 10:15:00', '2026-09-03 10:15:00'),
(5, 'admin', '修改密码', '用户admin', '192.168.1.50', '2026-09-04 10:20:00', '2026-09-04 10:20:00');
