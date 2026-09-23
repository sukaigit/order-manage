<template>
  <div class="login-page">
    <div class="login-card">
      <h1><img src="/logo.svg?v2" style="width:28px;height:28px;vertical-align:middle;margin-right:10px" />订单管理系统</h1>
      <div class="form-group">
        <label class="form-label">用户名</label>
        <input class="form-input" v-model="form.username" placeholder="请输入用户名" @keyup.enter="submit" :disabled="locked" />
      </div>
      <div class="form-group">
        <label class="form-label">密码</label>
        <input class="form-input" type="password" v-model="form.password" placeholder="请输入密码" @keyup.enter="submit" :disabled="locked" />
        <div style="font-size:11px;color:var(--color-text-muted);margin-top:4px">8-20位，含大写字母、小写字母、数字、特殊字符中至少3种</div>
      </div>
      <div class="form-group">
        <label class="form-label">验证码</label>
        <div style="display:flex;gap:10px;align-items:center">
          <input class="form-input" v-model="form.captcha" placeholder="请输入验证码" style="flex:1" maxlength="4" @keyup.enter="submit" :disabled="locked" />
          <img v-if="captchaImage" :src="captchaImage" alt="验证码" style="height:38px;border-radius:6px;cursor:pointer;flex-shrink:0" @click="refreshCaptcha" />
          <div v-else style="width:100px;height:38px;border-radius:6px;background:#f0f5ff;flex-shrink:0;cursor:pointer" @click="refreshCaptcha"></div>
        </div>
      </div>

      <div v-if="errorMsg" style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:10px 14px;font-size:13px;color:#dc2626;margin-bottom:12px;text-align:center">{{ errorMsg }}</div>

      <div v-if="locked" style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:14px;text-align:center;margin-bottom:12px">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:8px">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <div style="font-size:14px;font-weight:600;color:#dc2626;margin-bottom:4px">账户已锁定</div>
        <div style="font-size:12px;color:#9ca3af">密码错误次数过多，请联系系统管理员解锁</div>
      </div>

      <button class="btn btn-primary" style="width:100%;justify-content:center;padding:10px 0" @click="submit" :disabled="locked || loading" :style="{opacity:locked||loading?0.5:1}">{{ loading ? '登录中…' : '登 录' }}</button>
      <div style="text-align:center;font-size:12px;color:var(--color-text-muted);margin-top:16px">忘记密码？请联系系统管理员</div>
    </div>
  </div>
</template>
<script>
import { getCaptcha, login, setToken, setSession } from '../api/auth.js'

export default {
  emits: ['toast'],
  data: () => ({
    form: { username: '', password: '', captcha: '' },
    captchaId: '', captchaImage: '', errorMsg: '', locked: false, loading: false,
  }),
  mounted() { this.refreshCaptcha() },
  methods: {
    async refreshCaptcha() {
      try {
        const data = await getCaptcha()
        this.captchaId = data.captcha_id
        this.captchaImage = data.image
      } catch (e) {
        this.errorMsg = e.message || '获取验证码失败'
      }
    },
    async submit() {
      this.errorMsg = ''
      if (!this.form.username || !this.form.password) {
        this.errorMsg = '请输入用户名和密码'; return
      }
      if (!this.form.captcha) {
        this.errorMsg = '请输入验证码'; return
      }
      this.loading = true
      try {
        const data = await login({
          username: this.form.username,
          password: this.form.password,
          captcha: this.form.captcha,
          captcha_id: this.captchaId,
        })
        setToken(data.token)
        setSession({ user: data.user, menus: data.menus, permissions: data.permissions })
        if (data.first_login) {
          this.$router.push('/force-password?force=true')
        } else {
          this.$router.push('/workbench')
        }
      } catch (e) {
        const msg = e.message || '登录失败'
        this.errorMsg = msg
        if (msg.includes('锁定')) this.locked = true
        this.form.password = ''
        this.form.captcha = ''
        this.refreshCaptcha()
      } finally {
        this.loading = false
      }
    },
  },
}
</script>
