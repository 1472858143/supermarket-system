<template>
  <main class="login-page">
    <section class="login-visual">
      <h1 class="login-title">超市库存管理系统</h1>
      <p class="login-copy">
        围绕商品、库存、入库、出库、盘点和报表形成闭环管理。系统采用前后端分离架构，并基于角色控制菜单、路由和操作按钮。
      </p>
    </section>
    <section class="login-panel">
      <div class="card">
        <div class="page-header">
          <div>
            <h2 class="page-title">登录</h2>
            <p class="page-desc">使用系统账号进入库存管理后台</p>
          </div>
        </div>
        <div v-if="message" class="message message-error">{{ message }}</div>
        <form class="form-grid" @submit.prevent="handleLogin">
          <label class="form-item full">
            <span class="form-label">用户名</span>
            <input v-model.trim="form.username" class="input" autocomplete="username" />
          </label>
          <label class="form-item full">
            <span class="form-label">密码</span>
            <input v-model="form.password" class="input" type="password" autocomplete="current-password" />
          </label>
          <button class="btn btn-primary form-item full" type="submit" :disabled="loading">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>
        <div class="demo-hint">
          默认演示账号：管理员 admin / admin123；普通用户 user / user123。
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const message = ref('')
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

async function handleLogin() {
  message.value = ''
  if (!form.username || !form.password) {
    message.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    await authStore.login(form)
    router.replace(route.query.redirect || '/dashboard')
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}
</script>
