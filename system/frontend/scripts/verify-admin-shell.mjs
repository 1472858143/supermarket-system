import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const layout = readFileSync(resolve(root, 'src/layout/AdminLayout.vue'), 'utf8')
const styles = readFileSync(resolve(root, 'src/assets/styles.css'), 'utf8')
const router = readFileSync(resolve(root, 'src/router/index.js'), 'utf8')
const reportRouteIndex = router.indexOf("path: '/reports'")
const supplierRouteIndex = router.indexOf("path: '/suppliers'")
const routeLine = (path) => router.split('\n').find((line) => line.includes(`path: '${path}'`)) || ''
const stocksRouteLine = routeLine('/stocks')
const purchaseInboundHasSidebarBadge = /['"]\/purchase-inbounds['"]:\s*\{[^}]*badge\s*:/.test(layout)

const checks = [
  ['AdminLayout uses migrated app shell', layout.includes('class="app"')],
  ['AdminLayout renders shared sidebar navigation', layout.includes('class="nav"')],
  ['AdminLayout renders migrated topbar search', layout.includes('class="search"')],
  ['AdminLayout renders topbar tools', layout.includes('class="top-tools"')],
  ['AdminLayout renders breadcrumb area', layout.includes('class="crumb"')],
  ['AdminLayout wraps sidebar logo in brand mark', layout.includes('class="brand-mark"')],
  ['AdminLayout wraps sidebar title in brand copy', layout.includes('class="brand-copy"')],
  ['styles define dark sidebar width token', styles.includes('--side-w')],
  ['styles define migrated sidebar', styles.includes('.sidebar {')],
  ['styles define refined brand mark', styles.includes('.brand-mark {')],
  ['styles define refined brand copy', styles.includes('.brand-copy {')],
  ['styles define migrated nav item active state', styles.includes('.nav a.router-link-active')],
  ['styles define migrated topbar', styles.includes('.topbar {')],
  ['styles define migrated search control', styles.includes('.search {')],
  ['styles define responsive collapsed sidebar', styles.includes('--side-w-mini')],
  ['styles define admin shell zoom token', styles.includes('--admin-zoom: 0.85')],
  ['styles apply admin shell zoom', styles.includes('zoom: var(--admin-zoom)')],
  ['styles compensate zoomed viewport height', styles.includes('calc(100vh / var(--admin-zoom))')],
  ['styles define sidebar scrollbar color', styles.includes('scrollbar-color: rgba(77, 155, 255, 0.42) transparent')],
  ['styles define webkit sidebar scrollbar thumb', styles.includes('.nav::-webkit-scrollbar-thumb')],
  ['legacy category route is hidden from sidebar', /path:\s*'\/categories'[\s\S]*?hideInMenu:\s*true/.test(router)],
  ['legacy stock management route is hidden from sidebar', /hideInMenu:\s*true/.test(stocksRouteLine)],
  ['purchase inbound sidebar badge is removed', !purchaseInboundHasSidebarBadge],
  ['supplier menu title is shortened to supplier', /path:\s*'\/suppliers'[\s\S]*?title:\s*'供应商'/.test(router)],
  ['supplier menu route appears after report statistics', reportRouteIndex >= 0 && supplierRouteIndex > reportRouteIndex]
]

const failures = checks.filter(([, pass]) => !pass)

if (failures.length) {
  console.error('Admin shell verification failed:')
  for (const [name] of failures) {
    console.error(`- ${name}`)
  }
  process.exit(1)
}

console.log('Admin shell verification passed')
