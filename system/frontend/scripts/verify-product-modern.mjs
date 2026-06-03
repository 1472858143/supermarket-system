import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const routerPath = resolve(root, 'src/router/index.js')
const viewPath = resolve(root, 'src/views/product/ProductModernView.vue')
const componentsDir = resolve(root, 'src/views/product/components')
const stylesPath = resolve(root, 'src/assets/styles.css')

const failures = []
const read = (path) => (existsSync(path) ? readFileSync(path, 'utf8') : '')

const router = read(routerPath)
const view = read(viewPath)
const styles = read(stylesPath)
const componentFiles = [
  'ProductModuleTabs.vue',
  'ProductFilterBar.vue',
  'ProductStatusTabs.vue',
  'ProductVisualTable.vue',
  'ProductBulkBar.vue'
]

if (!existsSync(viewPath)) {
  failures.push('ProductModernView.vue is missing')
}

for (const file of componentFiles) {
  if (!existsSync(resolve(componentsDir, file))) {
    failures.push(`${file} is missing`)
  }
}

for (const token of [
  "path: '/products-modern'",
  "name: 'ProductModern'",
  'ProductModernView',
  "title: '商品管理新版'"
]) {
  if (!router.includes(token)) failures.push(`router missing ${token}`)
}

for (const token of [
  'product-modern-page',
  'ProductModuleTabs',
  'ProductFilterBar',
  'ProductVisualTable',
  'ProductBulkBar',
  'listProducts',
  'getCategoryTree',
  'listStocks'
]) {
  if (!view.includes(token)) failures.push(`view missing ${token}`)
}

const visualTable = read(resolve(componentsDir, 'ProductVisualTable.vue'))
if (!visualTable.includes('ProductStatusTabs')) {
  failures.push('ProductVisualTable missing ProductStatusTabs')
}

for (const token of [
  '.product-modern-page',
  '.modern-filter-bar',
  '.modern-status-tabs',
  '.modern-product-table',
  '.modern-bulk-bar',
  '.modern-margin-ring'
]) {
  if (!styles.includes(token)) failures.push(`styles missing ${token}`)
}

if (failures.length) {
  console.error('Product modern verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Product modern verification passed')
