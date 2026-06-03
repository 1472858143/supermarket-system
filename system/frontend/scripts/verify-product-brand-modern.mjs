import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const errors = []

function read(relativePath) {
  const fullPath = path.join(root, relativePath)
  if (!existsSync(fullPath)) {
    errors.push(`Missing file: ${relativePath}`)
    return ''
  }
  return readFileSync(fullPath, 'utf8')
}

function expectIncludes(file, needle) {
  const content = read(file)
  if (!content.includes(needle)) {
    errors.push(`${file} missing "${needle}"`)
  }
}

for (const needle of [
  'product-brand-modern-page',
  'ProductModuleTabs',
  'active="brand"',
  'viewMode',
  'brand-grid',
  'brand-list',
  'openBrandModal',
  'brandMetrics',
  'derivedBrands',
  'listBrandOptions',
  'createBrand',
  'updateBrand',
  'deleteBrand'
]) {
  expectIncludes('src/views/brand/BrandsView.vue', needle)
}

expectIncludes('src/router/index.js', "path: '/brands'")
expectIncludes('src/views/product/components/ProductModuleTabs.vue', "active === 'brand'")
expectIncludes('src/views/product/ProductModernView.vue', 'useRoute')
expectIncludes('src/views/product/ProductModernView.vue', 'route.query.brandId')

for (const needle of [
  '.product-brand-modern-page',
  '.brand-grid',
  '.brand-card',
  '.brand-toolbar',
  '.brand-view-switch',
  '.brand-logo',
  '.brand-stats',
  '.brand-list',
  '.brand-modal-preview'
]) {
  expectIncludes('src/assets/styles.css', needle)
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('Product brand modern verification passed')
