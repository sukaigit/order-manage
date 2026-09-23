<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">用户名</label><input class="form-input" v-model="fKeyword" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">角色</label>
        <select class="form-select" v-model="fRoleId">
          <option :value="''">全部</option>
          <option v-for="r in roleOptions" :key="r.id" :value="String(r.id)">{{ r.name }}</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">部门</label>
        <select class="form-select" v-model="fDeptId">
          <option :value="''">全部</option>
          <option v-for="d in deptOptions" :key="d.id" :value="String(d.id)">{{ d.name }}</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">机构</label>
        <select class="form-select" v-model="fOrgId">
          <option :value="''">全部</option>
          <option v-for="o in orgOptions" :key="o.id" :value="String(o.id)">{{ o.name }}</option>
        </select>
      </div>
      <div class="form-group"><label class="form-label">状态</label>
        <select class="form-select" v-model="fStatus">
          <option value="">全部</option><option>启用</option><option>停用</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增用户</button>
    </div>
    <table class="data-table">
      <tr><th>用户名</th><th>姓名</th><th>角色</th><th>部门</th><th>机构</th><th>状态</th><th>创建时间</th><th>备注</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="9" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="u in list" :key="u.id">
        <td>{{ u.username }}</td>
        <td>{{ u.real_name }}</td>
        <td>{{ u.role_name }}</td>
        <td style="font-size:13px">{{ u.department_name }}</td>
        <td style="font-size:13px">{{ u.organization_name }}</td>
        <td>
          <span v-if="u.locked" class="badge badge-disabled" style="background:#fef2f2;color:#dc2626;border-color:#fecaca">锁定</span>
          <span v-else class="badge" :class="u.status==='启用'?'badge-active':'badge-disabled'">{{ u.status }}</span>
        </td>
        <td>{{ u.create_time }}</td>
        <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ u.remark }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button class="btn btn-text btn-sm" @click="openEdit(u)">编辑</button>
          <button class="btn btn-text btn-sm" :style="{color:u.status==='启用'?'var(--color-danger)':'var(--color-primary)'}" @click="toggleStatus(u)">{{ u.status==='启用'?'禁用':'启用' }}</button>
          <button class="btn btn-text btn-sm" @click="openResetPwd(u)">重置密码</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(u)">删除</button>
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
        <div class="modal-title">{{ formMode==='add'?'新增用户':'编辑用户' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div><label class="form-label">用户名 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.username" /></div>
          <div><label class="form-label">姓名 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.real_name" /></div>
          <div><label class="form-label">角色 <span style="color:var(--color-danger)">*</span></label>
            <select class="form-select" v-model="form.role_id"><option value="">请选择</option><option v-for="r in roleOptions" :key="r.id" :value="String(r.id)">{{ r.name }}</option></select>
          </div>
          <div><label class="form-label">部门 <span style="color:var(--color-danger)">*</span></label>
            <select class="form-select" v-model="form.department_id"><option value="">请选择</option><option v-for="d in deptOptions" :key="d.id" :value="String(d.id)">{{ d.name }}</option></select>
          </div>
          <div><label class="form-label">机构 <span style="color:var(--color-danger)">*</span></label>
            <select class="form-select" v-model="form.organization_id"><option value="">请选择</option><option v-for="o in orgOptions" :key="o.id" :value="String(o.id)">{{ o.name }}</option></select>
          </div>
          <div>
            <label class="form-label">状态</label>
            <div style="display:flex;gap:0;border:1px solid var(--color-border);border-radius:8px;overflow:hidden;width:fit-content">
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

    <div class="modal-overlay" v-if="showResetPwd" @click.self="showResetPwd=false">
      <div class="modal" style="min-width:380px;text-align:center">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <div class="modal-title" style="text-align:center">重置密码</div>
        <template v-if="resetPassword">
          <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:8px">用户「{{ resetPwdTarget?.username }}」的密码已重置为：</div>
          <div style="font-size:18px;font-weight:700;color:var(--color-primary);margin-bottom:8px;background:var(--color-bg);padding:10px;border-radius:8px;font-family:monospace">{{ resetPassword.default_password }}</div>
          <div style="font-size:12px;color:var(--color-text-muted);margin-bottom:24px">请立即复制告知用户，该密码仅显示一次；用户下次登录将强制修改密码</div>
          <div class="modal-footer" style="justify-content:center">
            <button class="btn btn-secondary" @click="closeResetPwd">关闭</button>
          </div>
        </template>
        <template v-else>
          <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">将重置用户「{{ resetPwdTarget?.username }}」的密码为默认初始密码？</div>
          <div class="modal-footer" style="justify-content:center">
            <button class="btn btn-secondary" @click="closeResetPwd">取消</button>
            <button class="btn btn-primary" :disabled="resetting" @click="confirmResetPwd">{{ resetting ? '重置中…' : '确认重置' }}</button>
          </div>
        </template>
      </div>
    </div>

    <div class="modal-overlay" v-if="showDelete" @click.self="showDelete=false">
      <div class="modal" style="min-width:380px;text-align:center">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--color-danger)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px">
          <path d="M3 6h18"/><path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/>
        </svg>
        <div class="modal-title" style="text-align:center">确认删除</div>
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除用户「{{ deleteTarget?.username }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getUsers, createUser, updateUser, updateUserStatus, resetUserPassword, deleteUser } from '../api/system.js'
import { getRoles, getDepartments, getOrganizations } from '../api/system.js'
import { getOrganizations as getOrgTree } from '../api/system.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '', fRoleId: '', fDeptId: '', fOrgId: '', fStatus: '',
    list: [], total: 0, loading: false, saving: false, resetting: false,
    showForm: false, showResetPwd: false, showDelete: false, formMode: 'add',
    form: { username: '', real_name: '', role_id: '', department_id: '', organization_id: '', status: '启用', remark: '' },
    resetPwdTarget: null, resetPassword: null, deleteTarget: null,
    roleOptions: [], deptOptions: [], orgOptions: [],
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
        role_id: this.fRoleId || undefined,
        department_id: this.fDeptId || undefined,
        organization_id: this.fOrgId || undefined,
        status: this.fStatus || undefined,
        page: this.page,
        pageSize: this.pageSize,
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await getUsers(this.buildQuery())
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载用户失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    async loadOptions() {
      try {
        const [roles, depts, orgs] = await Promise.all([
          getRoles({ page: 1, pageSize: 100 }),
          getDepartments({ page: 1, pageSize: 100 }),
          getOrgTree({ page: 1, pageSize: 100 }),
        ])
        this.roleOptions = roles.list || []
        this.deptOptions = depts.list || []
        this.orgOptions = this.flattenOrgs(orgs.list || [])
      } catch { /* 非管理员打开页面时选项为空即可 */ }
    },
    flattenOrgs(nodes, depth = 0) {
      const out = []
      for (const n of nodes || []) {
        out.push({ ...n, depth })
        if (n.children?.length) out.push(...this.flattenOrgs(n.children, depth + 1))
      }
      return out
    },
    query() { this.page = 1; this.load() },
    resetQuery() {
      this.fKeyword = ''; this.fRoleId = ''; this.fDeptId = ''; this.fOrgId = ''; this.fStatus = ''
      this.page = 1
      this.load()
    },
    openAdd() {
      this.formMode = 'add'
      this.form = { username: '', real_name: '', role_id: '', department_id: '', organization_id: '', status: '启用', remark: '' }
      this.showForm = true
    },
    openEdit(u) {
      this.formMode = 'edit'
      this.form = {
        id: u.id, username: u.username, real_name: u.real_name,
        role_id: String(u.role_id), department_id: String(u.department_id),
        organization_id: String(u.organization_id), status: u.status, remark: u.remark || '',
      }
      this.showForm = true
    },
    async saveForm() {
      if (!this.form.username || !this.form.real_name || !this.form.role_id || !this.form.department_id || !this.form.organization_id) {
        this.$emit('toast', { msg: '请填写完整信息', type: 'warning' }); return
      }
      const body = {
        username: this.form.username,
        real_name: this.form.real_name,
        role_id: Number(this.form.role_id),
        department_id: Number(this.form.department_id),
        organization_id: Number(this.form.organization_id),
        status: this.form.status,
        remark: this.form.remark || '',
      }
      this.saving = true
      try {
        if (this.formMode === 'add') await createUser(body)
        else await updateUser(this.form.id, body)
        this.$emit('toast', { msg: this.formMode === 'add' ? '新增用户成功' : '编辑成功', type: 'success' })
        this.showForm = false
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '保存失败', type: 'error' })
      } finally {
        this.saving = false
      }
    },
    async toggleStatus(u) {
      const next = u.status === '启用' ? '停用' : '启用'
      try {
        await updateUserStatus(u.id, next)
        this.$emit('toast', { msg: next === '启用' ? '已启用' : '已停用', type: 'success' })
        this.load()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '操作失败', type: 'error' })
      }
    },
    openResetPwd(u) {
      this.resetPwdTarget = u
      this.resetPassword = null
      this.showResetPwd = true
    },
    closeResetPwd() {
      this.showResetPwd = false
      this.resetPwdTarget = null
      this.resetPassword = null
    },
    async confirmResetPwd() {
      this.resetting = true
      try {
        this.resetPassword = await resetUserPassword(this.resetPwdTarget.id)
        this.$emit('toast', { msg: '密码已重置为 ' + this.resetPassword.default_password, type: 'success' })
      } catch (e) {
        this.$emit('toast', { msg: e.message || '重置失败', type: 'error' })
        this.closeResetPwd()
      } finally {
        this.resetting = false
      }
    },
    doDelete(u) {
      this.deleteTarget = u
      this.showDelete = true
    },
    async confirmDelete() {
      if (!this.deleteTarget) { this.showDelete = false; return }
      try {
        await deleteUser(this.deleteTarget.id)
        this.$emit('toast', { msg: '已删除用户「' + this.deleteTarget.username + '」', type: 'success' })
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
    this.loadOptions()
  },
}
</script>
