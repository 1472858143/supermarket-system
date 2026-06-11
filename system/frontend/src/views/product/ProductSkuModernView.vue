<template>
  <div class="product-modern-page product-sku-modern-page">
    <div class="page-head">
      <div>
        <h1>SKU 管理</h1>
        <div class="greet-sub">
          选择商品 SPU，维护下属 SKU 的规格、条码与价格 · 共 <b>{{ formatNumber(productTotal) }}</b> 个 SPU ·
          当前 <b>{{ formatNumber(skuItems.length) }}</b> 个 SKU · 最近同步 {{ syncTime || '--:--' }}
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" @click="safeAction('SKU 模板已准备导出')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出 SKU
        </button>
        <button class="btn" type="button" @click="safeAction('批量导入向导待接入')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          批量导入
        </button>
        <button class="btn primary" type="button" :disabled="!selectedProduct" @click="openSkuCreate">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新增 SKU
        </button>
      </div>
    </div>

    <ProductModuleTabs active="sku" :total-count="productTotal" @action="safeAction" />

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">
      {{ message }}
    </div>

    <div class="sku-workspace">
      <article class="card sku-picker">
        <div class="picker-head">
          <label class="picker-search">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input v-model.trim="productKeyword" placeholder="搜索商品名 / SPU / 品牌" @keyup.enter="loadProducts" />
          </label>
          <button class="icon-refresh" type="button" title="刷新商品" @click="loadProducts">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10" />
              <path d="M20.49 15a9 9 0 0 1-14.85 3.36L1 14" />
            </svg>
          </button>
        </div>

        <div class="picker-list">
          <button
            v-for="product in productRows"
            :key="product.id"
            class="picker-row"
            :class="{ on: selectedProduct?.id === product.id }"
            type="button"
            @click="selectProduct(product.raw)"
          >
            <span class="prod-thumb" :style="product.thumbStyle"><span>{{ product.thumbText }}</span></span>
            <span class="picker-info">
              <span class="name">{{ product.productName }}</span>
              <span class="meta">{{ product.productCode || '保存后生成' }} · {{ product.brandName || '未设置品牌' }}</span>
            </span>
            <span class="sku-count">{{ formatNumber(product.skuCount) }}</span>
          </button>

          <div v-if="productsLoading" class="picker-state">正在加载商品...</div>
          <div v-else-if="!productRows.length" class="picker-state">没有找到匹配的商品</div>
        </div>

        <div v-if="productTotal > productRows.length" class="picker-foot">
          当前显示前 {{ productRows.length }} 个 SPU，可通过搜索缩小范围
        </div>
      </article>

      <section class="sku-main">
        <article v-if="selectedProduct" class="sku-spu-head">
          <div class="prod-thumb big" :style="selectedProductThumbStyle">
            <span>{{ thumbText(selectedProduct.productName) }}</span>
          </div>
          <div class="spu-info">
            <div class="spu-name">{{ selectedProduct.productName }}</div>
            <div class="spu-meta">
              <span>{{ selectedProduct.productCode || '保存后生成' }}</span>
              <span>{{ selectedProduct.brandName || '未设置品牌' }}</span>
              <span>{{ selectedProduct.categoryName || '未分类' }}</span>
            </div>
          </div>
          <div class="spu-actions">
            <button class="btn sm" type="button" :disabled="skuLoading" @click="loadSkus">刷新 SKU</button>
            <button class="btn sm" type="button" @click="goProductArchive">返回档案</button>
          </div>
        </article>

        <article v-else class="sku-empty-card">
          <div class="modern-empty">
            <div class="icon-circle">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                <polygon points="12 2 2 7 12 12 22 7 12 2" />
                <polyline points="2 17 12 22 22 17" />
                <polyline points="2 12 12 17 22 12" />
              </svg>
            </div>
            <h4>请选择商品 SPU</h4>
            <p>从左侧选择商品后维护下属 SKU</p>
          </div>
        </article>

        <article class="card sku-table-card">
          <div class="sku-table-head">
            <div class="sku-stats">
              <span>SKU 总数 <b>{{ formatNumber(skuItems.length) }}</b></span>
              <span>启用 <b class="ok">{{ formatNumber(activeSkuCount) }}</b></span>
              <span>停用 <b class="danger">{{ formatNumber(disabledSkuCount) }}</b></span>
              <span>默认 <b>{{ defaultSkuName }}</b></span>
            </div>
            <div class="head-actions">
              <button class="btn sm" type="button" :disabled="!selectedProduct" @click="safeAction('批量改价待接入')">批量改价</button>
              <button class="btn primary sm" type="button" :disabled="!selectedProduct" @click="openSkuCreate">新增 SKU</button>
            </div>
          </div>

          <div class="modern-product-table sku-table-host">
            <table class="t">
              <thead>
                <tr>
                  <th style="min-width: 220px">SKU 规格</th>
                  <th>SKU 编码</th>
                  <th>条形码</th>
                  <th>售价 / 进价</th>
                  <th>基础单位</th>
                  <th>库存</th>
                  <th>状态</th>
                  <th>默认</th>
                  <th style="width: 120px">操作</th>
                </tr>
              </thead>
              <tbody v-if="skuRows.length">
                <tr v-for="sku in skuRows" :key="sku.id" @click="selectSku(sku.raw)">
                  <td>
                    <div class="sku-name">
                      <b>{{ sku.skuName || '未命名 SKU' }}</b>
                      <span>{{ sku.spec || '默认规格' }}</span>
                    </div>
                  </td>
                  <td><span class="mono">{{ sku.skuCode || '-' }}</span></td>
                  <td><span class="mono">{{ sku.barcode || '-' }}</span></td>
                  <td>
                    <div class="price-stack">
                      <b>{{ money(sku.salePrice) }}</b>
                      <span>进价 {{ money(sku.purchasePrice) }}</span>
                    </div>
                  </td>
                  <td>{{ sku.baseUnit || '-' }}</td>
                  <td><span class="mono">{{ sku.stockText }}</span></td>
                  <td>
                    <span class="pill" :class="sku.statusClass">
                      <span class="dot"></span>{{ sku.statusLabel }}
                    </span>
                  </td>
                  <td>{{ sku.isDefault === 1 ? '是' : '否' }}</td>
                  <td @click.stop>
                    <div class="row-actions">
                      <button type="button" title="编辑 SKU" @click="openSkuEdit(sku.raw)">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M12 20h9" />
                          <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                        </svg>
                      </button>
                      <button type="button" title="单位换算" @click="selectSku(sku.raw)">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M16 3h5v5" />
                          <path d="M8 21H3v-5" />
                          <path d="M21 3l-7 7" />
                          <path d="M3 21l7-7" />
                        </svg>
                      </button>
                      <button class="danger" type="button" title="删除 SKU" :disabled="sku.isDefault === 1" @click="removeSku(sku.raw)">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6l-1 14H6L5 6" />
                          <path d="M10 11v6" />
                          <path d="M14 11v6" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-if="skuLoading" class="modern-loading">正在加载 SKU 数据...</div>
            <div v-else-if="selectedProduct && !skuRows.length" class="modern-empty">
              <div class="icon-circle">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
              </div>
              <h4>暂无 SKU</h4>
              <p>为当前 SPU 新增第一个销售规格</p>
              <button class="btn primary sm" type="button" @click="openSkuCreate">新增 SKU</button>
            </div>
          </div>
        </article>

        <article v-if="selectedSku" class="card unit-card">
          <div class="unit-head">
            <div>
              <h3>单位换算</h3>
              <p>{{ selectedSku.skuName }} · 1 {{ selectedSku.baseUnit || '基础单位' }} 作为库存核算基准</p>
            </div>
            <button class="btn primary sm" type="button" @click="openUnitCreate">新增单位</button>
          </div>
          <div class="table-host">
            <table class="t compact">
              <thead>
                <tr>
                  <th>单位名称</th>
                  <th>换算关系</th>
                  <th style="width: 90px">操作</th>
                </tr>
              </thead>
              <tbody v-if="selectedSkuUnits.length">
                <tr v-for="unit in selectedSkuUnits" :key="unit.id">
                  <td><b>{{ unit.unitName }}</b></td>
                  <td>1 {{ unit.unitName }} = {{ unit.conversionRate }} {{ selectedSku.baseUnit }}</td>
                  <td>
                    <div class="row-actions">
                      <button type="button" title="编辑单位" @click="openUnitEdit(unit)">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M12 20h9" />
                          <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                        </svg>
                      </button>
                      <button class="danger" type="button" title="删除单位" @click="removeUnit(unit)">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6l-1 14H6L5 6" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!selectedSkuUnits.length" class="unit-empty">暂无单位换算</div>
          </div>
        </article>
      </section>
    </div>

    <div v-if="skuFormVisible" class="sku-modal-mask" @click.self="skuFormVisible = false">
      <section class="sku-modal-card">
        <header class="modal-head">
          <h3>{{ editingSkuId ? '编辑 SKU' : '新增 SKU' }}</h3>
          <button class="close-x" type="button" aria-label="关闭弹窗" @click="skuFormVisible = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>
        <div class="modal-body">
          <div class="form-grid">
            <label class="form-item full">
              <span class="form-label"><span class="req">*</span>SKU 名称</span>
              <input v-model.trim="skuForm.skuName" class="input" maxlength="60" placeholder="例：原味 205g*12" />
            </label>
            <label class="form-item full">
              <span class="form-label"><span class="req">*</span>规格描述</span>
              <input v-model.trim="skuForm.spec" class="input" maxlength="80" placeholder="例：205g*12 / 箱" />
            </label>
            <label class="form-item">
              <span class="form-label">条码</span>
              <input v-model.trim="skuForm.barcode" class="input mono" maxlength="40" placeholder="例：6901234567890" />
            </label>
            <label class="form-item">
              <span class="form-label"><span class="req">*</span>基础单位</span>
              <input v-model.trim="skuForm.baseUnit" class="input" maxlength="12" placeholder="例：瓶、袋、箱" />
            </label>
            <label class="form-item">
              <span class="form-label"><span class="req">*</span>进价</span>
              <input v-model.number="skuForm.purchasePrice" class="input mono" type="number" min="0" step="0.01" />
            </label>
            <label class="form-item">
              <span class="form-label"><span class="req">*</span>售价</span>
              <input v-model.number="skuForm.salePrice" class="input mono" type="number" min="0" step="0.01" />
            </label>
            <label class="form-item full">
              <span class="form-label">状态</span>
              <select v-model.number="skuForm.status" class="select">
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </label>
          </div>
        </div>
        <footer class="modal-foot">
          <button class="btn" type="button" @click="skuFormVisible = false">取消</button>
          <button class="btn primary" type="button" :disabled="skuSubmitting" @click="submitSku">
            {{ skuSubmitting ? '提交中...' : '保存' }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="unitFormVisible" class="sku-modal-mask" @click.self="unitFormVisible = false">
      <section class="sku-modal-card small">
        <header class="modal-head">
          <h3>{{ editingUnitId ? '编辑单位换算' : '新增单位换算' }}</h3>
          <button class="close-x" type="button" aria-label="关闭弹窗" @click="unitFormVisible = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>
        <div class="modal-body">
          <div class="form-grid">
            <label class="form-item">
              <span class="form-label"><span class="req">*</span>单位名称</span>
              <input v-model.trim="unitForm.unitName" class="input" maxlength="16" placeholder="例：箱" />
            </label>
            <label class="form-item">
              <span class="form-label"><span class="req">*</span>换算比例</span>
              <input v-model.number="unitForm.conversionRate" class="input mono" type="number" min="1" step="1" />
            </label>
          </div>
        </div>
        <footer class="modal-foot">
          <button class="btn" type="button" @click="unitFormVisible = false">取消</button>
          <button class="btn primary" type="button" :disabled="unitSubmitting" @click="submitUnit">
            {{ unitSubmitting ? '提交中...' : '保存' }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listProducts } from '../../api/product'
import { createSku, createUnit, deleteSku, deleteUnit, listSkus, updateSku, updateUnit } from '../../api/sku'
import ProductModuleTabs from './components/ProductModuleTabs.vue'

const route = useRoute()
const router = useRouter()

const productKeyword = ref('')
const productItems = ref([])
const productTotal = ref(0)
const productsLoading = ref(false)
const skuLoading = ref(false)
const skuSubmitting = ref(false)
const unitSubmitting = ref(false)
const selectedProduct = ref(null)
const selectedSku = ref(null)
const skuItems = ref([])
const message = ref('')
const messageType = ref('success')
const syncTime = ref('')
const skuFormVisible = ref(false)
const unitFormVisible = ref(false)
const editingSkuId = ref(null)
const editingUnitId = ref(null)

const skuForm = reactive({
  skuName: '',
  spec: '',
  barcode: '',
  baseUnit: '个',
  purchasePrice: 0,
  salePrice: 0,
  status: 1
})

const unitForm = reactive({
  unitName: '',
  conversionRate: 1
})

const palettes = [
  ['#4d9bff', '#1e63e0'],
  ['#34d399', '#0f766e'],
  ['#fbbf24', '#d97706'],
  ['#f472b6', '#be185d'],
  ['#c084fc', '#7c3aed'],
  ['#22d3ee', '#0e7490']
]

const productRows = computed(() =>
  productItems.value.map((product) => ({
    id: product.id,
    raw: product,
    productCode: product.productCode,
    productName: product.productName,
    brandName: product.brandName,
    skuCount: Array.isArray(product.skus) ? product.skus.length : 0,
    thumbText: thumbText(product.productName),
    thumbStyle: thumbStyle(product.productName, product.id)
  }))
)

const selectedProductThumbStyle = computed(() =>
  thumbStyle(selectedProduct.value?.productName || '商品', selectedProduct.value?.id || 0)
)

const skuRows = computed(() =>
  skuItems.value
    .slice()
    .sort((a, b) => Number(b.isDefault === 1) - Number(a.isDefault === 1))
    .map((sku) => ({
      ...sku,
      raw: sku,
      statusLabel: sku.status === 0 ? '停用' : '启用',
      statusClass: sku.status === 0 ? 'mute' : 'ok',
      stockText: resolveStockText(sku)
    }))
)

const activeSkuCount = computed(() => skuItems.value.filter((sku) => sku.status !== 0).length)
const disabledSkuCount = computed(() => skuItems.value.filter((sku) => sku.status === 0).length)
const defaultSkuName = computed(() => skuItems.value.find((sku) => sku.isDefault === 1)?.skuName || '-')
const selectedSkuUnits = computed(() => selectedSku.value?.units || [])

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value) {
  const amount = Number(value)
  if (Number.isNaN(amount)) return '¥0.00'
  return `¥${amount.toFixed(2)}`
}

