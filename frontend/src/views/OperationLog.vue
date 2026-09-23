<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">操作人</label><input class="form-input" v-model="fOperator" placeholder="搜索" /></div>
      <div class="form-group"><label class="form-label">操作类型</label><input class="form-input" v-model="fAction" placeholder="搜索" style="width:130px" /></div>
      <div class="form-group"><label class="form-label">操作目标</label><input class="form-input" v-model="fTarget" placeholder="搜索" style="width:130px" /></div>
      <div class="form-group"><label class="form-label">IP</label><input class="form-input" v-model="fIp" placeholder="搜索" style="width:140px" /></div>
      <div class="form-group"><label class="form-label">日期</label><input class="form-input" type="date" v-model="fDate" style="width:150px" /></div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
    </div>
    <table class="data-table">
      <tr><th>操作人</th><th>操作类型</th><th>操作目标</th><th>IP</th><th>时间</th></tr>
      <tr v-if="list.length===0"><td :colspan="5" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="l in list" :key="l.id"><td>{{ l.operator }}</td><td>{{ l.action }}</td><td>{{ l.target }}</td><td>{{ l.ip }}</td><td>{{ l.create_time }}</td></tr>
    </table>
    <div class="pagination">
      <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
        <select class="form-select" v-model.number="pageSize" @change="page=1;load()" style="padding:4px 8px;font-size:12px;width:auto">
          <option :value="5">5条/页</option><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option>
        </select>
        <span v-if="totalCount>0">显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize,totalCount) }} 条，共 {{ totalCount }} 条</span>
        <span v-else>共 0 条</span>
      </div>
      <div style="display:flex;gap:4px">
        <button class="page-btn" :disabled="page<=1" @click="page--;load()">上一页</button>
        <button class="page-btn" v-for="n in pageNumbers" :key="n" :class="{active:n===page}" @click="typeof n==='number'&&n!==page&&(page=n,load())" v-text="n"></button>
        <button class="page-btn" :disabled="page>=totalPages" @click="page++;load()">下一页</button>
      </div>
    </div>
  </div>
</template>
<script>
import { getLogs } from '../api/system.js'

export default {
  emits: ['toast'],
  data: () => ({
    fOperator: '', fAction: '', fTarget: '', fIp: '', fDate: '',
    list: [], total: 0, loading: false,
    page: 1, pageSize: 5,
  }),
  computed: {
    totalCount() { return this.total },
    totalPages() { return Math.ceil(this.total / this.pageSize) || 1 },
    pageNumbers() {
      const tp = this.totalPages, cp = this.page
      if (tp <= 7) return Array.from({ length: tp }, (_, i) => i + 1)
      const p = [1]
      if (cp > 3) p.push('...')
      for (let i = Math.max(2, cp - 1); i <= Math.min(tp - 1, cp + 1); i++) p.push(i)
      if (cp < tp - 2) p.push('...')
      p.push(tp)
      return p
    },
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const data = await getLogs({
          operator: this.fOperator || undefined,
          action: this.fAction || undefined,
          target: this.fTarget || undefined,
          ip: this.fIp || undefined,
          date: this.fDate || undefined,
          page: this.page,
          pageSize: this.pageSize,
        })
        this.list = data.list || []
        this.total = data.total || 0
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载日志失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    query() { this.page = 1; this.load() },
    resetQuery() {
      this.fOperator = ''; this.fAction = ''; this.fTarget = ''; this.fIp = ''; this.fDate = ''
      this.page = 1
      this.load()
    },
  },
  mounted() { this.load() },
}
</script>
