# 登录页迁移设计

> 日期：2026-06-01
> 目标：将 `system/pages/登录页.html` 完整迁移至 Vue 3 前端项目，1:1 复刻深蓝科技风 UI，对接后端账号密码登录接口。

---

## 一、设计决策

| 决策点 | 方案 | 理由 |
|---|---|---|
| 视觉风格 | 深蓝色科技风（毛玻璃、渐变、发光边框） | 1:1 复刻 HTML 原始设计 |
| 样式组织 | 纯组件隔离，`<style scoped>` | 登录页风格与后台完全不同，不污染全局 |
| 图片资源 | 复制到 `src/assets/`，通过 import 引用 | 与前端项目资源管理方式一致 |
| Google Fonts | 在 `index.html` 全局加载 | Noto Sans SC 无副作用，Orbitron 仅登录页使用 |
| 验证码 | 前端生成 4 位随机字符 | 后端验证码接口暂未开发，临时方案 |
| 短信登录 | 保留 Tab UI，点击弹"暂未开放" | 1:1 迁移，功能后置 |
| 其他登录方式 | 保留图标按钮，点击弹"暂未开放" | 同上 |
| 开发便利 | 预填 admin/admin123 | 方便开发调试 |

---

## 二、文件变更清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 重写 | `system/frontend/src/views/login/LoginView.vue` | 登录页主组件 |
| 复制 | `system/frontend/src/assets/back_login.png` | 背景图，来源 `system/pages/` |
| 复制 | `system/frontend/src/assets/logo_A.png` | Logo，来源 `system/pages/` |
| 修改 | `system/frontend/index.html` | 添加 Google Fonts `<link>` |

不变的文件：
- `api/auth.js` — 登录 API 已就绪
- `stores/auth.js` — 认证 Store 已就绪
- `router/index.js` — `/login` 路由已配置
- `api/request.js` — axios 拦截器已就绪

---

## 三、组件结构

### 模板层级

```
LoginView.vue
├── div.stage                          // 背景层（CSS background-image）
├── header.topbar                      // 顶部导航栏（Logo + 品牌名）
├── main.layout                        // 两列网格布局
│   ├── section.hero                   // 左侧宣传文案
│   └── section.card-wrap > div.card   // 右侧登录卡片
│       ├── span.corner × 4            // 四角装饰
│       ├── h2 + sub-en                // 标题"欢迎登录"
│       ├── div.tabs                   // Tab 切换（账号登录 / 短信登录）
│       ├── form#form-account          // 账号登录表单
│       │   ├── 用户名输入框（带用户图标 SVG）
│       │   ├── 密码输入框（带锁图标 SVG + 显示/隐藏眼睛）
│       │   ├── 验证码输入框 + 前端生成图片
│       │   ├── 记住账号 checkbox + 忘记密码链接
│       │   ├── 错误提示区
│       │   └── 登录按钮（含 loading 状态）
│       ├── form#form-sms              // 短信登录表单（默认隐藏）
│       ├── div.alt-login              // 分隔线 + "其他登录方式"
│       ├── div.alt-icons              // 微信/钉钉/企业账户按钮
│       └── div.signup                 // "还没有账号？联系管理员开通"
└── footer                             // 页脚版权信息
```

### 响应式状态

```js
// 表单数据
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

// UI 状态
const activeTab = ref('account')     // 'account' | 'sms'
const showPassword = ref(false)       // 密码可见性
const captchaText = ref('')           // 前端生成的 4 位验证码
const captchaInput = ref('')          // 用户输入的验证码
const rememberMe = ref(true)          // 记住账号
const loading = ref(false)            // 登录按钮 loading
const errorMessage = ref('')          // 错误提示信息
const toastMessage = ref('')          // Toast 提示文本
const toastVisible = ref(false)       // Toast 显隐
```

### 原始 JS → Vue 映射

