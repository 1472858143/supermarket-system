import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const projectRoot = path.resolve(frontendRoot, '..', '..')
const backendRoot = path.join(projectRoot, 'system/backend')

const errors = []

function readFrom(root, relativePath) {
  const fullPath = path.join(root, relativePath)
  if (!existsSync(fullPath)) {
    errors.push(`Missing file: ${relativePath}`)
    return ''
  }
  return readFileSync(fullPath, 'utf8')
}

function expectIncludes(root, file, needle) {
  const content = readFrom(root, file)
  if (!content.includes(needle)) {
    errors.push(`${file} missing "${needle}"`)
  }
}

function expectExcludes(root, file, needle) {
  const content = readFrom(root, file)
  if (content.includes(needle)) {
    errors.push(`${file} should not contain "${needle}"`)
  }
}

expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/dto/CategorySortOrderRequest.java', 'class CategorySortOrderRequest')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/dto/CategorySortOrderRequest.java', '@NotNull')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/controller/CategoryController.java', '@PutMapping("/sort-order")')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/controller/CategoryController.java', 'List<CategorySortOrderRequest>')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/service/CategoryService.java', 'void updateSortOrders')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/service/CategoryService.java', 'categoryMapper.updateSortOrder')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/mapper/CategoryMapper.java', 'void updateSortOrder')
expectIncludes(backendRoot, 'src/main/java/com/supermarket/inventory/category/mapper/CategoryMapper.java', 'UPDATE category SET sort_order = ? WHERE id = ?')
expectIncludes(backendRoot, 'src/test/java/com/supermarket/inventory/category/service/CategoryServiceTest.java', 'updateSortOrders_updatesOnlySortOrderWithoutCategoryName')
expectExcludes(frontendRoot, 'src/views/product/ProductCategoryModernView.vue', "updateCategory(item.id, { sortOrder")
expectIncludes(frontendRoot, 'src/api/category.js', "request.put('/categories/sort-order', items)")

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('Category sort order API verification passed')
