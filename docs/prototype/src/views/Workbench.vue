<template>
  <div class="page-body">
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:24px">
      <div class="card">
        <div class="card-title">订单总数</div>
        <div style="font-size:32px;font-weight:700;color:var(--color-text)">{{ stats.total }}</div>
      </div>
      <div class="card">
        <div class="card-title">待审核</div>
        <div style="font-size:32px;font-weight:700;color:#ff9f0a">{{ stats.pending }}</div>
      </div>
      <div class="card">
        <div class="card-title">已完成</div>
        <div style="font-size:32px;font-weight:700;color:var(--color-primary)">{{ stats.done }}</div>
      </div>
      <div class="card">
        <div class="card-title">启用供应商</div>
        <div style="font-size:32px;font-weight:700;color:var(--color-text)">{{ stats.suppliers }}</div>
      </div>
    </div>
    <div class="card">
      <div class="card-title">欢迎使用订单管理系统</div>
      <p style="color:var(--color-text-secondary);font-size:14px;line-height:1.8;margin:0">
        当前有 <b style="color:#ff9f0a">{{ stats.pending }}</b> 笔订单待审核，<b style="color:var(--color-danger)">{{ stats.rejected }}</b> 笔已驳回待处理。<br>
        可前往「订单管理」维护订单，「订单审核」处理审核，「统计报表」查看经营分析。
      </p>
    </div>
  </div>
</template>
<script>
import { getOrders } from '../store/orderStore.js'
import { getSuppliers } from '../store/supplierStore.js'
export default {
  emits: ['toast'],
  computed:{
    stats(){
      const orders=getOrders()
      return {
        total: orders.length,
        pending: orders.filter(o=>o.status==='待审核').length,
        rejected: orders.filter(o=>o.status==='已驳回').length,
        done: orders.filter(o=>o.status==='已完成').length,
        suppliers: getSuppliers().filter(s=>s.active).length,
      }
    }
  }
}
</script>
