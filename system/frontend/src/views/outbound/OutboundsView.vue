<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">出库管理</h1>
        <p class="page-desc">出库记录库存减少原因，库存不足由后端拒绝</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="商品、SKU或操作人" @search="reload" @reset="resetQuery">
        <button class="btn btn-primary" type="button" @click="openCreate">+ 新增出库</button>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" empty-text="暂无出库记录" @page-change="changePage" />
    </section>
    <BaseDialog v-model="dialogVisible" title="新增出库">
      <form class="form-grid">
        <SkuSelector v-model="form.skuId" :products="products" @sku-selected="handleSkuSelected" />
        <UnitSelector v-model="form.unit" :base-unit="selectedSku?.baseUnit || ''" :units="units" @rate-changed="setConversionRate" />
        <label class="form-item">
          <span class="form-label">数量</span>
          <input v-model.number="form.quantity" class="input" type="number" min="1" />
        </label>
        <label class="form-item">
          <span class="form-label">操作人</span>
          <input v-model.trim="form.operator" class="input" placeholder="默认当前用户" />
        </label>
        <div v-if="conversionPreview" class="form-hint full">{{ conversionPreview }}</div>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import BaseDialog from '../../components/BaseDialog.vue'
import BaseTable from '../../components/BaseTable.vue'
import PageToolbar from '../../components/PageToolbar.vue'
import SkuSelector from '../../components/SkuSelector.vue'
import UnitSelector from '../../components/UnitSelector.vue'
import { listProducts } from '../../api/product'
import { createOutbound, listOutbounds } from '../../api/outbound'

const columns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'skuName', title: 'SKU名称' },
  { key: 'quantity', title: '出库数量' },
  { key: 'unit', title: '单位' },
  { key: 'conversionRate', title: '换算率' },
  { key: 'baseQuantity', title: '基础单位数量' },
  { key: 'operator', title: '操作人' },
  { key: 'createTime', title: '出库时间' }
]
const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const products = ref([])
const units = ref([])
const selectedSku = ref(null)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const message = ref('')
const messageType = ref('success')
const form = reactive({ skuId: null, quantity: 1, unit: '', conversionRate: 1, operator: '' })
const conversionPreview = computed(() => {
  if (!selectedSku.value || !form.unit || !form.quantity || form.quantity <= 0) {
    return ''
  }
  const baseUnit = selectedSku.value.baseUnit || '基础单位'
  return `${form.quantity} ${form.unit} = ${form.quantity * form.conversionRate} ${baseUnit}`
})

function showMessage(text, type = 'success') { message.value = text; messageType.value = type }
async function loadData() {
  loading.value = true
  try {
    const data = await listOutbounds(query)
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
function handleSkuSelected(sku) {
  selectedSku.value = sku
  units.value = sku?.units || []
  Object.assign(form, { skuId: sku?.id || null, unit: sku?.baseUnit || '', conversionRate: 1 })
}
function setConversionRate(rate) { form.conversionRate = rate || 1 }
function reload() { query.page = 1; loadData() }
function resetQuery() { query.keyword = ''; reload() }
function changePage(page) { query.page = page; loadData() }
function openCreate() {
  selectedSku.value = null
  units.value = []
  Object.assign(form, { skuId: null, quantity: 1, unit: '', conversionRate: 1, operator: '' })
  dialogVisible.value = true
}
async function submit() {
  if (!form.skuId || !form.quantity || form.quantity <= 0 || !form.unit) {
    showMessage('请选择SKU、单位并填写正确数量', 'error')
    return
  }
  submitting.value = true
  try {
    await createOutbound({
      skuId: form.skuId,
      quantity: form.quantity,
      unit: form.unit,
      conversionRate: form.conversionRate,
      operator: form.operator
    })
    dialogVisible.value = false
    showMessage('出库成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}
onMounted(() => { loadProducts(); loadData() })
</script>
