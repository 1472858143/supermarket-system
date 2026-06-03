import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const viewPath = resolve(root, 'src/views/stock/StocksView.vue')
const failures = []
const view = existsSync(viewPath) ? readFileSync(viewPath, 'utf8') : ''

for (const token of [
  'totalQuantity',
  'availableQuantity',
  'lockedQuantity',
  'expiredQuantity',
  'purchaseInboundReceiptBatchId',
  '总库存',
  '可用库存',
  '锁定库存',
  '过期库存'
]) {
  if (!view.includes(token)) failures.push(`StocksView missing ${token}`)
}

if (failures.length) {
  console.error('Stock aggregate view verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Stock aggregate view verification passed')
