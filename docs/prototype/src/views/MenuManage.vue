<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">菜单编号</label><input class="form-input" v-model="fCode" placeholder="搜索" style="width:140px" /></div>
      <div class="form-group"><label class="form-label">菜单名称</label><input class="form-input" v-model="fName" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">级别</label><select class="form-select" v-model="fType"><option>全部</option><option>一级菜单</option><option>二级菜单</option></select></div>
      <div class="form-group"><label class="form-label">路由</label><input class="form-input" v-model="fRoute" placeholder="搜索" style="width:150px" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增菜单</button>
    </div>
    <table class="data-table">
      <tr><th>菜单编号</th><th>菜单名称</th><th>级别</th><th>路由</th><th>备注</th><th>操作</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="6" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="m in pagedList" :key="m.code">
        <td style="color:var(--color-text-muted);font-size:12px">{{ m.code }}</td>
        <td>
          <span v-if="m.type==='level1'" style="display:inline-flex;align-items:center;gap:4px;cursor:pointer" @click="toggleExpand(m)">
            <span v-if="hasChildren(m)" style="font-size:10px;color:var(--color-text-muted);width:14px;text-align:center;transition:transform 0.15s">{{ expanded[m.code]||(fName||fCode||fRoute||fType!=='全部')?'▾':'▸' }}</span>
            <span v-else style="width:14px"></span>
            {{ m.label }}
          </span>
          <span v-else style="display:inline-flex;align-items:center;gap:6px;padding-left:28px;color:var(--color-text-secondary)">
            {{ m.label }}
          </span>
        </td>
        <td><span class="badge" :class="m.type==='level1'?'badge-active':'badge-pending'" style="font-size:11px">{{ m.type==='level1'?'一级菜单':'二级菜单' }}</span></td>
        <td><code v-if="m.route" style="background:var(--color-bg);padding:2px 6px;border-radius:4px;font-size:12px">{{ m.route }}</code><span v-else style="color:var(--color-text-muted);font-size:12px">—</span></td>
                <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ m.remark }}</td>
                <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button v-if="m.type==='level1'" class="btn btn-text btn-sm" @click="openAddChild(m)">新增子菜单</button>
          <button class="btn btn-text btn-sm" @click="openEdit(m)">编辑</button>
          <button v-if="canMoveUp(m)" class="btn btn-text btn-sm" @click="moveUp(m)" title="上移">↑</button>
          <button v-if="canMoveDown(m)" class="btn btn-text btn-sm" @click="moveDown(m)" title="下移">↓</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(m)">删除</button>
        </td>
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
        <span>显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize, treeList.length) }}条，共 {{ treeList.length }} 条</span>
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
        <div class="modal-title">{{ formTitle }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-if="formMode==='edit'"><label class="form-label">菜单编号</label><input class="form-input" :value="form.code" disabled style="color:var(--color-text-muted)" /></div>
          <div><label class="form-label">菜单名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.label" /></div>
          <div><label class="form-label">路由 <span v-if="form.type==='level2'" style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.route" :placeholder="form.type==='level2'?'例: /orders/list':'例: /workbench（目录类可为空）'" /></div>
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
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除「{{ deleteTarget?.label }}」吗？<br>此操作不可撤销。</div>
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
    fName:'', fCode:'', fType:'全部', fRoute:'', showForm:false, showDelete:false, formMode:'add', deleteTarget:null,
    form:{code:'',label:'',route:'',type:'level1',parent:'',remark:''},
    page: 1, pageSize: 5, expanded: {MENU_WORKBENCH:false,MENU_ORDER:false,MENU_AUDIT:false,MENU_SUPPLIER:false,MENU_REPORT:false,MENU_SYSTEM:false},
    menus:[
      // 一级菜单
      {code:'MENU_WORKBENCH',label:'工作台',route:'/workbench',type:'level1',parent:''},
      {code:'MENU_ORDER',label:'订单管理',route:'',type:'level1',parent:''},
      {code:'MENU_ORDER_LIST',label:'订单列表',route:'/orders/list',type:'level2',parent:'MENU_ORDER'},
      {code:'MENU_AUDIT',label:'订单审核',route:'',type:'level1',parent:''},
      {code:'MENU_AUDIT_LIST',label:'审核列表',route:'/order-audit/list',type:'level2',parent:'MENU_AUDIT'},
      {code:'MENU_SUPPLIER',label:'供应商管理',route:'',type:'level1',parent:''},
      {code:'MENU_SUPPLIER_LIST',label:'供应商列表',route:'/suppliers/list',type:'level2',parent:'MENU_SUPPLIER'},
      {code:'MENU_REPORT',label:'统计报表',route:'',type:'level1',parent:''},
      {code:'MENU_REPORT_HOME',label:'报表首页',route:'/reports/home',type:'level2',parent:'MENU_REPORT'},
      {code:'MENU_SYSTEM',label:'系统管理',route:'',type:'level1',parent:''},
      // 二级菜单
      {code:'MENU_USERS',label:'用户管理',route:'/system/users',type:'level2',parent:'MENU_SYSTEM'},
      {code:'MENU_ROLES',label:'角色管理',route:'/system/roles',type:'level2',parent:'MENU_SYSTEM'},
      {code:'MENU_DEPTS',label:'部门管理',route:'/system/departments',type:'level2',parent:'MENU_SYSTEM'},
      {code:'MENU_ORGS',label:'机构管理',route:'/system/organizations',type:'level2',parent:'MENU_SYSTEM'},
      {code:'MENU_MENUS',label:'菜单管理',route:'/system/menus',type:'level2',parent:'MENU_SYSTEM'},
      {code:'MENU_FUNCS',label:'功能管理',route:'/system/functions',type:'level2',parent:'MENU_SYSTEM'},
      {code:'MENU_LOGS',label:'操作日志',route:'/system/logs',type:'level2',parent:'MENU_SYSTEM'},
    ]
  }),
  computed:{
    totalPages() { return Math.ceil(this.treeList.length / this.pageSize) || 1 },
    pagedList() {
      const start = (this.page - 1) * this.pageSize
      return this.treeList.slice(start, start + this.pageSize)
    },
    filteredMenus() {
      return this.menus.filter(m => {
        if (this.fName && !m.label.includes(this.fName)) return false
        if (this.fRoute && !m.route.includes(this.fRoute)) return false
        if (this.fCode && !m.code.includes(this.fCode.toUpperCase())) return false
        if (this.fType !== '全部') {
          const typeMap = {'一级菜单':'level1','二级菜单':'level2'}
          if (m.type !== typeMap[this.fType]) return false
        }
        return true
      })
    },
    treeList() {
      const level1 = this.filteredMenus.filter(m => m.type==='level1')
      const level2 = this.filteredMenus.filter(m => m.type==='level2')
      // 筛选时如果 level1 为空但 level2 有数据，从完整列表补充父级
      const parentList = level1.length === 0 && level2.length > 0
        ? this.menus.filter(m => {
            const childParents = level2.map(l2 => l2.parent)
            return m.type === 'level1' && childParents.includes(m.code)
          })
        : level1
      const result = []
      for (const l1 of parentList) {
        result.push(l1)
        // 筛选时自动展开显示子菜单
        const showChildren = this.expanded[l1.code] || (this.fName || this.fCode || this.fRoute || this.fType !== '全部')
        if (showChildren) {
          for (const l2 of level2) {
            if (l2.parent === l1.code) result.push(l2)
          }
        }
      }
      return result
    },
    formTitle() {
      if (this.formMode==='addChild') return '新增子菜单 — ' + (this.menus.find(m=>m.code===this.form.parent)?.label||'')
      if (this.formMode==='edit') return '编辑菜单'
      return '新增菜单'
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
    resetQuery(){this.fName='';this.fCode='';this.fType='全部';this.fRoute='';this.page=1},
    openAdd(){this.formMode='add';this.form={code:'',label:'',route:'',type:'level1',parent:''};this.showForm=true},
    openAddChild(parent){this.formMode='addChild';this.form={code:'',label:'',route:'',type:'level2',parent:parent.code};this.showForm=true},
    openEdit(m){this.formMode='edit';this.form={...m};this.showForm=true},
    saveForm(){
      if(!this.form.label){this.$emit('toast',{msg:'请输入菜单名称',type:'warning'});return}
      if(this.form.type==='level2'&&!this.form.route){this.$emit('toast',{msg:'二级菜单必须配置路由',type:'warning'});return}
      if(this.formMode!=='edit'){
        this.form.code = 'MENU_' + (this.form.route ? this.form.route.replace(/^\//,'').replace(/\//g,'_').toUpperCase() : this.form.label)
        if(this.menus.some(m=>m.code===this.form.code)){this.$emit('toast',{msg:'菜单编号已存在',type:'warning'});return}
      }
      if(this.formMode.startsWith('add')){
        this.menus.push({...this.form})
        this.$emit('toast',{msg:'新增成功',type:'success'})
      } else {
        const i=this.menus.findIndex(x=>x.code===this.form.code)
        if(i>=0)this.menus.splice(i,1,{...this.form})
        this.$emit('toast',{msg:'编辑成功',type:'success'})
      }
      this.showForm=false
    },
    doDelete(m){this.deleteTarget=m;this.showDelete=true},
    confirmDelete(){
      if(this.deleteTarget){
        this.menus=this.menus.filter(x=>x.code!==this.deleteTarget.code)
        this.$emit('toast',{msg:'已删除「'+this.deleteTarget.label+'」',type:'success'})
      }
      this.showDelete=false; this.deleteTarget=null
    },
    toggleExpand(m){ this.expanded[m.code] = !this.expanded[m.code] },
    hasChildren(m){ return this.menus.some(x => x.parent===m.code) },
    level1List() { return this.menus.filter(m => m.type==='level1') },
    siblings(m) {
      if(m.type==='level1') return this.menus.filter(x => x.type==='level1')
      return this.menus.filter(x => x.type==='level2' && x.parent===m.parent)
    },
    canMoveUp(m) { const s=this.siblings(m); return s.indexOf(m)>0 },
    canMoveDown(m) { const s=this.siblings(m); return s.indexOf(m)<s.length-1 },
    swapArray(arr, i, j) { const newArr = [...arr]; const t = newArr[i]; newArr[i] = newArr[j]; newArr[j] = t; return newArr },
    moveUp(m) { const s = this.siblings(m); const i = s.indexOf(m); if (i <= 0) return; this.menus = this.swapArray(this.menus, this.menus.indexOf(m), this.menus.indexOf(s[i-1])); this.syncOrder() },
    moveDown(m) { const s = this.siblings(m); const i = s.indexOf(m); if (i >= s.length - 1) return; this.menus = this.swapArray(this.menus, this.menus.indexOf(m), this.menus.indexOf(s[i+1])); this.syncOrder() },
    syncOrder() {
      const order = this.menus.filter(m => m.type==='level1').map(m => m.code)
      localStorage.setItem('menu_order', JSON.stringify(order))
    },
  }
}
</script>