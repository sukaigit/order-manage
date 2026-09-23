<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">产品名称</label><input class="form-input" v-model="fName" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">分类</label><select class="form-select" v-model="fCategory"><option>全部</option><option>电子产品</option><option>办公用品</option><option>家具</option></select></div>
      <div class="form-group"><label class="form-label">状态</label><select class="form-select" v-model="fStatus"><option>全部</option><option>上架</option><option>下架</option></select></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增产品</button>
      <button class="btn btn-secondary" @click="doImport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>导入</button>
      <button class="btn btn-secondary" @click="doExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>导出</button>
    </div>
    <table class="data-table">
      <tr><th>产品编号</th><th>产品名称</th><th>分类</th><th>单价</th><th>库存</th><th>状态</th><th>创建日期</th><th>备注</th><th>操作</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="9" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="p in pagedList" :key="p.code">
        <td style="color:var(--color-text-muted);font-size:12px">{{ p.code }}</td><td>{{ p.name }}</td><td>{{ p.category }}</td>
        <td>¥{{ p.price.toFixed(2) }}</td><td>{{ p.stock }}</td>
        <td><span class="badge" :class="p.status==='上架'?'badge-active':'badge-disabled'">{{ p.status }}</span></td>
        <td>{{ p.date }}</td>
        <td style="font-size:12px;max-width:140px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ p.remark }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button class="btn btn-text btn-sm" @click="openEdit(p)">编辑</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(p)">删除</button>
        </td>
      </tr>
    </table>
    <div class="pagination">
      <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
        <select class="form-select" v-model.number="pageSize" @change="page=1" style="padding:4px 8px;font-size:12px;width:auto">
          <option :value="5">5条/页</option><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option>
        </select>
        <span>显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize,totalCount) }} 条，共 {{ totalCount }} 条</span>
      </div>
      <div style="display:flex;gap:4px">
        <button class="page-btn" :disabled="page<=1" @click="page=Math.max(1,page-1)">上一页</button>
        <button class="page-btn" v-for="n in pageNumbers" :key="n" :class="{active:n===page}" @click="typeof n==='number'&&(page=n)" v-text="n"></button>
        <button class="page-btn" :disabled="page>=totalPages" @click="page=Math.min(totalPages,page+1)">下一页</button>
      </div>
    </div>

    <input type="file" ref="fileInput" accept=".xls,.csv" style="display:none" @change="handleImport" />

    <div class="modal-overlay" v-if="showForm" @click.self="showForm=false">
      <div class="modal" style="min-width:520px">
        <div class="modal-title">{{ formMode==='add'?'新增产品':'编辑产品' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">产品名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
            <div style="flex:1"><label class="form-label">分类 <span style="color:var(--color-danger)">*</span></label><select class="form-select" v-model="form.category"><option>电子产品</option><option>办公用品</option><option>家具</option></select></div>
          </div>
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">单价 <span style="color:var(--color-danger)">*</span></label><input class="form-input" type="number" min="0" step="0.01" v-model.number="form.price" /></div>
            <div style="flex:1"><label class="form-label">库存</label><input class="form-input" type="number" min="0" v-model.number="form.stock" /></div>
          </div>
          <div><label class="form-label">备注</label><textarea class="form-input" v-model="form.remark" placeholder="可选" rows="2" style="resize:vertical"></textarea></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showForm=false">取消</button>
          <button class="btn btn-primary" @click="saveForm">保存</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showDelete" @click.self="showDelete=false">
      <div class="modal" style="min-width:380px;text-align:center">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="var(--color-danger)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:12px">
          <path d="M3 6h18"/><path d="M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/>
        </svg>
        <div class="modal-title" style="text-align:center">确认删除</div>
        <div style="font-size:14px;color:var(--color-text-secondary);margin-bottom:24px">确定要删除产品「{{ deleteTarget?.name }}」吗？<br>此操作不可撤销。</div>
        <div class="modal-footer" style="justify-content:center">
          <button class="btn btn-secondary" @click="showDelete=false">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showImport" @click.self="showImport=false">
      <div class="modal" style="min-width:500px">
        <div class="modal-title">导入产品</div>
        <div style="padding:16px 0;font-size:13px;color:var(--color-text-secondary)">
          <p style="margin:0 0 12px">支持 .xls（Excel XML）和 .csv 格式，第1行为列标题</p>
          <div style="background:var(--color-bg);padding:12px;border-radius:8px;font-family:monospace;font-size:12px;white-space:pre;overflow-x:auto">产品名称,分类,单价,库存,备注\n蓝牙耳机,电子产品,199.00,100,新品</div>
          <button class="btn btn-primary" style="margin-top:16px" @click="$refs.fileInput.click()">选择文件</button>
          <span v-if="importFileName" style="margin-left:12px;font-size:13px;color:var(--color-primary)">{{ importFileName }}</span>
        </div>
        <div v-if="importPreview.length" style="margin-bottom:12px">
          <div style="font-size:13px;font-weight:600;margin-bottom:8px">预览（共 {{ importPreview.length }} 条）</div>
          <div style="max-height:200px;overflow-y:auto;border:1px solid var(--color-border);border-radius:8px">
            <table style="width:100%;font-size:12px;border-collapse:collapse">
              <tr style="background:var(--color-bg);font-weight:600"><th style="padding:6px 8px;text-align:left">名称</th><th style="padding:6px 8px;text-align:left">分类</th><th style="padding:6px 8px;text-align:left">单价</th><th style="padding:6px 8px;text-align:left">库存</th></tr>
              <tr v-for="(r,i) in importPreview" :key="i" style="border-top:1px solid var(--color-border-light)">
                <td style="padding:4px 8px">{{ r.name }}</td><td style="padding:4px 8px">{{ r.category }}</td><td style="padding:4px 8px">{{ r.price }}</td><td style="padding:4px 8px">{{ r.stock }}</td>
              </tr>
            </table>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showImport=false;importPreview=[];importFileName=''">取消</button>
          <button class="btn btn-primary" :disabled="!importPreview.length" @click="confirmImport">{{ importPreview.length ? '确认导入 '+importPreview.length+' 条' : '请选择文件' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  emits: ['toast'],
  data: () => ({
    fName:'', fCategory:'全部', fStatus:'全部', showForm:false, showDelete:false, showImport:false,
    formMode:'add', deleteTarget:null,
    form:{code:'',name:'',category:'电子产品',price:0,stock:0,remark:''},
    page:1, pageSize:5,
    importPreview:[], importFileName:'',
    products:[
      {code:'PROD-001',name:'无线鼠标',category:'电子产品',price:89.00,stock:200,status:'上架',date:'2024-01-15',remark:''},
      {code:'PROD-002',name:'机械键盘',category:'电子产品',price:299.00,stock:150,status:'上架',date:'2024-01-20',remark:''},
      {code:'PROD-003',name:'A4打印纸',category:'办公用品',price:25.00,stock:500,status:'上架',date:'2024-02-01',remark:''},
      {code:'PROD-004',name:'办公椅',category:'家具',price:899.00,stock:30,status:'上架',date:'2024-02-10',remark:''},
      {code:'PROD-005',name:'显示器支架',category:'电子产品',price:159.00,stock:80,status:'下架',date:'2024-03-05',remark:''},
      {code:'PROD-006',name:'文件夹',category:'办公用品',price:8.50,stock:1000,status:'上架',date:'2024-03-12',remark:''},
      {code:'PROD-007',name:'桌面台灯',category:'家具',price:129.00,stock:60,status:'上架',date:'2024-04-01',remark:''},
      {code:'PROD-008',name:'移动硬盘',category:'电子产品',price:399.00,stock:45,status:'下架',date:'2024-04-15',remark:''},
    ]
  }),
  computed:{
    totalCount(){return this.filteredList.length},
    totalPages(){return Math.ceil(this.filteredList.length/this.pageSize)||1},
    pagedList(){const s=(this.page-1)*this.pageSize;return this.filteredList.slice(s,s+this.pageSize)},
    filteredList(){
      return this.products.filter(p=>{
        if(this.fName&&!p.name.includes(this.fName))return false
        if(this.fCategory!=='全部'&&p.category!==this.fCategory)return false
        if(this.fStatus!=='全部'&&p.status!==this.fStatus)return false
        return true
      })
    },
    pageNumbers(){
      const tp=this.totalPages,cp=this.page
      if(tp<=7)return Array.from({length:tp},(_,i)=>i+1)
      const p=[1]
      if(cp>3)p.push('...')
      for(let i=Math.max(2,cp-1);i<=Math.min(tp-1,cp+1);i++)p.push(i)
      if(cp<tp-2)p.push('...')
      p.push(tp)
      return p
    },
  },
  methods:{
    query(){this.page=1},
    resetQuery(){this.fName='';this.fCategory='全部';this.fStatus='全部';this.page=1},
    openAdd(){this.formMode='add';this.form={code:'PROD-'+String(Date.now()).slice(-3),name:'',category:'电子产品',price:0,stock:0,remark:''};this.showForm=true},
    openEdit(p){this.formMode='edit';this.form={...p};this.showForm=true},
    saveForm(){
      if(!this.form.name){this.$emit('toast',{msg:'请输入产品名称',type:'warning'});return}
      if(this.form.price<0){this.$emit('toast',{msg:'单价不能为负数',type:'warning'});return}
      if(this.form.stock<0){this.$emit('toast',{msg:'库存不能为负数',type:'warning'});return}
      if(this.formMode==='add'){this.products.push({...this.form,status:'上架',date:new Date().toISOString().slice(0,10)});this.$emit('toast',{msg:'新增产品成功',type:'success'})}
      else{const i=this.products.findIndex(x=>x.code===this.form.code);if(i>=0)this.products.splice(i,1,{...this.form});this.$emit('toast',{msg:'编辑成功',type:'success'})}
      this.showForm=false
    },
    doDelete(p){this.deleteTarget=p;this.showDelete=true},
    confirmDelete(){if(this.deleteTarget){this.products=this.products.filter(x=>x.code!==this.deleteTarget.code);this.$emit('toast',{msg:'已删除「'+this.deleteTarget.name+'」',type:'success'})}this.showDelete=false;this.deleteTarget=null},
    doExport(){
      const data=this.filteredList.map(p=>[p.code,p.name,p.category,p.price,p.stock,p.status,p.date,p.remark])
      data.unshift(['产品编号','产品名称','分类','单价','库存','状态','创建日期','备注'])
      const xml=['<?xml version="1.0" encoding="UTF-8"?><?mso-application progid="Excel.Sheet"?>']
      xml.push('<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">')
      xml.push('<Worksheet ss:Name="产品列表"><Table>')
      for(const row of data){
        xml.push('<Row>')
        for(const cell of row) xml.push('<Cell><Data ss:Type="String">'+String(cell).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</Data></Cell>')
        xml.push('</Row>')
      }
      xml.push('</Table></Worksheet></Workbook>')
      const blob=new Blob([xml.join('\n')],{type:'application/vnd.ms-excel;charset=utf-8'})
      const url=URL.createObjectURL(blob),a=document.createElement('a')
      a.href=url;a.download='产品列表_'+new Date().toISOString().slice(0,10)+'.xls'
      document.body.appendChild(a);a.click();document.body.removeChild(a);URL.revokeObjectURL(url)
      this.$emit('toast',{msg:'导出成功：'+this.filteredList.length+' 条记录',type:'success'})
    },
    doImport(){this.showImport=true;this.importPreview=[];this.importFileName=''},
    handleImport(e){
      const file=e.target.files[0];if(!file)return
      this.importFileName=file.name
      const reader=new FileReader()
      reader.onload=(ev)=>{
        const text=ev.target.result
        let preview=[]
        if(text.includes('mso-application')||file.name.endsWith('.xls')){
          // 解析 XML Excel 格式（与导出格式一致）
          const rows=text.split('</Row>')
          for(const r of rows){
            const cells=r.match(/<Data[^>]*>([^<]*)<\/Data>/g)
            if(!cells||!cells.length)continue
            const vals=cells.map(c=>c.replace(/<[^>]+>/g,''))
            if(vals[0]==='产品名称'||!vals[0])continue
            preview.push({name:vals[0],category:vals[1]||'电子产品',price:parseFloat(vals[2])||0,stock:parseInt(vals[3])||0,remark:vals[4]||''})
          }
        }else{
          // 解析 CSV
          const lines=text.split('\n').filter(l=>l.trim())
          for(let i=1;i<lines.length;i++){
            const cols=lines[i].split(',').map(c=>c.trim().replace(/^"|"$/g,''))
            if(!cols[0])continue
            preview.push({name:cols[0],category:cols[1]||'电子产品',price:parseFloat(cols[2])||0,stock:parseInt(cols[3])||0,remark:cols[4]||''})
          }
        }
        this.importPreview=preview
      }
      reader.readAsText(file)
      e.target.value=''
    },
    confirmImport(){
      for(const p of this.importPreview){
        this.products.push({code:'PROD-'+String(Date.now()).slice(-3)+String(Math.random().toString(36).slice(2,4)).toUpperCase(),name:p.name,category:p.category,price:p.price,stock:p.stock,status:'上架',date:new Date().toISOString().slice(0,10),remark:p.remark})
      }
      this.$emit('toast',{msg:'导入成功：'+this.importPreview.length+' 条记录',type:'success'})
      this.showImport=false;this.importPreview=[];this.importFileName=''
    },
  }
}
</script>