<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">功能编号</label><input class="form-input" v-model="fCode" placeholder="搜索" style="width:140px" /></div>
      <div class="form-group"><label class="form-label">功能名称</label><input class="form-input" v-model="fName" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">所属菜单</label><select class="form-select" v-model="fMenu"><option>全部</option><option v-for="m in menus" :key="m">{{ m }}</option></select></div>
      <div class="form-group"><label class="form-label">权限标识</label><input class="form-input" v-model="fPerm" placeholder="搜索" style="width:160px" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增功能</button>
    </div>
    <table class="data-table">
      <tr><th>功能编号</th><th>功能名称</th><th>所属菜单</th><th>权限标识</th><th>备注</th><th>操作</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="6" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="f in pagedList" :key="f.code">
        <td style="color:var(--color-text-muted);font-size:12px">{{ f.code }}</td><td>{{ f.name }}</td><td>{{ f.menu }}</td><td><code style="background:var(--color-bg);padding:2px 6px;border-radius:4px;font-size:12px">{{ f.perm }}</code></td>
                <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ f.remark }}</td>
                <td><button class="btn btn-text btn-sm" @click="openEdit(f)">编辑</button><button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(f)">删除</button></td>
      </tr>
    </table>
    <div class="pagination">
      <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
        <select class="form-select" v-model.number="pageSize" @change="page=1" style="padding:4px 8px;font-size:12px;width:auto">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <span>显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize, funcList.length) }}条，共 {{ funcList.length }} 条</span>
      </div>
      <div style="display:flex;align-items:center;gap:4px">
        <button class="page-btn" :disabled="page<=1" @click="page=Math.max(1,page-1)">上一页</button>
        <template v-for="(n,i) in pageNumbers" :key="i">
          <span v-if="n==='...'" style="padding:0 4px;color:var(--color-text-muted)">…</span>
          <button v-else class="page-btn" :class="{active:n===page}" @click="page=n">{{ n }}</button>
        </template>
        <button class="page-btn" :disabled="page>=totalPages" @click="page=Math.min(totalPages,page+1)">下一页</button>
      </div>
    </div>

    <div class="modal-overlay" v-if="showForm" @click.self="showForm=false">
      <div class="modal" style="min-width:420px">
        <div class="modal-title">{{ formMode==='add'?'新增功能':'编辑功能' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'"><label class="form-label">功能编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
          <div><label class="form-label">功能名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          <div><label class="form-label">所属菜单 <span style="color:var(--color-danger)">*</span></label><select class="form-select" v-model="form.menu"><option v-if="formMode==='add'" value="">请选择</option><option v-for="m in menus" :key="m">{{ m }}</option></select></div>
          <div><label class="form-label">权限标识 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.perm" placeholder="例: supplier:query" /></div>
          <div><label class="form-label">备注</label><textarea class="form-input" v-model="form.remark" placeholder="可选" rows="2" style="resize:vertical"></textarea></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showForm=false">取消</button>
          <button class="btn btn-primary" @click="saveForm">保存</button>
        </div>
      </div>
    </div>
    <!-- 删除确认 -->
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
export default {
  emits: ['toast'],
  data: () => ({
    fName:'', fMenu:'全部', fCode:'', fPerm:'', showForm:false, showDelete:false, formMode:'add', form:{code:'',name:'',menu:'',perm:'',remark:''},
    page: 1, pageSize: 5, deleteTarget: null,
    menus: ['订单列表','审核列表','供应商列表','报表首页','用户管理','角色管理','部门管理','机构管理','菜单管理','功能管理','操作日志'],
    funcs:[
      // 订单列表
      {code:'FUNC_ORDER_QUERY',name:'订单查询',menu:'订单列表',perm:'order:query'},
      {code:'FUNC_ORDER_CREATE',name:'订单新增',menu:'订单列表',perm:'order:create'},
      {code:'FUNC_ORDER_UPDATE',name:'订单编辑',menu:'订单列表',perm:'order:update'},
      {code:'FUNC_ORDER_DELETE',name:'订单删除',menu:'订单列表',perm:'order:delete'},
      {code:'FUNC_ORDER_EXPORT',name:'订单导出',menu:'订单列表',perm:'order:export'},
      // 审核列表
      {code:'FUNC_AUDIT_PASS',name:'审核通过',menu:'审核列表',perm:'audit:pass'},
      {code:'FUNC_AUDIT_REJECT',name:'审核驳回',menu:'审核列表',perm:'audit:reject'},
      // 供应商列表
      {code:'FUNC_SUPPLIER_QUERY',name:'供应商查询',menu:'供应商列表',perm:'supplier:query'},
      {code:'FUNC_SUPPLIER_CREATE',name:'供应商新增',menu:'供应商列表',perm:'supplier:create'},
      {code:'FUNC_SUPPLIER_UPDATE',name:'供应商编辑',menu:'供应商列表',perm:'supplier:update'},
      {code:'FUNC_SUPPLIER_DELETE',name:'供应商删除',menu:'供应商列表',perm:'supplier:delete'},
      {code:'FUNC_SUPPLIER_EXPORT',name:'供应商导出',menu:'供应商列表',perm:'supplier:export'},
      // 报表首页
      {code:'FUNC_REPORT_EXPORT',name:'报表导出',menu:'报表首页',perm:'report:export'},
      // 用户管理
      {code:'FUNC_USER_CREATE',name:'用户新增',menu:'用户管理',perm:'user:create'},
      {code:'FUNC_USER_UPDATE',name:'用户编辑',menu:'用户管理',perm:'user:update'},
      {code:'FUNC_USER_DELETE',name:'用户删除',menu:'用户管理',perm:'user:delete'},
      // 角色管理
      {code:'FUNC_ROLE_CREATE',name:'角色新增',menu:'角色管理',perm:'role:create'},
      {code:'FUNC_ROLE_UPDATE',name:'角色编辑',menu:'角色管理',perm:'role:update'},
      {code:'FUNC_ROLE_DELETE',name:'角色删除',menu:'角色管理',perm:'role:delete'},
      // 部门管理
      {code:'FUNC_DEPT_CREATE',name:'部门新增',menu:'部门管理',perm:'dept:create'},
      {code:'FUNC_DEPT_UPDATE',name:'部门编辑',menu:'部门管理',perm:'dept:update'},
      {code:'FUNC_DEPT_DELETE',name:'部门删除',menu:'部门管理',perm:'dept:delete'},
      // 机构管理
      {code:'FUNC_ORG_CREATE',name:'机构新增',menu:'机构管理',perm:'org:create'},
      {code:'FUNC_ORG_UPDATE',name:'机构编辑',menu:'机构管理',perm:'org:update'},
      {code:'FUNC_ORG_DELETE',name:'机构删除',menu:'机构管理',perm:'org:delete'},
      // 菜单管理
      {code:'FUNC_MENU_CREATE',name:'菜单新增',menu:'菜单管理',perm:'menu:create'},
      {code:'FUNC_MENU_UPDATE',name:'菜单编辑',menu:'菜单管理',perm:'menu:update'},
      {code:'FUNC_MENU_DELETE',name:'菜单删除',menu:'菜单管理',perm:'menu:delete'},
      // 功能管理
      {code:'FUNC_FUNC_CREATE',name:'功能新增',menu:'功能管理',perm:'func:create'},
      {code:'FUNC_FUNC_UPDATE',name:'功能编辑',menu:'功能管理',perm:'func:update'},
      {code:'FUNC_FUNC_DELETE',name:'功能删除',menu:'功能管理',perm:'func:delete'},
      // 操作日志
      {code:'FUNC_LOG_VIEW',name:'日志查看',menu:'操作日志',perm:'log:view'},
    ]
  }),
  computed:{
    totalPages() { return Math.ceil(this.funcList.length / this.pageSize) || 1 },
    pagedList() {
      const start = (this.page - 1) * this.pageSize
      return this.funcList.slice(start, start + this.pageSize)
    },
    funcList(){return this.funcs.filter(f=>{
      if(this.fName&&!f.name.includes(this.fName))return false
      if(this.fMenu!=='全部'&&f.menu!==this.fMenu)return false
      if(this.fCode&&!f.code.toLowerCase().includes(this.fCode.toLowerCase()))return false
      if(this.fPerm&&!f.perm.toLowerCase().includes(this.fPerm.toLowerCase()))return false
      return true
    })},
    pageNumbers() {
      const tp = this.totalPages, cp = this.page
      if (tp <= 7) return Array.from({length: tp}, (_,i) => i + 1)
      const pages = []
      pages.push(1)
      if (cp > 3) pages.push('...')
      for (let i = Math.max(2, cp - 1); i <= Math.min(tp - 1, cp + 1); i++) pages.push(i)
      if (cp < tp - 2) pages.push('...')
      pages.push(tp)
      return pages
    }
  },
  methods:{
    query(){this.page=1},
    resetQuery(){this.fName='';this.fMenu='全部';this.fCode='';this.fPerm='';this.page=1},
    openAdd(){this.formMode='add';this.form={code:'',name:'',menu:'',perm:'',remark:''};this.showForm=true},
    openEdit(f){this.formMode='edit';this.form={...f};this.showForm=true},
    saveForm(){
      if(!this.form.name||!this.form.perm||!this.form.menu){this.$emit('toast',{msg:'请填写完整信息',type:'warning'});return}
      if(!/^[^:]+:[^:]+$/.test(this.form.perm)){this.$emit('toast',{msg:'权限标识格式须为 域:操作（如 order:query）',type:'warning'});return}
      if(this.formMode==='add'){
        if(this.funcs.some(f=>f.perm===this.form.perm)){this.$emit('toast',{msg:'权限标识已存在',type:'warning'});return}
        this.funcs.push({...this.form,code:this.form.perm.replace(':','_').toUpperCase()})
        this.$emit('toast',{msg:'新增功能成功',type:'success'})
      } else {
        if(this.funcs.some(f=>f.perm===this.form.perm&&f.code!==this.form.code)){this.$emit('toast',{msg:'权限标识已存在',type:'warning'});return}
        const i=this.funcs.findIndex(x=>x.code===this.form.code)
        if(i>=0)this.funcs.splice(i,1,{...this.form})
        this.$emit('toast',{msg:'编辑成功',type:'success'})
      }
      this.showForm=false
    },
    doDelete(f){this.deleteTarget=f;this.showDelete=true},
    confirmDelete(){
      if(this.deleteTarget){
        this.funcs=this.funcs.filter(x=>x.code!==this.deleteTarget.code)
        this.$emit('toast',{msg:'已删除功能「'+this.deleteTarget.name+'」',type:'success'})
      }
      this.showDelete=false; this.deleteTarget=null
    },
  }
}
</script>
