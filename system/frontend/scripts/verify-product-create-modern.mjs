import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(process.cwd())
const routerPath = resolve(root, 'src/router/index.js')
const listViewPath = resolve(root, 'src/views/product/ProductModernView.vue')
const createViewPath = resolve(root, 'src/views/product/ProductCreateModernView.vue')
const moduleTabsPath = resolve(root, 'src/views/product/components/ProductModuleTabs.vue')
const stylesPath = resolve(root, 'src/assets/styles.css')

const failures = []
const read = (path) => (existsSync(path) ? readFileSync(path, 'utf8') : '')

const router = read(routerPath)
const listView = read(listViewPath)
const createView = read(createViewPath)
const moduleTabs = read(moduleTabsPath)
const styles = read(stylesPath)

if (!existsSync(createViewPath)) {
  failures.push('ProductCreateModernView.vue is missing')
}

for (const token of [
  "path: '/products-modern/new'",
  "name: 'ProductCreateModern'",
  'ProductCreateModernView',
  "title: '新建商品'"
]) {
  if (!router.includes(token)) failures.push(`router missing ${token}`)
}

for (const token of [
  "router.push('/products-modern/new')",
  'goCreateProduct',
  '新建商品'
]) {
  if (!listView.includes(token)) failures.push(`ProductModernView missing ${token}`)
}

for (const token of [
  "active === 'create'",
  'to="/products-modern/new"',
  '新建商品'
]) {
  if (!moduleTabs.includes(token)) failures.push(`ProductModuleTabs missing ${token}`)
}

for (const token of [
  'product-create-modern-page',
  'ProductModuleTabs',
  'createProduct',
  'createSku',
  'getCategoryTree',
  'productForm',
  'skuRows',
  'skuForm',
  'addSku',
  'editSku',
  'removeSku',
  'submitProduct',
  '商品编号',
  '商品名称',
  '商品分类',
  'SKU列表',
  'SKU设置',
  'SKU名称',
  '规格描述',
  '条码',
  '基础单位',
  '进价',
  '售价'
]) {
  if (!createView.includes(token)) failures.push(`ProductCreateModernView missing ${token}`)
}

for (const forbidden of ['上架与标签', '营销标签', '立即上架', '保存并上架']) {
  if (createView.includes(forbidden)) failures.push(`ProductCreateModernView should not contain ${forbidden}`)
}

for (const token of [
  '.product-create-modern-page',
  '.product-editor-grid',
  '.product-editor-toc',
  '.product-form-card',
  '.sku-editor-panel',
  '.sku-list-table',
  '.product-action-bar'
]) {
  if (!styles.includes(token)) failures.push(`styles missing ${token}`)
}

if (failures.length) {
  console.error('Product create modern verification failed:')
  for (const failure of failures) console.error(`- ${failure}`)
  process.exit(1)
}

console.log('Product create modern verification passed')
