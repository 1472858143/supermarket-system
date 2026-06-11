import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const projectRoot = path.resolve(frontendRoot, '..', '..')
const backendRoot = path.join(projectRoot, 'system/backend')
const sqlRoot = path.join(projectRoot, 'system/sql')
const errors = []

function read(root, file) {
  const fullPath = path.join(root, file)
  if (!existsSync(fullPath)) {
    errors.push(`Missing file: ${file}`)
    return ''
  }
  return readFileSync(fullPath, 'utf8')
}

function expectIncludes(root, file, needle) {
  const content = read(root, file)
  if (!content.includes(needle)) errors.push(`${file} missing "${needle}"`)
}

function expectExcludes(root, file, needle) {
  const content = read(root, file)
  if (content.includes(needle)) errors.push(`${file} should not contain "${needle}"`)
}

expectIncludes(sqlRoot, '13_add_brand.sql', 'CREATE TABLE IF NOT EXISTS brand')
expectIncludes(sqlRoot, '13_add_brand.sql', 'ADD COLUMN brand_id')
expectIncludes(sqlRoot, 'market.sql', 'CREATE TABLE brand')
expectIncludes(sqlRoot, 'market.sql', 'brand_id BIGINT NOT NULL')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/brand/controller/BrandController.java', '@RequestMapping("/api/brands")')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/brand/controller/BrandController.java', '@GetMapping("/options")')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/brand/service/BrandService.java', 'requireEnabledBrand')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/product/dto/ProductRequest.java', 'private Long brandId')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/product/vo/ProductVO.java', 'private String brandName')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/product/service/ProductService.java', 'brandService.requireEnabledBrand')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/product/mapper/ProductMapper.java', 'left join brand b on b.id = p.brand_id')
expectExcludes(backendRoot, 'src/main/java/com/supermarket/inventory/sku/entity/Sku.java', 'brandId')
expectIncludes(frontendRoot, 'src/api/brand.js', "request.get('/brands/options')")
expectIncludes(frontendRoot, 'src/views/brand/BrandsView.vue', '品牌管理')
expectIncludes(frontendRoot, 'src/router/index.js', "path: '/brands'")
expectIncludes(frontendRoot, 'src/views/product/ProductModernView.vue', 'brandOptions')
expectIncludes(frontendRoot, 'src/views/product/components/ProductFilterBar.vue', 'brands')
expectIncludes(frontendRoot, 'src/views/product/components/ProductVisualTable.vue', 'brandName')

if (errors.length) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('Brand management verification passed')
