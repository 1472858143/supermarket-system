<template>
  <div class="stockcheck-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">盘点管理</h1>
        <p class="page-desc">按范围创建盘点单，系统自动展开批次，完成后同步批次库存和SKU总库存</p>
      </div>
    </div>

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>

    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="盘点单号、名称、状态或分类" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增盘点单</PermissionButton>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" empty-text="暂无盘点单" @page-change="changePage">
        <template #cell-scopeType="{ item }">
          {{ scopeLabel(item) }}
        </template>
        <template #cell-status="{ item }">
          <span class="tag" :class="item.status === 'COMPLETED' ? 'tag-success' : 'tag-warning'">{{ statusLabel(item.status) }}</span>
        </template>
        <template #cell-totalDifference="{ item }">
          <span :class="differenceClass(item.totalDifference)">{{ item.totalDifference }}</span>
        </template>
        <template #cell-createTime="{ item }">
          {{ formatTime(item.createTime) }}
        </template>
        <template #cell-completeTime="{ item }">
          {{ formatTime(item.completeTime) }}
        </template>
        <template #actions="{ item }">
          <button class="btn btn-ghost" type="button" @click="openDetail(item.id)">查看</button>
        </template>
      </BaseTable>
    </section>

    <BaseDialog v-model="createDialogVisible" title="新增盘点单">
      <form class="form-grid" @submit.prevent>
        <label class="form-item full">
          <span class="form-label">盘点名称</span>
          <input v-model.trim="form.name" class="input" maxlength="100" placeholder="例如：6月冻品盘点" />
        </label>
        <label class="form-item">
          <span class="form-label">盘点范围</span>
          <select v-model="form.scopeType" class="select" @change="handleScopeChange">
            <option value="ALL">全部商品</option>
            <option value="CATEGORY_LEVEL1">一级分类</option>
            <option value="CATEGORY_LEVEL2">二级分类</option>
            <option value="SKU">指定SKU</option>
          </select>
        </label>
        <label v-if="form.scopeType === 'CATEGORY_LEVEL1'" class="form-item">
          <span class="form-label">一级分类</span>
          <select v-model.number="form.categoryId" class="select">
            <option :value="null" disabled>请选择一级分类</option>
            <option v-for="category in categoryTree" :key="category.id" :value="category.id">{{ category.name }}</option>
          </select>
        </label>
        <div v-if="form.scopeType === 'CATEGORY_LEVEL2'" class="selector-grid full">
          <label class="form-item">
            <span class="form-label">一级分类</span>
            <select v-model.number="selectedParentCategoryId" class="select" @change="form.categoryId = null">
              <option :value="null" disabled>请选择一级分类</option>
              <option v-for="category in categoryTree" :key="category.id" :value="category.id">{{ category.name }}</option>
            </select>
          </label>
          <label class="form-item">
            <span class="form-label">二级分类</span>
            <select v-model.number="form.categoryId" class="select" :disabled="!selectedParentCategoryId">
              <option :value="null" disabled>请选择二级分类</option>
              <option v-for="category in childCategoryOptions" :key="category.id" :value="category.id">{{ category.name }}</option>
            </select>
          </label>
        </div>
        <label v-if="form.scopeType === 'SKU'" class="form-item">
          <span class="form-label">SKU选择</span>
          <select v-model="form.skuSelectType" class="select" @change="resetSkuSelection">
            <option value="ALL">全部SKU</option>
            <option value="MULTI">多选SKU</option>
            <option value="SINGLE">单选SKU</option>
          </select>
        </label>
        <div v-if="form.scopeType === 'SKU' && form.skuSelectType === 'ALL'" class="form-hint full">
          将按当前全部有效SKU创建盘点范围。
        </div>
        <div v-if="form.scopeType === 'SKU' && form.skuSelectType !== 'ALL'" class="form-item full">
          <span class="form-label">选择SKU</span>
          <div class="sku-pick-list">
            <label v-for="sku in skuOptions" :key="sku.id" class="sku-pick-item">
              <input
                :type="form.skuSelectType === 'SINGLE' ? 'radio' : 'checkbox'"
                :name="form.skuSelectType === 'SINGLE' ? 'stockcheckSku' : undefined"
                :value="sku.id"
                :checked="form.skuIds.includes(sku.id)"
                @change="toggleSku(sku.id, $event.target.checked)"
              />
              <span>{{ sku.skuCode }} / {{ sku.skuName }}</span>
              <small>{{ sku.productName }}</small>
            </label>
          </div>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="createDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submitCreate">{{ submitting ? '创建中...' : '创建并展开批次' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="detailDialogVisible" :title="detailTitle">
      <div v-if="detailLoading" class="table-state">
        <span class="state-dot"></span>
        加载中...
      </div>
      <div v-else-if="detail" class="check-detail">
        <div class="detail-summary">
          <div>
            <span class="form-label">盘点单号</span>
            <strong>{{ detail.checkNo }}</strong>
          </div>
          <div>
            <span class="form-label">范围</span>
            <strong>{{ scopeLabel(detail) }}</strong>
          </div>
          <div>
            <span class="form-label">批次数</span>
            <strong>{{ detail.totalBatchCount }}</strong>
          </div>
          <div>
            <span class="form-label">状态</span>
            <strong>{{ statusLabel(detail.status) }}</strong>
          </div>
        </div>
        <div class="table-wrap stockcheck-items-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>商品</th>
                <th>SKU</th>
                <th>批次号</th>
                <th>到期日期</th>
                <th>账面数量</th>
                <th>实际数量</th>
                <th>差异</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in detail.items" :key="item.id">
                <td>{{ item.productCode }} / {{ item.productName }}</td>
                <td>{{ item.skuCode }} / {{ item.skuName }}</td>
                <td>{{ item.batchNo }}</td>
                <td>{{ item.expireDate || '-' }}</td>
                <td>{{ item.systemQuantity }}</td>
                <td>
                  <input
                    v-model.number="actualQuantities[item.id]"
                    class="input quantity-input"
                    type="number"
                    min="0"
                    :disabled="detail.status !== 'DRAFT'"
                  />
                </td>
                <td>
                  <span :class="differenceClass(itemDifference(item))">{{ itemDifference(item) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="detailDialogVisible = false">关闭</button>
        <button v-if="detail?.status === 'DRAFT'" class="btn btn-ghost" type="button" :disabled="submitting" @click="saveItems">保存实盘数</button>
        <button v-if="detail?.status === 'DRAFT'" class="btn btn-primary" type="button" :disabled="submitting" @click="completeCurrent">{{ submitting ? '提交中...' : '完成盘点' }}</button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import BaseDialog from '../../components/BaseDialog.vue'
import BaseTable from '../../components/BaseTable.vue'
import PageToolbar from '../../components/PageToolbar.vue'
import PermissionButton from '../../components/PermissionButton.vue'
import { getCategoryTree } from '../../api/category'
import { listProducts } from '../../api/product'
import { listSkus } from '../../api/sku'
import {
  completeStockcheck,
  createStockcheck,
  getStockcheck,
  listStockchecks,
  updateStockcheckItems
} from '../../api/stockcheck'

const columns = [
  { key: 'checkNo', title: '盘点单号' },
  { key: 'name', title: '名称' },
  { key: 'scopeType', title: '范围' },
  { key: 'totalSkuCount', title: 'SKU数' },
  { key: 'totalBatchCount', title: '批次数' },
  { key: 'totalDifference', title: '总差异' },
  { key: 'status', title: '状态' },
  { key: 'createTime', title: '创建时间' },
  { key: 'completeTime', title: '完成时间' }
]

const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const message = ref('')
const messageType = ref('success')
const categoryTree = ref([])
const products = ref([])
const skuOptions = ref([])
const selectedParentCategoryId = ref(null)
const actualQuantities = reactive({})

const form = reactive({
  name: '',
  scopeType: 'ALL',
  categoryId: null,
  skuSelectType: 'ALL',
  skuIds: []
})

const childCategoryOptions = computed(() => {
  const parent = categoryTree.value.find((category) => category.id === selectedParentCategoryId.value)
  return parent?.children || []
})

const detailTitle = computed(() => detail.value ? `${detail.value.name} - 批次盘点` : '批次盘点')

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

async function loadData() {
  loading.value = true
  try {
    const data = await listStockchecks(query)
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [categories, productPage] = await Promise.all([
      getCategoryTree(),
      listProducts({ page: 1, pageSize: 1000 })
    ])
    categoryTree.value = categories || []
    products.value = productPage.items || []
    await loadSkuOptions()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function loadSkuOptions() {
  const allSkus = []
  for (const product of products.value) {
    const skus = await listSkus(product.id)
    for (const sku of skus) {
      allSkus.push({
        ...sku,
        productId: product.id,
        productName: product.productName,
        productCode: product.productCode
      })
    }
  }
  skuOptions.value = allSkus
}

function reload() {
  query.page = 1
  loadData()
}

function resetQuery() {
  query.keyword = ''
  reload()
}

function changePage(page) {
  query.page = page
  loadData()
}

function openCreate() {
  Object.assign(form, {
    name: '',
    scopeType: 'ALL',
    categoryId: null,
    skuSelectType: 'ALL',
    skuIds: []
  })
  selectedParentCategoryId.value = null
  createDialogVisible.value = true
}

function handleScopeChange() {
  form.categoryId = null
  form.skuSelectType = form.scopeType === 'SKU' ? 'ALL' : 'ALL'
  form.skuIds = []
  selectedParentCategoryId.value = null
}

function resetSkuSelection() {
  form.skuIds = []
}

function toggleSku(skuId, checked) {
  if (form.skuSelectType === 'SINGLE') {
    form.skuIds = checked ? [skuId] : []
    return
  }
  if (checked && !form.skuIds.includes(skuId)) {
    form.skuIds = [...form.skuIds, skuId]
  } else if (!checked) {
    form.skuIds = form.skuIds.filter((id) => id !== skuId)
  }
}

async function submitCreate() {
  if (!form.name.trim()) {
    showMessage('请填写盘点名称', 'error')
    return
  }
  if ((form.scopeType === 'CATEGORY_LEVEL1' || form.scopeType === 'CATEGORY_LEVEL2') && !form.categoryId) {
    showMessage('请选择分类', 'error')
    return
  }
  if (form.scopeType === 'SKU' && form.skuSelectType !== 'ALL' && form.skuIds.length === 0) {
    showMessage('请选择SKU', 'error')
    return
  }
  submitting.value = true
  try {
    const created = await createStockcheck({
      name: form.name,
      scopeType: form.scopeType === 'SKU' && form.skuSelectType === 'ALL' ? 'ALL' : form.scopeType,
      categoryId: form.categoryId,
      skuSelectType: form.scopeType === 'SKU' ? form.skuSelectType : 'ALL',
      skuIds: form.scopeType === 'SKU' && form.skuSelectType !== 'ALL' ? form.skuIds : []
    })
    createDialogVisible.value = false
    showMessage('盘点单创建成功')
    await loadData()
    await openDetail(created.id)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function openDetail(id) {
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getStockcheck(id)
    resetActualQuantities()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    detailLoading.value = false
  }
}

function resetActualQuantities() {
  Object.keys(actualQuantities).forEach((key) => delete actualQuantities[key])
  for (const item of detail.value?.items || []) {
    actualQuantities[item.id] = item.actualQuantity ?? item.systemQuantity
  }
}

async function saveItems() {
  if (!detail.value) return
  const payload = buildItemsPayload()
  if (!payload) return
  submitting.value = true
  try {
    detail.value = await updateStockcheckItems(detail.value.id, { items: payload })
    resetActualQuantities()
    showMessage('实盘数量已保存')
    await loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function completeCurrent() {
  if (!detail.value) return
  submitting.value = true
  try {
    const payload = buildItemsPayload()
    if (!payload) {
      submitting.value = false
      return
    }
    await updateStockcheckItems(detail.value.id, { items: payload })
    detail.value = await completeStockcheck(detail.value.id)
    resetActualQuantities()
    showMessage('盘点完成，库存已同步')
    await loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

function buildItemsPayload() {
  const payload = []
  for (const item of detail.value.items || []) {
    const value = Number(actualQuantities[item.id])
    if (!Number.isInteger(value) || value < 0) {
      showMessage(`请填写批次 ${item.batchNo} 的正确实际数量`, 'error')
      return null
    }
    payload.push({ itemId: item.id, actualQuantity: value })
  }
  return payload
}

function itemDifference(item) {
  const value = Number(actualQuantities[item.id] ?? item.actualQuantity ?? item.systemQuantity)
  return value - item.systemQuantity
}

function differenceClass(value) {
  if (value > 0) return 'diff-positive'
  if (value < 0) return 'diff-negative'
  return 'diff-zero'
}

function statusLabel(status) {
  return {
    DRAFT: '草稿',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }[status] || status || '-'
}

function scopeLabel(item) {
  const labels = {
    ALL: '全部商品',
    CATEGORY_LEVEL1: `一级分类${item.categoryName ? ` / ${item.categoryName}` : ''}`,
    CATEGORY_LEVEL2: `二级分类${item.categoryName ? ` / ${item.categoryName}` : ''}`,
    SKU: item.skuSelectType === 'SINGLE' ? '指定SKU / 单选' : '指定SKU / 多选'
  }
  return labels[item.scopeType] || item.scopeType || '-'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

onMounted(() => {
  loadOptions()
  loadData()
})
</script>

<style scoped>
.sku-pick-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  max-height: 260px;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
  background: #fafcfc;
}

.sku-pick-item {
  min-height: 42px;
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr);
  gap: 4px 8px;
  align-items: center;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
}

.sku-pick-item small {
  grid-column: 2;
  color: var(--muted);
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.detail-summary > div {
  min-height: 62px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
  background: #fafcfc;
}

.detail-summary strong {
  display: block;
  margin-top: 6px;
}

.stockcheck-items-wrap {
  max-height: 430px;
}

.quantity-input {
  width: 110px;
}

.diff-positive {
  color: var(--success);
  font-weight: 700;
}

.diff-negative {
  color: var(--danger);
  font-weight: 700;
}

.diff-zero {
  color: var(--muted);
}

@media (max-width: 900px) {
  .sku-pick-list,
  .detail-summary {
    grid-template-columns: 1fr;
  }
}
</style>