function thumbText(text) {
  return [...String(text || '商品')].slice(0, 2).join('')
}

function hashText(text) {
  return [...String(text || '')].reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

function thumbStyle(name, id) {
  const index = Math.abs(Number(id) || hashText(name)) % palettes.length
  const [from, to] = palettes[index]
  return { background: `linear-gradient(135deg, ${from}, ${to})` }
}

function resolveStockText(sku) {
  const stock = sku.currentStock ?? sku.stockQuantity ?? sku.quantity ?? sku.availableQuantity
  if (stock === undefined || stock === null || stock === '') return '-'
  return formatNumber(stock)
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

function syncProductQuery(productId) {
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      productId: String(productId)
    }
  })
}

async function loadProducts() {
  productsLoading.value = true
  try {
    const data = await listProducts({
      keyword: productKeyword.value || undefined,
      page: 1,
      pageSize: 100
    })
    productItems.value = data.items || []
    productTotal.value = data.total || productItems.value.length
    syncTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

    const routeProductId = route.query.productId ? String(route.query.productId) : ''
    const preferredId = routeProductId || (selectedProduct.value ? String(selectedProduct.value.id) : '')
    const matched = preferredId
      ? productItems.value.find((product) => String(product.id) === preferredId)
      : null

    if (matched) {
      await selectProduct(matched, { syncRoute: false })
    } else if (productItems.value.length) {
      if (routeProductId) {
        showMessage('未在当前商品列表中找到指定 SPU，请搜索后选择', 'error')
      }
      await selectProduct(productItems.value[0], { syncRoute: true })
    } else {
      selectedProduct.value = null
      selectedSku.value = null
      skuItems.value = []
    }
  } catch (error) {
    showMessage(error.message || '商品数据加载失败', 'error')
  } finally {
    productsLoading.value = false
  }
}

