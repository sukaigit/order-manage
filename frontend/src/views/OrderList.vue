<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号/名称</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <div class="form-group"><label class="form-label">供应商</label>
        <select class="form-select" v-model="fSupplierId">
          <option :value="''">全部</option>
          <option v-for="s in supplierOptions" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">状态</label>
        <select class="form-select" v-model="fStatus">
          <option value="">全部</option><option>待审核</option><option>已驳回</option><option>已完成</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">创建时间</label><div style="display:flex;align-items:center;gap:6px"><input class="form-input" type="date" v-model="fStart" style="width:145px" /><span style="color:var(--color-text-muted)">至</span><input class="form-input" type="date" v-model="fEnd" style="width:145px" /></div></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增订单</button>
      <button class="btn btn-secondary" @click="doExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>导出</button>
    </div>
    <table class="data-table">
      <tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>供应商</th><th>状态</th><th>创建时间</th><th>备注</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="8" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="o in list" :key="o.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ o.order_no }}</td>
        <td>{{ o.name }}</td>
        <td>¥{{ Number(o.amount).toFixed(2) }}</td>
        <td>{{ o.supplier_name }}</td>
        <td><span class="badge" :class="statusClass(o.status)">{{ o.status }}</span></td>
        <td>{{ o.create_time }}</td>
        <td style="font-size:12px;max-width:140px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ o.remark }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button class="btn btn-text btn-sm" @click="openEdit(o)">编辑</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(o)">删除</button>
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
        <div class="modal-title">{{ formMode==='add'?'新增订单':'编辑订单' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">订单编号 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.order_no" :disabled="formMode==='edit'" :style="formMode==='edit'?{color:'var(--color-text-muted)'}:{}" /></div>
            <div style="flex:1"><label class="form-label">订单名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          </div>
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">金额 <span style="color:var(--color-danger)">*</span></label><input class="form-input" type="number" min="0" step="0.01" v-model.number="form.amount" /></div>
            <div style="flex:1"><label class="form-label">供应商 <span style="color:var(--color-danger)">*</span></label>
              <select class="form-select" v-model="form.supplier_id">
                <option value="">请选择</option>
                <option v-for="s in supplierOptions" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
              </select>
            </div>
          </div>
          <div><label class="form-label">备注</label><textarea class="form-input" v-model="form.remark" placeholder="可选" rows="2" style="resize:vertical"></textarea></div>
          <div v-if="formMode==='edit' && form.status==='已驳回'" style="font-size:12px;color:var(--color-text-muted);background:var(--color-bg);padding:8px 12px;border-radius:8px">保存后订单状态将自动回到「待审核」</div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除订单「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getOrders, createOrder, updateOrder, deleteOrder, exportOrders } from '../api/orders.js'
import { getSupplierOptions } from '../api/suppliers.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '', fSupplierId: '', fStatus: '', fStart: '', fEnd: '',
    supplierOptions: [],
    list: [], total: 0, loading: false, saving: false,
    showForm: false, showDelete: false, formMode: 'add', deleteTarget: null,
    form: { order_no: '', name: '', amount: 0, supplier_id: '', remark: '', status: '' },
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
    statusClass(s) { return s === '已完成' ? 'badge-active' : s === '待审核' ? 'badge-pending' : '' },
    buildQuery() {
      return {
        keyword: this.fKeyword || undefined,
        supplier_id: this.fSupplierId || undefined,
        status: this.fStatus || undefined,
        start_date: this.fStart || undefined,
        end_date: this.fEnd || undefined,
        page: this.page,
        pageSize: this.pageSize,
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await getOrders(this.buildQuery())
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载订单失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    async loadOptions() {
      try {
        this.supplierOptions = await getSupplierOptions() || []
      } catch { this.supplierOptions = [] }
    },
    query() { this.page = 1; this.load() },
    resetQuery() {
      this.fKeyword = ''; this.fSupplierId = ''; this.fStatus = ''; this.fStart = ''; this.fEnd = ''
      this.page = 1
      this.load()
    },
    openAdd() {
      this.formMode = 'add'
      const d = new Date()
      const ymd = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
      this.form = { order_no: 'ORD-' + ymd, name: '', amount: 0, supplier_id: '', remark: '', status: '' }
      this.showForm = true
    },
    openEdit(o) {
      if (o.status === '已完成') { this.$emit('toast', { msg: '已完成订单不可编辑', type: 'warning' }); return }
      this.formMode = 'edit'
      this.form = {
        id: o.id, order_no: o.order_no, name: o.name, amount: Number(o.amount),
        supplier_id: String(o.supplier_id), remark: o.remark || '', status: o.status,
      }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.order_no || !this.form.name || !this.form.supplier_id || this.form.amount === '' || this.form.amount === null) {
        this.$emit('toast', { msg: '请填写必填字段（编号/名称/金额/供应商）', type: 'warning' }); return
      }
      if (Number(this.form.amount) < 0) {
        this.$emit('toast', { msg: '金额不能为负数', type: 'warning' }); return
      }
      const body = {
        order_no: this.form.order_no,
        name: this.form.name,
        amount: Number(this.form.amount),
        supplier_id: Number(this.form.supplier_id),
        remark: this.form.remark || '',
      }
      this.saving = true
      try {
        if (this.formMode === 'add') {
          await createOrder(body)
          this.$emit('toast', { msg: '新增订单成功，状态为待审核', type: 'success' })
        } else {
          const wasRejected = this.form.status === '已驳回'
          await updateOrder(this.form.id, body)
          this.$emit('toast', { msg: wasRejected ? '编辑成功，订单已回到待审核' : '编辑成功', type: 'success' })
        }
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    doDelete(o) {
      if (o.status === '已完成') { this.$emit('toast', { msg: '已完成订单不可删除', type: 'warning' }); return }
      this.deleteTarget = o
      this.showDelete = true
    },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteOrder(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除订单「' + this.deleteTarget.name + '」', type: 'success' })
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
        await exportOrders(rest)
        this.$emit('toast', { msg: '导出成功', type: 'success' })
      } catch (e) {
        this.$emit('toast', { msg: e.message || '导出失败', type: 'error' })
      }
    },
  },
  mounted() {
    this.load()
    this.loadOptions()
  },
}
</script>
