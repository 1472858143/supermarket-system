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
import { reactive, ref, onMounted, onBeforeUnmount } from 'vue'
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
const toastTimer = ref(null)

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
  if (toastTimer.value) clearTimeout(toastTimer.value)
  toastTimer.value = setTimeout(() => { toastVisible.value = false }, 2000)
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

onBeforeUnmount(() => {
  if (toastTimer.value) clearTimeout(toastTimer.value)
})
</script>

<style scoped>
.login-page {
  --bg-deep: #0a1d4a;
  --bg-deeper: #061236;
  --brand: #2f7cff;
  --brand-bright: #4d9bff;
  --brand-glow: #1e63e0;
  --text: #ffffff;
  --text-dim: rgba(255, 255, 255, 0.72);
  --text-mute: rgba(255, 255, 255, 0.50);
  --line: rgba(255, 255, 255, 0.14);
  --line-strong: rgba(255, 255, 255, 0.28);
  --card-bg: rgba(8, 24, 64, 0.55);
  --field-bg: rgba(255, 255, 255, 0.06);
  --field-bg-focus: rgba(255, 255, 255, 0.10);
  --danger: #ff6b6b;

  margin: 0;
  padding: 0;
  min-height: 100vh;
  min-height: 100dvh;
  font-family: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  color: var(--text);
  background: #061236;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  zoom: 0.9;
}

/* ───────── stage ───────── */
.stage {
  position: fixed;
  inset: 0;
  background-color: var(--bg-deeper);
  background-size: cover;
  background-position: left center;
  background-repeat: no-repeat;
}
.stage::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 70% at 80% 50%, rgba(8, 32, 90, 0.55), transparent 70%),
    linear-gradient(90deg, transparent 0%, transparent 45%, rgba(4, 12, 42, 0.35) 70%, rgba(4, 12, 42, 0.55) 100%);
  pointer-events: none;
}

/* ───────── header bar ───────── */
.topbar {
  position: relative;
  z-index: 4;
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}
.topbar .logo {
  width: 38px;
  height: 38px;
  display: block;
  filter: drop-shadow(0 4px 16px rgba(47, 124, 255, 0.55));
}
.topbar .brand-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1.5px;
  line-height: 1;
}
.topbar .brand-en {
  margin-top: 4px;
  font-family: "Orbitron", sans-serif;
  font-size: 10px;
  letter-spacing: 3px;
  color: var(--text-mute);
  text-transform: uppercase;
}
.topbar .brand-block {
  display: flex;
  flex-direction: column;
}

/* ───────── layout ───────── */
.layout {
  position: relative;
  z-index: 3;
  flex: 1 1 auto;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  align-items: center;
  padding: 0 60px;
  gap: 40px;
  min-height: 0;
}

/* ───────── left hero copy ───────── */
.hero {
  align-self: end;
  padding-bottom: 6vh;
  max-width: 520px;
}
.hero .eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 10px;
  border: 1px solid rgba(77, 155, 255, 0.4);
  border-radius: 999px;
  background: rgba(47, 124, 255, 0.10);
  font-size: 12px;
  letter-spacing: 2px;
  color: #b8d3ff;
}
.hero .eyebrow .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4d9bff;
  box-shadow: 0 0 10px #4d9bff;
}
.hero h1 {
  margin: 18px 0 14px;
  font-size: clamp(32px, 3.4vw, 46px);
  line-height: 1.15;
  font-weight: 700;
  letter-spacing: 2px;
  color: #fff;
  text-shadow: 0 2px 24px rgba(8, 32, 90, 0.6);
}
.hero h1 .accent {
  background: linear-gradient(135deg, #4d9bff 10%, #ffffff 90%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero p.lead {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-dim);
  letter-spacing: 0.5px;
  max-width: 460px;
}
.hero .en {
  margin-top: 18px;
  font-family: "Orbitron", sans-serif;
  font-size: 11px;
  letter-spacing: 4px;
  color: rgba(180, 200, 255, 0.55);
  text-transform: uppercase;
}

/* ───────── login card ───────── */
.card-wrap {
  justify-self: end;
  width: 100%;
  max-width: 420px;
  position: relative;
}
.card-wrap::before {
  content: "";
  position: absolute;
  inset: -2px;
  border-radius: 20px;
  background: linear-gradient(140deg, rgba(77, 155, 255, 0.55), rgba(77, 155, 255, 0) 40%, rgba(77, 155, 255, 0) 60%, rgba(77, 155, 255, 0.45));
  filter: blur(0.5px);
  z-index: 0;
  pointer-events: none;
}
.card {
  position: relative;
  z-index: 1;
  padding: 32px 36px 28px;
  border-radius: 18px;
  background: var(--card-bg);
  backdrop-filter: blur(18px) saturate(140%);
  -webkit-backdrop-filter: blur(18px) saturate(140%);
  border: 1px solid var(--line);
  box-shadow:
    0 30px 80px rgba(2, 8, 28, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

/* corner ticks */
.corner {
  position: absolute;
  width: 18px;
  height: 18px;
  border: 1.5px solid var(--brand-bright);
  opacity: 0.85;
}
.corner.tl { top: -1px; left: -1px; border-right: 0; border-bottom: 0; border-top-left-radius: 18px; }
.corner.tr { top: -1px; right: -1px; border-left: 0; border-bottom: 0; border-top-right-radius: 18px; }
.corner.bl { bottom: -1px; left: -1px; border-right: 0; border-top: 0; border-bottom-left-radius: 18px; }
.corner.br { bottom: -1px; right: -1px; border-left: 0; border-top: 0; border-bottom-right-radius: 18px; }

.card h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 4px;
  color: #fff;
}
.card .sub-en {
  margin-top: 6px;
  font-family: "Orbitron", sans-serif;
  font-size: 11px;
  letter-spacing: 3px;
  color: var(--text-mute);
  text-transform: uppercase;
}

/* tabs */
.tabs {
  margin-top: 20px;
  display: flex;
  gap: 24px;
  border-bottom: 1px solid var(--line);
}
.tabs button {
  appearance: none;
  background: none;
  border: 0;
  color: var(--text-mute);
  font: inherit;
  font-size: 14px;
  padding: 8px 0 12px;
  cursor: pointer;
  position: relative;
  letter-spacing: 1.5px;
  transition: color .2s ease;
}
.tabs button.active {
  color: #fff;
  font-weight: 500;
}
.tabs button.active::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--brand-bright), transparent);
}
.tabs button:hover:not(.active) {
  color: var(--text-dim);
}

