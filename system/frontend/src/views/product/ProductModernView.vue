<template>
  <div class="product-modern-page">
    <div class="page-head">
      <div>
        <h1>商品档案</h1>
        <div class="greet-sub">
          商品主数据 · 共维护 <b>{{ formatNumber(total) }}</b> 个 SPU · 启用 <b>{{ formatNumber(statusCounts.active) }}</b> ·
          停用 {{ formatNumber(statusCounts.disabled) }} · 草稿 {{ formatNumber(statusCounts.draft) }} · 最近同步 {{ syncTime || '--:--' }}
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
        <button class="btn" type="button" @click="safeAction('正在按当前筛选导出商品档案')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出表格
        </button>
        <button class="btn primary" type="button" @click="openCreateDrawer">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新建商品档案
        </button>
      </div>
    </div>

    <ProductModuleTabs :total-count="filteredRows.length" @action="safeAction" />

    <!-- SPU 汇总卡片 -->
    <section class="kpi-row spu-kpi">
      <div class="kpi" style="--accent: #4d9bff">
        <div class="label">SPU 总数</div>
        <div class="value">{{ formatNumber(total) }}<span class="unit">个</span></div>
        <div>
          <span class="delta">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 15 12 9 18 15"/></svg>
            启用 {{ formatNumber(statusCounts.active) }}
          </span>
          <span class="sub">停用 {{ formatNumber(statusCounts.disabled) }} · 草稿 {{ formatNumber(statusCounts.draft) }}</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/></svg>
        </div>
      </div>

      <div class="kpi" style="--accent: #34d399">
        <div class="label">关联 SKU 总数</div>
        <div class="value">{{ formatNumber(totalSkuCount) }}<span class="unit">个</span></div>
        <div>
          <span class="delta">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 15 12 9 18 15"/></svg>
            均 {{ averageSkuPerSpu }}
          </span>
          <span class="sub">个 SKU / SPU</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
        </div>
      </div>

      <div class="kpi" style="--accent: #c084fc">
        <div class="label">基础品类覆盖</div>
        <div class="value">{{ categoryStats.large }}<span class="unit">大类 / {{ categoryStats.sub }} 子类</span></div>
        <div>
          <span class="sub">最多：{{ categoryStats.maxCatName }} {{ categoryStats.maxCatCount }} 个 SPU</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/></svg>
        </div>
      </div>

      <div class="kpi" style="--accent: #fbbf24">
        <div class="label">本月新增 SPU</div>
        <div class="value">{{ formatNumber(newSpuCount) }}<span class="unit">个</span></div>
        <div>
          <span class="delta">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 15 12 9 18 15"/></svg>
            +18%
          </span>
          <span class="sub">较上月</span>
        </div>
        <div class="icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12h14"/></svg>
        </div>
      </div>
    </section>

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
      @edit="openEditDrawer"
      @manage-sku="goSkuManage"
      @reset="resetQuery"
      @reset-page="resetLocalPage"
      @change-page="changePage"
    />

    <ProductBulkBar
      :selected-count="selectedCount"
      @bulk-action="bulkAction"
      @clear-selection="clearSelection"
    />

    <!-- ════ 新建 / 编辑 商品档案 drawer ════ -->
    <div class="drawer-mask" :class="{ show: drawerVisible }" @click="closeDrawer"></div>
    <aside class="drawer-panel ed-drawer" :class="{ show: drawerVisible }">
      <div class="ed-head">
        <div class="ti">
          <span>{{ drawerTitle }}</span>
          <span class="tag" :class="isCreateMode ? 'new' : 'edit'">{{ drawerTagText }}</span>
        </div>
        <button class="x" type="button" @click="closeDrawer">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <div class="ed-body">
        <!-- live preview -->
        <div class="ed-hero">
          <div class="prod-thumb" :style="previewThumbStyle">
            <span>{{ previewThumbText }}</span>
          </div>
          <div style="min-width: 0">
            <div class="eh-name">{{ editForm.productName || '商品名称将显示在此处' }}</div>
            <div class="eh-sub">
              <span>{{ editForm.productCode || '提交后自动生成' }}</span>
              <span v-if="previewCategoryName" class="cat-tag">
                <span class="swatch" :style="{ background: previewCategoryColor }"></span>
                <span class="t">{{ previewCategoryName }}</span>
              </span>
            </div>
          </div>
        </div>

        <form @submit.prevent>
          <div class="ed-section">
            <div class="sec-label">基本信息</div>
            <div class="field-row" :class="{ err: errors.productName }">
              <div class="label"><span class="req">*</span>商品名称</div>
              <div class="input-wrap">
                <input class="input" v-model.trim="editForm.productName" placeholder="例：伊利安慕希希腊式酸奶" maxlength="60" />
                <div class="err" v-if="errors.productName">商品名称不能为空</div>
              </div>
            </div>
            <div class="field-row" :class="{ err: errors.brandId }">
              <div class="label"><span class="req">*</span>品牌</div>
              <div class="input-wrap">
                <select class="select" v-model="editForm.brandId">
                  <option value="">未指定品牌</option>
                  <option v-for="brand in brandOptions" :key="brand.id" :value="brand.id">
                    {{ brand.brandName }}
                  </option>
                </select>
                <div class="err" v-if="errors.brandId">请选择品牌</div>
              </div>
            </div>
            <div class="field-row" :class="{ err: errors.parentId }">
              <div class="label"><span class="req">*</span>一级分类</div>
              <div class="input-wrap">
                <select class="select" v-model.number="editForm.parentId" @change="onParentCategoryChange">
                  <option :value="null" disabled>请选择一级分类</option>
                  <option v-for="p in categories" :key="p.id" :value="p.id">{{ p.name }}</option>
                </select>
                <div class="err" v-if="errors.parentId">请选择一级分类</div>
              </div>
            </div>
            <div class="field-row" :class="{ err: errors.categoryId }" style="border-bottom: 0">
              <div class="label"><span class="req">*</span>二级分类</div>
              <div class="input-wrap">
                <select class="select" v-model.number="editForm.categoryId">
                  <option :value="null" disabled>请选择二级分类</option>
                  <option v-for="c in childCategories" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
                <div class="err" v-if="errors.categoryId">请选择二级分类</div>
              </div>
            </div>
          </div>

          <div class="ed-section">
            <div class="sec-label">
              基础商品属性
              <span style="font-family:'Noto Sans SC'; font-weight:400; letter-spacing:0; text-transform:none; color:#a4adc2; font-size:11px; margin-left: 6px;">
                规格 / 条码 / 包装下沉至 SKU 维护
              </span>
            </div>
            <div class="attr-chips">
              <span v-if="!editForm.attrs || !editForm.attrs.length" class="attr-empty">
                尚未添加属性 · 例如「口味：原味」「产地：内蒙古」
              </span>
              <span v-for="(attr, i) in editForm.attrs" :key="i" class="attr-chip">
                <span class="k">{{ attr[0] }}</span>
                <span class="v">{{ attr[1] }}</span>
                <button type="button" @click="removeEdAttr(i)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round">
                    <line x1="18" y1="6" x2="6" y2="18"/>
                    <line x1="6" y1="6" x2="18" y2="18"/>
                  </svg>
                </button>
              </span>
            </div>
            <div class="attr-add-row">
              <input class="input k" v-model.trim="newAttrKey" placeholder="属性名 如 口味" @keydown.enter.prevent="addEdAttr" />
              <input class="input v" v-model.trim="newAttrVal" placeholder="属性值 如 原味" @keydown.enter.prevent="addEdAttr" />
              <button type="button" class="add-btn" @click="addEdAttr">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                添加
              </button>
            </div>
          </div>

          <div class="ed-section">
            <div class="sec-label">档案状态</div>
            <div class="field-row" style="border-bottom: 0">
              <div class="label">状态</div>
              <div class="input-wrap">
                <div class="radio-chips">
                  <label :class="{ checked: editForm.status === 1 }">
                    <input type="radio" :value="1" v-model.number="editForm.status" />
                    <span>启用</span>
                  </label>
                  <label :class="{ checked: editForm.status === 0 }">
                    <input type="radio" :value="0" v-model.number="editForm.status" />
                    <span>停用</span>
                  </label>
                </div>
              </div>
            </div>
          </div>
        </form>
      </div>
      <div class="ed-foot">
        <span style="font-size: 12px; color: #7b86a0">填写商品名称与基础品类即可保存</span>
        <div style="margin-left: auto; display: flex; gap: 10px">
          <button class="btn" type="button" @click="closeDrawer">取消</button>
          <button class="btn primary" type="button" :disabled="submitting" @click="saveProductData">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
            <span>{{ saveButtonText }}</span>
          </button>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategoryTree } from '../../api/category'
