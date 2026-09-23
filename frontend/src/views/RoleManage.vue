<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">角色编号</label><input class="form-input" v-model="fKeyword" placeholder="搜索" style="width:150px" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增角色</button>
    </div>
    <table class="data-table">
      <tr><th>角色编号</th><th>角色名称</th><th>菜单数</th><th>功能数</th><th>用户数</th><th>备注</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="7" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="r in list" :key="r.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ r.code }}</td>
        <td>{{ r.name }}</td>
        <td>{{ r.menu_count }}</td>
        <td>{{ r.function_count }}</td>
        <td>{{ r.user_count }}</td>
        <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ r.remark }}</td>
        <td>
          <button class="btn btn-text btn-sm" @click="openEdit(r)">编辑</button>
          <button class="btn btn-text btn-sm" @click="openPerm(r)">分配权限</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(r)">删除</button>
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
        <div class="modal-title">{{ formMode==='add'?'新增角色':'编辑角色' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'"><label class="form-label">角色编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
          <div><label class="form-label">角色名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          <div><label class="form-label">角色编号 <span v-if="formMode==='add'" style="color:var(--color-text-muted);font-weight:400">（可选，缺省自动生成）</span></label><input class="form-input" v-model="form.code" :disabled="formMode==='edit'" placeholder="例: ROLE_FINANCE" /></div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除角色「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showPerm" @click.self="showPerm=false">
      <div class="modal" style="min-width:750px;max-height:85vh;overflow-y:auto">
        <div class="modal-title">分配权限 — {{ permRole?.name }}</div>
        <div v-if="permLoading" style="text-align:center;padding:24px;color:var(--color-text-muted)">加载中…</div>
        <div v-else style="display:flex;flex-direction:column;gap:16px">
          <div v-for="group in topGroups" :key="group.id" style="border:1px solid var(--color-border-light);border-radius:12px;overflow:hidden">
            <div style="display:flex;align-items:center;gap:10px;padding:10px 16px;background:var(--color-bg)" :style="{borderBottom: group.children && group.children.length ? '1px solid var(--color-border-light)' : 'none'}">
              <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:14px;font-weight:600;user-select:none">
                <input type="checkbox" :checked="selectedMenuIds.includes(group.id)" @change="toggleMenu(group)" style="accent-color:var(--color-primary)" />
                <span style="color:var(--color-text-muted);font-size:11px;font-weight:400">{{ group.code }}</span>
                {{ group.name }}
              </label>
              <span v-if="group.functions && group.functions.length" style="font-size:12px;color:var(--color-text-muted)">（{{ groupFuncCount(group) }} / {{ groupFuncTotal(group) }}）</span>
              <span v-if="group.children && group.children.length" style="font-size:10px;color:var(--color-text-muted);cursor:pointer;margin-left:auto" @click.stop="sysExpanded=!sysExpanded">{{ sysExpanded?'▾ 收起':'▸ 展开' }}</span>
            </div>
            <div v-if="group.functions && group.functions.length" style="display:flex;flex-wrap:wrap;gap:6px;padding:10px 16px">
              <label v-for="f in group.functions" :key="f.id" :style="{display:'flex',alignItems:'center',gap:'5px',padding:'5px 10px',border:'1px solid '+(selectedFuncIds.includes(f.id)?'var(--color-primary)':'var(--color-border-light)'),borderRadius:'6px',cursor:'pointer',fontSize:'12px',userSelect:'none',background:selectedFuncIds.includes(f.id)?'var(--color-primary-bg)':'transparent'}">
                <input type="checkbox" :checked="selectedFuncIds.includes(f.id)" @change="toggleFunc(f)" style="accent-color:var(--color-primary)" />
                <span style="color:var(--color-text-muted);font-size:10px">{{ f.code }}</span>
                <span>{{ f.name }}</span>
              </label>
            </div>
            <div v-if="group.children && group.children.length && sysExpanded" style="display:flex;flex-direction:column;gap:10px;padding:10px 16px 14px">
              <div v-for="child in group.children" :key="child.id" style="border:1px solid var(--color-border-light);border-radius:8px;overflow:hidden;margin-left:20px">
                <div style="display:flex;align-items:center;gap:8px;padding:8px 12px;background:var(--color-bg);border-bottom:1px solid var(--color-border-light)">
                  <label style="display:flex;align-items:center;gap:5px;cursor:pointer;font-size:13px;font-weight:500;user-select:none">
                    <input type="checkbox" :checked="selectedMenuIds.includes(child.id)" @change="toggleMenu(child)" style="accent-color:var(--color-primary)" />
                    <span style="color:var(--color-text-muted);font-size:10px;font-weight:400">{{ child.code }}</span>
                    {{ child.name }}
                  </label>
                  <span v-if="child.functions && child.functions.length" style="font-size:11px;color:var(--color-text-muted)">（{{ groupFuncCount(child) }} / {{ groupFuncTotal(child) }}）</span>
                </div>
                <div v-if="child.functions && child.functions.length" style="display:flex;flex-wrap:wrap;gap:5px;padding:8px 12px">
                  <label v-for="f in child.functions" :key="f.id" :style="{display:'flex',alignItems:'center',gap:'4px',padding:'4px 8px',border:'1px solid '+(selectedFuncIds.includes(f.id)?'var(--color-primary)':'var(--color-border-light)'),borderRadius:'5px',cursor:'pointer',fontSize:'11px',userSelect:'none',background:selectedFuncIds.includes(f.id)?'var(--color-primary-bg)':'transparent'}">
                    <input type="checkbox" :checked="selectedFuncIds.includes(f.id)" @change="toggleFunc(f)" style="accent-color:var(--color-primary)" />
                    <span style="color:var(--color-text-muted);font-size:9px">{{ f.code }}</span>
                    <span>{{ f.name }}</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showPerm=false">取消</button>
          <button class="btn btn-primary" :disabled="permSaving" @click="savePerm">{{ permSaving ? '保存中…' : '保存权限' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getRoles, createRole, updateRole, deleteRole, getRolePermissions, updateRolePermissions, getMenuTree } from '../api/system.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '',
    showForm: false, showDelete: false, showPerm: false, formMode: 'add', sysExpanded: true,
    form: { code: '', name: '', remark: '' },
    permRole: null, selectedMenuIds: [], selectedFuncIds: [],
    permLoading: false, permSaving: false, saving: false,
    list: [], total: 0, loading: false, deleteTarget: null,
    menuTree: [],
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
    topGroups() {
      return this.menuTree.filter(m => !m.parent_id || m.menu_type === 'level1')
        .map(m => ({ ...m, children: (m.children || []).filter(c => c) }))
    },
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const data = await getRoles({ keyword: this.fKeyword || undefined, page: this.page, pageSize: this.pageSize })
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载角色失败', type: 'error' })
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
    openEdit(r) {
      this.formMode = 'edit'
      this.form = { id: r.id, code: r.code, name: r.name, remark: r.remark || '' }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.name) { this.$emit('toast', { msg: '请输入角色名称', type: 'warning' }); return }
      const body = { name: this.form.name, remark: this.form.remark || '' }
      if (this.formMode === 'add' && this.form.code) body.code = this.form.code
      this.saving = true
      try {
        if (this.formMode === 'add') await createRole(body)
        else await updateRole(this.form.id, body)
        this.$emit('toast', { msg: this.formMode === 'add' ? '新增角色成功' : '编辑成功', type: 'success' })
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    doDelete(r) { this.deleteTarget = r; this.showDelete = true },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteRole(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除角色「' + this.deleteTarget.name + '」', type: 'success' })
        this.showDelete = false
        this.deleteTarget = null
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '删除失败', type: 'error' })
      }
    },
    async openPerm(r) {
      this.permRole = r
      this.showPerm = true
      this.permLoading = true
      this.selectedMenuIds = []
      this.selectedFuncIds = []
      try {
        if (!this.menuTree.length) this.menuTree = await getMenuTree() || []
        const perm = await getRolePermissions(r.id)
        this.selectedMenuIds = perm.menu_ids || []
        this.selectedFuncIds = perm.function_ids || []
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载权限失败', type: 'error' })
      } finally {
        this.permLoading = false
      }
    },
    collectMenuIds(node, out = []) {
      out.push(node.id)
      for (const c of node.children || []) this.collectMenuIds(c, out)
      return out
    },
    collectFuncIds(node, out = []) {
      for (const f of node.functions || []) out.push(f.id)
      for (const c of node.children || []) this.collectFuncIds(c, out)
      return out
    },
    toggleMenu(node) {
      const ids = this.collectMenuIds(node, [])
      const fids = this.collectFuncIds(node, [])
      const allSelected = ids.every(id => this.selectedMenuIds.includes(id))
      if (allSelected) {
        this.selectedMenuIds = this.selectedMenuIds.filter(id => !ids.includes(id))
        this.selectedFuncIds = this.selectedFuncIds.filter(id => !fids.includes(id))
      } else {
        this.selectedMenuIds = [...new Set([...this.selectedMenuIds, ...ids])]
        this.selectedFuncIds = [...new Set([...this.selectedFuncIds, ...fids])]
      }
    },
    toggleFunc(f) {
      if (this.selectedFuncIds.includes(f.id)) {
        this.selectedFuncIds = this.selectedFuncIds.filter(id => id !== f.id)
      } else {
        this.selectedFuncIds = [...this.selectedFuncIds, f.id]
      }
    },
    groupFuncCount(g) {
      const funcs = g.functions || []
      let n = funcs.filter(f => this.selectedFuncIds.includes(f.id)).length
      for (const c of g.children || []) n += this.groupFuncCount(c)
      if (this.selectedMenuIds.includes(g.id)) n += 0
      return n
    },
    groupFuncTotal(g) {
      let n = (g.functions || []).length
      for (const c of g.children || []) n += this.groupFuncTotal(c)
      return n
    },
    async savePerm() {
      this.permSaving = true
      try {
        await updateRolePermissions(this.permRole.id, {
          menu_ids: this.selectedMenuIds,
          function_ids: this.selectedFuncIds,
        })
        this.$emit('toast', { msg: '已为「' + this.permRole.name + '」分配 ' + (this.selectedMenuIds.length + this.selectedFuncIds.length) + ' 项权限', type: 'success' })
        this.showPerm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存权限失败', type: 'error' })
      } finally {
        this.permSaving = false
      }
    },
  },
  mounted() { this.load() },
}
</script>
