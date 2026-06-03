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
  'remainingBaseQuantity',
  'plannedBaseQuantity',
  'inboundedBaseQuantity',
  'approvalLogs',
  'receipts'
]) {
  if (!view.includes(token)) failures.push(`PurchaseInboundsView missing ${token}`)
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
