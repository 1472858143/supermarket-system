<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">采购入库</h1>
        <p class="page-desc">采购计划、审批与实际入库分层管理，库存由实际收货批次驱动</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>

    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="单号、供应商、商品或SKU" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openPlanDialog()">新增采购计划</PermissionButton>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" empty-text="暂无采购入库记录" @page-change="changePage">
        <template #cell-status="{ item }">
          <StatusTag type="purchase" :value="item.status" />
        </template>
        <template #cell-plannedTotalAmount="{ item }">{{ formatMoney(item.plannedTotalAmount) }}</template>
        <template #cell-inboundTotalAmount="{ item }">{{ formatMoney(item.inboundTotalAmount) }}</template>
        <template #actions="{ item }">
          <div class="workflow-actions">
            <button class="btn btn-ghost btn-small" type="button" @click="openDetail(item)">详情</button>
            <button v-if="canEditPlan(item)" class="btn btn-ghost btn-small" type="button" @click="openPlanDialog(item)">编辑</button>
            <button v-if="canSubmit(item)" class="btn btn-ghost btn-small" type="button" :disabled="actioningId === item.id" @click="submitOrder(item)">提交</button>
            <button v-if="canApprove(item)" class="btn btn-ghost btn-small" type="button" :disabled="actioningId === item.id" @click="approveOrder(item)">审批</button>
            <button v-if="canReturn(item)" class="btn btn-ghost btn-small" type="button" :disabled="actioningId === item.id" @click="returnOrder(item)">退回</button>
            <button v-if="canCancel(item)" class="btn btn-ghost btn-small" type="button" :disabled="actioningId === item.id" @click="cancelOrder(item)">取消</button>
            <button v-if="canReceive(item)" class="btn btn-primary btn-small" type="button" :disabled="actioningId === item.id" @click="openReceiptDialog(item)">收货</button>
            <button v-if="canClose(item)" class="btn btn-ghost btn-small" type="button" :disabled="actioningId === item.id" @click="closeOrder(item)">关闭</button>
          </div>
        </template>
      </BaseTable>
    </section>

    <BaseDialog v-model="planDialogVisible" :title="planDialogTitle">
      <label class="form-item full">
        <span class="form-label">供应商</span>
        <select v-model.number="form.supplierId" class="select">
          <option :value="null">请选择供应商</option>
          <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
            {{ supplier.supplierCode }} / {{ supplier.supplierName }}
          </option>
        </select>
      </label>

      <div class="detail-lines">
        <div v-for="(line, index) in form.items" :key="line.key" class="line-panel">
          <div class="line-header">
            <strong>计划明细 {{ index + 1 }}</strong>
            <button v-if="form.items.length > 1" class="btn btn-ghost btn-small" type="button" @click="removeLine(index)">删除</button>
          </div>
          <div class="form-grid">
            <label class="form-item">
              <span class="form-label">供应商SKU</span>
              <select
                v-model.number="line.bindingId"
                class="select"
                :disabled="!form.supplierId || supplierSkuLoading"
                @change="handleSupplierSkuSelected(line)"
              >
                <option :value="null">{{ supplierSkuLoading ? '加载中...' : '请选择SKU' }}</option>
                <option v-for="binding in supplierSkus" :key="binding.id" :value="binding.id">
                  {{ binding.supplierSkuCode || binding.skuCode }} / {{ binding.supplierSkuName || binding.skuName }}
                </option>
              </select>
            </label>
            <UnitSelector v-model="line.unit" :base-unit="line.selectedSku?.baseUnit || ''" :units="line.units" @rate-changed="(rate) => setConversionRate(line, rate)" />
            <label class="form-item">
              <span class="form-label">计划数量</span>
              <input v-model.number="line.quantity" class="input" type="number" min="1" />
            </label>
            <label class="form-item">
              <span class="form-label">审批采购价</span>
              <input v-model.number="line.purchasePrice" class="input" type="number" min="0" step="0.01" />
            </label>
            <div v-if="linePreviews[line.key]" class="form-hint full">{{ linePreviews[line.key] }}</div>
          </div>
        </div>
        <button class="btn btn-ghost add-line-button" type="button" @click="addLine">+ 添加明细</button>
      </div>

      <div class="summary-bar">
        <span>计划基础数量：{{ totalBaseQuantity }}</span>
        <span>计划金额：{{ formatMoney(totalAmount) }}</span>
      </div>
      <label class="form-item full">
        <span class="form-label">备注</span>
        <input v-model.trim="form.remark" class="input" maxlength="200" placeholder="可选" />
      </label>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="planDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="savePlan">{{ submitting ? '保存中...' : '保存草稿' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="receiptVisible" title="登记实际入库">
      <div v-if="receiptOrder" class="detail-block">
        <div class="detail-grid">
          <span>单号</span><strong>{{ receiptOrder.orderNo }}</strong>
          <span>状态</span><strong><StatusTag type="purchase" :value="receiptOrder.status" /></strong>
          <span>计划基础数量</span><strong>{{ receiptOrder.plannedTotalQuantity ?? '-' }}</strong>
          <span>已入库基础数量</span><strong>{{ receiptOrder.inboundTotalQuantity ?? 0 }}</strong>
        </div>

        <div class="detail-lines">
          <div v-for="receiptItem in receiptForm.items" :key="receiptItem.key" class="line-panel">
            <div class="line-header">
              <div>
                <strong>{{ receiptItem.item.supplierSkuCodeSnapshot || receiptItem.item.skuCode }}</strong>
                <div class="muted">{{ receiptItem.item.supplierSkuNameSnapshot || receiptItem.item.productName }}</div>
              </div>
              <span class="muted">剩余 {{ remainingBaseQuantity(receiptItem.item) }} 基础单位</span>
            </div>
            <div v-for="(batch, batchIndex) in receiptItem.batches" :key="batch.key" class="form-grid receipt-batch-grid">
              <label class="form-item">
                <span class="form-label">实收数量</span>
                <input v-model.number="batch.quantity" class="input" type="number" min="1" />
              </label>
              <label class="form-item">
                <span class="form-label">生产日期</span>
                <input v-model="batch.productionDate" class="input" type="date" />
              </label>
              <label class="form-item">
                <span class="form-label">保质期天数</span>
                <input v-model.number="batch.shelfLifeDays" class="input" type="number" min="1" step="1" />
              </label>
              <div class="form-item batch-actions">
                <span class="form-label">批次</span>
                <button v-if="receiptItem.batches.length > 1" class="btn btn-ghost btn-small" type="button" @click="removeReceiptBatch(receiptItem, batchIndex)">删除</button>
              </div>
              <div class="form-hint full">
                {{ batch.quantity || 0 }} {{ receiptItem.item.unit }} = {{ receiptBatchBaseQuantity(receiptItem.item, batch) }} 基础单位
              </div>
            </div>
            <button class="btn btn-ghost btn-small" type="button" @click="addReceiptBatch(receiptItem)">+ 添加批次</button>
          </div>
        </div>

        <div class="summary-bar">
          <span>本次实收基础数量：{{ totalReceiptBaseQuantity }}</span>
          <span>本次实收金额：{{ formatMoney(totalReceiptAmount, 6) }}</span>
        </div>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <input v-model.trim="receiptForm.remark" class="input" maxlength="200" placeholder="可选" />
        </label>
      </div>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="receiptVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submitReceipt">{{ submitting ? '保存中...' : '确认入库' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="detailVisible" title="采购入库详情">
      <div v-if="detail" class="detail-block">
        <div class="detail-grid">
          <span>单号</span><strong>{{ detail.orderNo }}</strong>
          <span>供应商</span><strong>{{ detail.supplierCode }} / {{ detail.supplierName }}</strong>
          <span>状态</span><strong><StatusTag type="purchase" :value="detail.status" /></strong>
          <span>计划基础数量</span><strong>{{ detail.plannedTotalQuantity ?? '-' }}</strong>
          <span>已入库基础数量</span><strong>{{ detail.inboundTotalQuantity ?? 0 }}</strong>
          <span>计划金额</span><strong>{{ formatMoney(detail.plannedTotalAmount) }}</strong>
          <span>入库金额</span><strong>{{ formatMoney(detail.inboundTotalAmount, 6) }}</strong>
          <span>创建人</span><strong>{{ detail.creatorUsername || detail.operator || '-' }}</strong>
          <span>创建时间</span><strong>{{ detail.createTime || '-' }}</strong>
          <span>备注</span><strong>{{ detail.remark || '-' }}</strong>
        </div>

        <h2 class="subsection-title">计划明细</h2>
        <div class="compact-table">
          <BaseTable :columns="detailColumns" :items="detail.items || []" :total="detail.items?.length || 0" :page="1" :page-size="detail.items?.length || 1" :show-actions="false" empty-text="暂无明细">
            <template #cell-purchasePrice="{ item }">{{ formatMoney(item.purchasePrice) }}</template>
            <template #cell-costPrice="{ item }">{{ formatMoney(item.costPrice, 8) }}</template>
            <template #cell-plannedAmount="{ item }">{{ formatMoney(item.plannedAmount) }}</template>
          </BaseTable>
        </div>

        <h2 class="subsection-title">审批日志</h2>
        <div class="compact-table">
          <BaseTable :columns="approvalLogColumns" :items="detail.approvalLogs || []" :total="detail.approvalLogs?.length || 0" :page="1" :page-size="detail.approvalLogs?.length || 1" :show-actions="false" empty-text="暂无审批日志">
            <template #cell-fromStatus="{ item }"><StatusTag type="purchase" :value="item.fromStatus" /></template>
            <template #cell-toStatus="{ item }"><StatusTag type="purchase" :value="item.toStatus" /></template>
          </BaseTable>
        </div>

        <h2 class="subsection-title">实际入库</h2>
        <div v-if="detail.receipts?.length" class="detail-lines">
          <div v-for="receipt in detail.receipts" :key="receipt.id" class="line-panel">
            <div class="line-header">
              <strong>{{ receipt.receiptNo }}</strong>
              <span class="muted">{{ receipt.operatorUsername }} / {{ receipt.createTime }}</span>
            </div>
            <div class="meta-row">
              <span>基础数量 {{ receipt.totalBaseQuantity }}</span>
              <span>金额 {{ formatMoney(receipt.totalAmount, 6) }}</span>
              <span>{{ receipt.remark || '-' }}</span>
            </div>
            <BaseTable :columns="receiptBatchColumns" :items="receipt.batches || []" :total="receipt.batches?.length || 0" :page="1" :page-size="receipt.batches?.length || 1" :show-actions="false" empty-text="暂无入库批次">
              <template #cell-amount="{ item }">{{ formatMoney(item.amount, 6) }}</template>
              <template #cell-costPriceSnapshot="{ item }">{{ formatMoney(item.costPriceSnapshot, 8) }}</template>
            </BaseTable>
          </div>
        </div>
        <div v-else class="empty-note">暂无实际入库记录</div>
      </div>
      <template #footer>
        <button class="btn btn-primary" type="button" @click="detailVisible = false">关闭</button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import BaseDialog from '../../components/BaseDialog.vue'
import BaseTable from '../../components/BaseTable.vue'
import PageToolbar from '../../components/PageToolbar.vue'
import PermissionButton from '../../components/PermissionButton.vue'
import StatusTag from '../../components/StatusTag.vue'
import UnitSelector from '../../components/UnitSelector.vue'
import { listEnabledSupplierSkus, listSuppliers } from '../../api/supplier'
import {
  approvePurchaseInbound,
  cancelPurchaseInbound,
  closePurchaseInbound,
  createPurchaseInboundDraft,
  getPurchaseInbound,
  listPurchaseInbounds,
  receivePurchaseInbound,
  returnPurchaseInbound,
  submitPurchaseInbound,
  updatePurchaseInboundPlan
} from '../../api/purchaseInbound'

const columns = [
  { key: 'orderNo', title: '单号' },
  { key: 'supplierName', title: '供应商' },
  { key: 'status', title: '状态' },
  { key: 'plannedTotalQuantity', title: '计划数量' },
  { key: 'inboundTotalQuantity', title: '已入库' },
  { key: 'plannedTotalAmount', title: '计划金额' },
  { key: 'inboundTotalAmount', title: '入库金额' },
  { key: 'createTime', title: '创建时间' }
]
const detailColumns = [
  { key: 'supplierSkuCodeSnapshot', title: '供应商SKU' },
  { key: 'supplierSkuNameSnapshot', title: '供应商品名' },
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'productName', title: '商品名称' },
  { key: 'plannedQuantity', title: '计划数量' },
  { key: 'unit', title: '单位' },
  { key: 'conversionRate', title: '换算率' },
  { key: 'plannedBaseQuantity', title: '计划基础数量' },
  { key: 'inboundedBaseQuantity', title: '已入库基础数量' },
  { key: 'purchasePrice', title: '审批采购价' },
  { key: 'costPrice', title: '基础成本价' },
  { key: 'plannedAmount', title: '计划小计' }
]
const approvalLogColumns = [
  { key: 'action', title: '动作' },
  { key: 'fromStatus', title: '原状态' },
  { key: 'toStatus', title: '新状态' },
  { key: 'operatorUsername', title: '操作人' },
  { key: 'reason', title: '原因' },
  { key: 'createTime', title: '时间' }
]
const receiptBatchColumns = [
  { key: 'supplierSkuCodeSnapshot', title: '供应商SKU' },
  { key: 'supplierSkuNameSnapshot', title: '供应商品名' },
  { key: 'quantity', title: '实收数量' },
  { key: 'baseQuantity', title: '基础数量' },
  { key: 'productionDate', title: '生产日期' },
  { key: 'shelfLifeDays', title: '保质期' },
  { key: 'expireDate', title: '到期日期' },
  { key: 'costPriceSnapshot', title: '基础成本价' },
  { key: 'amount', title: '金额' }
]

const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const suppliers = ref([])
const supplierSkus = ref([])
const supplierSkuLoading = ref(false)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const actioningId = ref(null)
const planDialogVisible = ref(false)
const planMode = ref('create')
const editingId = ref(null)
const receiptVisible = ref(false)
const receiptOrder = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const message = ref('')
const messageType = ref('success')
const suppressSupplierWatch = ref(false)
const form = reactive({ supplierId: null, items: [], remark: '' })
const receiptForm = reactive({ items: [], remark: '' })

const planDialogTitle = computed(() => (planMode.value === 'edit' ? '编辑采购计划' : '新增采购计划'))
const totalBaseQuantity = computed(() => form.items.reduce((sum, line) => sum + baseQuantity(line), 0))
const totalAmount = computed(() => form.items.reduce((sum, line) => sum + lineAmount(line), 0))
const totalReceiptBaseQuantity = computed(() => receiptForm.items.reduce((sum, receiptItem) => (
  sum + receiptItem.batches.reduce((batchSum, batch) => batchSum + receiptBatchBaseQuantity(receiptItem.item, batch), 0)
), 0))
const totalReceiptAmount = computed(() => receiptForm.items.reduce((sum, receiptItem) => {
  const costPrice = Number(receiptItem.item.costPrice || 0)
  return sum + receiptItem.batches.reduce((batchSum, batch) => (
    batchSum + receiptBatchBaseQuantity(receiptItem.item, batch) * costPrice
  ), 0)
}, 0))
const linePreviews = computed(() => {
  const previews = {}
  form.items.forEach((line) => {
    previews[line.key] = buildLinePreview(line)
  })
  return previews
})

function canEditPlan(row) {
  return ['DRAFT', 'RETURNED'].includes(row.status)
}

function canSubmit(row) {
  return ['DRAFT', 'RETURNED'].includes(row.status)
}

function canApprove(row) {
  return row.status === 'SUBMITTED'
}

function canReturn(row) {
  return row.status === 'SUBMITTED'
}

function canCancel(row) {
  return ['DRAFT', 'SUBMITTED', 'RETURNED', 'APPROVED'].includes(row.status) && Number(row.inboundTotalQuantity || 0) === 0
}

function canReceive(row) {
  return ['APPROVED', 'PARTIALLY_INBOUNDED'].includes(row.status)
}

function canClose(row) {
  return row.status === 'PARTIALLY_INBOUNDED'
}

function createLine(item = null) {
  const binding = item ? findBinding(item.supplierSkuId, item.skuId) : null
  return {
    key: Date.now() + Math.random(),
    bindingId: item?.supplierSkuId || null,
    skuId: item?.skuId || null,
    selectedSku: binding,
    units: binding?.units || [],
    minPurchaseQuantity: binding?.minPurchaseQuantity || 1,
    quantity: item?.plannedQuantity || 1,
    unit: item?.unit || binding?.baseUnit || '',
    conversionRate: item?.conversionRate || 1,
    purchasePrice: Number(item?.purchasePrice || binding?.defaultPurchasePrice || 0)
  }
}

function createReceiptBatch() {
  return {
    key: Date.now() + Math.random(),
    quantity: 1,
    productionDate: '',
    shelfLifeDays: 180
  }
}

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

function formatMoney(value, digits = 2) {
  const number = Number(value || 0)
  return number.toFixed(digits)
}

function baseQuantity(line) {
  const quantity = Number(line.quantity || 0)
  const rate = Number(line.conversionRate || 1)
  return quantity > 0 ? quantity * rate : 0
}

function lineAmount(line) {
  const quantity = Number(line.quantity || 0)
  const price = Number(line.purchasePrice || 0)
  return quantity > 0 && price >= 0 ? quantity * price : 0
}

function remainingBaseQuantity(item) {
  return Number(item.plannedBaseQuantity || 0) - Number(item.inboundedBaseQuantity || 0)
}

function receiptBatchBaseQuantity(item, batch) {
  return Number(batch.quantity || 0) * Number(item.conversionRate || 1)
}

function buildLinePreview(line) {
  if (!line.selectedSku || !line.unit || !line.quantity || line.quantity <= 0) {
    return ''
  }
  const baseUnit = line.selectedSku.baseUnit || '基础单位'
  return `${line.quantity} ${line.unit} = ${baseQuantity(line)} ${baseUnit}，计划小计 ${formatMoney(lineAmount(line))}`
}

function findBinding(bindingId, skuId) {
  return supplierSkus.value.find((item) => item.id === bindingId || item.skuId === skuId) || null
}

async function loadData() {
  loading.value = true
  try {
    const data = await listPurchaseInbounds(query)
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadSuppliers() {
  try {
    const data = await listSuppliers({ page: 1, pageSize: 100 })
    suppliers.value = (data.items || []).filter((supplier) => supplier.status === 1)
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function loadSupplierSkus(supplierId) {
  supplierSkus.value = []
  if (!supplierId) return
  supplierSkuLoading.value = true
  try {
    supplierSkus.value = await listEnabledSupplierSkus(supplierId)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    supplierSkuLoading.value = false
  }
}

watch(
  () => form.supplierId,
  async (supplierId, previousSupplierId) => {
    if (suppressSupplierWatch.value) return
    if (previousSupplierId !== undefined && supplierId !== previousSupplierId) {
      form.items.splice(0, form.items.length, createLine())
    }
    await loadSupplierSkus(supplierId)
  }
)

function handleSupplierSkuSelected(line) {
  const binding = findBinding(line.bindingId, null)
  line.selectedSku = binding
  line.skuId = binding?.skuId || null
  line.units = binding?.units || []
  line.unit = binding?.baseUnit || ''
  line.conversionRate = 1
  line.purchasePrice = Number(binding?.defaultPurchasePrice || 0)
  line.minPurchaseQuantity = binding?.minPurchaseQuantity || 1
}

function setConversionRate(line, rate) {
  line.conversionRate = rate || 1
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

function addLine() {
  form.items.push(createLine())
}

function removeLine(index) {
  form.items.splice(index, 1)
}

async function openPlanDialog(row = null) {
  planMode.value = row ? 'edit' : 'create'
  editingId.value = row?.id || null
  form.remark = ''
  form.items.splice(0, form.items.length)
  supplierSkus.value = []
  suppressSupplierWatch.value = true
  try {
    if (row) {
      const data = await getPurchaseInbound(row.id)
      form.supplierId = data.supplierId
      form.remark = data.remark || ''
      await loadSupplierSkus(data.supplierId)
      form.items.splice(0, form.items.length, ...(data.items || []).map((item) => createLine(item)))
    } else {
      form.supplierId = null
      form.items.splice(0, form.items.length, createLine())
    }
    planDialogVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    suppressSupplierWatch.value = false
  }
}

async function openDetail(item) {
  try {
    detail.value = await getPurchaseInbound(item.id)
    detailVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function openReceiptDialog(row) {
  try {
    const data = await getPurchaseInbound(row.id)
    const receivableItems = (data.items || []).filter((item) => remainingBaseQuantity(item) > 0)
    if (!receivableItems.length) {
      showMessage('当前采购单没有可入库的剩余数量', 'error')
      return
    }
    receiptOrder.value = data
    receiptForm.remark = ''
    receiptForm.items.splice(0, receiptForm.items.length, ...receivableItems.map((item) => ({
      key: item.id,
      item,
      purchaseInboundItemId: item.id,
      batches: [createReceiptBatch()]
    })))
    receiptVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

function addReceiptBatch(receiptItem) {
  receiptItem.batches.push(createReceiptBatch())
}

function removeReceiptBatch(receiptItem, batchIndex) {
  receiptItem.batches.splice(batchIndex, 1)
}

function validatePlanForm() {
  if (!form.supplierId) {
    return '请选择供应商'
  }
  if (!form.items.length) {
    return '请至少添加一条明细'
  }
  const invalid = form.items.some((line) => (
    !line.skuId ||
    !line.unit ||
    !line.quantity ||
    line.quantity <= 0 ||
    Number(line.quantity) < Number(line.minPurchaseQuantity || 1) ||
    line.purchasePrice === '' ||
    Number(line.purchasePrice) < 0
  ))
  return invalid ? '请选择供应商SKU，并填写正确数量和采购价；数量不能低于最小采购量' : ''
}

function validateReceiptForm() {
  if (!receiptForm.items.length) {
    return '实际入库明细不能为空'
  }
  for (const receiptItem of receiptForm.items) {
    if (!receiptItem.batches.length) {
      return '实际入库批次不能为空'
    }
    const receivedBaseQuantity = receiptItem.batches.reduce((sum, batch) => sum + receiptBatchBaseQuantity(receiptItem.item, batch), 0)
    if (receivedBaseQuantity <= 0) {
      return '实际入库数量必须大于0'
    }
    if (receivedBaseQuantity > remainingBaseQuantity(receiptItem.item)) {
      return '实际入库数量不能超过计划剩余数量'
    }
    const invalidBatch = receiptItem.batches.some((batch) => (
      !batch.quantity ||
      Number(batch.quantity) <= 0 ||
      !batch.productionDate ||
      !batch.shelfLifeDays ||
      !Number.isInteger(Number(batch.shelfLifeDays)) ||
      Number(batch.shelfLifeDays) <= 0
    ))
    if (invalidBatch) {
      return '请填写正确的实收数量、生产日期和保质期'
    }
  }
  return ''
}

async function savePlan() {
  const error = validatePlanForm()
  if (error) {
    showMessage(error, 'error')
    return
  }
  submitting.value = true
  try {
    const payload = {
      supplierId: form.supplierId,
      items: form.items.map((line) => ({
        skuId: line.skuId,
        quantity: Number(line.quantity),
        unit: line.unit,
        purchasePrice: Number(line.purchasePrice)
      })),
      remark: form.remark
    }
    if (planMode.value === 'edit') {
      await updatePurchaseInboundPlan(editingId.value, payload)
      showMessage('采购计划已更新')
    } else {
      await createPurchaseInboundDraft(payload)
      showMessage('采购计划已保存')
    }
    planDialogVisible.value = false
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function submitReceipt() {
  const error = validateReceiptForm()
  if (error) {
    showMessage(error, 'error')
    return
  }
  submitting.value = true
  try {
    await receivePurchaseInbound(receiptOrder.value.id, {
      items: receiptForm.items.map((receiptItem) => ({
        purchaseInboundItemId: receiptItem.purchaseInboundItemId,
        batches: receiptItem.batches.map((batch) => ({
          quantity: Number(batch.quantity),
          productionDate: batch.productionDate,
          shelfLifeDays: Number(batch.shelfLifeDays)
        }))
      })),
      remark: receiptForm.remark
    })
    receiptVisible.value = false
    showMessage('实际入库已保存')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function submitOrder(row) {
  await runWorkflowAction(row, () => submitPurchaseInbound(row.id), '采购计划已提交')
}

async function approveOrder(row) {
  await runWorkflowAction(row, () => approvePurchaseInbound(row.id), '采购计划已审批')
}

async function returnOrder(row) {
  const reason = window.prompt('请输入退回原因')
  if (reason === null) return
  await runWorkflowAction(row, () => returnPurchaseInbound(row.id, { reason }), '采购计划已退回')
}

async function cancelOrder(row) {
  const reason = window.prompt('请输入取消原因')
  if (reason === null) return
  await runWorkflowAction(row, () => cancelPurchaseInbound(row.id, { reason }), '采购计划已取消')
}

async function closeOrder(row) {
  const reason = window.prompt('请输入关闭原因')
  if (reason === null) return
  await runWorkflowAction(row, () => closePurchaseInbound(row.id, { reason }), '采购计划已关闭')
}

async function runWorkflowAction(row, action, successMessage) {
  actioningId.value = row.id
  try {
    await action()
    showMessage(successMessage)
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    actioningId.value = null
  }
}

onMounted(() => {
  loadSuppliers()
  loadData()
})
</script>

<style scoped>
.workflow-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  min-width: 280px;
}

.detail-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.line-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
}

.line-header,
.summary-bar,
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.summary-bar {
  margin: 16px 0;
  padding: 12px;
  background: var(--surface-muted);
  border-radius: 8px;
  font-weight: 650;
}

.meta-row {
  justify-content: flex-start;
  color: var(--muted);
  flex-wrap: wrap;
}

.add-line-button {
  align-self: flex-start;
}

.detail-grid {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 10px 16px;
  margin-bottom: 16px;
}

.detail-grid span,
.muted {
  color: var(--muted);
}

.detail-grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.subsection-title {
  margin: 18px 0 10px;
  font-size: 15px;
}

.receipt-batch-grid {
  margin-bottom: 8px;
}

.batch-actions {
  justify-content: flex-end;
}

.empty-note {
  color: var(--muted);
  padding: 12px 0;
}

.btn-small {
  min-height: 30px;
  padding: 0 10px;
  font-size: 13px;
}

@media (max-width: 720px) {
  .workflow-actions {
    justify-content: flex-start;
    min-width: 0;
  }

  .line-header,
  .summary-bar,
  .meta-row {
    align-items: stretch;
    flex-direction: column;
  }

  .add-line-button {
    align-self: stretch;
  }
}
</style>
