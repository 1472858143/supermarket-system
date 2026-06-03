import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const projectRoot = path.resolve(root, '..', '..')

const requiredFiles = [
  'src/views/product/ProductCategoryModernView.vue',
  'src/views/product/components/ProductCategoryTree.vue',
  'src/views/product/components/ProductCategoryDetail.vue',
  'src/views/product/components/ProductCategoryModal.vue'
]

const errors = []

for (const relativePath of requiredFiles) {
  const fullPath = path.join(root, relativePath)
  if (!existsSync(fullPath)) {
    errors.push(`Missing file: ${relativePath}`)
  }
}

function read(relativePath) {
  const fullPath = path.join(root, relativePath)
  if (!existsSync(fullPath)) return ''
  return readFileSync(fullPath, 'utf8')
}

function expectIncludes(file, needle) {
  const content = read(file)
  if (!content.includes(needle)) {
    errors.push(`${file} missing "${needle}"`)
  }
}

function expectExcludes(file, needle) {
  const content = read(file)
  if (content.includes(needle)) {
    errors.push(`${file} should not contain "${needle}"`)
  }
}

expectIncludes('src/router/index.js', "path: '/product-categories-modern'")
expectIncludes('src/router/index.js', "name: 'ProductCategoryModern'")
expectIncludes('src/router/index.js', 'ProductCategoryModernView')
expectIncludes('src/router/index.js', "title: '商品分类新版'")
expectIncludes('src/router/index.js', 'hideInMenu: true')
expectIncludes('src/layout/AdminLayout.vue', 'hideInMenu')

for (const needle of [
  'product-category-modern-page',
  'ProductModuleTabs',
  'ProductCategoryTree',
  'ProductCategoryDetail',
  'ProductCategoryModal',
  'getCategoryTree',
  'createCategory',
  'updateCategory',
  'updateCategorySortOrder',
  'deleteCategory',
  'listProducts'
]) {
  expectIncludes('src/views/product/ProductCategoryModernView.vue', needle)
}

expectIncludes('src/views/product/components/ProductCategoryModal.vue', 'parentOptions')
expectIncludes('src/views/product/components/ProductCategoryModal.vue', "form.level === '2'")
expectExcludes('src/views/product/components/ProductCategoryModal.vue', '--（顶级）')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'defaultAttributes')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'localAttributes')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'addAttribute')
expectExcludes('src/views/product/components/ProductCategoryDetail.vue', 'ProductCategoryAttributes')
expectExcludes('src/views/product/components/ProductCategoryDetail.vue', 'attributes')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'isEditing')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'startEditing')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'cancelEditing')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'dialogState')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'openConfirmDialog')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@save="openConfirmDialog(\'save\')"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', "action === 'save'")
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'await saveDetail()')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'function openFeedbackDialog')
expectIncludes('src/views/product/ProductCategoryModernView.vue', "action: 'feedback'")
expectIncludes('src/views/product/ProductCategoryModernView.vue', "dialogState.action !== 'feedback'")
expectExcludes('src/views/product/ProductCategoryModernView.vue', '@save="saveDetail"')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'v-if="message"')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'const message = ref')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'messageType')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'function showMessage')
expectIncludes('src/views/product/components/ProductCategoryDetail.vue', 'is-editing')
expectIncludes('src/views/product/components/ProductCategoryDetail.vue', '查看模式')
expectIncludes('src/views/product/components/ProductCategoryDetail.vue', '编辑')
expectIncludes('src/views/product/components/ProductCategoryDetail.vue', 'v-if="isEditing"')
expectIncludes('src/views/product/components/ProductCategoryDetail.vue', ':disabled="!isEditing"')
expectIncludes('src/views/product/components/ProductCategoryDetail.vue', 'cat-detail-edit')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@edit="editFromTree"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@create-root="openCreateRoot"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@toggle-sort="toggleSortMode"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@reorder="applyDraftTree"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@cancel-sort="cancelSortMode"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', '@save-sort="saveCategoryOrder"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', ':sort-mode="sortMode"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', ':sort-dirty="sortDirty"')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'const sortMode = ref(false)')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'const sortDraftTree = ref([])')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'function applyDraftTree')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'async function saveCategoryOrder')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'await updateCategorySortOrder(changes)')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'collectSortChanges')
expectExcludes('src/views/product/ProductCategoryModernView.vue', "updateCategory(item.id, { sortOrder")
expectIncludes('src/api/category.js', "request.put('/categories/sort-order', items)")
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'nextTick')
expectIncludes('src/views/product/ProductCategoryModernView.vue', 'function editFromTree')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'title="编辑"')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'create-root')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'toggle-sort')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'save-sort')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'cancel-sort')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', '@pointerdown="startPointerDrag(row, $event)"')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'dragPreview')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'tree-drag-preview')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'dragPreview.width')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'dragPreview.left')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'dragPreview.value.offsetY')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'renderDragPreviewContent')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'document.addEventListener(\'pointermove\'')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'document.addEventListener(\'pointerup\'')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'finishPointerDrag')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'moveNodeWithinParent')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'class="tc-add"')
expectIncludes('src/views/product/components/ProductCategoryTree.vue', 'class="tc-sort"')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'draggable="sortMode"')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', '@dragstart')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', '@drop')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'class="drag-handle"')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'create-child')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'row.depth === 0')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'title="添加子分类"')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'title="删除"')
expectExcludes('src/views/product/components/ProductCategoryTree.vue', 'class="danger"')
expectExcludes('src/views/product/ProductCategoryModernView.vue', '@create-child')
expectExcludes('src/views/product/ProductCategoryModernView.vue', '@delete="removeCategory"')
expectExcludes('src/views/product/ProductCategoryModernView.vue', 'window.confirm')
expectIncludes('src/views/product/components/ProductModuleTabs.vue', "active")
expectIncludes('src/views/product/components/ProductModuleTabs.vue', '/product-categories-modern')

for (const needle of [
  '.product-category-modern-page',
  '.cat-workspace',
  '.tree-card',
  '.tree-row',
  '.cat-detail-head',
  '.cat-icon-circle',
  '.stat-grid',
  '.cat-save-row .btn',
  '.cat-save-row .btn.primary',
  '.cat-save-row .btn.danger',
  '.category-confirm-card',
  '.cat-readonly-field',
  '.cat-detail-edit',
  '.category-modal-mask'
]) {
  expectIncludes('src/assets/styles.css', needle)
}

const prototypePath = path.join(projectRoot, 'system/pages/product/商品管理-分类.html')
if (!existsSync(prototypePath)) {
  errors.push('Prototype page missing: system/pages/product/商品管理-分类.html')
}

if (errors.length > 0) {
  console.error(errors.join('\n'))
  process.exit(1)
}

console.log('Product category modern verification passed')
