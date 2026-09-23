<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号/名称</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <div class="form-group"><label class="form-label">供应商</label><select class="form-select" v-model="fSupplier"><option>全部</option><option v-for="s in supplierOptions" :key="s.code" :value="s.code">{{ s.name }}</option></select></div>
      <div class="form-group"><label class="form-label">状态</label><select class="form-select" v-model="fStatus"><option>全部</option><option>待审核</option><option>已驳回</option><option>已完成</option></select></div>
      <div class="form-group"><label class="form-label">创建时间</label><div style="display:flex;align-items:center;gap:6px"><input class="form-input" type="date" v-model="fStart" style="width:145px" /><span style="color:var(--color-text-muted)">至</span><input class="form-input" type="date" v-model="fEnd" style="width:145px" /></div></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-primary" @click="openAdd">新增订单</button>
      <button class="btn btn-secondary" @click="doExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>导出</button>
    </div>
    <table class="data-table">
      <tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>供应商</th><th>状态</th><th>创建时间</th><th>备注</th><th>操作</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="8" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="o in pagedList" :key="o.code">
        <td style="color:var(--color-text-muted);font-size:12px">{{ o.code }}</td>
        <td>{{ o.name }}</td>
        <td>¥{{ o.amount.toFixed(2) }}</td>
        <td>{{ supName(o.supplier) }}</td>
        <td><span class="badge" :class="statusClass(o.status)">{{ o.status }}</span></td>
        <td>{{ o.date }}</td>
        <td style="font-size:12px;max-width:140px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ o.remark }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button class="btn btn-text btn-sm" @click="openEdit(o)">编辑</button>
          <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="doDelete(o)">删除</button>
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

    <div class="modal-overlay" v-if="showForm" @click.self="showForm=false">
      <div class="modal" style="min-width:520px">
        <div class="modal-title">{{ formMode==='add'?'新增订单':'编辑订单' }}</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">订单编号 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.code" :disabled="formMode==='edit'" :style="formMode==='edit'?{color:'var(--color-text-muted)'}:{}" /></div>
            <div style="flex:1"><label class="form-label">订单名称 <span style="color:var(--color-danger)">*</span></label><input class="form-input" v-model="form.name" /></div>
          </div>
          <div style="display:flex;gap:16px">
            <div style="flex:1"><label class="form-label">金额 <span style="color:var(--color-danger)">*</span></label><input class="form-input" type="number" min="0" step="0.01" v-model.number="form.amount" /></div>
            <div style="flex:1"><label class="form-label">供应商 <span style="color:var(--color-danger)">*</span></label>
              <select class="form-select" v-model="form.supplier">
                <option value="">请选择</option>
                <option v-for="s in supplierOptions" :key="s.code" :value="s.code">{{ s.name }}</option>
              </select>
            </div>
          </div>
          <div><label class="form-label">备注</label><textarea class="form-input" v-model="form.remark" placeholder="可选" rows="2" style="resize:vertical"></textarea></div>
          <div v-if="formMode==='edit' && form.status==='已驳回'" style="font-size:12px;color:var(--color-text-muted);background:var(--color-bg);padding:8px 12px;border-radius:8px">保存后订单状态将自动回到「待审核」</div>
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
import { getOrders, addOrder, updateOrder, deleteOrder } from '../store/orderStore.js'
import { getSuppliers } from '../store/supplierStore.js'
export default {
  emits: ['toast'],
  data: () => ({
    fKeyword:'', fSupplier:'全部', fStatus:'全部', fStart:'', fEnd:'',
    showForm:false, showDelete:false, formMode:'add', deleteTarget:null,
    form:{code:'',name:'',amount:0,supplier:'',remark:''},
    page:1, pageSize:5,
  }),
  computed:{
    supplierOptions(){ return getSuppliers() },
    totalCount(){ return this.filteredList.length },
    totalPages(){ return Math.ceil(this.filteredList.length/this.pageSize)||1 },
    pagedList(){ const s=(this.page-1)*this.pageSize; return this.filteredList.slice(s,s+this.pageSize) },
    filteredList(){
      const kw = this.fKeyword
      return getOrders().filter(o=>{
        if(kw && !o.code.toLowerCase().includes(kw.toLowerCase()) && !o.name.includes(kw)) return false
        if(this.fSupplier!=='全部' && o.supplier!==this.fSupplier) return false
        if(this.fStatus!=='全部' && o.status!==this.fStatus) return false
        if(this.fStart && o.date < this.fStart) return false
        if(this.fEnd && o.date > this.fEnd) return false
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
    supName(code){ const s=getSuppliers().find(x=>x.code===code); return s?s.name:code },
    statusClass(s){ return s==='已完成'?'badge-active':s==='待审核'?'badge-pending':'' },
    query(){ this.page=1 },
    resetQuery(){ this.fKeyword='';this.fSupplier='全部';this.fStatus='全部';this.fStart='';this.fEnd='';this.page=1 },
    openAdd(){
      this.formMode='add'
      this.form={code:'ORD'+new Date().toISOString().slice(0,10).replace(/-/g,'')+String(getOrders().length+1).padStart(2,'0'),name:'',amount:0,supplier:'',remark:''}
      this.showForm=true
    },
    openEdit(o){
      if(o.status==='已完成'){ this.$emit('toast',{msg:'已完成订单不可编辑',type:'warning'}); return }
      this.formMode='edit'; this.form={...o}; this.showForm=true
    },
    saveForm(){
      if(!this.form.code||!this.form.name||!this.form.supplier||this.form.amount===''||this.form.amount===null){ this.$emit('toast',{msg:'请填写必填字段（编号/名称/金额/供应商）',type:'warning'}); return }
      if(this.form.amount<0){ this.$emit('toast',{msg:'金额不能为负数',type:'warning'}); return }
      if(this.formMode==='add'){
        if(getOrders().some(o=>o.code===this.form.code)){ this.$emit('toast',{msg:'订单编号已存在',type:'warning'}); return }
        addOrder({...this.form,status:'待审核',date:new Date().toISOString().slice(0,10),auditUser:'',auditTime:'',opinion:''})
        this.$emit('toast',{msg:'新增订单成功，状态为待审核',type:'success'})
      }else{
        const status = this.form.status==='已驳回' ? '待审核' : this.form.status
        updateOrder(this.form.code,{...this.form,status})
        this.$emit('toast',{msg:status==='待审核'&&this.form.status==='已驳回'?'编辑成功，订单已回到待审核':'编辑成功',type:'success'})
      }
      this.showForm=false
    },
    doDelete(o){
      if(o.status==='已完成'){ this.$emit('toast',{msg:'已完成订单不可删除',type:'warning'}); return }
      this.deleteTarget=o; this.showDelete=true
    },
    confirmDelete(){
      if(this.deleteTarget){ deleteOrder(this.deleteTarget.code); this.$emit('toast',{msg:'已删除订单「'+this.deleteTarget.name+'」',type:'success'}) }
      this.showDelete=false; this.deleteTarget=null
    },
    doExport(){
      const data=this.filteredList.map(o=>[o.code,o.name,o.amount.toFixed(2),this.supName(o.supplier),o.status,o.date,o.remark])
      data.unshift(['订单编号','订单名称','金额','供应商','状态','创建时间','备注'])
      const xml=['<?xml version="1.0" encoding="UTF-8"?><?mso-application progid="Excel.Sheet"?>']
      xml.push('<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">')
      xml.push('<Worksheet ss:Name="订单列表"><Table>')
      for(const row of data){
        xml.push('<Row>')
        for(const cell of row) xml.push('<Cell><Data ss:Type="String">'+String(cell).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</Data></Cell>')
        xml.push('</Row>')
      }
      xml.push('</Table></Worksheet></Workbook>')
      const blob=new Blob([xml.join('\n')],{type:'application/vnd.ms-excel;charset=utf-8'})
      const url=URL.createObjectURL(blob),a=document.createElement('a')
      a.href=url;a.download='订单列表_'+new Date().toISOString().slice(0,10)+'.xls'
      document.body.appendChild(a);a.click();document.body.removeChild(a);URL.revokeObjectURL(url)
      this.$emit('toast',{msg:'导出成功：'+this.filteredList.length+' 条记录',type:'success'})
    },
  }
}
</script>