async function selectProduct(product, options = {}) {
  if (!product) return
  const { syncRoute = true } = options
  selectedProduct.value = product
  selectedSku.value = null
  if (syncRoute) {
    syncProductQuery(product.id)
  }
  await loadSkus()
}

async function loadSkus() {
  if (!selectedProduct.value) return
  skuLoading.value = true
  try {
    skuItems.value = await listSkus(selectedProduct.value.id)
    if (selectedSku.value) {
      selectedSku.value = skuItems.value.find((sku) => sku.id === selectedSku.value.id) || null
    }
  } catch (error) {
    showMessage(error.message || 'SKU 加载失败', 'error')
  } finally {
    skuLoading.value = false
  }
}

async function refreshSkusAndSelection(skuId = selectedSku.value?.id) {
  await loadSkus()
  selectedSku.value = skuItems.value.find((sku) => sku.id === skuId) || null
}

function goProductArchive() {
  router.push('/products-modern')
}

function selectSku(sku) {
  selectedSku.value = sku
}

function resetSkuForm() {
  Object.assign(skuForm, {
    skuName: '',
    spec: '',
    barcode: '',
    baseUnit: '个',
    purchasePrice: 0,
    salePrice: 0,
    status: 1
  })
  editingSkuId.value = null
}

function openSkuCreate() {
  if (!selectedProduct.value) {
    showMessage('请先选择商品 SPU', 'error')
    return
  }
  resetSkuForm()
  skuFormVisible.value = true
}

