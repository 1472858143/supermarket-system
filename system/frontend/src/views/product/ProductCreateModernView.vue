<template>
  <div class="product-modern-page product-create-modern-page">
    <div class="page-head">
      <div>
        <h1>新建商品</h1>
        <div class="greet-sub">
          录入商品基础资料与 SKU 设置 · 带 <b>*</b> 为必填项 · 保存后自动回到商品管理新版
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" @click="goList">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5" />
            <path d="M12 19l-7-7 7-7" />
          </svg>
          返回列表
        </button>
        <button class="btn primary" type="button" :disabled="submitting" @click="submitProduct">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
            <path d="M17 21v-8H7v8" />
            <path d="M7 3v5h8" />
          </svg>
          {{ submitting ? '保存中...' : '保存商品' }}
        </button>
      </div>
    </div>

    <ProductModuleTabs active="create" />

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">
      {{ message }}
    </div>

    <div class="product-editor-grid">
      <aside class="product-editor-toc">
        <a class="on" href="#product-basic">
          <span class="step">1</span>
          基本信息
        </a>
        <a href="#product-skus">
          <span class="step">2</span>
          规格属性
        </a>
      </aside>

      <main class="product-editor-main">
        <article id="product-basic" class="card product-form-card">
          <header class="card-head">
            <div class="title-block">
              <h3>基本信息</h3>
              <div class="sub">对齐现有商品属性：编号、名称、分类</div>
            </div>
            <span class="pill ok"><span class="dot"></span>必填</span>
          </header>

          <div class="field-row">
            <div class="label"><span class="req">*</span>商品编号</div>
            <div class="input-wrap">
              <div class="inline-control">
                <input v-model.trim="productForm.productCode" class="input mono" maxlength="40" placeholder="例：P-10001" />
                <button class="btn sm" type="button" @click="generateProductCode">生成</button>
              </div>
              <div class="hint-line">商品编号创建后不可在旧版维护页中修改，请按门店编码规则填写。</div>
            </div>
          </div>

          <div class="field-row">
            <div class="label"><span class="req">*</span>商品名称</div>
            <div class="input-wrap wide">
              <input v-model.trim="productForm.productName" class="input" maxlength="60" placeholder="例：伊利安慕希希腊式酸奶 205g*12" />
            </div>
          </div>

          <div class="field-row last">
            <div class="label"><span class="req">*</span>商品分类</div>
            <div class="input-wrap">
              <select v-model.number="productForm.categoryId" class="select">
                <option :value="null" disabled>请选择商品分类</option>
                <option v-for="category in flatCategories" :key="category.id" :value="category.id">
                  {{ category.label }}
                </option>
              </select>
              <div class="hint-line">分类来源于商品分类新版，当前仅保存分类 ID。</div>
            </div>
          </div>
          <div class="field-row last">
            <div class="label"><span class="req">*</span>商品品牌</div>
            <div class="input-wrap">
              <select v-model.number="productForm.brandId" class="select">
                <option :value="null" disabled>请选择商品品牌</option>
                <option v-for="brand in brandOptions" :key="brand.id" :value="brand.id">
                  {{ brand.brandName }}
                </option>
              </select>
              <div class="hint-line">品牌保存到商品SPU主档，不直接绑定SKU。</div>
            </div>
          </div>
        </article>

        <article id="product-skus" class="card product-form-card">
          <header class="card-head">
            <div class="title-block">
              <h3>规格属性</h3>
              <div class="sub">展示 SKU 列表，并在右侧填写 SKU 设置</div>
            </div>
            <button class="btn sm" type="button" @click="addSku">新增SKU</button>
          </header>

          <div class="sku-editor-layout">
            <section class="sku-list-panel">
              <div class="sku-list-head">
                <div>
                  <h4>SKU列表</h4>
                  <span>{{ skuRows.length }} 条规格</span>
                </div>
                <button class="btn sm" type="button" @click="addSku">添加规格</button>
              </div>

              <div class="sku-list-table">
                <table>
                  <thead>
                    <tr>
                      <th>SKU名称</th>
                      <th>规格描述</th>
                      <th>售价</th>
                      <th>默认</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody v-if="skuRows.length">
                    <tr v-for="(sku, index) in skuRows" :key="sku.localId" :class="{ active: editingSkuIndex === index }" @click="editSku(index)">
                      <td>
                        <b>{{ sku.skuName || '未命名SKU' }}</b>
                        <span>{{ sku.barcode || '暂无条码' }}</span>
                      </td>
                      <td>{{ sku.spec || '--' }}</td>
                      <td>{{ money(sku.salePrice) }}</td>
                      <td>
                        <span class="pill" :class="index === 0 ? 'ok' : 'mute'">
                          <span class="dot"></span>{{ index === 0 ? '默认' : '普通' }}
                        </span>
                      </td>
                      <td @click.stop>
                        <div class="row-actions">
                          <button type="button" title="编辑" @click="editSku(index)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <path d="M12 20h9" />
                              <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                            </svg>
                          </button>
                          <button class="danger" type="button" title="删除" :disabled="skuRows.length === 1" @click="removeSku(index)">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                              <path d="M3 6h18" />
                              <path d="M8 6V4h8v2" />
                              <path d="M19 6l-1 14H6L5 6" />
                            </svg>
                          </button>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="!skuRows.length" class="sku-empty">暂无 SKU，点击“新增SKU”填写规格属性。</div>
              </div>
            </section>

            <section class="sku-editor-panel">
              <header>
                <div>
                  <h4>SKU设置</h4>
                  <span>当前编辑：{{ editingSkuIndex + 1 }} / {{ skuRows.length }}</span>
                </div>
                <span class="pill info"><span class="dot"></span>保存时生成编码</span>
              </header>

              <div class="sku-form-grid">
                <label class="form-item">
                  <span class="form-label"><span class="req">*</span>SKU名称</span>
                  <input v-model.trim="skuForm.skuName" class="input" maxlength="60" placeholder="例：原味 205g*12" @input="syncSkuForm" />
                </label>
                <label class="form-item">
                  <span class="form-label"><span class="req">*</span>规格描述</span>
                  <input v-model.trim="skuForm.spec" class="input" maxlength="80" placeholder="例：205g*12 / 箱" @input="syncSkuForm" />
                </label>
                <label class="form-item">
                  <span class="form-label">条码</span>
                  <input v-model.trim="skuForm.barcode" class="input mono" maxlength="40" placeholder="例：6901234567890" @input="syncSkuForm" />
                </label>
                <label class="form-item">
                  <span class="form-label"><span class="req">*</span>基础单位</span>
                  <input v-model.trim="skuForm.baseUnit" class="input" maxlength="12" placeholder="例：瓶、袋、箱" @input="syncSkuForm" />
                </label>
                <label class="form-item">
                  <span class="form-label"><span class="req">*</span>进价</span>
                  <input v-model.number="skuForm.purchasePrice" class="input" type="number" min="0" step="0.01" @input="syncSkuForm" />
                </label>
                <label class="form-item">
                  <span class="form-label"><span class="req">*</span>售价</span>
                  <input v-model.number="skuForm.salePrice" class="input" type="number" min="0" step="0.01" @input="syncSkuForm" />
                </label>
              </div>
            </section>
          </div>
        </article>
      </main>

      <aside class="product-preview-card">
        <div class="prod-thumb" :style="previewStyle">
          <span>{{ previewText }}</span>
        </div>
        <h3>{{ productForm.productName || '商品名称' }}</h3>
        <p>{{ productForm.productCode || '商品编号待填写' }}</p>
        <div class="preview-price">{{ money(defaultSku.salePrice) }}</div>
        <dl>
          <div>
            <dt>分类</dt>
            <dd>{{ selectedCategoryName || '未选择' }}</dd>
          </div>
          <div>
            <dt>品牌</dt>
            <dd>{{ selectedBrandName || '未选择' }}</dd>
          </div>
          <div>
            <dt>默认SKU</dt>
            <dd>{{ defaultSku.skuName || '待填写' }}</dd>
          </div>
          <div>
            <dt>基础单位</dt>
            <dd>{{ defaultSku.baseUnit || '--' }}</dd>
          </div>
        </dl>
      </aside>
    </div>

    <div class="product-action-bar">
      <span>将保存 1 个商品和 {{ skuRows.length }} 个 SKU，首个 SKU 自动作为默认 SKU。</span>
      <div>
        <button class="btn" type="button" @click="goList">取消</button>
        <button class="btn primary" type="button" :disabled="submitting" @click="submitProduct">
          {{ submitting ? '保存中...' : '保存商品' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryTree } from '../../api/category'
import { listBrandOptions } from '../../api/brand'
import { createProduct } from '../../api/product'
import { createSku } from '../../api/sku'
import ProductModuleTabs from './components/ProductModuleTabs.vue'

const router = useRouter()
const categories = ref([])
const brandOptions = ref([])
const submitting = ref(false)
const message = ref('')
const messageType = ref('success')
const editingSkuIndex = ref(0)

const productForm = reactive({
  productCode: '',
  productName: '',
  categoryId: null,
  brandId: null,
  status: 1
})

const skuRows = ref([createEmptySku()])
const skuForm = reactive({ ...skuRows.value[0] })

const flatCategories = computed(() => flattenCategories(categories.value))
const categoryLookup = computed(() => new Map(flatCategories.value.map((item) => [String(item.id), item.name])))
const selectedCategoryName = computed(() => categoryLookup.value.get(String(productForm.categoryId)) || '')
const brandLookup = computed(() => new Map(brandOptions.value.map((item) => [String(item.id), item.brandName])))
const selectedBrandName = computed(() => brandLookup.value.get(String(productForm.brandId)) || '')
const defaultSku = computed(() => skuRows.value[0] || createEmptySku())
const previewText = computed(() => [...(productForm.productName || '商品')].slice(0, 2).join(''))
const previewStyle = computed(() => {
  const colors = [
    ['#4d9bff', '#1e63e0'],
    ['#34d399', '#0f766e'],
    ['#fbbf24', '#d97706'],
    ['#f472b6', '#be185d']
  ]
  const index = hashText(productForm.productName || productForm.productCode) % colors.length
  return { background: `linear-gradient(135deg, ${colors[index][0]}, ${colors[index][1]})` }
})

function createEmptySku() {
  return {
    localId: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    skuName: '',
    spec: '',
    barcode: '',
    baseUnit: '个',
    purchasePrice: 0,
    salePrice: 0,
    status: 1
  }
}

function flattenCategories(nodes, depth = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({
      id: node.id,
      name: node.name,
      label: `${'　'.repeat(depth)}${node.name}`
    })
    result.push(...flattenCategories(node.children || [], depth + 1))
  }
  return result
}

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

