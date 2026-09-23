<template>
  <div>
    <router-view @toast="addToast" />
    <div class="toast-container">
      <div v-for="t in toasts" :key="t.id" class="toast" :class="'toast-' + t.type">{{ t.msg }}</div>
    </div>
  </div>
</template>
<script>
export default {
  data: () => ({ toasts: [] }),
  methods: {
    addToast(t) {
      const id = Date.now()
      this.toasts.push({ ...t, id })
      setTimeout(() => { this.toasts = this.toasts.filter(x => x.id !== id) }, 2500)
    }
  }
}
</script>
