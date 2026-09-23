<template>
  <div class="page-body">
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:24px">
      <div class="card">
        <div class="card-title">订单总数</div>
        <div style="font-size:32px;font-weight:700;color:var(--color-text)">{{ stats.total_orders ?? 0 }}</div>
      </div>
      <div class="card">
        <div class="card-title">待审核</div>
        <div style="font-size:32px;font-weight:700;color:#ff9f0a">{{ stats.pending_orders ?? 0 }}</div>
      </div>
      <div class="card">
        <div class="card-title">已完成</div>
        <div style="font-size:32px;font-weight:700;color:var(--color-primary)">{{ stats.done_orders ?? 0 }}</div>
      </div>
      <div class="card">
        <div class="card-title">启用供应商</div>
        <div style="font-size:32px;font-weight:700;color:var(--color-text)">{{ stats.active_suppliers ?? 0 }}</div>
      </div>
    </div>
    <div class="card">
      <div class="card-title">{{ stats.welcome?.title || '欢迎使用订单管理系统' }}</div>
      <p style="color:var(--color-text-secondary);font-size:14px;line-height:1.8;margin:0">
        当前有 <b style="color:#ff9f0a">{{ stats.welcome?.pending_count ?? stats.pending_orders ?? 0 }}</b> 笔订单待审核，<b style="color:var(--color-danger)">{{ stats.welcome?.rejected_count ?? stats.rejected_orders ?? 0 }}</b> 笔已驳回待处理。<br>
        {{ stats.welcome?.tips || '可前往「订单管理」维护订单，「订单审核」处理审核，「统计报表」查看经营分析。' }}
      </p>
    </div>
  </div>
</template>
<script>
import { getStats } from '../api/workbench.js'

export default {
  emits: ['toast'],
  data: () => ({ stats: {} }),
  async mounted() {
    try {
      this.stats = await getStats()
    } catch (e) {
      this.$emit('toast', { msg: e.message || '加载统计失败', type: 'error' })
    }
  },
}
</script>
