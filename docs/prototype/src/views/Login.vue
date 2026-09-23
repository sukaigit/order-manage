<template>
  <div class="login-page">
    <div class="login-card">
      <h1><img src="/logo.svg?v2" style="width:28px;height:28px;vertical-align:middle;margin-right:10px" />订单管理系统</h1>
      <div class="form-group">
        <label class="form-label">用户名</label>
        <input class="form-input" v-model="form.username" placeholder="请输入用户名" @keyup.enter="login" :disabled="locked" />
      </div>
      <div class="form-group">
        <label class="form-label">密码</label>
        <input class="form-input" type="password" v-model="form.password" placeholder="请输入密码" @keyup.enter="login" :disabled="locked" />
        <div style="font-size:11px;color:var(--color-text-muted);margin-top:4px">8-20位，含大写字母、小写字母、数字、特殊字符中至少3种</div>
      </div>
      <div class="form-group">
        <label class="form-label">验证码</label>
        <div style="display:flex;gap:10px;align-items:center">
          <input class="form-input" v-model="form.captcha" placeholder="请输入验证码" style="flex:1" maxlength="4" @keyup.enter="login" :disabled="locked" />
          <canvas ref="captchaCanvas" width="100" height="38" style="border-radius:6px;cursor:pointer;flex-shrink:0" @click="refreshCaptcha" />
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

      <button class="btn btn-primary" style="width:100%;justify-content:center;padding:10px 0" @click="login" :disabled="locked" :style="{opacity:locked?0.5:1}">登 录</button>
      <div style="text-align:center;font-size:12px;color:var(--color-text-muted);margin-top:16px">忘记密码？请联系系统管理员</div>
    </div>
  </div>
</template>
<script>
export default {
  emits: ['toast'],
  data: () => ({
    form: { username: '', password: '', captcha: '' },
    captchaCode: '', errorMsg: '', locked: false,
    users: [
      { name:'admin', pwd:'Uu888888!', role:'管理员', active:true, locked:false, failCount:0, firstLogin:false },
    ]
  }),
  mounted() { this.refreshCaptcha() },
  methods: {
    refreshCaptcha() {
      const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
      let code = ''
      for (let i = 0; i < 4; i++) code += chars[Math.floor(Math.random() * chars.length)]
      this.captchaCode = code
      this.drawCaptcha(code)
    },
    drawCaptcha(code) {
      const cvs = this.$refs.captchaCanvas
      if (!cvs) return
      const ctx = cvs.getContext('2d'), w = cvs.width, h = cvs.height
      ctx.fillStyle = '#f0f5ff'; ctx.fillRect(0, 0, w, h)
      for (let i = 0; i < 4; i++) {
        ctx.strokeStyle = `rgba(${Math.random()*200},${Math.random()*200},255,0.3)`
        ctx.lineWidth = 1.5; ctx.beginPath()
        ctx.moveTo(Math.random()*w, Math.random()*h); ctx.lineTo(Math.random()*w, Math.random()*h); ctx.stroke()
      }
      for (let i = 0; i < 30; i++) {
        ctx.fillStyle = `rgba(${Math.random()*200},${Math.random()*200},255,0.4)`
        ctx.beginPath(); ctx.arc(Math.random()*w, Math.random()*h, Math.random()*2+1, 0, Math.PI*2); ctx.fill()
      }
      const colors = ['#0066cc','#cc3300','#339933','#9933cc','#cc6600','#006699']
      const sizes = [20,22,18,24,20]
      for (let i = 0; i < code.length; i++) {
        const x = 10 + i * 22, y = 20 + Math.random() * 4
        ctx.font = `bold ${sizes[i%5]}px sans-serif`
        ctx.fillStyle = colors[i%6]; ctx.textBaseline = 'middle'
        ctx.save(); ctx.translate(x, y); ctx.rotate((Math.random()-0.5)*0.4); ctx.fillText(code[i], 0, 0); ctx.restore()
      }
      ctx.strokeStyle = '#d0d5ff'; ctx.lineWidth = 1; ctx.strokeRect(0.5, 0.5, w-1, h-1)
    },
    login() {
      this.errorMsg = ''
      if (!this.form.username || !this.form.password) {
        this.errorMsg = '请输入用户名和密码'; return
      }
      if (!this.form.captcha) {
        this.errorMsg = '请输入验证码'; return
      }
      if (this.form.captcha.toUpperCase() !== this.captchaCode) {
        this.errorMsg = '验证码错误'
        this.form.captcha = ''; this.refreshCaptcha(); return
      }
      const user = this.users.find(u => u.name === this.form.username)
      if (!user) {
        this.errorMsg = '用户名或密码错误'
        this.form.password = ''; this.form.captcha = ''; this.refreshCaptcha(); return
      }
      if (user.locked) {
        this.locked = true; return
      }
      if (!user.active) {
        this.errorMsg = '账户已禁用，请联系系统管理员启用'
        this.form.password = ''; this.form.captcha = ''; this.refreshCaptcha(); return
      }
      if (this.form.password !== user.pwd) {
        user.failCount++
        const remain = 5 - user.failCount
        if (remain <= 0) {
          user.locked = true
          this.locked = true
        } else {
          this.errorMsg = `密码错误，还剩 ${remain} 次机会`
        }
        this.form.password = ''; this.form.captcha = ''; this.refreshCaptcha(); return
      }
      user.failCount = 0
      if (user.firstLogin) {
        this.$router.push('/force-password?force=true')
      } else {
        this.$router.push('/workbench')
      }
    }
  }
}
</script>
