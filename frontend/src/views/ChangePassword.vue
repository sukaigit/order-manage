<template>
  <div>
    <!-- 强制修改密码（独立页面，居中） -->
    <div v-if="isForce" style="display:flex;align-items:center;justify-content:center;min-height:100vh;background:var(--color-bg)">
      <div class="card" style="max-width:460px;width:90%;padding:32px">
        <div style="text-align:center;margin-bottom:24px">
          <img src="/logo.svg?v2" style="width:36px;height:36px;margin-bottom:12px" />
          <div style="font-size:18px;font-weight:600;color:var(--color-text)">订单管理系统</div>
        </div>
        <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px 14px;margin-bottom:20px;display:flex;align-items:center;gap:10px">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="flex-shrink:0">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="M12 8v4"/><path d="M12 16h.01"/>
          </svg>
          <div>
            <div style="font-weight:600;font-size:13px;color:var(--color-primary)">首次登录，请修改密码</div>
            <div style="font-size:12px;color:var(--color-text-muted);margin-top:2px">为了账户安全，请设置一个新密码</div>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:16px">
          <div class="form-group" style="margin:0">
            <label class="form-label">新密码 <span style="color:var(--color-danger)">*</span></label>
            <input class="form-input" type="password" v-model="form.newPwd" @input="checkStrength" placeholder="请输入新密码" />
            <div style="margin-top:8px">
              <div style="display:flex;gap:4px;margin-bottom:4px">
                <div v-for="i in 4" :key="i" :style="{flex:1,height:4,borderRadius:2,background:pwdScore>=i?'var(--color-primary)':'var(--color-border)',opacity:pwdScore>=i?1:0.3,transition:'all 0.2s'}"></div>
              </div>
              <div style="font-size:11px;color:var(--color-text-muted);margin-bottom:4px">8-20位，含大写字母、小写字母、数字、特殊字符中至少3种</div>
              <div style="font-size:11px;display:flex;flex-wrap:wrap;gap:6px">
                <span :style="{color:hasLower?'var(--color-primary)':'var(--color-text-muted)'}">✓ 小写字母</span>
                <span :style="{color:hasUpper?'var(--color-primary)':'var(--color-text-muted)'}">✓ 大写字母</span>
                <span :style="{color:hasDigit?'var(--color-primary)':'var(--color-text-muted)'}">✓ 数字</span>
                <span :style="{color:hasSpecial?'var(--color-primary)':'var(--color-text-muted)'}">✓ 特殊字符</span>
                <span :style="{color:hasLength?'var(--color-primary)':'var(--color-text-muted)'}">✓ 8-20位</span>
              </div>
            </div>
          </div>
          <div class="form-group" style="margin:0">
            <label class="form-label">确认新密码 <span style="color:var(--color-danger)">*</span></label>
            <input class="form-input" type="password" v-model="form.confirmPwd" placeholder="请再次输入新密码" />
            <div v-if="form.confirmPwd && form.newPwd !== form.confirmPwd" style="font-size:11px;color:var(--color-danger);margin-top:4px">两次密码不一致</div>
          </div>
          <button class="btn btn-primary" style="width:100%;justify-content:center;padding:10px 0" @click="submit" :disabled="!!(form.newPwd && !pwdValid) || loading">{{ loading ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 正常修改密码（在系统内，带侧边栏） -->
    <div v-else class="page-body">
      <div class="card" style="max-width:500px;margin-top:24px">
        <div class="form-group">
          <label class="form-label">当前密码</label>
          <input class="form-input" type="password" v-model="form.oldPwd" />
        </div>
        <div class="form-group">
          <label class="form-label">新密码 <span style="color:var(--color-danger)">*</span></label>
          <input class="form-input" type="password" v-model="form.newPwd" @input="checkStrength" />
          <div style="margin-top:6px">
            <div style="display:flex;gap:4px;margin-bottom:4px">
              <div v-for="i in 4" :key="i" :style="{flex:1,height:4,borderRadius:2,background:pwdScore>=i?'var(--color-primary)':'var(--color-border)',opacity:pwdScore>=i?1:0.3,transition:'all 0.2s'}"></div>
            </div>
            <div style="font-size:11px;color:var(--color-text-muted)">8-20位，含大写字母、小写字母、数字、特殊字符中至少3种</div>
            <div style="font-size:11px;margin-top:2px;display:flex;flex-wrap:wrap;gap:6px">
              <span :style="{color:hasLower?'var(--color-primary)':'var(--color-text-muted)'}">✓ 小写字母</span>
              <span :style="{color:hasUpper?'var(--color-primary)':'var(--color-text-muted)'}">✓ 大写字母</span>
              <span :style="{color:hasDigit?'var(--color-primary)':'var(--color-text-muted)'}">✓ 数字</span>
              <span :style="{color:hasSpecial?'var(--color-primary)':'var(--color-text-muted)'}">✓ 特殊字符</span>
              <span :style="{color:hasLength?'var(--color-primary)':'var(--color-text-muted)'}">✓ 8-20位</span>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">确认新密码 <span style="color:var(--color-danger)">*</span></label>
          <input class="form-input" type="password" v-model="form.confirmPwd" />
          <div v-if="form.confirmPwd && form.newPwd !== form.confirmPwd" style="font-size:11px;color:var(--color-danger);margin-top:4px">两次密码不一致</div>
        </div>
        <button class="btn btn-primary" @click="submit" :disabled="!!(form.newPwd && !pwdValid) || loading">{{ loading ? '保存中…' : '保存' }}</button>
      </div>
    </div>
  </div>
</template>
<script>
import { changePassword, forceChangePassword, clearAuth } from '../api/auth.js'

export default {
  emits: ['toast'],
  data: () => ({
    form: { oldPwd: '', newPwd: '', confirmPwd: '' },
    isForce: false,
    loading: false,
    pwdScore: 0,
    hasLower: false, hasUpper: false, hasDigit: false, hasSpecial: false, hasLength: false
  }),
  computed: {
    pwdValid() {
      return this.hasLength && [this.hasLower, this.hasUpper, this.hasDigit, this.hasSpecial].filter(Boolean).length >= 3
    }
  },
  mounted() {
    this.isForce = this.$route.query.force === 'true' || this.$route.path === '/force-password'
  },
  methods: {
    checkStrength() {
      const p = this.form.newPwd
      this.hasLower = /[a-z]/.test(p)
      this.hasUpper = /[A-Z]/.test(p)
      this.hasDigit = /[0-9]/.test(p)
      this.hasSpecial = /[^a-zA-Z0-9]/.test(p)
      this.hasLength = p.length >= 8 && p.length <= 20
      this.pwdScore = [this.hasLower, this.hasUpper, this.hasDigit, this.hasSpecial].filter(Boolean).length
      if (!this.hasLength && p.length > 0) this.pwdScore = 0
    },
    async submit() {
      if (!this.isForce) {
        if (!this.form.oldPwd) { this.$emit('toast', { msg: '请输入当前密码', type: 'warning' }); return }
      }
      if (!this.form.newPwd || !this.form.confirmPwd) { this.$emit('toast', { msg: '请填写新密码', type: 'warning' }); return }
      if (!this.pwdValid) { this.$emit('toast', { msg: '密码不符合安全规则', type: 'warning' }); return }
      if (this.form.newPwd !== this.form.confirmPwd) { this.$emit('toast', { msg: '两次密码不一致', type: 'warning' }); return }
      if (!this.isForce && this.form.oldPwd === this.form.newPwd) { this.$emit('toast', { msg: '新密码不能与当前密码相同', type: 'warning' }); return }
      this.loading = true
      try {
        if (this.isForce) {
          await forceChangePassword({
            new_password: this.form.newPwd,
            confirm_password: this.form.confirmPwd,
          })
        } else {
          await changePassword({
            old_password: this.form.oldPwd,
            new_password: this.form.newPwd,
            confirm_password: this.form.confirmPwd,
          })
        }
        this.$emit('toast', { msg: '密码修改成功' + (this.isForce ? '，请重新登录' : ''), type: 'success' })
        this.form = { oldPwd: '', newPwd: '', confirmPwd: '' }
        if (this.isForce) {
          clearAuth()
          setTimeout(() => this.$router.push('/login'), 1500)
        }
      } catch (e) {
        this.$emit('toast', { msg: e.message || '修改失败', type: 'error' })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