function goList() {
  router.push('/products-modern')
}

function generateProductCode() {
  productForm.productCode = `P-${new Date().toISOString().slice(2, 10).replaceAll('-', '')}${Math.floor(100 + Math.random() * 900)}`
}

function addSku() {
  const sku = createEmptySku()
  skuRows.value.push(sku)
  editSku(skuRows.value.length - 1)
}

function editSku(index) {
  editingSkuIndex.value = index
  Object.assign(skuForm, skuRows.value[index])
}

function syncSkuForm() {
  const target = skuRows.value[editingSkuIndex.value]
  if (!target) return
  Object.assign(target, {
    skuName: skuForm.skuName,
    spec: skuForm.spec,
    barcode: skuForm.barcode,
    baseUnit: skuForm.baseUnit,
    purchasePrice: normalizeNumber(skuForm.purchasePrice),
    salePrice: normalizeNumber(skuForm.salePrice),
    status: 1
  })
}

function removeSku(index) {
  if (skuRows.value.length === 1) {
    showMessage('至少保留一个 SKU', 'error')
    return
  }
  skuRows.value.splice(index, 1)
  editSku(Math.min(index, skuRows.value.length - 1))
}

function validateProduct() {
  syncSkuForm()
  if (!productForm.productCode || !productForm.productName || !productForm.categoryId || !productForm.brandId) {
    return '请填写商品编号、商品名称、商品分类和商品品牌'
  }
  if (!skuRows.value.length) {
    return '请至少填写一个 SKU'
  }
  for (const [index, sku] of skuRows.value.entries()) {
    const rowNumber = index + 1
    const purchasePrice = Number(sku.purchasePrice)
    const salePrice = Number(sku.salePrice)
    if (!sku.skuName || !sku.spec || !sku.baseUnit) {
      editSku(index)
      return `请补全第 ${rowNumber} 个 SKU 的名称、规格和基础单位`
    }
    if (Number.isNaN(purchasePrice) || Number.isNaN(salePrice) || purchasePrice < 0 || salePrice < 0) {
      editSku(index)
      return `请填写第 ${rowNumber} 个 SKU 的有效进价和售价`
    }
    if (salePrice < purchasePrice) {
      editSku(index)
      return `第 ${rowNumber} 个 SKU 的售价不能低于进价`
    }
  }
  return ''
}

