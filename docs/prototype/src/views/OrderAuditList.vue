<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号/名称</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <div class="form-group"><label class="form-label">状态</label><select class="form-select" v-model="fStatus"><option>全部</option><option>待审核</option><option>已驳回</option><option>已完成</option></select></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <span style="font-size:13px;color:var(--color-text-muted);align-self:center">待审核 {{ pendingCount }} 单</span>
    </div>
    <table class="data-table">
      <tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>供应商</th><th>状态</th><th>创建时间</th><th>审核人</th><th>审核时间</th><th>审核意见</th><th>操作</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="10" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="o in pagedList" :key="o.code">
        <td style="color:var(--color-text-muted);font-size:12px">{{ o.code }}</td>
        <td>{{ o.name }}</td>
        <td>¥{{ o.amount.toFixed(2) }}</td>
        <td>{{ supName(o.supplier) }}</td>
        <td><span class="badge" :class="statusClass(o.status)">{{ o.status }}</span></td>
        <td>{{ o.date }}</td>
        <td>{{ o.auditUser || '—' }}</td>
        <td>{{ o.auditTime || '—' }}</td>
        <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" :title="o.opinion">{{ o.opinion || '—' }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <template v-if="o.status==='待审核'">
            <button class="btn btn-text btn-sm" style="color:var(--color-primary)" @click="doPass(o)">通过</button>
            <button class="btn btn-text btn-sm" style="color:var(--color-danger)" @click="openReject(o)">驳回</button>
          </template>
          <span v-else style="font-size:12px;color:var(--color-text-muted);align-self:center">已处理</span>
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

    <div class="modal-overlay" v-if="showReject" @click.self="showReject=false">
      <div class="modal" style="min-width:440px">
        <div class="modal-title">驳回订单</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div style="font-size:13px;color:var(--color-text-secondary)">订单「{{ rejectTarget?.name }}」（{{ rejectTarget?.code }}）</div>
          <div>
            <label class="form-label">审核意见 <span style="color:var(--color-danger)">*</span></label>
            <textarea class="form-input" v-model="rejectOpinion" placeholder="请填写驳回原因（必填）" rows="3" style="resize:vertical"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showReject=false">取消</button>
          <button class="btn btn-danger" @click="confirmReject">确认驳回</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getOrders, updateOrder } from '../store/orderStore.js'
import { getSuppliers } from '../store/supplierStore.js'
export default {
  emits: ['toast'],
  data: () => ({
    fKeyword:'', fStatus:'待审核',
    showReject:false, rejectTarget:null, rejectOpinion:'',
    page:1, pageSize:5,
  }),
  computed:{
    pendingCount(){ return getOrders().filter(o=>o.status==='待审核').length },
    totalCount(){ return this.filteredList.length },
    totalPages(){ return Math.ceil(this.filteredList.length/this.pageSize)||1 },
    pagedList(){ const s=(this.page-1)*this.pageSize; return this.filteredList.slice(s,s+this.pageSize) },
    filteredList(){
      const kw=this.fKeyword
      return getOrders().filter(o=>{
        if(kw && !o.code.toLowerCase().includes(kw.toLowerCase()) && !o.name.includes(kw)) return false
        if(this.fStatus!=='全部' && o.status!==this.fStatus) return false
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
    now(){ return new Date().toISOString().slice(0,19).replace('T',' ') },
    query(){ this.page=1 },
    resetQuery(){ this.fKeyword=''; this.fStatus='待审核'; this.page=1 },
    doPass(o){
      if(o.status!=='待审核'){ this.$emit('toast',{msg:'仅待审核订单可审核',type:'warning'}); return }
      updateOrder(o.code,{status:'已完成',auditUser:'审核员',auditTime:this.now(),opinion:''})
      this.$emit('toast',{msg:'审核通过，订单「'+o.name+'」已完成',type:'success'})
    },
    openReject(o){
      if(o.status!=='待审核'){ this.$emit('toast',{msg:'仅待审核订单可审核',type:'warning'}); return }
      this.rejectTarget=o; this.rejectOpinion=''; this.showReject=true
    },
    confirmReject(){
      if(!this.rejectOpinion.trim()){ this.$emit('toast',{msg:'驳回时审核意见必填',type:'warning'}); return }
      updateOrder(this.rejectTarget.code,{status:'已驳回',auditUser:'审核员',auditTime:this.now(),opinion:this.rejectOpinion.trim()})
      this.$emit('toast',{msg:'已驳回订单「'+this.rejectTarget.name+'」',type:'success'})
      this.showReject=false; this.rejectTarget=null; this.rejectOpinion=''
    },
  }
}
</script>