function openSkuEdit(sku) {
  editingSkuId.value = sku.id
  Object.assign(skuForm, {
    skuName: sku.skuName || '',
    spec: sku.spec || '',
    barcode: sku.barcode || '',
    baseUnit: sku.baseUnit || '个',
    purchasePrice: Number(sku.purchasePrice || 0),
    salePrice: Number(sku.salePrice || 0),
    status: sku.status === 0 ? 0 : 1
  })
  skuFormVisible.value = true
}

function normalizeNumber(value) {
  const amount = Number(value)
  return Number.isNaN(amount) ? 0 : amount
}

async function submitSku() {
  const purchasePrice = normalizeNumber(skuForm.purchasePrice)
  const salePrice = normalizeNumber(skuForm.salePrice)
  if (!skuForm.skuName || !skuForm.spec || !skuForm.baseUnit) {
    showMessage('请填写 SKU 名称、规格和基础单位', 'error')
    return
  }
  if (purchasePrice < 0 || salePrice < 0) {
    showMessage('进价和售价不能小于 0', 'error')
    return
  }
  if (salePrice < purchasePrice) {
    showMessage('售价不能小于进价', 'error')
    return
  }

  skuSubmitting.value = true
  try {
    const payload = {
      skuName: skuForm.skuName,
      spec: skuForm.spec,
      barcode: skuForm.barcode,
      baseUnit: skuForm.baseUnit,
      purchasePrice,
      salePrice,
      status: skuForm.status
    }
    let savedSku = null
    if (editingSkuId.value) {
      savedSku = await updateSku(selectedProduct.value.id, editingSkuId.value, payload)
    } else {
      savedSku = await createSku(selectedProduct.value.id, payload)
    }
    skuFormVisible.value = false
    showMessage('SKU 保存成功')
    await refreshSkusAndSelection(savedSku?.id || editingSkuId.value)
  } catch (error) {
    showMessage(error.message || 'SKU 保存失败', 'error')
  } finally {
    skuSubmitting.value = false
  }
}

