<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-title">超市库存管理系统</div>
        <div class="brand-subtitle">Inventory Console</div>
      </div>
      <nav class="menu">
        <RouterLink
          v-for="route in visibleMenus"
          :key="route.path"
          class="menu-link"
          :to="route.path"
        >
          <span class="menu-icon">{{ route.meta.icon }}</span>
          <span>{{ route.meta.title }}</span>
        </RouterLink>
      </nav>
    </aside>
    <main class="main-area">
      <header class="topbar">
        <div class="topbar-title">{{ currentTitle }}</div>
        <div class="topbar-actions">
          <span class="user-chip">
            <strong>{{ authStore.username }}</strong>
            <span>{{ roleText }}</span>
          </span>
          <button class="btn btn-ghost" type="button" @click="handleLogout">退出</button>
        </div>
      </header>
      <section class="content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menuRoutes } from '../router'
import { useAuthStore } from '../stores/auth'
import { hasAnyRole } from '../utils/permission'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const visibleMenus = computed(() =>
  menuRoutes.filter((item) => hasAnyRole(authStore.roles, item.meta.roles))
)

const currentTitle = computed(() => route.meta.title || '库存管理后台')
const roleLabels = {
  ADMIN: '管理员',
  USER: '普通用户'
}
const roleText = computed(() =>
  authStore.roles.map((role) => roleLabels[role] || role).join('、') || '未分配角色'
)

async function handleLogout() {
  await authStore.logout()
  router.replace('/login')
}
</script>
