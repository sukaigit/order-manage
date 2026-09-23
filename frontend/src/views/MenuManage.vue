<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号</label><input class="form-input" v-model="fCode" placeholder="搜索" style="width:140px" /></div>
      <div class="form-group"><label class="form-label">名称</label><input class="form-input" v-model="fName" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">路由</label><input class="form-input" v-model="fRoute" placeholder="搜索" style="width:150px" /></div>
      <div class="form-group"><label class="form-label">级别</label>
        <select class="form-select" v-model="fType">
          <option value="">全部</option><option value="level1">一级菜单</option><option value="level2">二级菜单</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增菜单</button>
    </div>
    <table class="data-table">
      <tr><th>菜单编号</th><th>菜单名称</th><th>级别</th><th>路由</th><th>备注</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="6" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="m in flatList" :key="m.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ m.code }}</td>
        <td>
          <span style="display:inline-flex;align-items:center;gap:4px;cursor:pointer" @click="toggleExpand(m)">
            <span v-if="m.children && m.children.length" style="font-size:10px;color:var(--color-text-muted);width:14px;text-align:center">{{ expanded[m.id] ? '▾' : '▸' }}</span>
            <span v-else style="width:14px"></span>
            <span :style="{paddingLeft: (m.depth||0)*16 + 'px', fontWeight: m.depth===0?'600':'400'}">{{ m.name }}</span>
          </span>
        </td>
        <td><span class="badge" :class="m.menu_type==='level1'?'badge-active':'badge-pending'" style="font-size:11px">{{ m.menu_type==='level1'?'一级菜单':'二级菜单' }}</span></td>
        <td><code v-if="m.route" style="background:var(--color-bg);padding:2px 6px;border-radius:4px;font-size:12px">{{ m.route }}</code><span v-else style="color:var(--color-text-muted);font-size:12px">—</span></td>
        <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ m.remark }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button v-if="m.menu_type==='level1'" class="btn btn-text btn-sm" @click="openAddChild(m)">新增子菜单</button>
          <button class="btn btn-text btn-sm" @click="openEdit(m)">编辑</button>
          <button v-if="canMove(m,-1)" class="btn btn-text btn-sm" @click="move(m,-1)" title="上移">↑</button>
          <button v-if="canMove(m,1)" class="btn btn-text btn-sm" @click="move(m,1)" title="下移">↓</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(m)">删除</button>
        </td>
      </tr>
    </table>
    <div class="pagination">
      <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
        <select class="form-select" v-model.number="pageSize" @change="page=1;load()" style="padding:4px 8px;font-size:12px;width:auto">
          <option :value="5">5条/页</option><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option>
        </select>
        <span v-if="totalCount>0">第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize,totalCount) }} 个一级菜单，共 {{ totalCount }} 个</span>
        <span v-else>共 0 条</span>
      </div>
      <div style="display:flex;gap:4px">
        <button class="page-btn" :disabled="page<=1" @click="page--;load()">上一页</button>
        <button class="page-btn" v-for="n in pageNumbers" :key="n" :class="{active:n===page}" @click="typeof n==='number'&&n!==page&&(page=n,load())" v-text="n"></button>
        <button class="page-btn" :disabled="page>=totalPages" @click="page++;load()">下一页</button>
      </div>
    </div>

    <div class="modal-overlay" v-if="showForm" @click.self="showForm=false">
      <div class="modal" style="min-width:420px">
        <div class="modal-title">{{ formTitle }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'"><label class="form-label">菜单编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
          <div><label class="form-label">菜单名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          <div><label class="form-label">路由 <span v-if="form.menu_type==='level2'" style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.route" :placeholder="form.menu_type==='level2'?'例: /orders/list':'例: /workbench（目录类可为空）'" /></div>
          <div><label class="form-label">备注</label><textarea class="form-input" v-model="form.remark" placeholder="可选" rows="2" style="resize:vertical"></textarea></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showForm=false">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="saveForm">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showDelete" @click.self="showDelete=false">
      <div class="modal" style="min-width:380px;text-align:center">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--color-danger)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px">
          <path d="M3 6h18"/><path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/>
        </svg>
        <div class="modal-title" style="text-align:center">确认删除</div>
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getMenus, createMenu, updateMenu, deleteMenu } from '../api/system.js'