async function removeSku(sku) {
  if (sku.isDefault === 1) {
    showMessage('默认 SKU 不能删除', 'error')
    return
  }
  if (!window.confirm(`确认删除 SKU ${sku.skuName}？`)) return
  try {
    await deleteSku(selectedProduct.value.id, sku.id)
    showMessage('SKU 删除成功')
    await refreshSkusAndSelection()
  } catch (error) {
    showMessage(error.message || 'SKU 删除失败', 'error')
  }
}

function resetUnitForm() {
  Object.assign(unitForm, {
    unitName: '',
    conversionRate: 1
  })
  editingUnitId.value = null
}

function openUnitCreate() {
  if (!selectedSku.value) {
    showMessage('请先选择 SKU', 'error')
    return
  }
  resetUnitForm()
  unitFormVisible.value = true
}

function openUnitEdit(unit) {
  editingUnitId.value = unit.id
  Object.assign(unitForm, {
    unitName: unit.unitName || '',
    conversionRate: Number(unit.conversionRate || 1)
  })
  unitFormVisible.value = true
}

async function submitUnit() {
  if (!selectedSku.value) return
  const conversionRate = Number(unitForm.conversionRate)
  if (!unitForm.unitName || Number.isNaN(conversionRate) || conversionRate < 1) {
    showMessage('请填写单位名称，且换算比例不能小于 1', 'error')
    return
  }
  unitSubmitting.value = true
  try {
    const payload = {
      unitName: unitForm.unitName,
      conversionRate
    }
    const skuId = selectedSku.value.id
    if (editingUnitId.value) {
      await updateUnit(selectedProduct.value.id, skuId, editingUnitId.value, payload)
    } else {
      await createUnit(selectedProduct.value.id, skuId, payload)
    }
    unitFormVisible.value = false
    showMessage('单位换算保存成功')
    await refreshSkusAndSelection(skuId)
  } catch (error) {
    showMessage(error.message || '单位换算保存失败', 'error')
  } finally {
    unitSubmitting.value = false
  }
}

