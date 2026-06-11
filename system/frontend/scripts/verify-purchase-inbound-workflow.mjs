import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const apiPath = resolve(root, 'src/api/purchaseInbound.js')
const viewPath = resolve(root, 'src/views/purchaseinbound/PurchaseInboundsView.vue')
const statusTagPath = resolve(root, 'src/components/StatusTag.vue')

const failures = []
const read = (path) => (existsSync(path) ? readFileSync(path, 'utf8') : '')

const api = read(apiPath)
const view = read(viewPath)
const statusTag = read(statusTagPath)
const orderTableStart = view.indexOf('v-if="activeModule === \'orders\'"')
const orderTableEnd = view.indexOf('v-else-if="viewMode === \'board\'"', orderTableStart)
const orderTableBlock = orderTableStart >= 0 && orderTableEnd > orderTableStart
  ? view.slice(orderTableStart, orderTableEnd)
  : ''
const inboundActionBlockStart = view.indexOf('v-else-if="viewMode === \'board\'"')
const inboundActionBlock = inboundActionBlockStart >= 0 ? view.slice(inboundActionBlockStart) : ''

for (const token of [
  'createPurchaseInboundDraft',
  'updatePurchaseInboundPlan',
  'submitPurchaseInbound',
  'approvePurchaseInbound',
  'returnPurchaseInbound',
  'cancelPurchaseInbound',
  'closePurchaseInbound',
  'receivePurchaseInbound'
]) {
  if (!api.includes(token)) failures.push(`purchaseInbound API missing ${token}`)
}

for (const token of [
  'canEditPlan',
  'canReceive',
  'canClose',
  'activeModule',
  'setActiveModule',
  'orderStatusTabs',
  'remainingBaseQuantity',
  'plannedBaseQuantity',
  'inboundedBaseQuantity',
  'approvalLogs',
  'receipts'
]) {
  if (!view.includes(token)) failures.push(`PurchaseInboundsView missing ${token}`)
}

if (!view.includes('class="kpi-row purchase-kpi-row"')) {
  failures.push('PurchaseInboundsView KPI row should use prototype kpi-row class')
}
if (!view.includes('class="kpi purchase-kpi-card"')) {
  failures.push('PurchaseInboundsView KPI cards should use prototype kpi class')
}
if (!view.includes('class="icon-wrap"')) {
  failures.push('PurchaseInboundsView KPI cards should include prototype icon-wrap')
}
if (!view.includes('class="delta"')) {
  failures.push('PurchaseInboundsView KPI cards should include prototype delta badge')
}
const purchaseSubTabsIndex = view.indexOf('class="sub-tabs"')
const purchaseKpiIndex = view.indexOf('class="kpi-row purchase-kpi-row"')
if (purchaseSubTabsIndex < 0 || purchaseKpiIndex < 0 || purchaseSubTabsIndex > purchaseKpiIndex) {
  failures.push('PurchaseInboundsView sub-tabs should appear before KPI cards')
}
if (view.includes('class="purchase-kpi"') || /\.purchase-kpi\s*\{/.test(view)) {
  failures.push('PurchaseInboundsView should not keep the old purchase-kpi card style')
}
if (view.includes('采购订单列表尚未接入独立页面')) {
  failures.push('PurchaseInboundsView purchase order sub-tab should be connected, not a placeholder toast')
}
if (!view.includes("setActiveModule('orders')")) {
  failures.push('PurchaseInboundsView purchase order sub-tab should switch to the orders module')
}
if (!view.includes("'SUBMITTED'") || !view.includes("'DRAFT'")) {
  failures.push('PurchaseInboundsView order module should expose existing purchase workflow statuses')
}
if (!orderTableBlock) {
  failures.push('PurchaseInboundsView should render a dedicated purchase order table block')
}
if (/canReceive\(item\)|openReceiptDialog\(item\)|确认入库/.test(orderTableBlock)) {
  failures.push('PurchaseInboundsView order module should not show receipt actions')
}
if (!/canReceive\(item\)[\s\S]*openReceiptDialog\(item\)/.test(inboundActionBlock)) {
  failures.push('PurchaseInboundsView inbound module should keep receipt actions')
}

for (const token of [
  'PARTIALLY_INBOUNDED',
  'INBOUNDED',
  'RETURNED',
  '已审批',
  '部分入库'
]) {
  if (!statusTag.includes(token) && !view.includes(token)) failures.push(`status display missing ${token}`)
}

if (failures.length) {
  console.error('Purchase inbound workflow verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Purchase inbound workflow verification passed')
