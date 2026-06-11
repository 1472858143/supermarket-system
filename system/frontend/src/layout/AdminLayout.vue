<template>
  <div class="app">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">
          <img :src="logoImage" alt="logo" />
        </div>
        <div class="brand-copy">
          <div class="name">超市库存管理系统</div>
          <div class="en">SMART INVENTORY</div>
        </div>
      </div>
      <nav class="nav">
        <template v-for="item in sidebarItems" :key="item.key">
          <div v-if="item.type === 'group'" class="nav-group" :class="{ 'nav-group-spaced': item.spaced }">
            {{ item.label }}
          </div>
          <RouterLink v-else :to="item.path" :class="{ 'is-active': item.path === activeSidebarPath }">
            <svg
              class="ico"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              v-html="item.icon"
            />
            <span>{{ item.title }}</span>
            <span v-if="item.badge" class="badge">{{ item.badge }}</span>
          </RouterLink>
        </template>
      </nav>
      <div class="side-foot">
        <span class="status"><span class="dot"></span><span>系统正常</span></span>
        <span>v 3.2.1</span>
      </div>
    </aside>
    <div class="main">
      <header class="topbar">
        <div class="crumb">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
          </svg>
          <RouterLink to="/dashboard">主页</RouterLink>
          <span class="sep">/</span>
          <span class="now">{{ currentTitle }}</span>
        </div>
        <div class="global-search" @click.stop>
          <label class="search">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              ref="globalSearchInput"
              v-model="globalKeyword"
              placeholder="全局搜索商品、采购单..."
              @focus="openGlobalSearch"
              @keydown.enter.prevent="submitGlobalSearch"
              @keydown.escape.prevent="closeGlobalSearch"
            />
            <kbd>Ctrl K</kbd>
          </label>
          <div v-if="globalSearchOpen && (trimmedGlobalKeyword || globalSearchLoading)" class="global-search-panel">
            <div class="global-search-head">
              <span>全局搜索</span>
              <button type="button" @click="clearGlobalSearch">清空</button>
            </div>
            <div v-if="globalSearchLoading" class="global-search-state">正在查询...</div>
            <div v-else-if="globalSearchError" class="global-search-state error">{{ globalSearchError }}</div>
            <template v-else>
              <section v-if="globalProductItems.length" class="global-search-section">
                <div class="global-search-section-title">商品</div>
                <button
                  v-for="item in globalProductItems"
                  :key="item.key"
                  class="global-search-row"
                  type="button"
                  @click="selectGlobalSearchItem(item)"
                >
                  <span class="global-search-type">{{ item.typeLabel }}</span>
                  <span class="global-search-copy">
                    <b>{{ item.title }}</b>
                    <small>{{ item.description }}</small>
                  </span>
                </button>
              </section>
              <section v-if="globalPurchaseItems.length" class="global-search-section">
                <div class="global-search-section-title">采购单</div>
                <button
                  v-for="item in globalPurchaseItems"
                  :key="item.key"
                  class="global-search-row"
                  type="button"
                  @click="selectGlobalSearchItem(item)"
                >
                  <span class="global-search-type">{{ item.typeLabel }}</span>
                  <span class="global-search-copy">
                    <b>{{ item.title }}</b>
                    <small>{{ item.description }}</small>
                  </span>
                </button>
              </section>
              <div v-if="!globalSearchItems.length" class="global-search-state">未找到匹配的商品或采购单</div>
              <div class="global-search-actions">
                <button class="global-search-action" type="button" @click="openGlobalSearchTarget('products')">查看全部商品结果</button>
                <button class="global-search-action" type="button" @click="openGlobalSearchTarget('purchases')">查看全部采购单结果</button>
              </div>
            </template>
          </div>
        </div>
        <div class="top-tools">
          <button class="icon-btn" type="button" title="扫码">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7" />
              <rect x="14" y="3" width="7" height="7" />
              <rect x="3" y="14" width="7" height="7" />
              <line x1="14" y1="14" x2="14" y2="21" />
              <line x1="18" y1="14" x2="18" y2="18" />
              <line x1="14" y1="18" x2="18" y2="18" />
              <line x1="21" y1="18" x2="21" y2="21" />
            </svg>
          </button>
          <button class="icon-btn" type="button" title="消息">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
              <path d="M13.73 21a2 2 0 0 1-3.46 0" />
            </svg>
            <span class="pip"></span>
          </button>
          <button class="icon-btn" type="button" title="帮助">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
          </button>
          <div class="divider-v"></div>
          <div class="user-menu-wrapper" @click.stop>
            <button
              class="user"
              type="button"
              title="打开用户菜单"
              aria-haspopup="menu"
              :aria-expanded="String(userMenuOpen)"
              @click.stop="toggleUserMenu"
            >
              <span class="avatar">{{ userInitial }}</span>
              <span class="info">
                <span class="name">{{ authStore.username || '未登录' }}</span>
                <span class="role">{{ roleText }}</span>
              </span>
              <svg
                class="user-menu-chevron"
                :class="{ 'is-open': userMenuOpen }"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>
            <div v-if="userMenuOpen" class="user-menu" role="menu">
              <button class="logout-action" type="button" role="menuitem" @click="handleLogout">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                  <polyline points="16 17 21 12 16 7" />
                  <line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                <span>退出登录</span>
              </button>
            </div>
          </div>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menuRoutes } from '../router'
