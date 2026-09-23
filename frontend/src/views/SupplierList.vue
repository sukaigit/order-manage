<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">名称/联系人</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <div class="form-group"><label class="form-label">状态</label>
        <select class="form-select" v-model="fStatus">
          <option value="">全部</option><option>启用</option><option>停用</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增供应商</button>
      <button class="btn btn-secondary" @click="doExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>导出</button>
    </div>
    <table class="data-table">
      <tr><th>供应商编号</th><th>名称</th><th>联系人</th><th>联系电话</th><th>地址</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="8" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="s in list" :key="s.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ s.code }}</td>
        <td>{{ s.name }}</td>
        <td>{{ s.contact }}</td>
        <td>{{ s.phone }}</td>
        <td style="font-size:13px">{{ s.address }}</td>
        <td><span class="badge" :class="s.status==='启用'?'badge-active':'badge-disabled'">{{ s.status }}</span></td>
        <td>{{ s.create_time }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button class="btn btn-text btn-sm" @click="openEdit(s)">编辑</button>
          <button class="btn btn-text btn-sm" :style="{color:s.status==='启用'?'var(--color-danger)':'var(--color-primary)'}" @click="toggleStatus(s)">{{ s.status==='启用'?'停用':'启用' }}</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(s)">删除</button>
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
      <div class="modal" style="min-width:520px">
        <div class="modal-title">{{ formMode==='add'?'新增供应商':'编辑供应商' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">供应商编号 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.code" :disabled="formMode==='edit'" :style="formMode==='edit'?{color:'var(--color-text-muted)'}:{}" placeholder="例: SUP-004" /></div>
            <div style="flex:1"><label class="form-label">名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          </div>
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">联系人 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.contact" /></div>
            <div style="flex:1"><label class="form-label">联系电话 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.phone" /></div>
          </div>
          <div><label class="form-label">地址</label><input class="form-input" v-model="form.address" placeholder="选填" /></div>
          <div v-if="formMode==='edit'">
            <label class="form-label">状态</label>
            <div style="display:flex;border:1px solid var(--color-border);border-radius:8px;overflow:hidden;width:fit-content">
              <div :style="{padding:'8px 20px',cursor:'pointer',fontSize:'13px',background:form.status==='启用'?'var(--color-primary)':'transparent',color:form.status==='启用'?'white':'var(--color-text)',transition:'all 0.15s'}" @click="form.status='启用'">启用</div>
              <div :style="{padding:'8px 20px',cursor:'pointer',fontSize:'13px',background:form.status==='停用'?'var(--color-danger)':'transparent',color:form.status==='停用'?'white':'var(--color-text)',transition:'all 0.15s',borderLeft:'1px solid var(--color-border)'}" @click="form.status='停用'">停用</div>
            </div>
          </div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除供应商「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier, updateSupplierStatus, exportSuppliers } from '../api/suppliers.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '', fStatus: '',
    list: [], total: 0, loading: false, saving: false,
    showForm: false, showDelete: false, formMode: 'add', deleteTarget: null,
    form: { code: '', name: '', contact: '', phone: '', address: '', status: '启用', remark: '' },
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
    buildQuery() {
      return {
        keyword: this.fKeyword || undefined,
        status: this.fStatus || undefined,
        page: this.page,
        pageSize: this.pageSize,
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await getSuppliers(this.buildQuery())
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载供应商失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    query() { this.page = 1; this.load() },
    resetQuery() { this.fKeyword = ''; this.fStatus = ''; this.page = 1; this.load() },
    openAdd() {
      this.formMode = 'add'
      this.form = { code: '', name: '', contact: '', phone: '', address: '', status: '启用', remark: '' }
      this.showForm = true
    },
    openEdit(s) {
      this.formMode = 'edit'
      this.form = { id: s.id, code: s.code, name: s.name, contact: s.contact, phone: s.phone, address: s.address || '', status: s.status, remark: s.remark || '' }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.code || !this.form.name || !this.form.contact || !this.form.phone) {
        this.$emit('toast', { msg: '请填写必填字段（编号/名称/联系人/电话）', type: 'warning' }); return
      }
      const body = {
        code: this.form.code,
        name: this.form.name,
        contact: this.form.contact,
        phone: this.form.phone,
        address: this.form.address || '',
        status: this.form.status,
        remark: this.form.remark || '',
      }
      this.saving = true
      try {
        if (this.formMode === 'add') {
          await createSupplier(body)
          this.$emit('toast', { msg: '新增供应商成功，状态为启用', type: 'success' })
        } else {
          await updateSupplier(this.form.id, body)
          this.$emit('toast', { msg: '编辑成功', type: 'success' })
        }
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    async toggleStatus(s) {
      const next = s.status === '启用' ? '停用' : '启用'
      try {
        await updateSupplierStatus(s.id, next)
        this.$emit('toast', { msg: '「' + s.name + '」已' + next, type: 'success' })
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '操作失败', type: 'error' })
      }
    },
    doDelete(s) {
      this.deleteTarget = s
      this.showDelete = true
    },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteSupplier(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除供应商「' + this.deleteTarget.name + '」', type: 'success' })
        this.showDelete = false
        this.deleteTarget = null
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '删除失败', type: 'error' })
      }
    },
    async doExport() {
      try {
        const { page, pageSize, ...rest } = this.buildQuery()
        await exportSuppliers(rest)
        this.$emit('toast', { msg: '导出成功', type: 'success' })
      } catch (e) {
        this.$emit('toast', { msg: e.message || '导出失败', type: 'error' })
      }
    },
  },
  mounted() { this.load() },
}
</script>
