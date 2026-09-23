<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号/名称</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <div class="form-group"><label class="form-label">所属菜单</label>
        <select class="form-select" v-model="fMenuId">
          <option :value="''">全部</option>
          <option v-for="m in menuOptions" :key="m.id" :value="String(m.id)">{{ m.name }}</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">权限标识</label><input class="form-input" v-model="fPerm" placeholder="搜索" style="width:160px" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增功能</button>
    </div>
    <table class="data-table">
      <tr><th>功能编号</th><th>功能名称</th><th>所属菜单</th><th>权限标识</th><th>备注</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="6" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="f in list" :key="f.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ f.code }}</td>
        <td>{{ f.name }}</td>
        <td>{{ f.menu_name }}</td>
        <td><code style="background:var(--color-bg);padding:2px 6px;border-radius:4px;font-size:12px">{{ f.perm }}</code></td>
        <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ f.remark }}</td>
        <td>
          <button class="btn btn-text btn-sm" @click="openEdit(f)">编辑</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(f)">删除</button>
        </td>
      </tr>
    </table>
    <div class="pagination">
      <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
        <select class="form-select" v-model.number="pageSize" @change="page=1;load()" style="padding:4px 8px;font-size:12px;width:auto">
          <option :value="5">5条/页</option><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option>
        </select>
        <span v-if="totalCount>0">显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize,totalCount) }} 条，共 {{ totalCount }} 条</span>
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
        <div class="modal-title">{{ formMode==='add'?'新增功能':'编辑功能' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'"><label class="form-label">功能编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
          <div><label class="form-label">功能名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          <div><label class="form-label">所属菜单 <span style="color:var(--color-danger)">*</span></label>
            <select class="form-select" v-model="form.menu_id"><option value="">请选择</option><option v-for="m in menuOptions" :key="m.id" :value="String(m.id)">{{ m.name }}</option></select>
          </div>
          <div><label class="form-label">权限标识 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.perm" placeholder="例: order:print" /></div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除功能「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getFunctions, createFunction, updateFunction, deleteFunction, getMenuTree } from '../api/system.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '', fMenuId: '', fPerm: '',
    list: [], total: 0, loading: false, saving: false,
    showForm: false, showDelete: false, formMode: 'add',
    form: { name: '', menu_id: '', perm: '', remark: '' },
    menuOptions: [], deleteTarget: null,
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
  },
  methods: {
    flattenMenus(nodes, out = []) {
      for (const n of nodes || []) {
        out.push(n)
        if (n.children?.length) this.flattenMenus(n.children, out)
      }
      return out
    },
    async load() {
      this.loading = true
      try {
        const data = await getFunctions({
          keyword: this.fKeyword || undefined,
          menu_id: this.fMenuId || undefined,
          perm: this.fPerm || undefined,
          page: this.page,
          pageSize: this.pageSize,
        })
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载功能失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    async loadMenus() {
      try {
        const tree = await getMenuTree()
        this.menuOptions = this.flattenMenus(tree, [])
      } catch { this.menuOptions = [] }
    },
    query() { this.page = 1; this.load() },
    resetQuery() { this.fKeyword = ''; this.fMenuId = ''; this.fPerm = ''; this.page = 1; this.load() },
    openAdd() {
      this.formMode = 'add'
      this.form = { name: '', menu_id: '', perm: '', remark: '' }
      this.showForm = true
    },
    openEdit(f) {
      this.formMode = 'edit'
      this.form = { id: f.id, code: f.code, name: f.name, menu_id: String(f.menu_id), perm: f.perm, remark: f.remark || '' }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.name || !this.form.menu_id || !this.form.perm) {
        this.$emit('toast', { msg: '请填写完整信息', type: 'warning' }); return
      }
      if (!/^[^:]+:[^:]+$/.test(this.form.perm)) {
        this.$emit('toast', { msg: '权限标识格式须为 域:操作（如 order:query）', type: 'warning' }); return
      }
      const body = {
        name: this.form.name,
        menu_id: Number(this.form.menu_id),
        perm: this.form.perm,
        remark: this.form.remark || '',
      }
      this.saving = true
      try {
        if (this.formMode === 'add') await createFunction(body)
        else await updateFunction(this.form.id, body)
        this.$emit('toast', { msg: this.formMode === 'add' ? '新增功能成功' : '编辑成功', type: 'success' })
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    doDelete(f) { this.deleteTarget = f; this.showDelete = true },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteFunction(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除功能「' + this.deleteTarget.name + '」', type: 'success' })
        this.showDelete = false
        this.deleteTarget = null
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '删除失败', type: 'error' })
      }
    },
  },
  mounted() {
    this.load()
    this.loadMenus()
  },
}
</script>
