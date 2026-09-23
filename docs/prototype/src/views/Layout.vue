<template>
  <div class="app-layout">
    <div class="sidebar">
      <div style="padding:16px 24px;font-weight:600;font-size:15px;border-bottom:1px solid var(--color-border-light);margin-bottom:8px;display:flex;align-items:center;gap:8px">
        <img src="/logo.svg?v2" style="width:22px;height:22px" />
        <span>订单管理系统</span>
      </div>
      <ul class="sidebar-menu">
        <li v-for="m in topMenus" :key="m.path" :class="{ active: $route.path === m.path }" @click="$router.push(m.path)">
          <span class="menu-icon" v-html="m.icon"></span>
          {{ m.label }}
        </li>
        <li @click="sysExpanded=!sysExpanded" :class="{ active: $route.path.startsWith('/system') }" style="cursor:pointer">
          <span class="menu-icon" v-html="sysIcon"></span>
          <span style="flex:1">系统管理</span>
          <span style="font-size:10px;color:var(--color-text-muted)">{{ sysExpanded ? '▾' : '▸' }}</span>
        </li>
        <li v-for="m in sysMenus" :key="m.path" v-show="sysExpanded" :class="{ active: $route.path === m.path }" @click="$router.push(m.path)" style="padding-left:48px;font-size:12px">
          <span class="menu-icon" v-html="m.icon" style="width:14px;height:14px"></span>
          {{ m.label }}
        </li>
      </ul>
    </div>
    <div class="main-content">
      <div class="navbar" style="display:flex;justify-content:flex-end;align-items:center;position:relative">
        <div style="font-size:13px;color:var(--color-text-muted);cursor:pointer;padding:4px 8px;border-radius:6px;user-select:none" @click.stop="showMenu=!showMenu">{{ currentUser.name }} / {{ currentUser.role }} ▾</div>
        <div v-if="showMenu" style="position:absolute;top:36px;right:0;background:var(--color-bg-white);border-radius:8px;box-shadow:var(--shadow-md);min-width:140px;z-index:100;overflow:hidden;color:var(--color-text)" @click="showMenu=false">
            <div style="padding:10px 16px;font-size:13px;cursor:pointer" @click="$router.push('/change-password')">修改密码</div>
            <div style="padding:10px 16px;font-size:13px;cursor:pointer;border-top:1px solid var(--color-border-light)" @click="logout">退出登录</div>
          </div>
        </div>
      <router-view @toast="addToast" />
    </div>
  </div>
  <div class="toast-container">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="'toast-' + t.type">{{ t.msg }}</div>
  </div>
</template>
<script>
export default {
  emits: ['toast'],
  data: () => ({
    showMenu: false,
    sysExpanded: true,
    toasts: [],
    currentUser: { name: 'admin', role: '管理员' },
    topMenus: [
      { code:'MENU_WORKBENCH', path: '/workbench', label: '工作台', icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="8" height="8" rx="1"/><rect x="13" y="3" width="8" height="8" rx="1"/><rect x="3" y="13" width="8" height="8" rx="1"/><rect x="13" y="13" width="8" height="8" rx="1"/></svg>' },
      { code:'MENU_ORDER', path: '/orders/list', label: '订单管理', icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
      { code:'MENU_AUDIT', path: '/order-audit/list', label: '订单审核', icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>' },
      { code:'MENU_SUPPLIER', path: '/suppliers/list', label: '供应商管理', icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v16"/></svg>' },
      { code:'MENU_REPORT', path: '/reports/home', label: '统计报表', icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>' },
    ],
    sysMenus: [
      { path: '/system/users', label: '用户管理', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>' },
      { path: '/system/roles', label: '角色管理', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>' },
      { path: '/system/departments', label: '部门管理', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>' },
      { path: '/system/organizations', label: '机构管理', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="3" width="20" height="5" rx="1"/><rect x="4" y="10" width="6" height="11" rx="1"/><rect x="14" y="10" width="6" height="11" rx="1"/><line x1="10" y1="7" x2="14" y2="7"/></svg>' },
      { path: '/system/menus', label: '菜单管理', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 6h16M4 12h16M4 18h16"/></svg>' },
      { path: '/system/functions', label: '功能管理', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>' },
      { path: '/system/logs', label: '操作日志', icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>' },
    ],
    sysIcon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>',
  }),
  methods: {
    addToast(t) {
      const id = Date.now()
      this.toasts.push({ ...t, id })
      setTimeout(() => { this.toasts = this.toasts.filter(x => x.id !== id) }, 2500)
    },
    logout() {
      this.$router.push('/login')
    }
  },
  mounted() {
    document.addEventListener('click', () => { this.showMenu = false })
    const stored = localStorage.getItem('menu_order')
    if (stored) {
      const order = JSON.parse(stored).filter(c => c !== 'MENU_SYSTEM')
      const codeMap = {}
      this.topMenus.forEach(m => { codeMap[m.code] = m })
      this.topMenus = order.filter(c => codeMap[c]).map(c => codeMap[c]).concat(this.topMenus.filter(m => !order.includes(m.code)))
    }
  }
}
</script>