/* fields */
.fields {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  position: relative;
  display: flex;
  align-items: center;
  height: 44px;
  padding: 0 14px;
  background: var(--field-bg);
  border: 1px solid var(--line);
  border-radius: 10px;
  transition: border-color .2s ease, background .2s ease, box-shadow .2s ease;
}
.field:hover {
  border-color: var(--line-strong);
}
.field.is-focus {
  background: var(--field-bg-focus);
  border-color: var(--brand-bright);
  box-shadow: 0 0 0 3px rgba(77, 155, 255, 0.18);
}
.field .icon {
  width: 18px;
  height: 18px;
  margin-right: 10px;
  color: var(--text-mute);
  flex-shrink: 0;
}
.field.is-focus .icon {
  color: var(--brand-bright);
}
.field input {
  appearance: none;
  background: transparent;
  border: 0;
  outline: 0;
  color: #fff;
  font: inherit;
  font-size: 14.5px;
  letter-spacing: 0.5px;
  width: 100%;
  height: 100%;
}
.field input::placeholder {
  color: var(--text-mute);
  letter-spacing: 0.5px;
}
.field .toggle-eye,
.field .send-code {
  appearance: none;
  background: none;
  border: 0;
  cursor: pointer;
  color: var(--text-mute);
  font: inherit;
  flex-shrink: 0;
}
.field .toggle-eye:hover {
  color: var(--text);
}
.field .send-code {
  color: var(--brand-bright);
  font-size: 13px;
  padding-left: 12px;
  border-left: 1px solid var(--line);
  margin-left: 8px;
  letter-spacing: 0.5px;
}
.field .send-code:disabled {
  color: var(--text-mute);
  cursor: not-allowed;
}

/* captcha */
.field.captcha {
  padding-right: 6px;
}
.captcha-img {
  margin-left: 8px;
  width: 96px;
  height: 36px;
  border-radius: 6px;
  background: #fff;
  color: #1a3578;
  font-family: "Orbitron", sans-serif;
  font-weight: 700;
  font-size: 19px;
  letter-spacing: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  background-image:
    repeating-linear-gradient(35deg, rgba(47, 124, 255, 0.10) 0 2px, transparent 2px 7px),
    repeating-linear-gradient(-35deg, rgba(255, 107, 107, 0.10) 0 2px, transparent 2px 9px);
  flex-shrink: 0;
}

/* options row */
.options {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text-dim);
  user-select: none;
}
.check input {
  display: none;
}
.check .box {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid var(--line-strong);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--field-bg);
  transition: all .2s ease;
}
.check input:checked + .box {
  background: var(--brand);
  border-color: var(--brand);
}
.check .box svg {
  width: 10px;
  height: 10px;
  color: #fff;
  opacity: 0;
  transition: opacity .15s ease;
}
.check input:checked + .box svg {
  opacity: 1;
}
.options a {
  color: var(--brand-bright);
  text-decoration: none;
}
.options a:hover {
  text-decoration: underline;
}

/* submit */
.submit {
  margin-top: 18px;
  width: 100%;
  height: 46px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  font: inherit;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 8px;
  text-indent: 8px;
  cursor: pointer;
  background: linear-gradient(135deg, #4d9bff 0%, #2f7cff 50%, #1e63e0 100%);
  box-shadow:
    0 10px 24px rgba(30, 99, 224, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.25);
  transition: transform .15s ease, box-shadow .2s ease, filter .2s ease;
  position: relative;
  overflow: hidden;
}
.submit:hover {
  filter: brightness(1.07);
  box-shadow: 0 14px 28px rgba(30, 99, 224, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}
.submit:active {
  transform: translateY(1px);
}
.submit::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent 30%, rgba(255, 255, 255, 0.25) 50%, transparent 70%);
  transform: translateX(-100%);
  transition: transform .6s ease;
}
.submit:hover::after {
  transform: translateX(100%);
}
.submit:disabled {
  filter: grayscale(0.4) brightness(0.8);
  cursor: not-allowed;
}

