<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">库存管理</h1>
        <p class="page-desc">库存数量只能由入库、出库和盘点触发变更，本页仅维护上下限</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="商品编号、名称、SKU或分类" @search="reload" @reset="resetQuery" />
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" :show-actions="true" empty-text="暂无库存记录" @page-change="changePage">
        <template #cell-warningStatus="{ item }">
          <StatusTag type="warning" :value="item.warningStatus" />
        </template>
        <template #actions="{ item }">
          <button class="btn btn-ghost btn-small" type="button" @click="openBatches(item)">批次</button>
          <PermissionButton :roles="['ADMIN']" @click="openLimit(item)">维护上下限</PermissionButton>
        </template>
      </BaseTable>
    </section>
    <BaseDialog v-model="dialogVisible" title="维护库存上下限">
      <form class="form-grid">
        <label class="form-item full">
          <span class="form-label">商品</span>
          <input class="input" :value="currentStockLabel" disabled />
        </label>
        <label class="form-item">
          <span class="form-label">库存下限</span>
          <input v-model.number="form.minStock" class="input" type="number" min="0" />
        </label>
        <label class="form-item">
          <span class="form-label">库存上限</span>
          <input v-model.number="form.maxStock" class="input" type="number" min="0" />
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="batchDialogVisible" title="SKU库存批次">
      <div v-if="batchStock" class="detail-block">
        <div class="detail-grid">
          <span>商品</span><strong>{{ batchStock.productName || '-' }}</strong>
          <span>商品编号</span><strong>{{ batchStock.productCode || '-' }}</strong>
          <span>SKU编码</span><strong>{{ batchStock.skuCode || '-' }}</strong>
          <span>规格</span><strong>{{ batchStock.spec || '-' }}</strong>
          <span>当前库存</span><strong>{{ batchStock.quantity ?? '-' }}</strong>
        </div>
        <BaseTable :columns="batchColumns" :items="batches" :total="batches.length" :page="1" :page-size="batches.length || 1" :loading="batchLoading" :show-actions="false" empty-text="暂无批次">
          <template #cell-quantity="{ item }">{{ item.initialQuantity ?? '-' }} / {{ item.quantity ?? '-' }}</template>
          <template #cell-purchasePrice="{ item }">{{ formatMoney(item.purchasePrice) }}</template>
        </BaseTable>
      </div>
      <template #footer>
        <button class="btn btn-primary" type="button" @click="batchDialogVisible = false">关闭</button>
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
import StatusTag from '../../components/StatusTag.vue'
import { listStocks, updateStockLimit } from '../../api/stock'
import { listStockBatches } from '../../api/stockBatch'
import { useAuthStore } from '../../stores/auth'

const columns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'spec', title: '规格' },
  { key: 'baseUnit', title: '基础单位' },
  { key: 'category', title: '分类' },
  { key: 'quantity', title: '当前库存' },
  { key: 'minStock', title: '下限' },
  { key: 'maxStock', title: '上限' },
  { key: 'warningStatus', title: '预警' },
  { key: 'updateTime', title: '更新时间' }
]
const batchColumns = [
  { key: 'batchNo', title: '批次号' },
  { key: 'quantity', title: '批次数量/剩余数量' },
  { key: 'purchasePrice', title: '进价' },
  { key: 'productionDate', title: '生产日期' },
  { key: 'shelfLifeDays', title: '保质期' },
  { key: 'expireDate', title: '到期日期' },
  { key: 'createTime', title: '创建时间' }
]
const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const batchLoading = ref(false)
const editingSkuId = ref(null)
const editingStock = ref(null)
const batchStock = ref(null)
const batches = ref([])
const message = ref('')
const messageType = ref('success')
const form = reactive({ minStock: 0, maxStock: 100 })
const authStore = useAuthStore()
const currentStockLabel = computed(() => {
  if (!editingStock.value) return ''
  const { productCode, productName, skuCode, spec } = editingStock.value
  return [productCode, productName, skuCode, spec].filter(Boolean).join(' / ')
})
const canMaintainLimit = computed(() => authStore.hasRole('ADMIN'))

function showMessage(text, type = 'success') { message.value = text; messageType.value = type }
function formatMoney(value, digits = 2) {
  const number = Number(value || 0)
  return number.toFixed(digits)
}
async function loadData() {
  loading.value = true
  try {
    const data = await listStocks(query)
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
}
function reload() { query.page = 1; loadData() }
function resetQuery() { query.keyword = ''; reload() }
function changePage(page) { query.page = page; loadData() }
function openLimit(item) {
  editingSkuId.value = item.skuId
  editingStock.value = item
  form.minStock = item.minStock
  form.maxStock = item.maxStock
  dialogVisible.value = true
}
async function openBatches(item) {
  batchStock.value = item
  batches.value = []
  batchDialogVisible.value = true
  batchLoading.value = true
  try {
    const data = await listStockBatches(item.skuId)
    batches.value = Array.isArray(data) ? data : (data.items || [])
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    batchLoading.value = false
  }
}
async function submit() {
  submitting.value = true
  try {
    await updateStockLimit(editingSkuId.value, form)
    dialogVisible.value = false
    showMessage('库存上下限已更新')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}
onMounted(loadData)
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px 16px;
  margin-bottom: 16px;
}

.detail-grid span {
  color: var(--muted);
}

.detail-grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.btn-small {
  min-height: 30px;
  padding: 0 10px;
  font-size: 13px;
}
</style>
