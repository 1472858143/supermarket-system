import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { hasRouteAccess } from '../utils/permission'
import AdminLayout from '../layout/AdminLayout.vue'
import LoginLayout from '../layout/LoginLayout.vue'
import LoginView from '../views/login/LoginView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import UsersView from '../views/user/UsersView.vue'
import BrandsView from '../views/brand/BrandsView.vue'
import SuppliersView from '../views/supplier/SuppliersView.vue'
import StocksView from '../views/stock/StocksView.vue'
import PurchaseInboundsView from '../views/purchaseinbound/PurchaseInboundsView.vue'
import OutboundsView from '../views/outbound/OutboundsView.vue'
import StockchecksView from '../views/stockcheck/StockchecksView.vue'
import ReportsView from '../views/report/ReportsView.vue'
import CategoryView from '../views/category/CategoryView.vue'
import SystemView from '../views/system/SystemView.vue'

const ProductModernView = () => import('../views/product/ProductModernView.vue')
const ProductCategoryModernView = () => import('../views/product/ProductCategoryModernView.vue')
const ProductSkuModernView = () => import('../views/product/ProductSkuModernView.vue')
const InventoryCenterView = () => import('../views/inventory/InventoryCenterView.vue')

export const menuRoutes = [
  { path: '/brands', name: 'BrandManage', component: BrandsView, meta: { title: '品牌管理', roles: ['ADMIN', 'USER'], permissions: ['brand:view'], icon: 'B', hideInMenu: true } },
  { path: '/dashboard', name: 'Dashboard', component: DashboardView, meta: { title: '首页概览', roles: ['ADMIN', 'USER'], permissions: ['dashboard:view'], icon: '□' } },
  { path: '/users', name: 'UserManage', component: UsersView, meta: { title: '用户与权限', roles: ['ADMIN'], icon: 'U' } },
  { path: '/products-modern', name: 'ProductModern', component: ProductModernView, meta: { title: '商品中心', roles: ['ADMIN', 'USER'], permissions: ['product:view'], icon: 'P' } },
  { path: '/product-categories-modern', name: 'ProductCategoryModern', component: ProductCategoryModernView, meta: { title: '商品分类新版', roles: ['ADMIN', 'USER'], permissions: ['category:view'], icon: 'G', hideInMenu: true } },
  { path: '/product-skus-modern', name: 'ProductSkuModern', component: ProductSkuModernView, meta: { title: 'SKU 管理', roles: ['ADMIN', 'USER'], permissions: ['product:view'], icon: 'P', hideInMenu: true } },
  { path: '/categories', name: 'CategoryManage', component: CategoryView, meta: { title: '分类管理', roles: ['ADMIN', 'USER'], permissions: ['category:view'], icon: 'G', hideInMenu: true } },
  { path: '/stocks', name: 'StockManage', component: StocksView, meta: { title: '库存管理', roles: ['ADMIN', 'USER'], permissions: ['stock:view'], icon: 'S', hideInMenu: true } },
  { path: '/inventory-center', name: 'InventoryCenter', component: InventoryCenterView, meta: { title: '库存中心', roles: ['ADMIN', 'USER'], permissions: ['inventory:view'], icon: 'W' } },
  { path: '/purchase-inbounds', name: 'PurchaseInboundManage', component: PurchaseInboundsView, meta: { title: '采购入库', roles: ['ADMIN', 'USER'], permissions: ['purchase:view'], icon: '$' } },
  { path: '/outbounds', name: 'OutboundManage', component: OutboundsView, meta: { title: '出库管理', roles: ['ADMIN', 'USER'], permissions: ['outbound:view'], icon: '-' } },
  { path: '/stockchecks', name: 'StockcheckManage', component: StockchecksView, meta: { title: '盘点管理', roles: ['ADMIN', 'USER'], permissions: ['stockcheck:view'], icon: 'C', hideInMenu: true } },
  { path: '/reports', name: 'ReportManage', component: ReportsView, meta: { title: '报表统计', roles: ['ADMIN', 'USER'], permissions: ['report:view'], icon: 'R' } },
  { path: '/suppliers', name: 'SupplierManage', component: SuppliersView, meta: { title: '供应商', roles: ['ADMIN', 'USER'], permissions: ['supplier:view'], icon: 'V' } },
  { path: '/system', name: 'SystemInfo', component: SystemView, meta: { title: '系统信息', roles: ['ADMIN', 'USER'], permissions: ['system:view'], icon: 'I' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: LoginLayout,
      children: [
        { path: '', name: 'Login', component: LoginView, meta: { public: true } }
      ]
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: menuRoutes
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!hasRouteAccess(authStore.roles, authStore.permissions, to.meta)) {
    const fallback = menuRoutes.find((item) => hasRouteAccess(authStore.roles, authStore.permissions, item.meta))
    return fallback?.path || false
  }
  return true
})

export default router