/* alt login */
.alt-login {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.alt-login .divider {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--line-strong), transparent);
}
.alt-login .label {
  font-size: 12px;
  color: var(--text-mute);
  letter-spacing: 1px;
}
.alt-icons {
  margin-top: 16px;
  display: flex;
  gap: 14px;
  justify-content: center;
}
.alt-icons button {
  appearance: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: var(--field-bg);
  color: var(--text-dim);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .2s ease;
}
.alt-icons button:hover {
  border-color: var(--brand-bright);
  color: var(--brand-bright);
  box-shadow: 0 0 0 4px rgba(77, 155, 255, 0.10);
}
.alt-icons svg {
  width: 18px;
  height: 18px;
}

.signup {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--text-mute);
}
.signup a {
  color: var(--brand-bright);
  text-decoration: none;
  margin-left: 4px;
}
.signup a:hover {
  text-decoration: underline;
}

/* error toast inside card */
.error {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(255, 107, 107, 0.10);
  border: 1px solid rgba(255, 107, 107, 0.4);
  border-radius: 8px;
  color: #ffb1b1;
  font-size: 13px;
}

/* footer */
.footer {
  position: relative;
  z-index: 3;
  height: 52px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  font-size: 12px;
  color: var(--text-mute);
  letter-spacing: 1px;
  flex-shrink: 0;
  text-align: center;
}
.footer .sep {
  opacity: 0.4;
}

/* toast */
.toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  padding: 12px 28px;
  background: rgba(8, 24, 64, 0.90);
  border: 1px solid var(--brand-bright);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  letter-spacing: 1px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity .3s ease, transform .3s ease;
}
.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

/* entrance animation */
@keyframes rise {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}
.hero > * {
  animation: rise .7s cubic-bezier(.2, .7, .2, 1) both;
}
.hero .eyebrow { animation-delay: .05s; }
.hero h1 { animation-delay: .15s; }
.hero .lead { animation-delay: .25s; }
.hero .en { animation-delay: .35s; }
.card-wrap {
  animation: rise .8s cubic-bezier(.2, .7, .2, 1) both;
  animation-delay: .15s;
}

/* ───────── responsive ───────── */
@media (max-width: 1366px) {
  .layout { padding: 0 44px; gap: 32px; }
  .hero { padding-bottom: 4vh; }
}

@media (max-width: 1024px) {
  .layout {
    grid-template-columns: 1fr;
    padding: 32px 40px;
    align-items: center;
    justify-items: center;
  }
  .hero { display: none; }
  .card-wrap { justify-self: center; max-width: 440px; }
  .stage { background-position: center center; background-size: cover; }
  .stage::after {
    background:
      radial-gradient(ellipse 80% 60% at 50% 50%, rgba(8, 32, 90, 0.45), transparent 70%),
      linear-gradient(180deg, rgba(4, 12, 42, 0.30), rgba(4, 12, 42, 0.55));
  }
}

@media (max-width: 640px) {
  .topbar { height: 56px; padding: 0 18px; gap: 10px; }
  .topbar .logo { width: 32px; height: 32px; }
  .topbar .brand-name { font-size: 15px; letter-spacing: 1px; }
  .topbar .brand-en { font-size: 9px; letter-spacing: 2px; }

  .layout { padding: 16px 16px 24px; }
  .card-wrap { max-width: 100%; }
  .card { padding: 26px 22px 22px; border-radius: 16px; }
  .card h2 { font-size: 20px; letter-spacing: 3px; }

  .tabs { gap: 20px; }
  .tabs button { font-size: 14px; }

  .field { height: 44px; }
  .submit { height: 46px; font-size: 15px; letter-spacing: 6px; }

  .alt-icons button { width: 38px; height: 38px; }

  .stage {
    background-image:
      radial-gradient(ellipse 90% 60% at 50% 0%, rgba(47, 124, 255, 0.28), transparent 65%),
      radial-gradient(ellipse 70% 50% at 80% 100%, rgba(30, 99, 224, 0.22), transparent 60%),
      linear-gradient(180deg, #061236 0%, #08184a 60%, #061236 100%);
  }
  .stage::after { background: none; }

  .footer {
    height: auto;
    padding: 14px 16px;
    flex-wrap: wrap;
    font-size: 11px;
    letter-spacing: 0.5px;
    gap: 8px;
  }
}

@media (max-height: 720px) and (min-width: 1025px) {
  .layout { padding-top: 16px; padding-bottom: 16px; }
  .hero { padding-bottom: 0; }
}
</style>