| HTML 原始 JS | Vue 实现 |
|-------------|---------|
| `focus/blur` 手动添加 `.is-focus` class | `@focus`/`@blur` 绑定 ref，`:class` 条件渲染 |
| `.toggle-eye` 点击切换 `input.type` | `:type="showPassword ? 'text' : 'password'"` |
| `Math.random()` 生成验证码 | `onMounted` 调用 `refreshCaptcha()`，点击刷新 |
| Tab 按钮切换 form 显示 | `v-show="activeTab === 'account'"` |
| SMS 倒计时 | 点击弹"暂未开放" toast |
| 表单 `submit` 事件 | `@submit.prevent="handleLogin"` |

---

## 四、样式策略

### 组织方式

所有样式写入 `<style scoped>`，包含：
1. CSS 变量 — 深蓝主题色系，定义在组件根元素
2. 布局样式 — stage、topbar、grid layout、card
3. 组件样式 — field、tabs、submit、captcha、checkbox
4. 动画 — `@keyframes rise` 入场动画
5. 响应式 — 三个断点：1366px / 1024px / 640px

### 背景图引用

```vue
<script setup>
import bgImage from '@/assets/back_login.png'
import logoImage from '@/assets/logo_A.png'
</script>

<template>
<div class="stage" :style="{ backgroundImage: `url(${bgImage})` }"></div>
<img class="logo" :src="logoImage" alt="logo" />
</template>
```

### 与全局样式的关系

登录页样式完全独立，不引用全局 CSS 变量。两个视觉体系互不干扰：
- 登录页：深蓝科技风，scoped 样式
- 后台页面：浅色简洁风，全局 styles.css

---

## 五、API 对接与登录流程

### 登录流程

```
用户点击"登录"
  │
  ▼
前端校验（从上到下逐项，遇到第一个空字段即停止）
  │
  ├─ username 为空 → errorMessage = '请输入用户名'
  ├─ password 为空 → errorMessage = '请输入密码'
  ├─ captchaInput 为空 → errorMessage = '请输入验证码'
  ├─ captchaInput !== captchaText → errorMessage = '验证码错误'，刷新验证码
  │
  ▼ 全部通过
调用 authStore.login({ username, password })
  │
  ├─ 成功 → router.replace(redirect || '/dashboard')
  │
  └─ 失败 → 显示后端错误信息，刷新验证码
```

### 验证码逻辑（前端临时方案）

```js
const CHARS = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

function refreshCaptcha() {
  let s = ''
  for (let i = 0; i < 4; i++) {
    s += CHARS[Math.floor(Math.random() * CHARS.length)]
  }
  captchaText.value = s
}
```

- `onMounted` 时调用一次
- 点击验证码图片刷新
- 登录失败后自动刷新
- 比对忽略大小写

### 错误处理

| 场景 | errorMessage |
|------|-------------|
| 用户名为空 | 请输入用户名 |
| 密码为空 | 请输入密码 |
| 验证码为空 | 请输入验证码 |
| 验证码错误 | 验证码错误 |
| 后端 401 | 后端 message（如"用户名或密码错误"） |
| 后端 403 | 用户已被禁用 |
| 网络异常 | 网络错误，请稍后重试 |

---

## 六、非功能区域处理

### 短信登录 Tab

- 保留 Tab UI，点击切换 `activeTab`
- 短信登录表单保留完整 UI
- 点击"获取验证码"或"登录"按钮 → `showToast('暂未开放')`

### 其他登录方式

- 微信、钉钉、企业账户图标按钮保留
- 点击任一 → `showToast('暂未开放')`

### 忘记密码

- 链接保留，点击 → `showToast('暂未开放')`

### Toast 提示

```js
function showToast(msg) {
  toastMessage.value = msg
  toastVisible.value = true
  setTimeout(() => { toastVisible.value = false }, 2000)
}
```

模板中固定定位 toast 元素，`v-show` 控制显隐，CSS 过渡动画。

### 页脚

原样迁移：`© 2026 超市库存管理系统 | 技术支持：智零科技 | 沪 ICP 备 2026000000 号`
