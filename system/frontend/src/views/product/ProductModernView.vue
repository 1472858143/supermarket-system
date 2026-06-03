<template>
  <div class="product-modern-page">
    <div class="page-head">
      <div>
        <h1>商品管理</h1>
        <div class="greet-sub">
          共维护 <b>{{ formatNumber(total) }}</b> 款商品 · 在售 <b>{{ formatNumber(statusCounts.on) }}</b> ·
          库存预警 <b>{{ formatNumber(statusCounts.low) }}</b> · 最近同步 {{ syncTime || '--:--' }}
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" @click="safeAction('批量导入模板已准备')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          批量导入
        </button>
        <button class="btn" type="button" @click="safeAction('正在按当前筛选导出商品列表')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出表格
        </button>
        <button class="btn primary" type="button" @click="goCreateProduct">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新建商品
        </button>
      </div>
    </div>

    <ProductModuleTabs :total-count="filteredRows.length" @action="safeAction" />

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">
      {{ message }}
    </div>

    <ProductFilterBar
      :query="query"
      :categories="flatCategories"
      :brands="brandOptions"
      :loading="loading"
      @reload="reload"
      @reset="resetQuery"
      @reset-page="resetLocalPage"
    />

    <ProductVisualTable
      :query="query"
      :status-tabs="statusTabs"
      :paged-rows="pagedRows"
      :selected-ids="selectedIds"
      :all-page-selected="allPageSelected"
      :loading="loading"
      :filtered-count="filteredRows.length"
      :page-buttons="pageButtons"
      :page-count="pageCount"
      @set-status="setStatus"
      @toggle-page="togglePageSelection"
      @toggle-row="toggleRow"
      @action="safeAction"
      @edit="goLegacyProducts"
      @reset="resetQuery"
      @reset-page="resetLocalPage"
      @change-page="changePage"
    />

    <ProductBulkBar
      :selected-count="selectedCount"
      @bulk-action="bulkAction"
      @clear-selection="clearSelection"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategoryTree } from '../../api/category'
import { listBrandOptions } from '../../api/brand'
import { listProducts } from '../../api/product'
import { listStocks } from '../../api/stock'
import ProductBulkBar from './components/ProductBulkBar.vue'
import ProductFilterBar from './components/ProductFilterBar.vue'
import ProductModuleTabs from './components/ProductModuleTabs.vue'
import ProductVisualTable from './components/ProductVisualTable.vue'

const router = useRouter()
const route = useRoute()
const products = ref([])
const stocks = ref([])
const categories = ref([])
const brandOptions = ref([])
const total = ref(0)
const loading = ref(false)
const message = ref('')
const messageType = ref('success')
const syncTime = ref('')
const selectedIds = ref(new Set())
const query = reactive({
  keyword: '',
  categoryId: '',
  brandId: route.query.brandId ? String(route.query.brandId) : '',
  status: 'all',
  priceMin: '',
  priceMax: '',
  page: 1,
  pageSize: 10
})

const palettes = [
  ['#4d9bff', '#1e63e0'],
  ['#34d399', '#0f766e'],
  ['#fbbf24', '#d97706'],
  ['#f472b6', '#be185d'],
  ['#c084fc', '#7c3aed'],
  ['#22d3ee', '#0e7490'],
  ['#fb7185', '#be123c'],
  ['#60a5fa', '#1d4ed8']
]

const stockBySkuId = computed(() => {
  const map = new Map()
  for (const stock of stocks.value) {
    if (stock.skuId != null) map.set(String(stock.skuId), stock)
  }
  return map
})

const flatCategories = computed(() => flattenCategories(categories.value))

const rows = computed(() =>
  products.value.map((product) => {
    const skus = Array.isArray(product.skus) ? product.skus : []
    const sku = skus.find((item) => item.isDefault === 1) || skus[0] || {}
    const stock = sku.id != null ? stockBySkuId.value.get(String(sku.id)) : null
    const salePrice = Number(sku.salePrice || 0)
    const purchasePrice = Number(sku.purchasePrice || 0)
    const quantity = Number(stock?.quantity ?? 0)
    const minStock = Number(stock?.minStock ?? 0)
    const maxStock = Number(stock?.maxStock ?? Math.max(quantity, minStock, 1))
    const safeStock = minStock || maxStock || 0
    const warningStatus = stock?.warningStatus || (quantity <= 0 ? 'OUT' : quantity < safeStock ? 'LOW' : 'NORMAL')
    const margin = salePrice > 0 ? Math.max(0, Math.round(((salePrice - purchasePrice) / salePrice) * 100)) : 0
    const isOut = quantity <= 0 && Boolean(sku.id)
    const isLow = !isOut && warningStatus !== 'NORMAL'
    const rowStatus = resolveStatus(product, sku, isLow, isOut)
    return {
      id: product.id,
      productCode: product.productCode,
      productName: product.productName,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      brandId: product.brandId,
      brandCode: product.brandCode,
      brandName: product.brandName,
      skuCode: sku.skuCode,
      skuName: sku.skuName,
      barcode: sku.barcode,
      salePrice,
      purchasePrice,
      skuCount: skus.length,
      quantity,
      safeStock,
      stockPercent: Math.min(100, Math.round((quantity / Math.max(maxStock, safeStock, 1)) * 100)),
      stockClass: isOut ? 'danger' : isLow ? 'warn' : '',
      margin,
      marginColor: marginColor(margin),
      isLow,
      isNew: isRecent(product.createTime),
      statusKey: rowStatus.key,
      statusLabel: rowStatus.label,
      statusClass: rowStatus.className,
      updateTime: formatDate(stock?.updateTime || product.createTime),
      thumbText: thumbText(product.productName),
      thumbStyle: thumbStyle(product.productName, product.id)
    }
  })
)

