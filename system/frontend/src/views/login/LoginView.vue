<template>
  <div class="login-page">
    <!-- 背景层 -->
    <div class="stage" :style="{ backgroundImage: `url(${bgImage})` }"></div>

    <!-- 顶部导航栏 -->
    <header class="topbar">
      <img class="logo" :src="logoImage" alt="logo" />
      <div class="brand-block">
        <span class="brand-name">超市库存管理系统</span>
        <span class="brand-en">SUPERMARKET INVENTORY MANAGEMENT</span>
      </div>
    </header>

    <!-- 主体布局 -->
    <main class="layout">
      <!-- 左侧宣传文案 -->
      <section class="hero">
        <span class="eyebrow"><span class="dot"></span>SMART INVENTORY · V 3.2</span>
        <h1>智慧零售 <br /><span class="accent">库存管理平台</span></h1>
        <p class="lead">实时同步门店库存数据，连接采购、仓储与销售全链路，让每一件商品的流转都清晰可见，让每一次决策都基于数据。</p>
        <div class="en">REAL-TIME · DATA-DRIVEN · ALL-IN-ONE</div>
      </section>

      <!-- 右侧登录卡片 -->
      <section class="card-wrap">
        <div class="card">
          <span class="corner tl"></span>
          <span class="corner tr"></span>
          <span class="corner bl"></span>
          <span class="corner br"></span>

          <h2>欢迎登录</h2>
          <div class="sub-en">WELCOME BACK</div>

          <!-- Tab 切换 -->
          <div class="tabs" role="tablist">
            <button type="button" :class="{ active: activeTab === 'account' }" @click="activeTab = 'account'" role="tab">账号登录</button>
            <button type="button" :class="{ active: activeTab === 'sms' }" @click="switchToSmsTab" role="tab">短信登录</button>
          </div>

          <!-- 账号登录表单 -->
          <form v-show="activeTab === 'account'" class="fields" @submit.prevent="handleLogin" autocomplete="off">
            <!-- 用户名 -->
            <label class="field" :class="{ 'is-focus': focusedField === 'username' }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8"/></svg>
              <input
                type="text"
                v-model.trim="form.username"
                placeholder="请输入账号 / 工号"
                autocomplete="username"
                @focus="focusedField = 'username'"
                @blur="focusedField = ''"
              />
            </label>

            <!-- 密码 -->
            <label class="field" :class="{ 'is-focus': focusedField === 'password' }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg>
              <input
                :type="showPassword ? 'text' : 'password'"
                v-model="form.password"
                placeholder="请输入登录密码"
                autocomplete="current-password"
                @focus="focusedField = 'password'"
                @blur="focusedField = ''"
              />
              <button type="button" class="toggle-eye" @click="showPassword = !showPassword" aria-label="显示密码">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
              </button>
            </label>

            <!-- 验证码 -->
            <label class="field captcha" :class="{ 'is-focus': focusedField === 'captcha' }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 4v5h-5"/></svg>
              <input
                type="text"
                v-model="captchaInput"
                placeholder="请输入验证码"
                maxlength="4"
                @focus="focusedField = 'captcha'"
                @blur="focusedField = ''"
              />
              <span class="captcha-img" @click="refreshCaptcha" title="点击刷新">{{ captchaText }}</span>
            </label>

            <!-- 选项行 -->
            <div class="options">
              <label class="check">
                <input type="checkbox" v-model="rememberMe" />
                <span class="box"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 12 10 18 20 6"/></svg></span>
                <span>记住账号</span>
              </label>
              <a href="#" @click.prevent="showToast('暂未开放')">忘记密码？</a>
            </div>

            <!-- 错误提示 -->
            <div v-if="errorMessage" class="error show">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="13"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <span>{{ errorMessage }}</span>
            </div>

            <!-- 登录按钮 -->
            <button type="submit" class="submit" :disabled="loading">
              {{ loading ? '登 录 中 ...' : '登 录' }}
            </button>
          </form>

          <!-- 短信登录表单 -->
          <form v-show="activeTab === 'sms'" class="fields" @submit.prevent="showToast('暂未开放')" autocomplete="off">
            <label class="field" :class="{ 'is-focus': focusedField === 'phone' }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="2" width="12" height="20" rx="2"/><line x1="11" y1="18" x2="13" y2="18"/></svg>
              <input
                type="tel"
                placeholder="请输入手机号"
                maxlength="11"
                @focus="focusedField = 'phone'"
                @blur="focusedField = ''"
              />
            </label>
            <label class="field" :class="{ 'is-focus': focusedField === 'smsCode' }">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16v16H4z"/><polyline points="4 4 12 13 20 4"/></svg>
              <input
                type="text"
                placeholder="请输入短信验证码"
                maxlength="6"
                @focus="focusedField = 'smsCode'"
                @blur="focusedField = ''"
              />
              <button type="button" class="send-code" @click="showToast('暂未开放')">获取验证码</button>
            </label>

            <div class="options" style="margin-top:24px">
              <label class="check">
                <input type="checkbox" />
                <span class="box"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 12 10 18 20 6"/></svg></span>
                <span>我已阅读并同意 <a href="#" @click.prevent style="color:var(--brand-bright)">服务条款</a></span>
              </label>
            </div>

            <button type="submit" class="submit">登 录</button>
          </form>

          <!-- 其他登录方式 -->
          <div class="alt-login">
            <span class="divider"></span>
            <span class="label">其他登录方式</span>
            <span class="divider"></span>
          </div>
          <div class="alt-icons">
            <button title="微信" type="button" @click="showToast('暂未开放')">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8.7 7.6c.5 0 1 .4 1 1s-.4 1-1 1-1-.4-1-1 .5-1 1-1zm4.5 0c.5 0 1 .4 1 1s-.4 1-1 1-1-.4-1-1 .5-1 1-1zM10.9 4C5.9 4 2 7.3 2 11.4c0 2.3 1.3 4.4 3.4 5.7l-.7 2.4 2.9-1.5c.9.2 1.8.4 2.7.4h.7c-.2-.6-.3-1.2-.3-1.9 0-3.5 3.4-6.4 7.6-6.4h.6C18.4 6.5 15 4 10.9 4zm9 6c-3.6 0-6.6 2.6-6.6 5.6 0 1.9 1.2 3.6 3 4.7L15.7 22l2.4-1.3c.7.2 1.5.3 2.3.3 3.6 0 6.6-2.6 6.6-5.6S23.5 10 19.9 10zm-2.5 3.3c.3 0 .6.3.6.6s-.3.6-.6.6-.6-.3-.6-.6.3-.6.6-.6zm4.5 0c.3 0 .6.3.6.6s-.3.6-.6.6-.6-.3-.6-.6.3-.6.6-.6z"/></svg>
            </button>
            <button title="钉钉" type="button" @click="showToast('暂未开放')">
              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm4.5 9.4l-2.7 5.4c-.2.4-.7.5-1.1.3-.3-.2-.5-.5-.5-.8 0-.2 0-.3.1-.4l1.2-2.6c-2 0-3.6-.7-4.3-2 0-.1.1-.2.2-.1.6.5 2 .9 3.6.6.5-.1 1-.3 1.5-.5l-3.5.4c-.7.1-1.4-.4-1.5-1.1L8 7.1c-.1-.4.2-.7.6-.8l8.4-.5c.5 0 .8.3.8.7v.1l-1.3 5.8z"/></svg>
            </button>
            <button title="企业账户" type="button" @click="showToast('暂未开放')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18M5 21V7l7-4 7 4v14M9 9h.01M9 13h.01M9 17h.01M15 9h.01M15 13h.01M15 17h.01"/></svg>
            </button>
          </div>

          <div class="signup">还没有账号？<a href="#" @click.prevent="showToast('暂未开放')">联系管理员开通</a></div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <span>© 2026 超市库存管理系统</span>
      <span class="sep">|</span>
      <span>技术支持：智零科技</span>
      <span class="sep">|</span>
      <span>沪 ICP 备 2026000000 号</span>
    </footer>

    <!-- Toast 提示 -->
    <transition name="toast-fade">
      <div v-if="toastVisible" class="toast">{{ toastMessage }}</div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import bgImage from '@/assets/back_login.png'
