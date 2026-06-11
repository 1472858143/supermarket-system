import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const layoutPath = resolve(root, 'src/layout/AdminLayout.vue')
const productViewPath = resolve(root, 'src/views/product/ProductModernView.vue')
const purchaseViewPath = resolve(root, 'src/views/purchaseinbound/PurchaseInboundsView.vue')
const stylesPath = resolve(root, 'src/assets/styles.css')

const read = (path) => (existsSync(path) ? readFileSync(path, 'utf8') : '')
const layout = read(layoutPath)
const productView = read(productViewPath)
const purchaseView = read(purchaseViewPath)
const styles = read(stylesPath)

const failures = []

for (const token of [
  "import { listProducts } from '../api/product'",
  "import { listPurchaseInbounds } from '../api/purchaseInbound'",
  'class="global-search"',
  'globalSearchItems',
  'performGlobalSearch',
  'openGlobalSearchTarget',
  "path: '/products-modern'",
  "path: '/purchase-inbounds'"
]) {
  if (!layout.includes(token)) failures.push(`AdminLayout missing ${token}`)
}

if (!/listProducts\(\{\s*keyword[\s\S]*pageSize:\s*5/.test(layout)) {
  failures.push('AdminLayout should request the first 5 matching products')
}
if (!/listPurchaseInbounds\(\{\s*keyword[\s\S]*pageSize:\s*5/.test(layout)) {
  failures.push('AdminLayout should request the first 5 matching purchase orders')
}
if (!/router\.push\(\{\s*path:\s*'\/products-modern'[\s\S]*keyword/.test(layout)) {
  failures.push('AdminLayout should route product search with keyword query')
}
if (!/router\.push\(\{\s*path:\s*'\/purchase-inbounds'[\s\S]*module:\s*'orders'[\s\S]*keyword/.test(layout)) {
  failures.push('AdminLayout should route purchase search with orders module and keyword query')
}

for (const token of [
  "keyword: route.query.keyword",
  'watch(',
  'applyRouteKeyword',
  'clearSelection()'
]) {
  if (!productView.includes(token)) failures.push(`ProductModernView missing ${token}`)
}

for (const token of [
  "keyword: route.query.keyword",
  'module: route.query.module',
  'applyRouteSearch',
  "activeModule.value = module === 'orders' ? 'orders' : 'inbound'",
  'watch('
]) {
  if (!purchaseView.includes(token)) failures.push(`PurchaseInboundsView missing ${token}`)
}

for (const token of [
  '.global-search {',
  '.global-search-panel',
  '.global-search-row',
  '.global-search-action'
]) {
  if (!styles.includes(token)) failures.push(`styles missing ${token}`)
}

if (failures.length) {
  console.error('Headbar global search verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Headbar global search verification passed')