export default {
  emits: ['toast'],
  data: () => ({
    fCode: '', fName: '', fRoute: '', fType: '',
    list: [], total: 0, loading: false, saving: false,
    showForm: false, showDelete: false, formMode: 'add',
    form: { code: '', name: '', route: '', menu_type: 'level1', parent_id: null, remark: '', sort: 0 },
    parentObj: null, deleteTarget: null, expanded: {},
    page: 1, pageSize: 5,
  }),
  computed: {
    totalCount() { return this.total },
    totalPages() { return Math.ceil(this.total / this.pageSize) || 1 },
    pageNumbers() {
      const tp = this.totalPages, cp = this.page
      if (tp <= 7) return Array.from({ length: tp }, (_, i) => i + 1)
      const p = [1]
      if (cp > 3) p.push('...')
      for (let i = Math.max(2, cp - 1); i <= Math.min(tp - 1, cp + 1); i++) p.push(i)
      if (cp < tp - 2) p.push('...')
      p.push(tp)
      return p
    },
    flatList() {
      const out = []
      const walk = (nodes, depth) => {
        for (const n of nodes || []) {
          out.push({ ...n, depth })
          if (n.children?.length && this.expanded[n.id]) walk(n.children, depth + 1)
        }
      }
      walk(this.list, 0)
      return out
    },
    formTitle() {
      if (this.formMode === 'addChild') return '新增子菜单 — ' + (this.parentObj?.name || '')
      if (this.formMode === 'edit') return '编辑菜单'
      return '新增菜单'
    },
  },
  methods: {
    toggleExpand(m) {
      if (m.children?.length) this.expanded[m.id] = !this.expanded[m.id]
    },
    siblings(m) {
      if (!m.parent_id) return this.list
      const parent = this.list.find(x => x.id === m.parent_id)
      return parent?.children || []
    },
    canMove(m, dir) {
      const s = this.siblings(m)
      const i = s.findIndex(x => x.id === m.id)
      return i >= 0 && i + dir >= 0 && i + dir < s.length
    },
    async move(m, dir) {
      const s = this.siblings(m)
      const i = s.findIndex(x => x.id === m.id)
      const j = i + dir
      if (j < 0 || j >= s.length) return
      try {
        await updateMenu(m.id, { name: m.name, route: m.route || '', remark: m.remark || '', sort: (s[j].sort || 0) })
        await updateMenu(s[j].id, { name: s[j].name, route: s[j].route || '', remark: s[j].remark || '', sort: m.sort || 0 })
        this.$emit('toast', { msg: '排序已更新', type: 'success' })
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '排序失败', type: 'error' })
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await getMenus({
          code: this.fCode || undefined,
          name: this.fName || undefined,
          route: this.fRoute || undefined,
          menu_type: this.fType || undefined,
          page: this.page,
          pageSize: this.pageSize,
        })
        this.list = data.list || []
        this.total = data.total || 0
        for (const r of this.list) if (r.children?.length && !this.expanded[r.id]) this.expanded[r.id] = true
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载菜单失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    query() { this.page = 1; this.load() },
    resetQuery() {
      this.fCode = ''; this.fName = ''; this.fRoute = ''; this.fType = ''
      this.page = 1
      this.load()
    },
    openAdd() {
      this.formMode = 'add'
      this.parentObj = null
      this.form = { code: '', name: '', route: '', menu_type: 'level1', parent_id: null, remark: '', sort: 0 }
      this.showForm = true
    },
    openAddChild(parent) {
      this.formMode = 'addChild'
      this.parentObj = parent
      this.form = { code: '', name: '', route: '', menu_type: 'level2', parent_id: parent.id, remark: '', sort: 0 }
      this.showForm = true
    },
    openEdit(m) {
      this.formMode = 'edit'
      this.form = { id: m.id, code: m.code, name: m.name, route: m.route || '', menu_type: m.menu_type, parent_id: m.parent_id, remark: m.remark || '', sort: m.sort || 0 }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.name) { this.$emit('toast', { msg: '请输入菜单名称', type: 'warning' }); return }
      if (this.form.menu_type === 'level2' && !this.form.route) {
        this.$emit('toast', { msg: '二级菜单必须配置路由', type: 'warning' }); return
      }
      const body = {
        name: this.form.name,
        route: this.form.route || '',
        remark: this.form.remark || '',
        sort: this.form.sort || 0,
      }
      if (this.formMode !== 'edit') {
        body.parent_id = this.form.parent_id
        body.menu_type = this.form.menu_type
        if (this.form.code) body.code = this.form.code
      }
      this.saving = true
      try {
        if (this.formMode === 'edit') await updateMenu(this.form.id, body)
        else await createMenu(body)
        this.$emit('toast', { msg: this.formMode === 'edit' ? '编辑成功' : '新增成功', type: 'success' })
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    doDelete(m) { this.deleteTarget = m; this.showDelete = true },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteMenu(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除「' + this.deleteTarget.name + '」', type: 'success' })
        this.showDelete = false
        this.deleteTarget = null
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '删除失败', type: 'error' })
      }
    },
  },
  mounted() { this.load() },
}
</script>
