<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">创建时间</label><div style="display:flex;align-items:center;gap:6px"><input class="form-input" type="date" v-model="fStart" style="width:145px" /><span style="color:var(--color-text-muted)">至</span><input class="form-input" type="date" v-model="fEnd" style="width:145px" /></div></div>
      <div class="form-group"><label class="form-label">供应商</label><select class="form-select" v-model="fSupplier"><option value="全部">全部</option><option v-for="s in supplierOptions" :key="s.code" :value="s.code">{{ s.name }}</option></select></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-secondary" @click="doExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>导出明细</button>
    </div>

    <div v-if="filteredOrders.length===0" class="card" style="text-align:center;padding:48px;color:var(--color-text-muted)">当前筛选条件下暂无订单数据</div>

    <template v-else>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px">
        <div class="card">
          <div class="card-title">订单状态分布</div>
          <div style="display:flex;align-items:center;gap:20px;padding:8px 0">
            <svg width="160" height="160" viewBox="0 0 42 42">
              <circle cx="21" cy="21" r="15.915" fill="none" stroke="#e5e5ea" stroke-width="6"/>
              <circle v-for="(seg,i) in pieSegments" :key="seg.label" cx="21" cy="21" r="15.915" fill="none"
                :stroke="seg.color" stroke-width="6"
                :stroke-dasharray="seg.dash + ' ' + (100 - seg.dash)"
                :stroke-dashoffset="seg.offset"
                transform="rotate(-90 21 21)"/>
              <text x="21" y="20" text-anchor="middle" style="font-size:6px;font-weight:700;fill:var(--color-text)">{{ filteredOrders.length }}</text>
              <text x="21" y="26" text-anchor="middle" style="font-size:2.6px;fill:var(--color-text-muted)">总订单</text>
            </svg>
            <div style="display:flex;flex-direction:column;gap:8px;font-size:13px">
              <div v-for="seg in pieSegments" :key="seg.label" style="display:flex;align-items:center;gap:8px">
                <span :style="{width:'10px',height:'10px',borderRadius:'3px',background:seg.color,display:'inline-block'}"></span>
                <span>{{ seg.label }}</span>
                <span style="color:var(--color-text-muted)">{{ seg.count }} 单（{{ seg.percent }}%）</span>
              </div>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-title">供应商订单金额汇总</div>
          <div style="display:flex;align-items:flex-end;justify-content:space-around;height:180px;padding:16px 8px 0">
            <div v-for="b in barData" :key="b.name" style="display:flex;flex-direction:column;align-items:center;gap:8px;flex:1;max-width:120px">
              <span style="font-size:12px;font-weight:600;color:var(--color-primary)">¥{{ formatW(b.amount) }}</span>
              <div :style="{width:'44px',height:Math.max(8,b.height)+'px',background:'var(--color-primary)',borderRadius:'6px 6px 0 0',transition:'height 0.3s'}"></div>
              <span style="font-size:11px;color:var(--color-text-muted);text-align:center;line-height:1.3">{{ b.name }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card" style="margin-bottom:16px">
        <div class="card-title">按月订单趋势</div>
        <svg viewBox="0 0 600 200" style="width:100%;height:200px">
          <line x1="40" y1="170" x2="580" y2="170" stroke="#d2d2d7" stroke-width="1"/>
          <line v-for="i in 4" :key="'g'+i" x1="40" :y1="170-(i-1)*37.5" x2="580" :y2="170-(i-1)*37.5" stroke="#e5e5ea" stroke-width="1" stroke-dasharray="3 3"/>
          <polyline :points="linePoints" fill="none" stroke="var(--color-primary)" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round"/>
          <g v-for="(m,i) in trendData" :key="m.month">
            <circle :cx="lineX(i)" :cy="lineY(m.count)" r="4" fill="var(--color-primary)"/>
            <text :x="lineX(i)" :y="lineY(m.count)-10" text-anchor="middle" style="font-size:11px;font-weight:600;fill:var(--color-text)">{{ m.count }}</text>
            <text :x="lineX(i)" y="188" text-anchor="middle" style="font-size:11px;fill:var(--color-text-muted)">{{ m.month }}</text>
          </g>
        </svg>
        <div style="font-size:12px;color:var(--color-text-muted);text-align:right">纵轴：订单数量（单）</div>
      </div>

      <div class="card">
        <div style="display:flex;align-items:center;margin-bottom:12px">
          <div class="card-title" style="margin:0">订单明细报表（{{ filteredOrders.length }} 条）</div>
        </div>
        <table class="data-table">
          <tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>供应商</th><th>状态</th><th>创建时间</th></tr>
          <tr v-for="o in filteredOrders" :key="o.code">
            <td style="color:var(--color-text-muted);font-size:12px">{{ o.code }}</td>
            <td>{{ o.name }}</td>
            <td>¥{{ o.amount.toFixed(2) }}</td>
            <td>{{ supName(o.supplier) }}</td>
            <td><span class="badge" :class="o.status==='已完成'?'badge-active':o.status==='待审核'?'badge-pending':''">{{ o.status }}</span></td>
            <td>{{ o.date }}</td>
          </tr>
        </table>
      </div>
    </template>
  </div>
</template>
<script>
import { getOrders } from '../store/orderStore.js'
import { getSuppliers } from '../store/supplierStore.js'
const COLORS = { '待审核':'#ff9f0a', '已驳回':'#dc2626', '已完成':'#1a8a3a' }
export default {
  emits: ['toast'],
  data: () => ({
    fStart:'', fEnd:'', fSupplier:'全部',
  }),
  computed:{
    supplierOptions(){ return getSuppliers() },
    filteredOrders(){
      return getOrders().filter(o=>{
        if(this.fStart && o.date < this.fStart) return false
        if(this.fEnd && o.date > this.fEnd) return false
        if(this.fSupplier!=='全部' && o.supplier!==this.fSupplier) return false
        return true
      })
    },
    pieSegments(){
      const total=this.filteredOrders.length
      if(!total) return []
      let acc=0
      return ['待审核','已驳回','已完成'].map(st=>{
        const count=this.filteredOrders.filter(o=>o.status===st).length
        const percent=Math.round(count/total*1000)/10
        const seg={label:st,count,percent,dash:count/total*100||0.001,offset:-(acc/total*100)||0,color:COLORS[st]}
        acc+=count
        return seg
      })
    },
    barData(){
      const map={}
      for(const o of this.filteredOrders) map[o.supplier]=(map[o.supplier]||0)+o.amount
      const entries=Object.entries(map).map(([code,amount])=>({name:this.supName(code),amount}))
      const max=Math.max(...entries.map(e=>e.amount),1)
      return entries.map(e=>({...e,height:Math.round(e.amount/max*130)}))
    },
    trendData(){
      const map={}
      for(const o of this.filteredOrders){ const m=o.date.slice(0,7); if(!map[m]) map[m]={month:m,count:0,amount:0}; map[m].count++; map[m].amount+=o.amount }
      return Object.values(map).sort((a,b)=>a.month.localeCompare(b.month))
    },
    maxCount(){ return Math.max(...this.trendData.map(m=>m.count),1) },
    linePoints(){ return this.trendData.map((m,i)=>this.lineX(i)+','+this.lineY(m.count)).join(' ') },
  },
  methods:{
    supName(code){ const s=getSuppliers().find(x=>x.code===code); return s?s.name:code },
    formatW(n){ return n>=10000 ? (n/10000).toFixed(1)+'万' : n.toFixed(0) },
    lineX(i){ const n=this.trendData.length; return n<=1 ? 310 : 60 + i*(500/(n-1)) },
    lineY(count){ return 170 - (count/this.maxCount)*150 },
    query(){},
    resetQuery(){ this.fStart=''; this.fEnd=''; this.fSupplier='全部' },
    doExport(){
      const data=this.filteredOrders.map(o=>[o.code,o.name,o.amount.toFixed(2),this.supName(o.supplier),o.status,o.date])
      data.unshift(['订单编号','订单名称','金额','供应商','状态','创建时间'])
      const xml=['<?xml version="1.0" encoding="UTF-8"?><?mso-application progid="Excel.Sheet"?>']
      xml.push('<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">')
      xml.push('<Worksheet ss:Name="订单明细报表"><Table>')
      for(const row of data){
        xml.push('<Row>')
        for(const cell of row) xml.push('<Cell><Data ss:Type="String">'+String(cell).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')+'</Data></Cell>')
        xml.push('</Row>')
      }
      xml.push('</Table></Worksheet></Workbook>')
      const blob=new Blob([xml.join('\n')],{type:'application/vnd.ms-excel;charset=utf-8'})
      const url=URL.createObjectURL(blob),a=document.createElement('a')
      a.href=url;a.download='订单明细报表_'+new Date().toISOString().slice(0,10)+'.xls'
      document.body.appendChild(a);a.click();document.body.removeChild(a);URL.revokeObjectURL(url)
      this.$emit('toast',{msg:'导出成功：'+this.filteredOrders.length+' 条记录',type:'success'})
    },
  }
}
</script>
