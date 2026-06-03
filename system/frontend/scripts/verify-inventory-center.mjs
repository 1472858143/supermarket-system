import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const routerPath = resolve(root, 'src/router/index.js')
const layoutPath = resolve(root, 'src/layout/AdminLayout.vue')
const viewPath = resolve(root, 'src/views/inventory/InventoryCenterView.vue')
const stylesPath = resolve(root, 'src/assets/styles.css')

const failures = []
const read = (path) => (existsSync(path) ? readFileSync(path, 'utf8') : '')

const router = read(routerPath)
const layout = read(layoutPath)
const view = read(viewPath)
const styles = read(stylesPath)

if (!existsSync(viewPath)) {
  failures.push('InventoryCenterView.vue is missing')
}

for (const token of [
  "path: '/inventory-center'",
  "name: 'InventoryCenter'",
  'InventoryCenterView',
  "title: '库存中心'"
]) {
  if (!router.includes(token)) failures.push(`router missing ${token}`)
}

for (const token of [
  "'/inventory-center'",
  "icon: 'warehouse'"
]) {
  if (!layout.includes(token)) failures.push(`AdminLayout missing ${token}`)
}

for (const token of [
  'inventory-center-page',
  'listStocks',
  'listStockBatches',
  'updateStockLimit',
  'InventoryBatchDialog',
  'filteredRows',
  'statusTabs',
  'kpiCards',
  '库存中心',
  '实时库存',
  '维护上下限'
]) {
  if (!view.includes(token)) failures.push(`InventoryCenterView missing ${token}`)
}

for (const token of [
  '.inventory-center-page',
  '.inventory-kpi-row',
  '.inventory-filter-bar',
  '.inventory-status-tabs',
  '.inventory-stock-table',
  '.inventory-batch-summary'
]) {
  if (!styles.includes(token)) failures.push(`styles missing ${token}`)
}

if (failures.length) {
  console.error('Inventory center verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Inventory center verification passed')