import { listBrandOptions } from '../../api/brand'
import { createProduct, listProducts, updateProduct } from '../../api/product'
import { listStocks } from '../../api/stock'
import ProductBulkBar from './components/ProductBulkBar.vue'
import ProductFilterBar from './components/ProductFilterBar.vue'
import ProductModuleTabs from './components/ProductModuleTabs.vue'
import ProductVisualTable from './components/ProductVisualTable.vue'

const router = useRouter()
const route = useRoute()
const products = ref([])
const categories = ref([])
const brandOptions = ref([])
const total = ref(0)
const loading = ref(false)
const message = ref('')
const messageType = ref('success')
const syncTime = ref('')
const selectedIds = ref(new Set())

const drawerVisible = ref(false)
const drawerMode = ref('edit')
const newAttrKey = ref('')
const newAttrVal = ref('')
const submitting = ref(false)

const editForm = reactive({
  id: null,
  productCode: '',
  productName: '',
  brandId: '',
  parentId: null,
  categoryId: null,
  status: 1,
  attrs: []
})

const errors = reactive({
  productName: false,
  brandId: false,
  parentId: false,
  categoryId: false
})

const loadProductAttrs = (productId, sku) => {
  const key = `spu_attrs_${productId}`
  const saved = localStorage.getItem(key)
  if (saved) {
    try {
      return JSON.parse(saved)
    } catch (e) {}
  }
  const attrs = []
  if (sku && sku.spec) attrs.push(['规格', sku.spec])
  if (sku && sku.baseUnit) attrs.push(['单位', sku.baseUnit])
  if (sku && sku.barcode) attrs.push(['条码', sku.barcode])
  return attrs
}

