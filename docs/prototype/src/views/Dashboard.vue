<template>
  <div class="page-body">
    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:24px">
      <div class="card"><div class="card-title">今日新增</div><div style="font-size:32px;font-weight:700;color:var(--color-primary)">0</div></div>
      <div class="card"><div class="card-title">待处理</div><div style="font-size:32px;font-weight:700;color:var(--color-warning)">0</div></div>
      <div class="card"><div class="card-title">累计总量</div><div style="font-size:32px;font-weight:700;color:var(--color-text)">0</div></div>
    </div>
    <div class="card">
      <div class="card-title">欢迎使用订单管理系统</div>
      <p style="color:var(--color-text-secondary);font-size:14px;line-height:1.8">
        请前往 <b>工作台</b> 查看概览，<br>
        <b>订单管理</b> 维护订单，<b>订单审核</b> 处理审核，<b>统计报表</b> 查看经营分析。
      </p>
    </div>
  </div>
</template>
<script>
export default { emits: ['toast'] }
</script>