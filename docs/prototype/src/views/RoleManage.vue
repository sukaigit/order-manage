<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">角色编号</label><input class="form-input" v-model="fCode" placeholder="搜索" style="width:150px" /></div>
      <div class="form-group"><label class="form-label">角色名称</label><input class="form-input" v-model="fName" placeholder="搜索" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增角色</button>
    </div>
    <table class="data-table">
      <tr><th>角色编号</th><th>角色名称</th><th>权限数</th><th>用户数</th><th>备注</th><th>操作</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="6" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="r in pagedList" :key="r.code">
        <td style="color:var(--color-text-muted);font-size:12px">{{ r.code }}</td><td>{{ r.name }}</td><td>{{ r.permCount }}</td><td>{{ r.userCount }}</td>
                <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ r.remark }}</td>
                <td><button class="btn btn-text btn-sm" @click="openEdit(r)">编辑</button><button class="btn btn-text btn-sm" @click="openPerm(r)">分配权限</button><button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(r)">删除</button></td>
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
        <span>显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize, roleList.length) }}条，共 {{ roleList.length }} 条</span>
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
        <div class="modal-title">{{ formMode==='add'?'新增角色':'编辑角色' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'"><label class="form-label">角色编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
          <div><label class="form-label">角色名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除角色「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>
    <!-- 分配权限 -->
    <div class="modal-overlay" v-if="showPerm" @click.self="showPerm=false">
      <div class="modal" style="min-width:750px;max-height:85vh;overflow-y:auto">
        <div class="modal-title">分配权限 — {{ permRole?.name }}</div>
        <div style="display:flex;flex-direction:column;gap:16px">
          <div v-for="group in topGroups" :key="group.code" style="border:1px solid var(--color-border-light);border-radius:12px;overflow:hidden">
            <!-- 一级菜单头 -->
            <div style="display:flex;align-items:center;gap:10px;padding:10px 16px;background:var(--color-bg)" :style="{borderBottom: group.children ? '1px solid var(--color-border-light)' : 'none'}">
              <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:14px;font-weight:600;user-select:none">
                <input type="checkbox" :checked="groupMenuSelected(group)" @change="toggleGroupMenu(group)" style="accent-color:var(--color-primary)" />
                <span style="color:var(--color-text-muted);font-size:11px;font-weight:400">{{ group.code }}</span>
                {{ group.name }}
              </label>
              <span style="font-size:12px;color:var(--color-text-muted)">（{{ groupMenuCount(group) }} / {{ allGroupCount(group) }}）</span>
              <span v-if="group.children" style="font-size:10px;color:var(--color-text-muted);cursor:pointer;margin-left:auto" @click.stop="toggleSys()">{{ sysExpanded?'▾ 收起':'▸ 展开' }}</span>
            </div>
            <!-- 一级菜单的功能 -->
            <div v-if="group.funcs.length > 0" style="display:flex;flex-wrap:wrap;gap:6px;padding:10px 16px">
              <label v-for="f in group.funcs" :key="f.code" :style="{display:'flex',alignItems:'center',gap:'5px',padding:'5px 10px',border:'1px solid '+(selectedFuncs.includes(f.code)?'var(--color-primary)':'var(--color-border-light)'),borderRadius:'6px',cursor:'pointer',fontSize:'12px',userSelect:'none',background:selectedFuncs.includes(f.code)?'var(--color-primary-bg)':'transparent'}">
                <input type="checkbox" :value="f.code" v-model="selectedFuncs" style="accent-color:var(--color-primary)" />
                <span style="color:var(--color-text-muted);font-size:10px">{{ f.code }}</span>
                <span>{{ f.name }}</span>
              </label>
            </div>
            <!-- 二级子菜单（系统管理下属） -->
            <div v-if="group.children && sysExpanded" style="display:flex;flex-direction:column;gap:10px;padding:10px 16px 14px">
              <div v-for="child in group.children" :key="child.code" style="border:1px solid var(--color-border-light);border-radius:8px;overflow:hidden;margin-left:20px">
                <div style="display:flex;align-items:center;gap:8px;padding:8px 12px;background:var(--color-bg);border-bottom:1px solid var(--color-border-light)">
                  <label style="display:flex;align-items:center;gap:5px;cursor:pointer;font-size:13px;font-weight:500;user-select:none">
                    <input type="checkbox" :checked="childMenuSelected(child)" @change="toggleChildMenu(child)" style="accent-color:var(--color-primary)" />
                    <span style="color:var(--color-text-muted);font-size:10px;font-weight:400">{{ child.code }}</span>
                    {{ child.name }}
                  </label>
                  <span style="font-size:11px;color:var(--color-text-muted)">（{{ groupMenuCount(child) }} / {{ allGroupCount(child) }}）</span>
                </div>
                <div style="display:flex;flex-wrap:wrap;gap:5px;padding:8px 12px">
                  <label v-for="f in child.funcs" :key="f.code" :style="{display:'flex',alignItems:'center',gap:'4px',padding:'4px 8px',border:'1px solid '+(selectedFuncs.includes(f.code)?'var(--color-primary)':'var(--color-border-light)'),borderRadius:'5px',cursor:'pointer',fontSize:'11px',userSelect:'none',background:selectedFuncs.includes(f.code)?'var(--color-primary-bg)':'transparent'}">
                    <input type="checkbox" :value="f.code" v-model="selectedFuncs" style="accent-color:var(--color-primary)" />
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
          <button class="btn btn-primary" @click="savePerm">保存权限</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  emits: ['toast'],
  data: () => ({
    fName:'', fCode:'', showForm:false, showDelete:false, showPerm:false, formMode:'add', sysExpanded:true,
    form:{code:'',name:'',remark:''}, permRole:null, selectedFuncs:[], selectedMenus:[],
    page: 1, pageSize: 5, deleteTarget: null,
    allMenus: [
      {code:'MENU_WORKBENCH',name:'工作台',parent:''},
      {code:'MENU_ORDER',name:'订单管理',parent:''},
      {code:'MENU_ORDER_LIST',name:'订单列表',parent:'MENU_ORDER'},
      {code:'MENU_AUDIT',name:'订单审核',parent:''},
      {code:'MENU_AUDIT_LIST',name:'审核列表',parent:'MENU_AUDIT'},
      {code:'MENU_SUPPLIER',name:'供应商管理',parent:''},
      {code:'MENU_SUPPLIER_LIST',name:'供应商列表',parent:'MENU_SUPPLIER'},
      {code:'MENU_REPORT',name:'统计报表',parent:''},
      {code:'MENU_REPORT_HOME',name:'报表首页',parent:'MENU_REPORT'},
      {code:'MENU_SYSTEM',name:'系统管理',parent:''},
      {code:'MENU_USERS',name:'用户管理',parent:'MENU_SYSTEM'},
      {code:'MENU_ROLES',name:'角色管理',parent:'MENU_SYSTEM'},
      {code:'MENU_DEPTS',name:'部门管理',parent:'MENU_SYSTEM'},
      {code:'MENU_ORGS',name:'机构管理',parent:'MENU_SYSTEM'},
      {code:'MENU_MENUS',name:'菜单管理',parent:'MENU_SYSTEM'},
      {code:'MENU_FUNCS',name:'功能管理',parent:'MENU_SYSTEM'},
      {code:'MENU_LOGS',name:'操作日志',parent:'MENU_SYSTEM'},
    ],
    allFuncs: [
      {code:'FUNC_ORDER_QUERY',name:'订单查询',menu:'订单列表'},{code:'FUNC_ORDER_CREATE',name:'订单新增',menu:'订单列表'},
      {code:'FUNC_ORDER_UPDATE',name:'订单编辑',menu:'订单列表'},{code:'FUNC_ORDER_DELETE',name:'订单删除',menu:'订单列表'},
      {code:'FUNC_ORDER_EXPORT',name:'订单导出',menu:'订单列表'},
      {code:'FUNC_AUDIT_PASS',name:'审核通过',menu:'审核列表'},{code:'FUNC_AUDIT_REJECT',name:'审核驳回',menu:'审核列表'},
      {code:'FUNC_SUPPLIER_QUERY',name:'供应商查询',menu:'供应商列表'},{code:'FUNC_SUPPLIER_CREATE',name:'供应商新增',menu:'供应商列表'},
      {code:'FUNC_SUPPLIER_UPDATE',name:'供应商编辑',menu:'供应商列表'},{code:'FUNC_SUPPLIER_DELETE',name:'供应商删除',menu:'供应商列表'},
      {code:'FUNC_SUPPLIER_EXPORT',name:'供应商导出',menu:'供应商列表'},
      {code:'FUNC_REPORT_EXPORT',name:'报表导出',menu:'报表首页'},
      {code:'FUNC_USER_CREATE',name:'用户新增',menu:'用户管理'},{code:'FUNC_USER_UPDATE',name:'用户编辑',menu:'用户管理'},
      {code:'FUNC_USER_DELETE',name:'用户删除',menu:'用户管理'},
      {code:'FUNC_ROLE_CREATE',name:'角色新增',menu:'角色管理'},{code:'FUNC_ROLE_UPDATE',name:'角色编辑',menu:'角色管理'},
      {code:'FUNC_ROLE_DELETE',name:'角色删除',menu:'角色管理'},
      {code:'FUNC_DEPT_CREATE',name:'部门新增',menu:'部门管理'},{code:'FUNC_DEPT_UPDATE',name:'部门编辑',menu:'部门管理'},
      {code:'FUNC_DEPT_DELETE',name:'部门删除',menu:'部门管理'},
      {code:'FUNC_ORG_CREATE',name:'机构新增',menu:'机构管理'},{code:'FUNC_ORG_UPDATE',name:'机构编辑',menu:'机构管理'},
      {code:'FUNC_ORG_DELETE',name:'机构删除',menu:'机构管理'},
      {code:'FUNC_MENU_CREATE',name:'菜单新增',menu:'菜单管理'},{code:'FUNC_MENU_UPDATE',name:'菜单编辑',menu:'菜单管理'},
      {code:'FUNC_MENU_DELETE',name:'菜单删除',menu:'菜单管理'},
      {code:'FUNC_FUNC_CREATE',name:'功能新增',menu:'功能管理'},{code:'FUNC_FUNC_UPDATE',name:'功能编辑',menu:'功能管理'},
      {code:'FUNC_FUNC_DELETE',name:'功能删除',menu:'功能管理'},
      {code:'FUNC_LOG_VIEW',name:'日志查看',menu:'操作日志'},
    ],
    roles:[
      {code:'ROLE_ADMIN',name:'管理员',permCount:49,userCount:1,remark:'拥有全部菜单与全部功能权限'},
      {code:'ROLE_AUDITOR',name:'审核员',permCount:11,userCount:0,remark:'工作台、订单审核、订单管理（查询）、供应商管理（查询）'},
      {code:'ROLE_USER',name:'普通用户',permCount:15,userCount:0,remark:'工作台、订单管理（增删改查导出）、供应商管理（增删改查导出）；无审核、无统计报表'},
    ]
  }),
  computed:{
    totalPages() { return Math.ceil(this.roleList.length / this.pageSize) || 1 },
    pagedList() {
      const start = (this.page - 1) * this.pageSize
      return this.roleList.slice(start, start + this.pageSize)
    },
    roleList(){return this.roles.filter(r=>{if(this.fName&&!r.name.includes(this.fName))return false;if(this.fCode&&!r.code.includes(this.fCode.toUpperCase()))return false;return true})},
    topGroups(){
      const map = {}, codeMap = {}
      this.allMenus.forEach(m => { codeMap[m.name] = m.code })
      this.allFuncs.forEach(f => {
        if(!map[f.menu]) map[f.menu] = { code: codeMap[f.menu] || '', name: f.menu, funcs: [], children: null }
        map[f.menu].funcs.push(f)
      })
      this.allMenus.forEach(m => {
        if(!map[m.name]) map[m.name] = { code: m.code, name: m.name, funcs: [], children: null }
      })
      const tops = this.allMenus.filter(m => !m.parent)
      tops.forEach(top => {
        const kids = this.allMenus.filter(m => m.parent === top.code)
        if (kids.length) {
          const g = map[top.name]
          g.children = kids.map(k => map[k.name]).filter(Boolean)
          g.children.forEach(c => { delete map[c.name] })
        }
      })
      return tops.map(m => map[m.name]).filter(Boolean)
    },
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
    resetQuery(){this.fName='';this.fCode='';this.page=1},
    openAdd(){this.formMode='add';this.form={code:'',name:''};this.showForm=true},
    openEdit(r){this.formMode='edit';this.form={...r};this.showForm=true},
    saveForm(){
      if(!this.form.name){this.$emit('toast',{msg:'请输入角色名称',type:'warning'});return}
      if(!this.form.code){
        const map = { '管理员':'ROLE_ADMIN','审核员':'ROLE_AUDITOR','普通用户':'ROLE_USER' }
        this.form.code = map[this.form.name] || this.form.name.replace(/[^a-zA-Z0-9]/g,'_').toUpperCase()
      }
      if(this.formMode==='add'){this.roles.push({...this.form,permCount:0,userCount:0});this.$emit('toast',{msg:'新增角色成功',type:'success'})}
      else{const i=this.roles.findIndex(x=>x.code===this.form.code);if(i>=0)this.roles.splice(i,1,{...this.form});this.$emit('toast',{msg:'编辑成功',type:'success'})}
      this.showForm=false
    },
    doDelete(r){this.deleteTarget=r;this.showDelete=true},
    confirmDelete(){
      if(this.deleteTarget){
        this.roles=this.roles.filter(x=>x.name!==this.deleteTarget.name)
        this.$emit('toast',{msg:'已删除角色「'+this.deleteTarget.name+'」',type:'success'})
      }
      this.showDelete=false; this.deleteTarget=null
    },
    openPerm(r){
      this.permRole=r
      this.selectedFuncs = r.perms?.funcs ? [...r.perms.funcs] : []
      this.selectedMenus = r.perms?.menus ? [...r.perms.menus] : []
      this.showPerm=true
    },
    savePerm(){
      this.permRole.perms = { funcs: [...this.selectedFuncs], menus: [...this.selectedMenus] }
      this.permRole.permCount = this.selectedMenus.length + this.selectedFuncs.length
      this.$emit('toast',{msg:'已为「'+this.permRole.name+'」分配 '+this.permRole.permCount+' 项权限',type:'success'})
      this.showPerm=false
    },
    toggleSys(){ this.sysExpanded = !this.sysExpanded },
    allGroupCount(g){ return (g.funcs ? g.funcs.length : 0) + (g.children ? g.children.reduce((s,c)=>s+1+c.funcs.length,0) : 1) },
    groupMenuCount(g){
      let count = this.selectedMenus.includes(g.code) ? 1 : 0
      if (g.funcs) count += g.funcs.filter(f => this.selectedFuncs.includes(f.code)).length
      if (g.children) g.children.forEach(c => {
        if (this.selectedMenus.includes(c.code)) count += 1
        count += c.funcs.filter(f => this.selectedFuncs.includes(f.code)).length
      })
      return count
    },
    groupMenuSelected(g){ return this.selectedMenus.includes(g.code) },
    toggleGroupMenu(g){
      if (this.selectedMenus.includes(g.code)) {
        this.selectedMenus = this.selectedMenus.filter(c => c !== g.code)
        if (g.children) g.children.forEach(c => { this.deselectChild(c) })
        else if (g.funcs) g.funcs.forEach(f => { const i=this.selectedFuncs.indexOf(f.code); if(i>=0) this.selectedFuncs.splice(i,1) })
      } else {
        this.selectedMenus.push(g.code)
        if (g.children) g.children.forEach(c => { this.selectChild(c) })
        else if (g.funcs) g.funcs.forEach(f => { if(!this.selectedFuncs.includes(f.code)) this.selectedFuncs.push(f.code) })
      }
    },
    childMenuSelected(c){ return this.selectedMenus.includes(c.code) },
    toggleChildMenu(c){
      if (this.selectedMenus.includes(c.code)) {
        this.selectedMenus = this.selectedMenus.filter(x => x !== c.code)
        c.funcs.forEach(f => { const i=this.selectedFuncs.indexOf(f.code); if(i>=0) this.selectedFuncs.splice(i,1) })
      } else {
        this.selectedMenus.push(c.code)
        c.funcs.forEach(f => { if(!this.selectedFuncs.includes(f.code)) this.selectedFuncs.push(f.code) })
      }
    },
    selectChild(c){
      if(!this.selectedMenus.includes(c.code)) this.selectedMenus.push(c.code)
      c.funcs.forEach(f => { if(!this.selectedFuncs.includes(f.code)) this.selectedFuncs.push(f.code) })
    },
    deselectChild(c){
      this.selectedMenus = this.selectedMenus.filter(x => x !== c.code)
      c.funcs.forEach(f => { const i=this.selectedFuncs.indexOf(f.code); if(i>=0) this.selectedFuncs.splice(i,1) })
    },
  }
}
</script>