import logoImage from '@/assets/logo_A.png'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

// 表单数据
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

// UI 状态
const activeTab = ref('account')
const showPassword = ref(false)
const captchaText = ref('')
const captchaInput = ref('')
const rememberMe = ref(true)
const loading = ref(false)
const errorMessage = ref('')
const focusedField = ref('')
const toastMessage = ref('')
const toastVisible = ref(false)

// 验证码生成
function refreshCaptcha() {
  let s = ''
  for (let i = 0; i < 4; i++) {
    s += CHARS[Math.floor(Math.random() * CHARS.length)]
  }
  captchaText.value = s
}

// Toast 提示
function showToast(msg) {
  toastMessage.value = msg
  toastVisible.value = true
  setTimeout(() => { toastVisible.value = false }, 2000)
}

// 切换到短信 Tab
function switchToSmsTab() {
  activeTab.value = 'sms'
  showToast('暂未开放')
}

// 登录处理
async function handleLogin() {
  errorMessage.value = ''

  // 从上到下逐项校验
  if (!form.username) {
    errorMessage.value = '请输入用户名'
    return
  }
  if (!form.password) {
    errorMessage.value = '请输入密码'
    return
  }
  if (!captchaInput.value) {
    errorMessage.value = '请输入验证码'
    return
  }
  if (captchaInput.value.toUpperCase() !== captchaText.value) {
    errorMessage.value = '验证码错误'
    refreshCaptcha()
    return
  }

  loading.value = true
  try {
    await authStore.login({ username: form.username, password: form.password })
    router.replace(route.query.redirect || '/dashboard')
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>/* styles will be added in Task 4 */</style>