async function removeUnit(unit) {
  if (!selectedSku.value) return
  if (!window.confirm(`确认删除单位 ${unit.unitName}？`)) return
  try {
    const skuId = selectedSku.value.id
    await deleteUnit(selectedProduct.value.id, skuId, unit.id)
    showMessage('单位换算删除成功')
    await refreshSkusAndSelection(skuId)
  } catch (error) {
    showMessage(error.message || '单位换算删除失败', 'error')
  }
}

onMounted(loadProducts)
</script>

<style scoped>
.product-sku-modern-page .btn svg {
  width: 14px;
  height: 14px;
}

.product-sku-modern-page .btn.primary {
  color: #ffffff;
  background: linear-gradient(135deg, var(--brand-bright), var(--brand-glow));
  border: 1px solid transparent;
  box-shadow: 0 6px 18px rgba(47, 124, 255, 0.18);
}

.product-sku-modern-page .btn.sm {
  min-height: 32px;
  height: 32px;
  padding: 0 11px;
  border-radius: 7px;
  font-size: 12.5px;
}

.sku-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.sku-picker {
  padding: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh / var(--admin-zoom) - 210px);
}

.picker-head {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--line);
}

.picker-search {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 11px;
  background: var(--bg-elev);
  border: 1px solid var(--line);
  border-radius: 8px;
  color: var(--text-mute);
}

.picker-search:focus-within {
  background: #ffffff;
  border-color: var(--brand-bright);
  box-shadow: 0 0 0 3px var(--brand-soft);
}

.picker-search svg,
.icon-refresh svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
}

.picker-search input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: 0;
  outline: 0;
  color: var(--text);
  font-size: 12.5px;
}

