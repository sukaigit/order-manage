<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">操作人</label><input class="form-input" v-model="fUser" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">操作类型</label><input class="form-input" v-model="fAction" placeholder="搜索" style="width:130px" /></div>
      <div class="form-group"><label class="form-label">操作目标</label><input class="form-input" v-model="fTarget" placeholder="搜索" style="width:130px" /></div>
      <div class="form-group"><label class="form-label">IP</label><input class="form-input" v-model="fIP" placeholder="搜索" style="width:140px" /></div>
      <div class="form-group"><label class="form-label">日期</label><input class="form-input" type="date" v-model="fDate" style="width:150px" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
    </div>
    <table class="data-table">
      <tr><th>操作人</th><th>操作类型</th><th>操作目标</th><th>IP</th><th>时间</th></tr>
      <tr v-if="pagedList.length===0"><td :colspan="5" style="text-align:center;padding:32px;color:var(--color-text-muted)">暂无数据</td></tr>
      <tr v-for="l in pagedList" :key="l.id"><td>{{ l.user }}</td><td>{{ l.action }}</td><td>{{ l.target }}</td><td>{{ l.ip }}</td><td>{{ l.time }}</td></tr>
    </table>
    <div class="pagination">
      <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
        <select class="form-select" v-model.number="pageSize" @change="page=1" style="padding:4px 8px;font-size:12px;width:auto">
          <option :value="5">5条/页</option>
          <option :value="10">10条/页</option>
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
        </select>
        <span>显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize, logs.length) }}条，共 {{ logs.length }} 条</span>
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
  </div>
</template>
<script>
export default {
  emits: ['toast'],
  data: () => ({
    page: 1, pageSize: 5,
    fUser:'', fAction:'', fTarget:'', fIP:'', fDate:'',
    logs: [
      {id:1,user:'admin',action:'登录',target:'系统',ip:'192.168.1.10',time:'2026-09-01 09:00:00'},
      {id:2,user:'admin',action:'新增订单',target:'ORD20260905',ip:'192.168.1.20',time:'2026-09-01 09:15:00'},
      {id:3,user:'审核员',action:'审核通过',target:'ORD20260905',ip:'192.168.1.30',time:'2026-09-02 09:15:00'},
      {id:4,user:'admin',action:'导出报表',target:'订单明细报表',ip:'192.168.1.40',time:'2026-09-03 10:15:00'},
      {id:5,user:'admin',action:'修改密码',target:'用户admin',ip:'192.168.1.50',time:'2026-09-04 10:20:00'},
      {id:6,user:'admin',action:'新增用户',target:'用户user01',ip:'192.168.1.60',time:'2026-09-05 10:25:00'},
      {id:7,user:'admin',action:'删除订单',target:'ORD20260807',ip:'192.168.1.70',time:'2026-09-06 10:30:00'},
      {id:8,user:'admin',action:'编辑订单',target:'ORD20260703',ip:'192.168.1.80',time:'2026-09-07 10:35:00'},
      {id:9,user:'审核员',action:'审核驳回',target:'ORD20260708',ip:'192.168.1.90',time:'2026-09-07 16:40:00'},
      {id:10,user:'admin',action:'停用供应商',target:'SUP-003',ip:'192.168.1.100',time:'2026-09-08 10:45:00'},
    ]
  }),
  computed: {
    totalPages() { return Math.ceil(this.filteredList.length / this.pageSize) || 1 },
    filteredList() {
      return this.logs.filter(l => {
        if(this.fUser && !l.user.includes(this.fUser)) return false
        if(this.fAction && !l.action.includes(this.fAction)) return false
        if(this.fTarget && !l.target.includes(this.fTarget)) return false
        if(this.fIP && !l.ip.includes(this.fIP)) return false
        if(this.fDate && l.time.slice(0,10) !== this.fDate) return false
        return true
      })
    },
    pagedList() {
      const start = (this.page - 1) * this.pageSize
      return this.filteredList.slice(start, start + this.pageSize)
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
  methods: {
    query() { this.page = 1 },
    resetQuery() { this.fUser=''; this.fAction=''; this.fTarget=''; this.fIP=''; this.fDate=''; this.page = 1 },
  }
}
</script>
