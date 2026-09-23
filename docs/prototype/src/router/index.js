import { createRouter, createWebHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import UserManage from '../views/UserManage.vue'
import DepartmentManage from '../views/DepartmentManage.vue'
import RoleManage from '../views/RoleManage.vue'
import MenuManage from '../views/MenuManage.vue'
import FuncManage from '../views/FuncManage.vue'
import ExamplePage from '../views/ExamplePage.vue'
import OperationLog from '../views/OperationLog.vue'
import OrganizationManage from '../views/OrganizationManage.vue'
import ChangePassword from '../views/ChangePassword.vue'
import Workbench from '../views/Workbench.vue'
import OrderList from '../views/OrderList.vue'
import OrderAuditList from '../views/OrderAuditList.vue'
import SupplierList from '../views/SupplierList.vue'
import ReportHome from '../views/ReportHome.vue'

const routes = [
  { path: '/login', component: Login },
  { path: '/force-password', component: ChangePassword },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', redirect: '/workbench' },
      { path: 'workbench', component: Workbench },
      { path: 'orders/list', component: OrderList },
      { path: 'order-audit/list', component: OrderAuditList },
      { path: 'suppliers/list', component: SupplierList },
      { path: 'reports/home', component: ReportHome },
      { path: 'system/users', component: UserManage },
      { path: 'system/roles', component: RoleManage },
      { path: 'system/departments', component: DepartmentManage },
      { path: 'system/organizations', component: OrganizationManage },
      { path: 'system/menus', component: MenuManage },
      { path: 'system/functions', component: FuncManage },
      { path: 'system/logs', component: OperationLog },
      { path: 'dashboard', component: Dashboard },
      { path: 'example', component: ExamplePage },
      { path: 'change-password', component: ChangePassword },
    ]
  }
]

export default createRouter({ history: createWebHistory(), routes })