.icon-refresh {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: #ffffff;
  color: var(--text-mute);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.icon-refresh:hover {
  color: var(--brand);
  border-color: var(--brand-bright);
  background: var(--brand-soft);
}

.picker-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.picker-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border: 0;
  border-left: 3px solid transparent;
  border-bottom: 1px solid var(--line);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.picker-row:hover {
  background: var(--bg-hover);
}

.picker-row.on {
  background: var(--brand-soft);
  border-left-color: var(--brand);
}

.picker-info {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.picker-info .name {
  color: var(--text);
  font-size: 12.5px;
  font-weight: 550;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.picker-info .meta {
  color: var(--text-faint);
  font-size: 10.5px;
  font-family: "DM Mono", monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sku-count {
  min-width: 28px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--bg-elev);
  color: var(--text-mute);
  font-family: "DM Mono", monospace;
  font-size: 11px;
  text-align: center;
}

.picker-row.on .sku-count {
  background: #ffffff;
  color: var(--brand);
}

.picker-state,
.picker-foot {
  padding: 18px 14px;
  color: var(--text-mute);
  font-size: 12.5px;
  text-align: center;
}

.picker-foot {
  border-top: 1px solid var(--line);
  background: #fafbfe;
}

.sku-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sku-spu-head,
.sku-empty-card {
  background: linear-gradient(135deg, rgba(47, 124, 255, 0.06), rgba(192, 132, 252, 0.04)), var(--bg-card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: 0 1px 2px rgba(20, 40, 80, 0.04);
}

.sku-spu-head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
}

.prod-thumb.big {
  width: 54px;
  height: 54px;
  font-size: 15px;
  border-radius: 12px;
}

.spu-info {
  flex: 1;
  min-width: 0;
}

.spu-name {
  font-size: 16px;
  font-weight: 650;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.spu-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 5px;
  color: var(--text-mute);
  font-family: "DM Mono", monospace;
  font-size: 12px;
}

.spu-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sku-empty-card {
  padding: 0;
}

.sku-table-card {
  padding: 0;
  overflow: hidden;
}

.sku-table-head {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--line);
  background: rgba(47, 124, 255, 0.03);
  flex-wrap: wrap;
}

.sku-stats {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--text-mute);
}

.sku-stats b {
  color: var(--text);
  font-weight: 600;
  font-family: "DM Mono", monospace;
}

.sku-stats b.ok {
  color: var(--ok);
}

.sku-stats b.danger {
  color: var(--danger);
}

.head-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.sku-table-host table.t {
  min-width: 980px;
}

.sku-name,
.price-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sku-name span,
.price-stack span {
  color: var(--text-mute);
  font-size: 11.5px;
}

.mono {
  font-family: "DM Mono", monospace;
  font-size: 11.5px;
}

.unit-card {
  padding: 0;
  overflow: hidden;
}

.unit-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

.unit-head h3 {
  margin: 0;
  color: var(--text);
  font-size: 15px;
  font-weight: 650;
}

.unit-head p {
  margin: 4px 0 0;
  color: var(--text-mute);
  font-size: 12.5px;
}

.unit-empty {
  padding: 26px 16px;
  text-align: center;
  color: var(--text-mute);
  font-size: 13px;
}

.sku-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(8, 20, 58, 0.38);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.sku-modal-card {
  width: min(560px, calc(100vw - 32px));
  max-height: calc(100vh - 40px);
  overflow: auto;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 12px;
  box-shadow: 0 24px 70px rgba(8, 20, 58, 0.26);
}

.sku-modal-card.small {
  width: min(460px, calc(100vw - 32px));
}

.modal-head,
.modal-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  padding: 0 18px;
  border-bottom: 1px solid var(--line);
}

.modal-head h3 {
  margin: 0;
  font-size: 17px;
  color: var(--text);
  font-weight: 650;
}

.modal-body {
  padding: 18px;
}

.modal-foot {
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--line);
  border-bottom: 0;
  background: #fafbfe;
}

.close-x {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  border: 0;
  background: transparent;
  color: var(--text-mute);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.close-x:hover {
  color: var(--text);
  background: var(--bg-elev);
}

.close-x svg {
  width: 15px;
  height: 15px;
}

.req {
  color: var(--danger);
  margin-right: 2px;
}

@media (max-width: 1100px) {
  .sku-workspace {
    grid-template-columns: 1fr;
  }

  .sku-picker {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .sku-spu-head,
  .unit-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .head-actions,
  .spu-actions {
    width: 100%;
  }

  .head-actions .btn,
  .spu-actions .btn {
    flex: 1;
  }
}
</style>
