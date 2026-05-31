<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">采购入库</h1>
        <p class="page-desc">采购入库记录到货明细和采购成本，库存变化由后端库存模块统一处理</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="单号、商品、SKU或操作人" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增采购入库</PermissionButton>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" empty-text="暂无采购入库记录" @page-change="changePage">
        <template #cell-totalAmount="{ item }">{{ formatMoney(item.totalAmount) }}</template>
        <template #actions="{ item }">
          <button class="btn btn-ghost btn-small" type="button" @click="openDetail(item)">详情</button>
        </template>
      </BaseTable>
    </section>

    <BaseDialog v-model="dialogVisible" title="新增采购入库">
      <div class="detail-lines">
        <div v-for="(line, index) in form.items" :key="line.key" class="line-panel">
          <div class="line-header">
            <strong>明细 {{ index + 1 }}</strong>
            <button v-if="form.items.length > 1" class="btn btn-ghost btn-small" type="button" @click="removeLine(index)">删除</button>
          </div>
          <div class="form-grid">
            <SkuSelector v-model="line.skuId" :products="products" @sku-selected="(sku) => handleSkuSelected(line, sku)" />
            <UnitSelector v-model="line.unit" :base-unit="line.selectedSku?.baseUnit || ''" :units="line.units" @rate-changed="(rate) => setConversionRate(line, rate)" />
            <label class="form-item">
              <span class="form-label">数量</span>
              <input v-model.number="line.quantity" class="input" type="number" min="1" />
            </label>
            <label class="form-item">
              <span class="form-label">采购单价</span>
              <input v-model.number="line.purchasePrice" class="input" type="number" min="0" step="0.01" />
            </label>
            <div v-if="linePreview(line)" class="form-hint full">{{ linePreview(line) }}</div>
          </div>
        </div>
        <button class="btn btn-ghost add-line-button" type="button" @click="addLine">+ 添加明细</button>
      </div>
      <div class="summary-bar">
        <span>汇总数量：{{ totalBaseQuantity }}</span>
        <span>汇总金额：{{ formatMoney(totalAmount) }}</span>
      </div>
      <label class="form-item full">
        <span class="form-label">备注</span>
        <input v-model.trim="form.remark" class="input" maxlength="200" placeholder="可选" />
      </label>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="detailVisible" title="采购入库详情">
      <div v-if="detail" class="detail-block">
        <div class="detail-grid">
          <span>单号</span><strong>{{ detail.orderNo }}</strong>
          <span>状态</span><strong>{{ detail.status }}</strong>
          <span>总数量</span><strong>{{ detail.totalQuantity }}</strong>
          <span>总金额</span><strong>{{ formatMoney(detail.totalAmount) }}</strong>
          <span>操作人</span><strong>{{ detail.operator }}</strong>
          <span>时间</span><strong>{{ detail.createTime }}</strong>
          <span>备注</span><strong>{{ detail.remark || '-' }}</strong>
        </div>
        <div class="compact-table">
          <BaseTable :columns="detailColumns" :items="detail.items || []" :total="detail.items?.length || 0" :page="1" :page-size="detail.items?.length || 1" :show-actions="false" empty-text="暂无明细">
            <template #cell-purchasePrice="{ item }">{{ formatMoney(item.purchasePrice) }}</template>
            <template #cell-costPrice="{ item }">{{ formatMoney(item.costPrice, 4) }}</template>
            <template #cell-amount="{ item }">{{ formatMoney(item.amount) }}</template>
          </BaseTable>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-primary" type="button" @click="detailVisible = false">关闭</button>
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
import SkuSelector from '../../components/SkuSelector.vue'
import UnitSelector from '../../components/UnitSelector.vue'
import { listProducts } from '../../api/product'
import { createPurchaseInbound, getPurchaseInbound, listPurchaseInbounds } from '../../api/purchaseInbound'

const columns = [
  { key: 'orderNo', title: '单号' },
  { key: 'totalQuantity', title: '总数量' },
  { key: 'totalAmount', title: '总金额' },
  { key: 'operator', title: '操作人' },
  { key: 'createTime', title: '创建时间' }
]
const detailColumns = [
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'skuName', title: 'SKU名称' },
  { key: 'productName', title: '商品名称' },
  { key: 'quantity', title: '数量' },
  { key: 'unit', title: '单位' },
  { key: 'conversionRate', title: '换算率' },
  { key: 'baseQuantity', title: '基础数量' },
  { key: 'purchasePrice', title: '采购单价' },
  { key: 'costPrice', title: '成本单价' },
  { key: 'amount', title: '小计' }
]

const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const message = ref('')
const messageType = ref('success')
const form = reactive({ items: [], remark: '' })

const totalBaseQuantity = computed(() => form.items.reduce((sum, line) => sum + baseQuantity(line), 0))
const totalAmount = computed(() => form.items.reduce((sum, line) => sum + lineAmount(line), 0))

function createLine() {
  return {
    key: Date.now() + Math.random(),
    skuId: null,
    selectedSku: null,
    units: [],
    quantity: 1,
    unit: '',
    conversionRate: 1,
    purchasePrice: 0
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
function linePreview(line) {
  if (!line.selectedSku || !line.unit || !line.quantity || line.quantity <= 0) {
    return ''
  }
  const baseUnit = line.selectedSku.baseUnit || '基础单位'
  return `${line.quantity} ${line.unit} = ${baseQuantity(line)} ${baseUnit}，小计 ${formatMoney(lineAmount(line))}`
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
async function loadProducts() {
  try {
    const data = await listProducts({ page: 1, pageSize: 100 })
    products.value = data.items || []
  } catch (error) {
    showMessage(error.message, 'error')
  }
}
function handleSkuSelected(line, sku) {
  line.selectedSku = sku
  line.units = sku?.units || []
  line.skuId = sku?.id || null
  line.unit = sku?.baseUnit || ''
  line.conversionRate = 1
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
function openCreate() {
  form.items.splice(0, form.items.length, createLine())
  form.remark = ''
  dialogVisible.value = true
}
async function openDetail(item) {
  try {
    detail.value = await getPurchaseInbound(item.id)
    detailVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  }
}
function validateForm() {
  if (!form.items.length) {
    return '请至少添加一条明细'
  }
  const invalid = form.items.some((line) => (
    !line.skuId ||
    !line.unit ||
    !line.quantity ||
    line.quantity <= 0 ||
    line.purchasePrice === '' ||
    Number(line.purchasePrice) < 0
  ))
  return invalid ? '请选择SKU、单位并填写正确数量和采购单价' : ''
}
async function submit() {
  const error = validateForm()
  if (error) {
    showMessage(error, 'error')
    return
  }
  submitting.value = true
  try {
    await createPurchaseInbound({
      items: form.items.map((line) => ({
        skuId: line.skuId,
        quantity: Number(line.quantity),
        unit: line.unit,
        purchasePrice: Number(line.purchasePrice)
      })),
      remark: form.remark
    })
    dialogVisible.value = false
    showMessage('采购入库成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProducts()
  loadData()
})
</script>

<style scoped>
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
.summary-bar {
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

.add-line-button {
  align-self: flex-start;
}

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

@media (max-width: 640px) {
  .line-header,
  .summary-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .add-line-button {
    align-self: stretch;
  }
}
</style>
