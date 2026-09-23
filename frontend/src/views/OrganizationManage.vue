<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号</label><input class="form-input" v-model="fCode" placeholder="搜索" style="width:120px" /></div>
      <div class="form-group"><label class="form-label">名称</label><input class="form-input" v-model="fName" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">简称</label><input class="form-input" v-model="fShortName" placeholder="搜索" style="width:120px" /></div>
      <div class="form-group"><label class="form-label">层级</label>
        <select class="form-select" v-model="fLevel">
          <option value="">全部</option><option value="hq">总行</option><option value="branch1">一级分行</option><option value="branch2">二级分行</option><option value="sub1">一级支行</option><option value="sub2">二级支行</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增机构</button>
    </div>
    <table class="data-table">
      <tr><th>机构编号</th><th>机构名称</th><th>简称</th><th>层级</th><th>联系人</th><th>联系电话</th><th>所在地区</th><th>详细地址</th><th>备注</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="10" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="o in flatList" :key="o.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ o.code }}</td>
        <td>
          <span style="display:inline-flex;align-items:center;gap:4px;cursor:pointer" @click="toggleExpand(o)">
            <span v-if="o.children && o.children.length" style="font-size:10px;color:var(--color-text-muted);width:14px;text-align:center">{{ expanded[o.id] ? '▾' : '▸' }}</span>
            <span v-else style="width:14px"></span>
            <span :style="{paddingLeft: (o.depth||0)*16 + 'px', fontWeight: o.depth===0?'600':'400'}">{{ o.name }}</span>
          </span>
        </td>
        <td style="font-size:13px">{{ o.short_name || '—' }}</td>
        <td><span class="badge" :class="badgeClass(o.level)" style="font-size:11px">{{ levelLabel(o.level) }}</span></td>
        <td style="font-size:13px">{{ o.contact || '—' }}</td>
        <td style="font-size:13px">{{ o.phone || '—' }}</td>
        <td style="font-size:13px">{{ o.region || '—' }}</td>
        <td style="font-size:13px;max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ o.address || '—' }}</td>
        <td>{{ o.remark }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button v-if="nextLevel[o.level]" class="btn btn-text btn-sm" @click="openAddChild(o)">新增下级</button>
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
        <span v-if="totalCount>0">第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize,totalCount) }} 个根节点，共 {{ totalCount }} 个</span>
        <span v-else>共 0 条</span>
      </div>
      <div style="display:flex;gap:4px">
        <button class="page-btn" :disabled="page<=1" @click="page--;load()">上一页</button>
        <button class="page-btn" v-for="n in pageNumbers" :key="n" :class="{active:n===page}" @click="typeof n==='number'&&n!==page&&(page=n,load())" v-text="n"></button>
        <button class="page-btn" :disabled="page>=totalPages" @click="page++;load()">下一页</button>
      </div>
    </div>

    <div class="modal-overlay" v-if="showForm" @click.self="showForm=false">
      <div class="modal" style="min-width:560px">
        <div class="modal-title">{{ formMode==='edit' ? '编辑机构' : (form.parent_id ? '新增下级机构' : '新增机构') }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'" style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">机构编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
            <div style="flex:1"><label class="form-label">机构层级</label><input class="form-input" :value="levelLabel(form.level)" disabled style="color:var(--color-text-muted)" /></div>
          </div>
          <div v-if="formMode==='add'" style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">机构编号 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.code" placeholder="例: BJ002" /></div>
            <div style="flex:1"><label class="form-label">机构层级</label><input class="form-input" :value="form.parent_id ? levelLabel(nextLevelOfParent) : '总行'" disabled style="color:var(--color-text-muted)" /></div>
          </div>
          <div v-if="formMode==='add' && form.parent_id"><label class="form-label">上级机构</label><input class="form-input" :value="parentName" disabled style="color:var(--color-text-muted)" /></div>
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">机构名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
            <div style="flex:1"><label class="form-label">机构简称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.short_name" /></div>
          </div>
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">联系人</label><input class="form-input" v-model="form.contact" placeholder="可选" /></div>
            <div style="flex:1"><label class="form-label">联系电话</label><input class="form-input" v-model="form.phone" placeholder="可选" /></div>
          </div>
          <div><label class="form-label">所在地区 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.region" placeholder="例: 北京市朝阳区" /></div>
          <div><label class="form-label">详细地址 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.address" placeholder="例: 建国路88号" /></div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除机构「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getOrganizations, createOrganization, updateOrganization, deleteOrganization } from '../api/system.js'

const levelLabels = { hq: '总行', branch1: '一级分行', branch2: '二级分行', sub1: '一级支行', sub2: '二级支行' }
const nextLevel = { '': 'hq', hq: 'branch1', branch1: 'branch2', branch2: 'sub1', sub1: 'sub2', sub2: '' }

export default {
  emits: ['toast'],
  data: () => ({
    fCode: '', fName: '', fShortName: '', fLevel: '',
    list: [], total: 0, loading: false, saving: false,
    showForm: false, showDelete: false, formMode: 'add',
    form: { code: '', name: '', short_name: '', parent_id: null, contact: '', phone: '', region: '', address: '', remark: '', level: '' },
    parentObj: null,
    deleteTarget: null, expanded: {},
    page: 1, pageSize: 5,
    nextLevel,
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
    parentName() {
      if (!this.parentObj) return ''
      return `${this.parentObj.name} (${levelLabels[this.parentObj.level] || this.parentObj.level})`
    },
    nextLevelOfParent() {
      return this.parentObj ? nextLevel[this.parentObj.level] || '' : 'hq'
    },
  },
  methods: {
    levelLabel(l) { return levelLabels[l] || l || '' },
    badgeClass(l) { return l === 'hq' ? 'badge-primary' : l === 'branch1' ? 'badge-active' : l === 'branch2' ? 'badge-pending' : 'badge-disabled' },
    toggleExpand(o) {
      if (o.children?.length) this.expanded[o.id] = !this.expanded[o.id]
    },
    async load() {
      this.loading = true
      try {
        const data = await getOrganizations({
          code: this.fCode || undefined,
          name: this.fName || undefined,
          short_name: this.fShortName || undefined,
          level: this.fLevel || undefined,
          page: this.page,
          pageSize: this.pageSize,
        })
        this.list = data.list || []
        this.total = data.total || 0
        for (const r of this.list) if (r.children?.length && !this.expanded[r.id]) this.expanded[r.id] = true
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载机构失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    query() { this.page = 1; this.load() },
    resetQuery() {
      this.fCode = ''; this.fName = ''; this.fShortName = ''; this.fLevel = ''
      this.page = 1
      this.load()
    },
    openAdd() {
      this.formMode = 'add'
      this.parentObj = null
      this.form = { code: '', name: '', short_name: '', parent_id: null, contact: '', phone: '', region: '', address: '', remark: '', level: 'hq' }
      this.showForm = true
    },
    openAddChild(parent) {
      const next = nextLevel[parent.level]
      if (!next) { this.$emit('toast', { msg: '已达最末级，不可新增下级', type: 'warning' }); return }
      this.formMode = 'add'
      this.parentObj = parent
      this.form = { code: '', name: '', short_name: '', parent_id: parent.id, contact: '', phone: '', region: '', address: '', remark: '', level: next }
      this.showForm = true
    },
    openEdit(o) {
      this.formMode = 'edit'
      this.form = {
        id: o.id, code: o.code, name: o.name, short_name: o.short_name || '',
        parent_id: o.parent_id, contact: o.contact || '', phone: o.phone || '',
        region: o.region || '', address: o.address || '', remark: o.remark || '', level: o.level,
      }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.code || !this.form.name || !this.form.short_name || !this.form.region || !this.form.address) {
        this.$emit('toast', { msg: '请填写必填信息', type: 'warning' }); return
      }
      if (this.formMode === 'add' && !/^[A-Za-z0-9]+$/.test(this.form.code)) {
        this.$emit('toast', { msg: '机构编号只能包含字母和数字', type: 'warning' }); return
      }
      const body = {
        code: this.form.code,
        name: this.form.name,
        short_name: this.form.short_name,
        parent_id: this.form.parent_id,
        contact: this.form.contact || '',
        phone: this.form.phone || '',
        region: this.form.region,
        address: this.form.address,
        remark: this.form.remark || '',
      }
      this.saving = true
      try {
        if (this.formMode === 'add') await createOrganization(body)
        else await updateOrganization(this.form.id, { ...body, code: undefined })
        this.$emit('toast', { msg: this.formMode === 'add' ? '新增机构成功' : '编辑成功', type: 'success' })
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    doDelete(o) {
      if (o.children?.length) { this.$emit('toast', { msg: '该机构下有下级机构，无法删除', type: 'warning' }); return }
      this.deleteTarget = o
      this.showDelete = true
    },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteOrganization(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除机构「' + this.deleteTarget.name + '」', type: 'success' })
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