async function submitProduct() {
  const error = validateProduct()
  if (error) {
    showMessage(error, 'error')
    return
  }

  submitting.value = true
  try {
    const product = await createProduct({
      productCode: productForm.productCode,
      productName: productForm.productName,
      categoryId: productForm.categoryId,
      brandId: productForm.brandId,
      status: 1
    })

    for (const sku of skuRows.value) {
      await createSku(product.id, {
        skuName: sku.skuName,
        spec: sku.spec,
        barcode: sku.barcode,
        baseUnit: sku.baseUnit,
        purchasePrice: normalizeNumber(sku.purchasePrice),
        salePrice: normalizeNumber(sku.salePrice),
        status: 1
      })
    }

    showMessage('商品保存成功')
    window.setTimeout(() => {
      router.push('/products-modern')
    }, 500)
  } catch (requestError) {
    showMessage(requestError.message || '商品保存失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategoryTree()
  } catch (error) {
    showMessage(error.message || '分类加载失败', 'error')
  }
}

async function loadBrands() {
  try {
    brandOptions.value = await listBrandOptions()
  } catch (error) {
    showMessage(error.message || '品牌加载失败', 'error')
  }
}

function normalizeNumber(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function money(value) {
  return `￥ ${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function hashText(text) {
  return [...(text || '')].reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

onMounted(() => {
  loadCategories()
  loadBrands()
})
</script>