import { listProducts } from '../api/product'
import { listPurchaseInbounds } from '../api/purchaseInbound'
import { useAuthStore } from '../stores/auth'
import { hasRouteAccess } from '../utils/permission'
import logoImage from '@/assets/logo.svg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const globalKeyword = ref('')
const globalSearchInput = ref(null)
const globalSearchOpen = ref(false)
const globalSearchLoading = ref(false)
const globalSearchError = ref('')
const globalProductResults = ref([])
const globalPurchaseResults = ref([])
const userMenuOpen = ref(false)
let globalSearchTimer = null
let globalSearchToken = 0

const visibleMenus = computed(() =>
  menuRoutes.filter((item) => !item.meta.hideInMenu && hasRouteAccess(authStore.roles, authStore.permissions, item.meta))
)

const navIcons = {
  dashboard: '<rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/>',
  box: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
  home: '<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>',
  cart: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6"/>',
  yen: '<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>',
  bars: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>',
  warehouse: '<path d="M3 21h18"/><path d="M5 21V8l7-4 7 4v13"/><path d="M9 21v-7h6v7"/><path d="M8 10h8"/><path d="M8 14h8"/>',
  truck: '<path d="M20 7h-9"/><path d="M14 17H5"/><circle cx="17" cy="17" r="3"/><circle cx="7" cy="7" r="3"/>',
  users: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  clipboard: '<path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/><line x1="9" y1="12" x2="15" y2="12"/><line x1="9" y1="16" x2="13" y2="16"/>',
  outbound: '<polyline points="7 13 12 8 17 13"/><line x1="12" y1="18" x2="12" y2="8"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>',
  cog: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>'
}

const navMeta = {
  '/dashboard': { icon: 'dashboard' },
  '/users': { icon: 'users' },
  '/products-modern': { icon: 'box' },
  '/product-categories-modern': { icon: 'box' },
  '/suppliers': { icon: 'truck' },
  '/categories': { icon: 'box' },
  '/stocks': { icon: 'home', badge: '12' },
  '/inventory-center': { icon: 'warehouse' },
  '/purchase-inbounds': { icon: 'cart' },
  '/outbounds': { icon: 'outbound' },
  '/stockchecks': { icon: 'clipboard' },
  '/reports': { icon: 'bars' },
  '/system': { icon: 'cog' }
}

const sidebarParentPaths = {
  '/brands': '/products-modern',
  '/categories': '/products-modern',
  '/product-categories-modern': '/products-modern',
  '/product-skus-modern': '/products-modern',
  '/stocks': '/inventory-center',
  '/stockchecks': '/inventory-center'
}

const activeSidebarPath = computed(() => sidebarParentPaths[route.path] || route.path)

const sidebarItems = computed(() => {
  const menus = visibleMenus.value.map((item) => {
    const meta = navMeta[item.path] || {}
    return {
      key: item.path,
      path: item.path,
      title: item.meta.title,
      badge: meta.badge,
      icon: navIcons[meta.icon] || navIcons.box
    }
  })

  const systemPaths = new Set(['/users', '/system'])
  const mainMenus = menus.filter((item) => !systemPaths.has(item.path))
  const systemMenus = menus.filter((item) => systemPaths.has(item.path))
  return [
    { key: 'main-group', type: 'group', label: '主菜单' },
    ...mainMenus,
    { key: 'system-group', type: 'group', label: '系统', spaced: true },
    ...systemMenus
  ]
})

const currentTitle = computed(() => route.meta.title || '库存管理后台')
const roleLabels = {
  ADMIN: '管理员',
  USER: '普通用户'
}
const roleText = computed(() =>
  authStore.roles.map((role) => roleLabels[role] || role).join('、') || '未分配角色'
)
const userInitial = computed(() => (authStore.username || '用').slice(0, 1))
const trimmedGlobalKeyword = computed(() => globalKeyword.value.trim())
const canSearchProducts = computed(() => canAccessRoute('/products-modern'))
const canSearchPurchases = computed(() => canAccessRoute('/purchase-inbounds'))
const globalProductItems = computed(() =>
  globalProductResults.value.map((product) => ({
    key: `product-${product.id}`,
    id: product.id,
    type: 'product',
    typeLabel: '商品',
    path: '/products-modern',
    query: { keyword: trimmedGlobalKeyword.value },
    title: product.productName || product.productCode || '未命名商品',
    description: compactText([product.productCode, product.brandName, product.categoryName])
  }))
)
const globalPurchaseItems = computed(() =>
  globalPurchaseResults.value.map((order) => ({
    key: `purchase-${order.id}`,
    id: order.id,
    type: 'purchase',
    typeLabel: '采购单',
    path: '/purchase-inbounds',
    query: { module: 'orders', keyword: trimmedGlobalKeyword.value },
    title: order.orderNo || `采购单 #${order.id}`,
    description: compactText([order.supplierName, order.supplierCode, purchaseStatusText(order.status)])
  }))
)
const globalSearchItems = computed(() => [...globalProductItems.value, ...globalPurchaseItems.value])

