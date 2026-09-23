<template>
  <div class="page-body">
    <div class="filter-bar">
      <div class="form-group"><label class="form-label">编号/名称</label><input class="form-input" v-model="fKeyword" placeholder="模糊搜索" /></div>
      <div class="form-group"><label class="form-label">状态</label>
        <select class="form-select" v-model="fStatus">
          <option value="">全部</option><option>待审核</option><option>已驳回</option><option>已完成</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="query">查询</button>
      <button class="btn btn-secondary" @click="resetQuery">重置</button>
      <div style="flex:1"></div>
      <span style="font-size:13px;color:var(--color-text-muted);align-self:center">待审核 {{ pendingCount }} 单</span>
    </div>
    <table class="data-table">
      <tr><th>订单编号</th><th>订单名称</th><th>金额</th><th>供应商</th><th>状态</th><th>创建时间</th><th>审核人</th><th>审核时间</th><th>审核意见</th><th>操作</th></tr>
      <tr v-if="list.length===0"><td :colspan="10" style="text-align:center;padding:32px;color:var(--color-text-muted)">{{ loading ? '加载中…' : '暂无数据' }}</td></tr>
      <tr v-for="o in list" :key="o.id">
        <td style="color:var(--color-text-muted);font-size:12px">{{ o.order_no }}</td>
        <td>{{ o.name }}</td>
        <td>¥{{ Number(o.amount).toFixed(2) }}</td>
        <td>{{ o.supplier_name }}</td>
        <td><span class="badge" :class="statusClass(o.status)">{{ o.status }}</span></td>
        <td>{{ o.create_time }}</td>
        <td>{{ o.auditor || '—' }}</td>
        <td>{{ o.audit_time || '—' }}</td>
        <td style="font-size:12px;max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" :title="o.opinion">{{ o.opinion || '—' }}</td>
        <td style="display:flex;flex-wrap:wrap;gap:4px;justify-content:center">
          <button class="btn btn-text btn-sm" @click="openHistory(o)">痕迹</button>
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

    <div class="modal-overlay" v-if="showReject" @click.self="showReject=false">
      <div class="modal" style="min-width:440px">
        <div class="modal-title">驳回订单</div>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div style="font-size:13px;color:var(--color-text-secondary)">订单「{{ rejectTarget?.name }}」（{{ rejectTarget?.order_no }}）</div>
          <div>
            <label class="form-label">审核意见 <span style="color:var(--color-danger)">*</span></label>
            <textarea class="form-input" v-model="rejectOpinion" placeholder="请填写驳回原因（必填）" rows="3" style="resize:vertical"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showReject=false">取消</button>
          <button class="btn btn-danger" :disabled="rejecting" @click="confirmReject">{{ rejecting ? '提交中…' : '确认驳回' }}</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showHistory" @click.self="showHistory=false">
      <div class="modal" style="min-width:480px">
        <div class="modal-title">审核痕迹</div>
        <div v-if="historyLoading" style="text-align:center;padding:24px;color:var(--color-text-muted)">加载中…</div>
        <div v-else-if="historyList.length===0" style="text-align:center;padding:24px;color:var(--color-text-muted)">暂无审核记录</div>
        <table v-else class="data-table" style="margin-bottom:0">
          <tr><th>结果</th><th>审核人</th><th>审核时间</th><th>意见</th></tr>
          <tr v-for="h in historyList" :key="h.id">
            <td><span class="badge" :class="h.result==='通过'?'badge-active':'badge-pending'">{{ h.result }}</span></td>
            <td>{{ h.auditor }}</td>
            <td>{{ h.create_time }}</td>
            <td style="font-size:12px;max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" :title="h.opinion">{{ h.opinion || '—' }}</td>
          </tr>
        </table>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="showHistory=false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { getOrderAudits, passAudit, rejectAudit } from '../api/audits.js'
import { getOrderAudits as getOrderHistory } from '../api/orders.js'

export default {
  emits: ['toast'],
  data: () => ({
    fKeyword: '', fStatus: '待审核',
    list: [], total: 0, pendingCount: 0, loading: false, rejecting: false,
    showReject: false, rejectTarget: null, rejectOpinion: '',
    showHistory: false, historyList: [], historyLoading: false,
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
    statusClass(s) { return s === '已完成' ? 'badge-active' : s === '待审核' ? 'badge-pending' : '' },
    buildQuery() {
      return {
        keyword: this.fKeyword || undefined,
        status: this.fStatus || undefined,
        page: this.page,
        pageSize: this.pageSize,
      }
    },
    async load() {
      this.loading = true
      try {
        const data = await getOrderAudits(this.buildQuery())
        this.list = data.list || []
        this.total = data.total || 0
        if (this.list.length && typeof this.list[0].pending_count === 'number') {
          this.pendingCount = this.list[0].pending_count
        } else if (!this.pendingLoaded) {
          this.pendingLoaded = true
          try {
            const p = await getOrderAudits({ status: '待审核', page: 1, pageSize: 1 })
            this.pendingCount = p.total || 0
          } catch { this.pendingCount = 0 }
        }
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载审核列表失败', type: 'error' })
      } finally {
        this.loading = false
      }
    },
    async loadPending() {
      try {
        const p = await getOrderAudits({ status: '待审核', page: 1, pageSize: 1 })
        this.pendingCount = p.total || 0
      } catch { /* ignore */ }
    },
    query() { this.page = 1; this.load() },
    resetQuery() { this.fKeyword = ''; this.fStatus = '待审核'; this.page = 1; this.load() },
    async doPass(o) {
      if (o.status !== '待审核') { this.$emit('toast', { msg: '仅待审核订单可审核', type: 'warning' }); return }
      try {
        await passAudit(o.id, { opinion: '' })
        this.$emit('toast', { msg: '审核通过，订单「' + o.name + '」已完成', type: 'success' })
        this.load()
        this.loadPending()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '审核失败', type: 'error' })
      }
    },
    openReject(o) {
      if (o.status !== '待审核') { this.$emit('toast', { msg: '仅待审核订单可审核', type: 'warning' }); return }
      this.rejectTarget = o
      this.rejectOpinion = ''
      this.showReject = true
    },
    async confirmReject() {
      if (!this.rejectOpinion.trim()) { this.$emit('toast', { msg: '驳回时审核意见必填', type: 'warning' }); return }
      this.rejecting = true
      try {
        await rejectAudit(this.rejectTarget.id, { opinion: this.rejectOpinion.trim() })
        this.$emit('toast', { msg: '已驳回订单「' + this.rejectTarget.name + '」', type: 'success' })
        this.showReject = false
        this.rejectTarget = null
        this.rejectOpinion = ''
        this.load()
        this.loadPending()
      } catch (e) {
        this.$emit('toast', { msg: e.message || '驳回失败', type: 'error' })
      } finally {
        this.rejecting = false
      }
    },
    async openHistory(o) {
      this.showHistory = true
      this.historyLoading = true
      this.historyList = []
      try {
        this.historyList = await getOrderHistory(o.id) || []
      } catch (e) {
        this.$emit('toast', { msg: e.message || '加载审核痕迹失败', type: 'error' })
      } finally {
        this.historyLoading = false
      }
    },
  },
  mounted() {
    this.load()
    this.loadPending()
  },
}
</script>
