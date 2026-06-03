import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { hasAnyRole } from '../utils/permission'
import AdminLayout from '../layout/AdminLayout.vue'
import LoginLayout from '../layout/LoginLayout.vue'
import LoginView from '../views/login/LoginView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import UsersView from '../views/user/UsersView.vue'
import ProductsView from '../views/product/ProductsView.vue'
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
const ProductCreateModernView = () => import('../views/product/ProductCreateModernView.vue')
const ProductCategoryModernView = () => import('../views/product/ProductCategoryModernView.vue')
const InventoryCenterView = () => import('../views/inventory/InventoryCenterView.vue')

export const menuRoutes = [
  { path: '/brands', name: 'BrandManage', component: BrandsView, meta: { title: '品牌管理', roles: ['ADMIN', 'USER'], icon: 'B' } },
  { path: '/dashboard', name: 'Dashboard', component: DashboardView, meta: { title: '首页概览', roles: ['ADMIN', 'USER'], icon: '□' } },
  { path: '/users', name: 'UserManage', component: UsersView, meta: { title: '用户管理', roles: ['ADMIN'], icon: 'U' } },
  { path: '/products', name: 'ProductManage', component: ProductsView, meta: { title: '商品管理', roles: ['ADMIN', 'USER'], icon: 'P' } },
  { path: '/products-modern', name: 'ProductModern', component: ProductModernView, meta: { title: '商品管理新版', roles: ['ADMIN', 'USER'], icon: 'P' } },
  { path: '/products-modern/new', name: 'ProductCreateModern', component: ProductCreateModernView, meta: { title: '新建商品', roles: ['ADMIN'], icon: 'P', hideInMenu: true } },
  { path: '/product-categories-modern', name: 'ProductCategoryModern', component: ProductCategoryModernView, meta: { title: '商品分类新版', roles: ['ADMIN', 'USER'], icon: 'G', hideInMenu: true } },
  { path: '/suppliers', name: 'SupplierManage', component: SuppliersView, meta: { title: '供应商管理', roles: ['ADMIN', 'USER'], icon: 'V' } },
  { path: '/categories', name: 'CategoryManage', component: CategoryView, meta: { title: '分类管理', roles: ['ADMIN', 'USER'], icon: 'G', hideInMenu: true } },
  { path: '/stocks', name: 'StockManage', component: StocksView, meta: { title: '库存管理', roles: ['ADMIN', 'USER'], icon: 'S' } },
  { path: '/inventory-center', name: 'InventoryCenter', component: InventoryCenterView, meta: { title: '库存中心', roles: ['ADMIN', 'USER'], icon: 'W' } },
  { path: '/purchase-inbounds', name: 'PurchaseInboundManage', component: PurchaseInboundsView, meta: { title: '采购入库', roles: ['ADMIN', 'USER'], icon: '$' } },
  { path: '/outbounds', name: 'OutboundManage', component: OutboundsView, meta: { title: '出库管理', roles: ['ADMIN', 'USER'], icon: '-' } },
  { path: '/stockchecks', name: 'StockcheckManage', component: StockchecksView, meta: { title: '盘点管理', roles: ['ADMIN', 'USER'], icon: 'C' } },
  { path: '/reports', name: 'ReportManage', component: ReportsView, meta: { title: '报表统计', roles: ['ADMIN', 'USER'], icon: 'R' } },
  { path: '/system', name: 'SystemInfo', component: SystemView, meta: { title: '系统信息', roles: ['ADMIN', 'USER'], icon: 'I' } }
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
  if (!hasAnyRole(authStore.roles, to.meta.roles)) {
    return '/dashboard'
  }
  return true
})

export default router