watch(globalKeyword, (value) => {
  window.clearTimeout(globalSearchTimer)
  const keyword = value.trim()
  globalSearchError.value = ''
  if (!keyword) {
    globalProductResults.value = []
    globalPurchaseResults.value = []
    globalSearchLoading.value = false
    globalSearchOpen.value = false
    return
  }
  globalSearchOpen.value = true
  globalSearchTimer = window.setTimeout(() => {
    performGlobalSearch(keyword)
  }, 250)
})

function canAccessRoute(path) {
  const item = menuRoutes.find((routeItem) => routeItem.path === path)
  return !item || hasRouteAccess(authStore.roles, authStore.permissions, item.meta)
}

function compactText(parts) {
  return parts.filter((part) => part !== undefined && part !== null && String(part).trim()).join(' / ') || '-'
}

function purchaseStatusText(status) {
  const labels = {
    DRAFT: '草稿',
    SUBMITTED: '待审批',
    RETURNED: '退回修改',
    APPROVED: '待入库',
    PARTIALLY_INBOUNDED: '部分入库',
    INBOUNDED: '已完成',
    CANCELLED: '已取消',
    CLOSED: '已关闭'
  }
  return labels[status] || status || ''
}

function openGlobalSearch() {
  if (!trimmedGlobalKeyword.value) return
  globalSearchOpen.value = true
  if (!globalSearchItems.value.length && !globalSearchLoading.value) {
    performGlobalSearch(trimmedGlobalKeyword.value)
  }
}

function closeGlobalSearch() {
  globalSearchOpen.value = false
}

function clearGlobalSearch() {
  globalKeyword.value = ''
  globalProductResults.value = []
  globalPurchaseResults.value = []
  globalSearchError.value = ''
  globalSearchLoading.value = false
  globalSearchInput.value?.focus()
}

async function performGlobalSearch(keyword = trimmedGlobalKeyword.value) {
  const token = ++globalSearchToken
  globalSearchLoading.value = true
  globalSearchError.value = ''
  try {
    const [productData, purchaseData] = await Promise.all([
      canSearchProducts.value ? listProducts({ keyword, page: 1, pageSize: 5 }) : Promise.resolve({ items: [] }),
      canSearchPurchases.value ? listPurchaseInbounds({ keyword, page: 1, pageSize: 5 }) : Promise.resolve({ items: [] })
    ])
    if (token !== globalSearchToken) return
    globalProductResults.value = productData.items || []
    globalPurchaseResults.value = purchaseData.items || []
  } catch (error) {
    if (token !== globalSearchToken) return
    globalProductResults.value = []
    globalPurchaseResults.value = []
    globalSearchError.value = error.message || '全局搜索失败'
  } finally {
    if (token === globalSearchToken) globalSearchLoading.value = false
  }
}

function selectGlobalSearchItem(item) {
  closeGlobalSearch()
  router.push({ path: item.path, query: item.query })
}

function submitGlobalSearch() {
  if (!trimmedGlobalKeyword.value) return
  const firstItem = globalSearchItems.value[0]
  if (firstItem) {
    selectGlobalSearchItem(firstItem)
    return
  }
  openGlobalSearchTarget(canSearchProducts.value ? 'products' : 'purchases')
}

function openGlobalSearchTarget(target) {
  const keyword = trimmedGlobalKeyword.value
  if (!keyword) return
  closeGlobalSearch()
  if (target === 'purchases') {
    router.push({ path: '/purchase-inbounds', query: { module: 'orders', keyword } })
    return
  }
  router.push({ path: '/products-modern', query: { keyword } })
}

function handleGlobalSearchShortcut(event) {
  if ((event.ctrlKey || event.metaKey) && event.key?.toLowerCase() === 'k') {
    event.preventDefault()
    globalSearchInput.value?.focus()
    openGlobalSearch()
  }
}

function closeUserMenu() {
  userMenuOpen.value = false
}

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value
}

async function handleLogout() {
  closeUserMenu()
  closeGlobalSearch()
  await authStore.logout()
  router.replace('/login')
}

function closeFloatingPanels() {
  closeUserMenu()
  closeGlobalSearch()
}

onMounted(() => {
  document.addEventListener('click', closeFloatingPanels)
  document.addEventListener('keydown', handleGlobalSearchShortcut)
})

onBeforeUnmount(() => {
  window.clearTimeout(globalSearchTimer)
  document.removeEventListener('click', closeFloatingPanels)
  document.removeEventListener('keydown', handleGlobalSearchShortcut)
})
</script>
