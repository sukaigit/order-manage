<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号/名称</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增部门</button>
    </div>
    <table class="data-table">
      <tr><th>部门编号</th><th>部门名称</th><th>备注</th><th>创建时间</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="5" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="d in list" :key="d.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ d.code }}</td>
        <td>{{ d.name }}</td>
        <td>{{ d.remark }}</td>
        <td>{{ d.create_time }}</td>
        <td>
          <button class="btn btn-text btn-sm" @click="openEdit(d)">编辑</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(d)">删除</button>
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
        <div class="modal-title">{{ formMode==='add'?'新增部门':'编辑部门' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div><label class="form-label">部门编号 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.code" placeholder="例: DEPT-001" /></div>
          <div><label class="form-label">部门名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除部门「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getDepartments, createDepartment, updateDepartment, deleteDepartment } from '../api/system.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '',
    list: [], total: 0, loading: false, saving: false,
    showForm: false, showDelete: false, formMode: 'add',
    form: { code: '', name: '', remark: '' },
    deleteTarget: null,
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
    async load() {
      this.loading = true
      try {
        const data = await getDepartments({ keyword: this.fKeyword || undefined, page: this.page, pageSize: this.pageSize })
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载部门失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    query() { this.page = 1; this.load() },
    resetQuery() { this.fKeyword = ''; this.page = 1; this.load() },
    openAdd() {
      this.formMode = 'add'
      this.form = { code: '', name: '', remark: '' }
      this.showForm = true
    },
    openEdit(d) {
      this.formMode = 'edit'
      this.form = { id: d.id, code: d.code, name: d.name, remark: d.remark || '' }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.code || !this.form.name) {
        this.$emit('toast', { msg: '请填写部门编号和名称', type: 'warning' }); return
      }
      const body = { code: this.form.code, name: this.form.name, remark: this.form.remark || '' }
      this.saving = true
      try {
        if (this.formMode === 'add') await createDepartment(body)
        else await updateDepartment(this.form.id, body)
        this.$emit('toast', { msg: this.formMode === 'add' ? '新增部门成功' : '编辑成功', type: 'success' })
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    doDelete(d) { this.deleteTarget = d; this.showDelete = true },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteDepartment(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除部门「' + this.deleteTarget.name + '」', type: 'success' })
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
