<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">创建时间</label><div style="display:flex;align-items:center;gap:6px"><input class="form-input" type="date" v-model="fStart" style="width:145px" /><span style="color:var(--color-text-muted)">至</span><input class="form-input" type="date" v-model="fEnd" style="width:145px" /></div></div>
      <div class="form-group"><label class="form-label">供应商</label>
        <select class="form-select" v-model="fSupplierId">
          <option value="">全部</option>
          <option v-for="s in supplierOptions" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <button class="btn btn-secondary" @click="doExport"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="vertical-align:middle;margin-right:4px"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>导出明细</button>
    </div>

    <div v-if="loading" class="card" style="text-align:center;padding:48px;color:var(--color-text-muted)">加载中…</div>
    <div v-else-if="error" class="card" style="text-align:center;padding:48px;color:var(--color-danger)">{{ error }}</div>
    <div v-else-if="summary.total === 0" class="card" style="text-align:center;padding:48px;color:var(--color-text-muted)">当前筛选条件下暂无订单数据</div>

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
              <text x="21" y="20" text-anchor="middle" style="font-size:6px;font-weight:700;fill:var(--color-text)">{{ summary.total }}</text>
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
          <div class="card-title" style="margin:0">订单明细报表（{{ detailTotal }} 条）</div>
        </div>
        <table class="data-table">
          <tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>供应商</th><th>状态</th><th>创建时间</th></tr>
          <tr v-if="detailList.length===0"><td :colspan="6" style="text-align:center;padding:24px;color:var(--color-text-muted)">暂无数据</td></tr>
          <tr v-for="o in detailList" :key="o.id">
            <td style="color:var(--color-text-muted);font-size:12px">{{ o.order_no }}</td>
            <td>{{ o.name }}</td>
            <td>¥{{ Number(o.amount).toFixed(2) }}</td>
            <td>{{ o.supplier_name }}</td>
            <td><span class="badge" :class="o.status==='已完成'?'badge-active':o.status==='待审核'?'badge-pending':''">{{ o.status }}</span></td>
            <td>{{ o.create_time }}</td>
          </tr>
        </table>
        <div class="pagination">
          <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:var(--color-text-muted)">
            <select class="form-select" v-model.number="pageSize" @change="page=1;loadDetail()" style="padding:4px 8px;font-size:12px;width:auto">
              <option :value="5">5条/页</option><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option>
            </select>
            <span v-if="detailTotal>0">显示第 {{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize,detailTotal) }} 条，共 {{ detailTotal }} 条</span>
            <span v-else>共 0 条</span>
          </div>
          <div style="display:flex;gap:4px">
            <button class="page-btn" :disabled="page<=1" @click="page--;loadDetail()">上一页</button>
            <button class="page-btn" v-for="n in pageNumbers" :key="n" :class="{active:n===page}" @click="typeof n==='number'&&n!==page&&(page=n,loadDetail())" v-text="n"></button>
            <button class="page-btn" :disabled="page>=totalPages" @click="page++;loadDetail()">下一页</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
<script>
import { getSummary, getDetail, exportReport } from '../api/reports.js'
import { getSupplierOptions } from '../api/suppliers.js'

const COLORS = { '待审核': '#ff9f0a', '已驳回': '#dc2626', '已完成': '#1a8a3a' }

export default {
  emits: ['toast'],
  data: () => ({
    fStart: '', fEnd: '', fSupplierId: '',
    supplierOptions: [],
    summary: { total: 0, status_dist: [], supplier_amount: [], monthly_trend: [] },
    detailList: [], detailTotal: 0,
    loading: false, error: '',
    page: 1, pageSize: 10,
  }),
  computed: {
    totalPages() { return Math.ceil(this.detailTotal / this.pageSize) || 1 },
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
    pieSegments() {
      const total = this.summary.total
      if (!total) return []
      let acc = 0
      return (this.summary.status_dist || []).map(st => {
        const count = st.count || 0
        const seg = {
          label: st.status, count, percent: st.percent ?? Math.round(count / total * 1000) / 10,
          dash: count / total * 100 || 0.001, offset: -(acc / total * 100) || 0,
          color: COLORS[st.status] || '#0066cc',
        }
        acc += count
        return seg
      })
    },
    barData() {
      const entries = (this.summary.supplier_amount || []).map(e => ({ name: e.supplier_name, amount: Number(e.amount) || 0 }))
      const max = Math.max(...entries.map(e => e.amount), 1)
      return entries.map(e => ({ ...e, height: Math.round(e.amount / max * 130) }))
    },
    trendData() { return this.summary.monthly_trend || [] },
    maxCount() { return Math.max(...this.trendData.map(m => m.count), 1) },
    linePoints() { return this.trendData.map((m, i) => this.lineX(i) + ',' + this.lineY(m.count)).join(' ') },
  },
  methods: {
    formatW(n) { return n >= 10000 ? (n / 10000).toFixed(1) + '万' : n.toFixed(0) },
    lineX(i) { const n = this.trendData.length; return n <= 1 ? 310 : 60 + i * (500 / (n - 1)) },
    lineY(count) { return 170 - (count / this.maxCount) * 150 },
    buildQuery() {
      return {
        start_date: this.fStart || undefined,
        end_date: this.fEnd || undefined,
        supplier_id: this.fSupplierId || undefined,
      }
    },
    async loadOptions() {
      try { this.supplierOptions = await getSupplierOptions() || [] } catch { this.supplierOptions = [] }
    },
    async loadSummary() {
      this.loading = true
      this.error = ''
      try {
        this.summary = await getSummary(this.buildQuery())
      } catch (e) {
        if (e.code === 403) this.error = '无权限访问统计报表'
        else this.error = e.message || '加载报表失败'
        this.summary = { total: 0, status_dist: [], supplier_amount: [], monthly_trend: [] }
      } finally {
        this.loading = false
      }
    },
    async loadDetail() {
      try {
        const data = await getDetail({ ...this.buildQuery(), page: this.page, pageSize: this.pageSize })
        this.detailList = data.list || []
        this.detailTotal = data.total || 0
      } catch (e) {
        if (e.code === 403) this.error = '无权限访问统计报表'
        else this.$emit('toast', { msg: e.message || '加载明细失败', type: 'error' })
        this.detailList = []
        this.detailTotal = 0
      }
    },
    query() {
      this.page = 1
      this.loadSummary()
      this.loadDetail()
    },
    resetQuery() {
      this.fStart = ''; this.fEnd = ''; this.fSupplierId = ''
      this.page = 1
      this.loadSummary()
      this.loadDetail()
    },
    async doExport() {
      try {
        await exportReport(this.buildQuery())
        this.$emit('toast', { msg: '导出成功', type: 'success' })
      } catch (e) {
        this.$emit('toast', { msg: e.message || '导出失败', type: 'error' })
      }
    },
  },
  mounted() {
    this.loadOptions()
    this.loadSummary()
    this.loadDetail()
  },
}
</script>
