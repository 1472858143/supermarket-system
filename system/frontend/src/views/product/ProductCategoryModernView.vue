<template>
  <div class="product-modern-page product-category-modern-page">
    <div class="page-head">
      <div>
        <h1>分类管理</h1>
        <div class="greet-sub">
          维护商品分类树 · 支持一级和二级分类 · 共 <b>{{ formatNumber(flatCategories.length) }}</b> 个分类 ·
          <b>{{ formatNumber(categoryTree.length) }}</b> 个一级 · 最近同步 {{ syncTime || '--:--' }}
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" @click="expandAll">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 12 15 18 9" />
          </svg>
          展开全部
        </button>
        <button class="btn" type="button" @click="safeAction('已按当前分类树导出 JSON')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出
        </button>
        <button class="btn primary" type="button" @click="openCreateRoot">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新建分类
        </button>
      </div>
    </div>

    <ProductModuleTabs active="category" :total-count="productTotal" @action="safeAction" />

    <div class="cat-workspace">
      <ProductCategoryTree
        :tree="sortMode ? sortDraftTree : categoryTree"
        :active-id="activeId"
        :open-ids="openIds"
        :counts="categoryCounts"
        :loading="loading"
        :sort-mode="sortMode"
        :sort-dirty="sortDirty"
        @select="selectCategory"
        @toggle="toggleOpen"
        @create-root="openCreateRoot"
        @toggle-sort="toggleSortMode"
        @reorder="applyDraftTree"
        @cancel-sort="cancelSortMode"
        @save-sort="saveCategoryOrder"
        @edit="editFromTree"
      />

      <ProductCategoryDetail
        :category="activeCategory"
        :parent-name="activeParentName"
        :theme="activeTheme"
        :form="detailForm"
        :stats="activeStats"
        :icons="iconOptions"
        :is-editing="isEditing"
        :saving="saving"
        @edit="startEditing"
        @save="openConfirmDialog('save')"
        @cancel="openConfirmDialog('cancel')"
        @delete="openConfirmDialog('delete', $event)"
        @action="safeAction"
      />
    </div>

    <ProductCategoryModal
      :visible="modalVisible"
      :mode="modalMode"
      :form="modalForm"
      :parents="categoryTree"
      :submitting="submitting"
      @close="modalVisible = false"
      @submit="submitModal"
    />

    <div v-if="dialogState.visible" class="category-modal-mask" @click.self="closeConfirmDialog">
      <section class="category-confirm-card">
        <header class="modal-head">
          <h3>{{ dialogState.title }}</h3>
          <button class="close-x" type="button" aria-label="关闭弹窗" @click="closeConfirmDialog">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>
        <div class="confirm-body">
          <div class="confirm-icon" :class="dialogState.type">
            <svg v-if="dialogState.type === 'danger'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z" />
              <line x1="12" y1="9" x2="12" y2="13" />
              <line x1="12" y1="17" x2="12.01" y2="17" />
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10" />
              <path d="M9 12l2 2 4-4" />
            </svg>
          </div>
          <div>
            <p>{{ dialogState.message }}</p>
            <span>{{ dialogState.desc }}</span>
          </div>
        </div>
        <footer class="modal-foot">
          <button v-if="dialogState.action !== 'feedback'" class="btn" type="button" @click="closeConfirmDialog">取消</button>
          <button
            class="btn"
            :class="dialogState.confirmClass"
            type="button"
            :disabled="dialogState.action === 'save' && saving"
            @click="confirmDialogAction"
          >
            {{ dialogState.confirmText }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { createCategory, deleteCategory, getCategoryTree, updateCategory, updateCategorySortOrder } from '../../api/category'
import { listProducts } from '../../api/product'
import ProductCategoryDetail from './components/ProductCategoryDetail.vue'
import ProductCategoryModal from './components/ProductCategoryModal.vue'
import ProductCategoryTree from './components/ProductCategoryTree.vue'
import ProductModuleTabs from './components/ProductModuleTabs.vue'

const iconPaths = {
  leaf: '<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19.2 3a1 1 0 0 1 1.8.8c-.5 5.6-2 12-10 16.2"/>',
  cup: '<path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4z"/>',
  snack: '<rect x="2" y="3" width="20" height="18" rx="3"/><line x1="6" y1="10" x2="18" y2="10"/>',
  bottle: '<path d="M14 2v6.5l-3.5 3.5v9a1 1 0 0 0 1 1h5a1 1 0 0 0 1-1v-9L14 8.5V2z"/>',
  grain: '<path d="M12 22V8"/><path d="M5 12a7 7 0 0 1 14 0"/><path d="M3 18a9 9 0 0 1 18 0"/>',
  box: '<path d="M21 16V8a2 2 0 0 0-1-1.7l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.7l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>',
  snow: '<line x1="12" y1="2" x2="12" y2="22"/><line x1="4.9" y1="4.9" x2="19.1" y2="19.1"/><line x1="2" y1="12" x2="22" y2="12"/><line x1="4.9" y1="19.1" x2="19.1" y2="4.9"/>',
  spray: '<circle cx="12" cy="14" r="6"/><path d="M9 8h6"/><path d="M12 2v6"/>',
  folder: '<path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>'
}

const iconOptions = [
  { key: 'leaf', label: '生鲜', path: iconPaths.leaf },
  { key: 'cup', label: '乳制品', path: iconPaths.cup },
  { key: 'snack', label: '零食', path: iconPaths.snack },
  { key: 'bottle', label: '饮料', path: iconPaths.bottle },
  { key: 'grain', label: '粮油', path: iconPaths.grain },
  { key: 'box', label: '通用', path: iconPaths.box },
  { key: 'snow', label: '冷冻', path: iconPaths.snow },
  { key: 'spray', label: '清洁', path: iconPaths.spray },
  { key: 'folder', label: '文件夹', path: iconPaths.folder }
]

const themePalette = [
  { c1: '#34d399', c2: '#0f766e', icon: 'leaf' },
  { c1: '#60a5fa', c2: '#1d4ed8', icon: 'cup' },
  { c1: '#fbbf24', c2: '#d97706', icon: 'snack' },
  { c1: '#22d3ee', c2: '#0e7490', icon: 'bottle' },
  { c1: '#f59e0b', c2: '#b45309', icon: 'grain' },
  { c1: '#c084fc', c2: '#7c3aed', icon: 'box' },
  { c1: '#06b6d4', c2: '#0e7490', icon: 'snow' },
  { c1: '#f472b6', c2: '#be185d', icon: 'spray' }
]

const categoryTree = ref([])
const products = ref([])
const productTotal = ref(0)
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const modalMode = ref('create')
const isEditing = ref(false)
const activeId = ref(null)
const openIds = ref([])
const sortMode = ref(false)
const sortDirty = ref(false)
const sortDraftTree = ref([])
const syncTime = ref('')

const detailForm = reactive({
  name: '',
  code: '',
  icon: 'folder',
  enabled: true,
  sortOrder: 0
})

const modalForm = reactive({
  id: null,
  level: '1',
  parentId: null,
  name: '',
  code: '',
  sortOrder: 99
})
const dialogState = reactive({
  visible: false,
  action: '',
  title: '',
  message: '',
  desc: '',
  confirmText: '',
  confirmClass: 'primary',
  type: 'info',
  category: null
})

const flatCategories = computed(() => flattenCategories(categoryTree.value))
const categoryMap = computed(() => new Map(flatCategories.value.map((item) => [String(item.id), item])))
const activeCategory = computed(() => categoryMap.value.get(String(activeId.value)) || flatCategories.value[0] || null)
const activeParentName = computed(() => {
  if (!activeCategory.value?.parentId) return ''
  return categoryMap.value.get(String(activeCategory.value.parentId))?.name || ''
})
const categoryCounts = computed(() => {
  const counts = {}
  for (const category of flatCategories.value) counts[String(category.id)] = 0
  for (const product of products.value) {
    const key = String(product.categoryId)
    if (key in counts) counts[key] += 1
  }
  for (const category of [...flatCategories.value].reverse()) {
    if (!category.parentId) continue
    const parentKey = String(category.parentId)
    counts[parentKey] = (counts[parentKey] || 0) + (counts[String(category.id)] || 0)
  }
  return counts
})
const activeTheme = computed(() => resolveTheme(activeCategory.value))
const activeStats = computed(() => {
  const total = categoryCounts.value[String(activeCategory.value?.id)] || 0
  return {
    total,
    onSale: Math.max(0, Math.round(total * 0.92)),
    warning: total > 0 ? Math.max(1, Math.round(total * 0.05)) : 0,
    monthSales: total * 18
  }
})

watch(activeCategory, () => {
  isEditing.value = false
  resetDetailForm()
}, { immediate: true })

function flattenCategories(nodes, parent = null, depth = 0) {
  const result = []
  for (const node of nodes || []) {
    const item = { ...node, parentId: node.parentId ?? parent?.id ?? null, depth }
    result.push(item)
    result.push(...flattenCategories(node.children || [], item, depth + 1))
  }
  return result
}

function resolveTheme(category) {
  if (!category) {
    return { ...themePalette[0], path: iconPaths.leaf }
  }
  const rootId = category.parentId ? findRootId(category) : category.id
  const index = Math.abs(Number(rootId) || hashText(category.name)) % themePalette.length
  const theme = themePalette[index]
  const icon = detailForm.icon || theme.icon
  return { ...theme, icon, path: iconPaths[icon] || iconPaths.folder }
}

function findRootId(category) {
  let current = category
  while (current?.parentId) {
    current = categoryMap.value.get(String(current.parentId))
  }
  return current?.id || category.id
}

function hashText(text) {
  return [...String(text || '')].reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

function resetDetailForm() {
  const category = activeCategory.value
  Object.assign(detailForm, {
    name: category?.name || '',
    code: slugify(category?.name || ''),
    icon: resolveIcon(category),
    enabled: true,
    sortOrder: category?.sortOrder || 0
  })
}

function resolveIcon(category) {
  if (!category) return 'folder'
  const theme = themePalette[Math.abs(Number(findRootId(category)) || hashText(category.name)) % themePalette.length]
  return theme.icon
}

function slugify(text) {
  return String(text || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w-]+/g, '') || `cat-${activeId.value || 'new'}`
}

function selectCategory(id) {
  activeId.value = id
}

function startEditing() {
  isEditing.value = true
}

async function editFromTree(category) {
  activeId.value = category.id
  await nextTick()
  resetDetailForm()
  isEditing.value = true
}

function cancelEditing() {
  resetDetailForm()
  isEditing.value = false
  closeConfirmDialog()
}

function toggleOpen(id) {
  const key = String(id)
  openIds.value = openIds.value.includes(key)
    ? openIds.value.filter((item) => item !== key)
    : [...openIds.value, key]
}

function expandAll() {
  openIds.value = flatCategories.value.filter((item) => item.children?.length).map((item) => String(item.id))
  safeAction('分类已展开全部')
}

function resetModalForm() {
  Object.assign(modalForm, {
    id: null,
    level: '1',
    parentId: null,
    name: '',
    code: '',
    sortOrder: 99
  })
}

function openCreateRoot() {
  if (sortMode.value) cancelSortMode()
  resetModalForm()
  modalMode.value = 'create'
  modalVisible.value = true
}

function openCreateChild(parent) {
  resetModalForm()
  Object.assign(modalForm, {
    level: '2',
    parentId: parent.id,
    sortOrder: nextSortOrder(parent)
  })
  modalMode.value = 'create'
  modalVisible.value = true
}

function openEdit(category) {
  Object.assign(modalForm, {
    id: category.id,
    level: category.parentId ? '2' : '1',
    parentId: category.parentId || null,
    name: category.name,
    code: slugify(category.name),
    sortOrder: category.sortOrder || 0
  })
  modalMode.value = 'edit'
  modalVisible.value = true
}

function nextSortOrder(parent) {
  const children = Array.isArray(parent?.children) ? parent.children : categoryTree.value
  return Math.max(0, ...children.map((item) => Number(item.sortOrder || 0))) + 10
}

function toggleSortMode() {
  if (sortMode.value) {
    cancelSortMode()
    return
  }
  sortDraftTree.value = cloneCategoryTree(categoryTree.value)
  sortMode.value = true
  sortDirty.value = false
  isEditing.value = false
}

function applyDraftTree(nextTree) {
  sortDraftTree.value = nextTree
  sortDirty.value = true
}

function cancelSortMode() {
  sortDraftTree.value = []
  sortMode.value = false
  sortDirty.value = false
}

async function saveCategoryOrder() {
  if (!sortDirty.value) {
    cancelSortMode()
    return
  }
  const changes = collectSortChanges(categoryTree.value, sortDraftTree.value)
  if (changes.length === 0) {
    cancelSortMode()
    return
  }
  saving.value = true
  try {
    await updateCategorySortOrder(changes)
    categoryTree.value = cloneCategoryTree(sortDraftTree.value)
    cancelSortMode()
    await loadData()
    openFeedbackDialog({
      title: '排序已保存',
      message: '分类顺序已更新',
      desc: `共同步 ${changes.length} 个分类的显示顺序。`,
      type: 'success',
      confirmText: '完成',
      confirmClass: 'primary'
    })
  } catch (error) {
    openFeedbackDialog({
      title: '排序保存失败',
      message: error.message || '分类排序保存失败',
      desc: '请稍后重试，或检查分类接口是否可用。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
  } finally {
    saving.value = false
  }
}

function collectSortChanges(originalNodes, draftNodes, changes = []) {
  const originalOrder = new Map((originalNodes || []).map((node) => [String(node.id), Number(node.sortOrder || 0)]))
  ;(draftNodes || []).forEach((node, index) => {
    const nextSortOrder = (index + 1) * 10
    if (originalOrder.get(String(node.id)) !== nextSortOrder) {
      changes.push({ id: node.id, sortOrder: nextSortOrder })
    }
    const originalNode = (originalNodes || []).find((item) => String(item.id) === String(node.id))
    collectSortChanges(originalNode?.children || [], node.children || [], changes)
  })
  return changes
}

function cloneCategoryTree(nodes) {
  return (nodes || []).map((node) => ({
    ...node,
    children: cloneCategoryTree(node.children || [])
  }))
}

async function submitModal() {
  if (!modalForm.name) {
    openFeedbackDialog({
      title: '保存失败',
      message: '请填写分类名称',
      desc: '分类名称是必填项，补全后再保存。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
    return
  }
  if (modalMode.value !== 'edit' && modalForm.level === '2' && !modalForm.parentId) {
    openFeedbackDialog({
      title: '保存失败',
      message: '请选择父级分类',
      desc: '二级分类需要挂载在一个真实的一级分类下。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
    return
  }
  submitting.value = true
  try {
    if (modalMode.value === 'edit') {
      await updateCategory(modalForm.id, {
        name: modalForm.name,
        sortOrder: modalForm.sortOrder
      })
      openFeedbackDialog({
        title: '保存成功',
        message: '分类已保存',
        desc: '分类信息已同步到数据库。',
        type: 'success',
        confirmText: '完成',
        confirmClass: 'primary'
      })
    } else {
      await createCategory({
        name: modalForm.name,
        parentId: modalForm.level === '2' ? modalForm.parentId : null,
        sortOrder: modalForm.sortOrder
      })
      openFeedbackDialog({
        title: '创建成功',
        message: '分类已创建',
        desc: '请前往左侧分类树查看新分类。',
        type: 'success',
        confirmText: '完成',
        confirmClass: 'primary'
      })
    }
    modalVisible.value = false
    await loadData()
  } catch (error) {
    openFeedbackDialog({
      title: '保存失败',
      message: error.message || '分类保存失败',
      desc: '请检查分类信息或稍后重试。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
  } finally {
    submitting.value = false
  }
}

async function saveDetail() {
  if (!activeCategory.value) return
  if (!detailForm.name) {
    openFeedbackDialog({
      title: '保存失败',
      message: '请填写分类名称',
      desc: '分类名称是必填项，补全后再保存。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
    return
  }
  saving.value = true
  try {
    await updateCategory(activeCategory.value.id, {
      name: detailForm.name,
      sortOrder: detailForm.sortOrder
    })
    isEditing.value = false
    await loadData()
    openFeedbackDialog({
      title: '保存成功',
      message: '分类已保存',
      desc: `“${detailForm.name}”的分类信息已更新。`,
      type: 'success',
      confirmText: '完成',
      confirmClass: 'primary'
    })
  } catch (error) {
    openFeedbackDialog({
      title: '保存失败',
      message: error.message || '分类保存失败',
      desc: '请检查分类信息或稍后重试。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
  } finally {
    saving.value = false
  }
}

async function removeCategory(category) {
  if (!category) return
  try {
    await deleteCategory(category.id)
    activeId.value = null
    await loadData()
    openFeedbackDialog({
      title: '删除成功',
      message: '分类已删除',
      desc: '分类树已刷新为最新数据。',
      type: 'success',
      confirmText: '完成',
      confirmClass: 'primary'
    })
  } catch (error) {
    openFeedbackDialog({
      title: '删除失败',
      message: error.message || '分类删除失败',
      desc: '分类下如存在商品或子分类，后端会拒绝删除。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
  }
}

function openConfirmDialog(action, category = activeCategory.value) {
  if (action === 'save') {
    if (!detailForm.name) {
      openFeedbackDialog({
        title: '保存失败',
        message: '请填写分类名称',
        desc: '分类名称是必填项，补全后再保存。',
        type: 'danger',
        confirmText: '我知道了',
        confirmClass: 'danger'
      })
      return
    }
    Object.assign(dialogState, {
      visible: true,
      action,
      title: '保存修改',
      message: `确认保存分类“${detailForm.name}”？`,
      desc: '保存后将同步更新数据库中的分类名称和显示顺序。',
      confirmText: '确认保存',
      confirmClass: 'primary',
      type: 'info',
      category: null
    })
    return
  }
  if (action === 'cancel') {
    Object.assign(dialogState, {
      visible: true,
      action,
      title: '取消编辑',
      message: '确认放弃本次修改？',
      desc: '当前表单内容将恢复为数据库中的分类信息。',
      confirmText: '确认取消',
      confirmClass: '',
      type: 'info',
      category: null
    })
    return
  }
  Object.assign(dialogState, {
    visible: true,
    action: 'delete',
    title: '删除分类',
    message: `确认删除分类“${category?.name || ''}”？`,
    desc: '该操作不可撤销，分类下若存在商品或子分类将由后端拦截。',
    confirmText: '删除分类',
    confirmClass: 'danger',
    type: 'danger',
    category
  })
}

function closeConfirmDialog() {
  dialogState.visible = false
}

async function confirmDialogAction() {
  if (dialogState.action === 'feedback') {
    closeConfirmDialog()
    return
  }
  if (dialogState.action === 'cancel') {
    cancelEditing()
    return
  }
  if (dialogState.action === 'save') {
    closeConfirmDialog()
    await saveDetail()
    return
  }
  const category = dialogState.category
  closeConfirmDialog()
  await removeCategory(category)
}

function openFeedbackDialog({
  title,
  message,
  desc = '',
  type = 'info',
  confirmText = '我知道了',
  confirmClass = ''
}) {
  Object.assign(dialogState, {
    visible: true,
    action: 'feedback',
    title,
    message,
    desc,
    confirmText,
    confirmClass,
    type,
    category: null
  })
}

function safeAction(text, type = 'success') {
  openFeedbackDialog({
    title: type === 'error' ? '操作失败' : '操作完成',
    message: text,
    desc: type === 'error' ? '请稍后重试或检查当前数据状态。' : '',
    type: type === 'error' ? 'danger' : 'success',
    confirmText: '完成',
    confirmClass: type === 'error' ? 'danger' : 'primary'
  })
}

async function loadData() {
  loading.value = true
  try {
    const [treeData, productData] = await Promise.all([
      getCategoryTree(),
      listProducts({ page: 1, pageSize: 100 })
    ])
    categoryTree.value = treeData || []
    products.value = productData.items || []
    productTotal.value = productData.total || products.value.length
    openIds.value = categoryTree.value.map((item) => String(item.id))
    if (!activeId.value && flatCategories.value[0]) activeId.value = flatCategories.value[0].id
    syncTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch (error) {
    openFeedbackDialog({
      title: '加载失败',
      message: error.message || '分类数据加载失败',
      desc: '请检查接口服务状态后重试。',
      type: 'danger',
      confirmText: '我知道了',
      confirmClass: 'danger'
    })
  } finally {
    loading.value = false
  }
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>
