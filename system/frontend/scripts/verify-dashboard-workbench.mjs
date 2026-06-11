import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const viewPath = resolve(root, 'src/views/dashboard/DashboardView.vue')

const failures = []
const view = existsSync(viewPath) ? readFileSync(viewPath, 'utf8') : ''

if (!existsSync(viewPath)) {
  failures.push('DashboardView.vue is missing')
}

for (const token of [
  'dashboard-workbench-page',
  'page-head',
  'quick-actions',
  'class="kpi-row dashboard-kpi-row"',
  'class="kpi dashboard-kpi-card"',
  'class="icon-wrap"',
  'class="delta"',
  'dashboard-grid',
  'dashboard-trend-bars',
  'dashboard-warning-list',
  'dashboard-todo-list'
]) {
  if (!view.includes(token)) failures.push(`DashboardView missing ${token}`)
}

for (const token of [
  'getStockReport',
  'getInboundReport',
  'getOutboundReport',
  'getWarningReport',
  'stock.value = stockData || {}',
  'inbound.value = inboundData || {}',
  'outbound.value = outboundData || {}',
  'warnings.value = warningData || []'
]) {
  if (!view.includes(token)) failures.push(`DashboardView should keep existing data source token ${token}`)
}

for (const token of [
  "go('/inventory-center')",
  "go('/purchase-inbounds')",
  "go('/reports')"
]) {
  if (!view.includes(token)) failures.push(`DashboardView missing quick action ${token}`)
}

for (const token of [
  'grid-cards',
  'metric-card',
  'BaseTable'
]) {
  if (view.includes(token)) failures.push(`DashboardView should remove old homepage token ${token}`)
}

if (failures.length) {
  console.error('Dashboard workbench verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Dashboard workbench verification passed')