const statusCounts = computed(() => {
  const counts = { all: rows.value.length, on: 0, off: 0, low: 0, out: 0, draft: 0 }
  for (const row of rows.value) {
    if (row.statusKey === 'on') counts.on += 1
    if (row.statusKey === 'off') counts.off += 1
    if (row.statusKey === 'low') counts.low += 1
    if (row.statusKey === 'out') counts.out += 1
    if (row.statusKey === 'draft') counts.draft += 1
  }
  return counts
})

const statusTabs = computed(() => [
  { key: 'all', label: '全部', count: statusCounts.value.all },
  { key: 'on', label: '在售', count: statusCounts.value.on },
  { key: 'off', label: '下架', count: statusCounts.value.off },
  { key: 'low', label: '库存预警', count: statusCounts.value.low },
  { key: 'out', label: '已售罄', count: statusCounts.value.out },
  { key: 'draft', label: '草稿', count: statusCounts.value.draft }
])

const filteredRows = computed(() => {
  const min = query.priceMin === '' ? null : Number(query.priceMin)
  const max = query.priceMax === '' ? null : Number(query.priceMax)
  return rows.value.filter((row) => {
    if (query.status !== 'all' && row.statusKey !== query.status) return false
    if (query.categoryId && String(row.categoryId) !== query.categoryId) return false
    if (query.brandId && String(row.brandId) !== query.brandId) return false
    if (min != null && row.salePrice < min) return false
    if (max != null && row.salePrice > max) return false
    return true
  })
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / query.pageSize)))
const pagedRows = computed(() => {
  const safePage = Math.min(query.page, pageCount.value)
  const start = (safePage - 1) * query.pageSize
  return filteredRows.value.slice(start, start + query.pageSize)
})
const pageButtons = computed(() => {
  const pages = []
  const start = Math.max(1, query.page - 2)
  const end = Math.min(pageCount.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})
const selectedCount = computed(() => selectedIds.value.size)
const allPageSelected = computed(() => pagedRows.value.length > 0 && pagedRows.value.every((row) => selectedIds.value.has(row.id)))

function flattenCategories(nodes, depth = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    result.push(...flattenCategories(node.children || [], depth + 1))
  }
  return result
}

function resolveStatus(product, sku, isLow, isOut) {
  if (product.status === 0) return { key: 'off', label: '已下架', className: 'mute' }
  if (!sku.id) return { key: 'draft', label: '草稿', className: 'info' }
  if (isOut) return { key: 'out', label: '已售罄', className: 'danger' }
  if (isLow) return { key: 'low', label: '库存低', className: 'warn' }
  return { key: 'on', label: '在售', className: 'ok' }
}

function marginColor(value) {
  if (value >= 40) return '#15803d'
  if (value >= 25) return 'var(--brand)'
  if (value >= 15) return 'var(--warn)'
  return 'var(--danger)'
}

function thumbStyle(name, key) {
  const index = Number(key || hashText(name)) % palettes.length
  const [from, to] = palettes[index]
  return { background: `linear-gradient(135deg, ${from}, ${to})` }
}

function thumbText(name) {
  return [...(name || '商品')].slice(0, 2).join('')
}

function hashText(text) {
  return [...(text || '')].reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

function isRecent(value) {
  if (!value) return false
  const time = new Date(value).getTime()
  return Number.isFinite(time) && Date.now() - time < 1000 * 60 * 60 * 24 * 14
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function resetLocalPage() {
  query.page = 1
}

function changePage(page) {
  query.page = Math.min(Math.max(1, page), pageCount.value)
}

function setStatus(status) {
  query.status = status
  resetLocalPage()
}

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
  window.clearTimeout(showMessage.timer)
  showMessage.timer = window.setTimeout(() => {
    message.value = ''
  }, 2600)
}

function safeAction(text, type = 'success') {
  showMessage(text, type)
}

function bulkAction(text, type = 'success') {
  showMessage(`${selectedCount.value} 件商品：${text}`, type)
}

function clearSelection() {
  selectedIds.value = new Set()
}

function toggleRow(id) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selectedIds.value = next
}

function togglePageSelection() {
  const next = new Set(selectedIds.value)
  if (allPageSelected.value) {
    for (const row of pagedRows.value) next.delete(row.id)
  } else {
    for (const row of pagedRows.value) next.add(row.id)
  }
  selectedIds.value = next
}

function goLegacyProducts() {
  router.push('/products')
  showMessage('已跳转到旧版商品管理，可继续新增或编辑商品')
}

function goCreateProduct() {
  router.push('/products-modern/new')
}

function resetQuery() {
  Object.assign(query, {
    keyword: '',
    categoryId: '',
    brandId: '',
    status: 'all',
    priceMin: '',
    priceMax: '',
    page: 1,
    pageSize: query.pageSize
  })
  clearSelection()
  loadData()
}

async function reload() {
  query.page = 1
  clearSelection()
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [productData, categoryData, stockData, brandData] = await Promise.all([
      listProducts({ keyword: query.keyword, brandId: query.brandId || undefined, page: 1, pageSize: 100 }),
      getCategoryTree(),
      listStocks({ keyword: query.keyword, page: 1, pageSize: 100 }),
      listBrandOptions()
    ])
    products.value = productData.items || []
    total.value = productData.total || products.value.length
    categories.value = categoryData || []
    stocks.value = stockData.items || []
    brandOptions.value = brandData || []
    syncTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch (error) {
    showMessage(error.message || '商品数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
