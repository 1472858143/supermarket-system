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
        <BaseTable :columns="batchColumns" :items="batches" :total="batches.length" :page="1" :page-size="batches.length || 1" :loading="batchLoading" :show-actions="true" empty-text="暂无批次">
          <template #cell-status="{ item }">
            <StatusTag type="batch" :value="item.status" />
          </template>
          <template #cell-purchasePrice="{ item }">{{ formatMoney(item.purchasePrice) }}</template>
          <template #actions="{ item }">
            <button v-if="canLockBatch(item)" class="btn btn-ghost btn-small" type="button" :disabled="batchActionLoading" @click="handleLockBatch(item)">冻结</button>
            <button v-if="canUnlockBatch(item)" class="btn btn-ghost btn-small" type="button" :disabled="batchActionLoading" @click="handleUnlockBatch(item)">解冻</button>
            <button v-if="canDamageBatch(item)" class="btn btn-ghost btn-small" type="button" :disabled="batchActionLoading" @click="openDamageDialog(item)">报损</button>
            <button v-if="canCloseBatch(item)" class="btn btn-ghost btn-small" type="button" :disabled="batchActionLoading" @click="handleCloseBatch(item)">关闭</button>
          </template>
        </BaseTable>
      </div>
      <template #footer>
        <button class="btn btn-primary" type="button" @click="batchDialogVisible = false">关闭</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="damageDialogVisible" title="批次报损">
      <form class="form-grid">
        <label class="form-item">
          <span class="form-label">数量</span>
          <input v-model.number="damageForm.quantity" class="input" type="number" min="1" :max="currentDamageMax" />
        </label>
        <label class="form-item">
          <span class="form-label">原因</span>
          <input v-model.trim="damageForm.reason" class="input" maxlength="50" />
        </label>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <textarea v-model.trim="damageForm.remark" class="input" rows="3" maxlength="200"></textarea>
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="damageDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="batchActionLoading" @click="submitDamage">{{ batchActionLoading ? '提交中...' : '确认' }}</button>
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
import {
  closeStockBatch,
  damageStockBatch,
  listStockBatches,
  lockStockBatch,
  unlockStockBatch
} from '../../api/stockBatch'

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
  { key: 'status', title: '状态' },
  { key: 'initialQuantity', title: '批次数量' },
  { key: 'quantity', title: '剩余数量' },
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
const damageDialogVisible = ref(false)
const batchLoading = ref(false)
const batchActionLoading = ref(false)
const editingSkuId = ref(null)
const editingStock = ref(null)
const batchStock = ref(null)
const damageBatch = ref(null)
const batches = ref([])
const message = ref('')
const messageType = ref('success')
const form = reactive({ minStock: 0, maxStock: 100 })
const damageForm = reactive({ quantity: 1, reason: '', remark: '' })
let batchRequestToken = 0
const currentStockLabel = computed(() => {
  if (!editingStock.value) return ''
  const { productCode, productName, skuCode, spec } = editingStock.value
  return [productCode, productName, skuCode, spec].filter(Boolean).join(' / ')
})
const currentDamageMax = computed(() => Number(damageBatch.value?.quantity) || 1)

function showMessage(text, type = 'success') { message.value = text; messageType.value = type }
function formatMoney(value, digits = 2) {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return number.toFixed(digits)
}
async function loadData() {
  loading.value = true
  try {
    const data = await listStocks(query)
    items.value = data.items || []
    total.value = data.total || 0
    if (batchStock.value) {
      batchStock.value = items.value.find((item) => item.skuId === batchStock.value.skuId) || batchStock.value
    }
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
  await loadBatches(item, true)
}
async function loadBatches(item = batchStock.value, openDialog = false) {
  if (!item?.skuId) return
  const requestToken = ++batchRequestToken
  batchStock.value = item
  if (openDialog) {
    batches.value = []
    batchDialogVisible.value = true
  }
  batchLoading.value = true
  try {
    const data = await listStockBatches(item.skuId)
    if (requestToken === batchRequestToken) {
      batches.value = Array.isArray(data) ? data : (data.items || [])
    }
  } catch (error) {
    if (requestToken === batchRequestToken) {
      showMessage(error.message, 'error')
    }
  } finally {
    if (requestToken === batchRequestToken) {
      batchLoading.value = false
    }
  }
}
function canLockBatch(item) {
  return ['AVAILABLE', 'EXPIRED'].includes(item.status)
}
function canUnlockBatch(item) {
  return item.status === 'LOCKED'
}
function canDamageBatch(item) {
  return ['AVAILABLE', 'EXPIRED', 'LOCKED'].includes(item.status) && Number(item.quantity) > 0
}
function canCloseBatch(item) {
  return Number(item.quantity) === 0 && item.status !== 'CLOSED'
}
async function refreshAfterBatchAction() {
  await Promise.all([loadBatches(), loadData()])
}
async function runBatchAction(action, successText) {
  if (!batchStock.value) return
  batchActionLoading.value = true
  try {
    await action()
    showMessage(successText)
    await refreshAfterBatchAction()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    batchActionLoading.value = false
  }
}
function handleLockBatch(item) {
  runBatchAction(() => lockStockBatch(batchStock.value.skuId, item.id), '批次已冻结')
}
function handleUnlockBatch(item) {
  runBatchAction(() => unlockStockBatch(batchStock.value.skuId, item.id), '批次已解冻')
}
function handleCloseBatch(item) {
  runBatchAction(() => closeStockBatch(batchStock.value.skuId, item.id), '批次已关闭')
}
function openDamageDialog(item) {
  damageBatch.value = item
  damageForm.quantity = Math.min(1, currentDamageMax.value)
  damageForm.reason = ''
  damageForm.remark = ''
  damageDialogVisible.value = true
}
async function submitDamage() {
  const quantity = Number(damageForm.quantity)
  if (!damageBatch.value || !batchStock.value) return
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > currentDamageMax.value) {
    showMessage('数量不合法', 'error')
    return
  }
  if (!damageForm.reason) {
    showMessage('原因必填', 'error')
    return
  }
  await runBatchAction(
    () => damageStockBatch(batchStock.value.skuId, damageBatch.value.id, {
      quantity,
      reason: damageForm.reason,
      remark: damageForm.remark
    }),
    '批次已报损'
  )
  if (messageType.value !== 'error') {
    damageDialogVisible.value = false
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