const saveProductAttrs = (productId, attrs) => {
  const key = `spu_attrs_${productId}`
  localStorage.setItem(key, JSON.stringify(attrs))
}

const query = reactive({
  keyword: route.query.keyword ? String(route.query.keyword) : '',
  categoryId: '',
  brandId: route.query.brandId ? String(route.query.brandId) : '',
  status: 'all',
  filterStatus: '',
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

const flatCategories = computed(() => flattenCategories(categories.value))

const rows = computed(() =>
  products.value.map((product) => {
    const skus = Array.isArray(product.skus) ? product.skus : []
    const sku = skus.find((item) => item.isDefault === 1) || skus[0] || {}
    const rowStatus = resolveStatus(product, sku)
    const localAttrs = loadProductAttrs(product.id, sku)
    const attrTags = localAttrs.length
      ? localAttrs.map(([k, v]) => ({ key: k, value: v }))
      : buildAttrTags(sku)
    return {
      id: product.id,
      productCode: product.productCode,
      spuCode: product.productCode,
      productName: product.productName,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      brandId: product.brandId,
      brandCode: product.brandCode,
      brandName: product.brandName,
      skuName: sku.skuName,
      barcode: sku.barcode,
      skuCount: skus.length,
      attrTags,
      categoryColor: categoryColor(product.categoryName || product.categoryId),
      isNew: isRecent(product.createTime),
      statusKey: rowStatus.key,
      statusLabel: rowStatus.label,
      statusClass: rowStatus.className,
      updateTime: formatDate(product.createTime),
      thumbText: thumbText(product.productName),
      thumbStyle: thumbStyle(product.productName, product.id)
    }
  })
)

const statusCounts = computed(() => {
  const counts = { all: rows.value.length, active: 0, disabled: 0, draft: 0 }
  for (const row of rows.value) {
    if (row.statusKey === 'active') counts.active += 1
    if (row.statusKey === 'disabled') counts.disabled += 1
    if (row.statusKey === 'draft') counts.draft += 1
  }
  return counts
})

const statusTabs = computed(() => [
  { key: 'all', label: '全部', count: statusCounts.value.all },
  { key: 'active', label: '启用', count: statusCounts.value.active },
  { key: 'disabled', label: '停用', count: statusCounts.value.disabled },
  { key: 'draft', label: '草稿', count: statusCounts.value.draft }
])

const filteredRows = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  return rows.value.filter((row) => {
    if (query.status !== 'all' && row.statusKey !== query.status) return false
    if (query.filterStatus && row.statusKey !== query.filterStatus) return false
    if (query.categoryId && String(row.categoryId) !== query.categoryId) return false
    if (query.brandId && String(row.brandId) !== query.brandId) return false
    if (keyword) {
      const haystack = [row.productName, row.brandName, row.spuCode].join(' ').toLowerCase()
      if (!haystack.includes(keyword)) return false
    }
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

const totalSkuCount = computed(() => {
  return rows.value.reduce((sum, row) => sum + (row.skuCount || 0), 0)
})

const averageSkuPerSpu = computed(() => {
  if (rows.value.length === 0) return '0.0'
  return (totalSkuCount.value / rows.value.length).toFixed(1)
})

const categoryStats = computed(() => {
  const large = categories.value.length
  const totalSub = flatCategories.value.length - large
  
  const counts = {}
  for (const row of rows.value) {
    if (row.categoryName) {
      counts[row.categoryName] = (counts[row.categoryName] || 0) + 1
    }
  }
  let maxCatName = ''
  let maxCatCount = 0
  for (const [name, count] of Object.entries(counts)) {
    if (count > maxCatCount) {
      maxCatCount = count
      maxCatName = name
    }
  }
  return {
    large,
    sub: totalSub,
    maxCatName: maxCatName || '无',
    maxCatCount
  }
})

const newSpuCount = computed(() => {
  return rows.value.filter(row => row.isNew).length
})

const isCreateMode = computed(() => drawerMode.value === 'create')
const drawerTitle = computed(() => isCreateMode.value ? '新建商品档案' : '编辑商品档案')
const drawerTagText = computed(() => isCreateMode.value ? 'NEW' : 'EDIT')
const saveButtonText = computed(() => {
  if (submitting.value) return isCreateMode.value ? '创建中...' : '保存中...'
  return isCreateMode.value ? '确认创建' : '保存修改'
})

function flattenCategories(nodes, depth = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ id: node.id, label: `${'　'.repeat(depth)}${node.name}` })
    result.push(...flattenCategories(node.children || [], depth + 1))
  }
  return result
}

function resolveStatus(product, sku) {
  if (product.status === 0) return { key: 'disabled', label: '停用', className: 'mute' }
  if (!sku.id) return { key: 'draft', label: '草稿', className: 'info' }
  return { key: 'active', label: '启用', className: 'ok' }
}

function buildAttrTags(sku) {
  const attrs = []
  if (sku.spec) attrs.push({ key: '规格', value: sku.spec })
  if (sku.baseUnit) attrs.push({ key: '单位', value: sku.baseUnit })
  if (sku.barcode) attrs.push({ key: '条码', value: sku.barcode })
  return attrs.length ? attrs : [{ key: '属性', value: '待完善' }]
}

function categoryColor(value) {
  const colors = ['#2fb36b', '#4d9bff', '#f0a020', '#c084fc', '#e8804d', '#5b8def']
  return colors[hashText(String(value || '')) % colors.length]
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

function bulkAction(action) {
  const n = selectedCount.value
  if (!n) return
  const msgs = {
    active:   { title: `${n} 个商品档案已启用`, type: 'success' },
    disabled: { title: `${n} 个商品档案已停用`, type: 'error' },
    cat:      { title: '调整基础品类向导已打开', type: 'success' },
    export:   { title: `正在导出 ${n} 个商品档案`, type: 'success' },
    del:      { title: `已删除 ${n} 个商品档案`, type: 'error' }
  }
  const msg = msgs[action] || { title: `${n} 个商品档案操作已记录`, type: 'success' }
  showMessage(msg.title, msg.type)
  if (action === 'del' || action === 'active' || action === 'disabled') {
    clearSelection()
  }
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

function goSkuManage(row) {
  router.push({ path: '/product-skus-modern', query: { productId: row.id } })
}

// ═══════════════════════════════════════════════
//  编辑商品档案 drawer methods & computed variables
// ═══════════════════════════════════════════════
const closeDrawer = () => {
  drawerVisible.value = false
}

const resetDrawerState = () => {
  errors.productName = false
  errors.brandId = false
  errors.parentId = false
  errors.categoryId = false
  newAttrKey.value = ''
  newAttrVal.value = ''
}

const openCreateDrawer = () => {
  drawerMode.value = 'create'
  Object.assign(editForm, {
    id: null,
    productCode: '',
    productName: '',
    brandId: '',
    parentId: null,
    categoryId: null,
    status: 1,
    attrs: []
  })
  resetDrawerState()
  drawerVisible.value = true
}

const openEditDrawer = (row) => {
  const rawProduct = products.value.find(p => p.id === row.id)
  if (!rawProduct) return
  
  // Find parent ID in category tree
  let foundParentId = null
  if (rawProduct.categoryId) {
    for (const parent of categories.value) {
      if (parent.id === rawProduct.categoryId) {
        foundParentId = parent.id
        break
      }
      const hasChild = parent.children && parent.children.some(c => c.id === rawProduct.categoryId)
      if (hasChild) {
        foundParentId = parent.id
        break
      }
    }
  }

  Object.assign(editForm, {
    id: rawProduct.id,
    productCode: rawProduct.productCode,
    productName: rawProduct.productName || '',
    brandId: rawProduct.brandId || '',
    parentId: foundParentId,
    categoryId: rawProduct.categoryId || null,
    status: rawProduct.status !== undefined ? rawProduct.status : 1,
    attrs: loadProductAttrs(rawProduct.id, rawProduct.skus?.[0])
  })

  drawerMode.value = 'edit'
  resetDrawerState()
  drawerVisible.value = true
}

const onParentCategoryChange = () => {
  editForm.categoryId = null
}

const childCategories = computed(() => {
  if (!editForm.parentId) return []
  const parent = categories.value.find(p => p.id === editForm.parentId)
  return parent ? parent.children : []
})

const previewThumbStyle = computed(() => {
  return thumbStyle(editForm.productName, editForm.id)
})

const previewThumbText = computed(() => {
  return thumbText(editForm.productName)
})

const previewCategoryName = computed(() => {
  if (!editForm.categoryId) return ''
  const cat = flatCategories.value.find(c => c.id === String(editForm.categoryId))
  return cat ? cat.label.trim() : ''
})

const previewCategoryColor = computed(() => {
  return categoryColor(previewCategoryName.value)
})

const removeEdAttr = (index) => {
  editForm.attrs.splice(index, 1)
}

const addEdAttr = () => {
  const key = newAttrKey.value.trim()
  const val = newAttrVal.value.trim()
  if (!key || !val) {
    showMessage('请填写属性名和属性值', 'error')
    return
  }
  editForm.attrs.push([key, val])
  newAttrKey.value = ''
  newAttrVal.value = ''
}

const saveProductData = async () => {
  errors.productName = !editForm.productName.trim()
  errors.brandId = !editForm.brandId
  errors.parentId = !editForm.parentId
  errors.categoryId = !editForm.categoryId

  if (errors.productName || errors.brandId || errors.parentId || errors.categoryId) {
    showMessage('请完善必填项，且分类必须选择到二级分类', 'error')
    return
  }

  submitting.value = true
  try {
    const payload = {
      productName: editForm.productName,
      categoryId: editForm.categoryId,
      brandId: Number(editForm.brandId),
      status: editForm.status
    }
    if (editForm.productCode) payload.productCode = editForm.productCode

    if (isCreateMode.value) {
      const created = await createProduct(payload)
      if (created?.id && editForm.attrs.length) saveProductAttrs(created.id, editForm.attrs)
      showMessage('商品档案已成功创建')
    } else {
      await updateProduct(editForm.id, payload)
      saveProductAttrs(editForm.id, editForm.attrs)
      showMessage('商品档案已成功更新')
    }
    
    drawerVisible.value = false
    await loadData()
  } catch (error) {
    showMessage(error.message || (isCreateMode.value ? '创建商品档案失败' : '更新商品档案失败'), 'error')
  } finally {
    submitting.value = false
  }
}

function resetQuery() {
  Object.assign(query, {
    keyword: '',
    categoryId: '',
    brandId: '',
    status: 'all',
    filterStatus: '',
    page: 1,
    pageSize: query.pageSize
  })
  clearSelection()
  loadData()
}

function applyRouteKeyword(keyword) {
  const nextKeyword = keyword ? String(keyword) : ''
  if (query.keyword === nextKeyword) return
  query.keyword = nextKeyword
  query.page = 1
  clearSelection()
  loadData()
}

watch(
  () => route.query.keyword,
  (keyword) => {
    applyRouteKeyword(keyword)
  }
)

async function reload() {
  query.page = 1
  clearSelection()
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [productData, categoryData, brandData] = await Promise.all([
      listProducts({ keyword: query.keyword, brandId: query.brandId || undefined, page: 1, pageSize: 100 }),
      getCategoryTree(),
      listBrandOptions()
    ])
    products.value = productData.items || []
    total.value = productData.total || products.value.length
    categories.value = categoryData || []
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

<style scoped>
/* ════ 新建 / 编辑 商品档案 drawer ════ */
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(4, 8, 24, 0.55);
  backdrop-filter: blur(4px);
  z-index: 80;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s;
}
.drawer-mask.show {
  opacity: 1;
  pointer-events: auto;
}
.drawer-panel.ed-drawer {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: min(560px, calc(100% - 48px));
  background: #fff;
  color: #18243d;
  z-index: 81;
  box-shadow: -18px 0 56px rgba(2, 10, 32, 0.30);
  transform: translateX(100%);
  transition: transform 0.3s cubic-bezier(.2, .7, .2, 1);
  display: flex;
  flex-direction: column;
}
.drawer-panel.ed-drawer.show {
  transform: translateX(0);
}
.ed-head { padding: 18px 22px; border-bottom: 1px solid #e4e8f1; display: flex; align-items: center; justify-content: space-between; }
.ed-head .ti { font-size: 16px; font-weight: 600; color: #18243d; display: flex; align-items: center; gap: 9px; }
.ed-head .ti .tag { font-size: 10.5px; font-weight: 500; padding: 2px 8px; border-radius: 999px; letter-spacing: 0.5px; }
.ed-head .ti .tag.new { background: rgba(47,124,255,0.12); color: #2f7cff; }
.ed-head .ti .tag.edit { background: rgba(251,191,36,0.16); color: #92400e; }
.ed-head .x { width: 28px; height: 28px; background: transparent; border: 0; border-radius: 6px; color: #7b86a0; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.ed-head .x:hover { background: #f1f4fa; color: #18243d; }
.ed-body { padding: 20px 22px; overflow: auto; flex: 1; color: #18243d; }
.ed-foot { padding: 14px 22px; border-top: 1px solid #e4e8f1; display: flex; gap: 10px; align-items: center; background: #fafbfe; }

/* live preview hero */
.ed-hero { display: flex; align-items: center; gap: 15px; padding: 16px; background: linear-gradient(135deg, rgba(47,124,255,0.07), rgba(192,132,252,0.05)); border: 1px solid #e4e8f1; border-radius: 12px; margin-bottom: 20px; }
.ed-hero .prod-thumb { width: 60px; height: 60px; font-size: 20px; border-radius: 13px; display: flex; align-items: center; justify-content: center; color: #fff; font-weight: 600; }
.ed-hero .eh-name { font-size: 15.5px; font-weight: 600; color: #18243d; line-height: 1.35; }
.ed-hero .eh-sub { font-size: 11.5px; color: #7b86a0; font-family: "DM Mono", monospace; margin-top: 4px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.ed-section { margin-bottom: 8px; }
.ed-section .sec-label { font-size: 11px; color: #7b86a0; letter-spacing: 1.5px; text-transform: uppercase; font-family: "Orbitron", monospace; margin: 16px 0 4px; display: flex; align-items: center; gap: 7px; }
.ed-section .sec-label::before { content: ""; width: 3px; height: 12px; border-radius: 2px; background: linear-gradient(180deg, #4d9bff, #1e63e0); }

/* base-attribute editor */
.attr-chips { display: flex; flex-wrap: wrap; gap: 7px; margin-bottom: 10px; }
.attr-chip { display: inline-flex; align-items: baseline; gap: 5px; padding: 5px 6px 5px 10px; background: #fff; border: 1px solid #e4e8f1; border-radius: 7px; font-size: 12px; }
.attr-chip .k { color: #a4adc2; font-size: 10.5px; }
.attr-chip .v { color: #18243d; font-weight: 500; }
.attr-chip button { width: 16px; height: 16px; border: 0; background: transparent; color: #7b86a0; cursor: pointer; padding: 0; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; }
.attr-chip button:hover { background: #fde8e8; color: #b91c1c; }
.attr-chip button svg { width: 10px; height: 10px; }
.attr-empty { font-size: 12px; color: #a4adc2; padding: 4px 0; }
.attr-add-row { display: flex; gap: 7px; margin-top: 10px; margin-bottom: 16px; }
.attr-add-row .input { height: 34px; }
.attr-add-row .input.k { flex: 0 0 120px; }
.attr-add-row .input.v { flex: 1; }
.attr-add-row .add-btn { flex-shrink: 0; height: 34px; padding: 0 14px; border: 1px solid #e4e8f1; background: #fff; color: #4a5878; border-radius: 6px; cursor: pointer; font: inherit; font-size: 12.5px; display: inline-flex; align-items: center; gap: 5px; transition: all .15s; }
.attr-add-row .add-btn:hover { border-color: #4d9bff; color: #2f7cff; background: rgba(47,124,255,0.06); }
.attr-add-row .add-btn svg { width: 13px; height: 13px; }

/* Form styles */
.field-row {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 14px;
  align-items: start;
  padding: 14px 0;
  border-bottom: 1px dashed #e4e8f1;
}
.field-row:last-child {
  border-bottom: 0;
}
.field-row > .label {
  font-size: 13px;
  color: #4a5878;
  line-height: 32px;
}
.field-row > .label .req {
  color: #ff6b6b;
  margin-right: 3px;
}
.field-row > .input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}
.field-row .err {
  display: none;
  font-size: 11.5px;
  color: #ff6b6b;
  margin-top: 2px;
}
.field-row.err .err {
  display: block;
}
.field-row.err .input,
.field-row.err .select {
  border-color: #ff6b6b;
  background: #fff8f8;
}

.input, .select {
  height: 36px;
  padding: 0 12px;
  background: #f1f4fa;
  border: 1px solid #e4e8f1;
  border-radius: 6px;
  color: #18243d;
  font: inherit;
  font-size: 13px;
  transition: border-color .15s, box-shadow .15s;
  width: 100%;
  outline: 0;
}
.input::placeholder {
  color: #7b86a0;
}
.input:hover, .select:hover {
  border-color: #cfd6e4;
}
.input:focus, .select:focus {
  border-color: #4d9bff;
  box-shadow: 0 0 0 3px rgba(47,124,255,0.10);
}
.select {
  appearance: none;
  padding-right: 28px;
  cursor: pointer;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%237b86a0' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'/></svg>");
  background-repeat: no-repeat;
  background-position: right 8px center;
}

/* Radio chips styling */
.radio-chips {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}
.radio-chips label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 12.5px;
  border: 1px solid #e4e8f1;
  border-radius: 6px;
  background: #f1f4fa;
  color: #4a5878;
  cursor: pointer;
  transition: all .15s;
}
.radio-chips label:hover {
  color: #18243d;
  border-color: #cfd6e4;
}
.radio-chips input {
  display: none;
}
.radio-chips label.checked {
  border-color: #2f7cff;
  background: rgba(47,124,255,0.10);
  color: #2f7cff;
}

.cat-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 500;
  color: #4a5878;
}
.cat-tag .swatch { width: 8px; height: 8px; border-radius: 2px; flex-shrink: 0; }
</style>
