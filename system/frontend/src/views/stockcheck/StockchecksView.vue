<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">盘点管理</h1>
        <p class="page-desc">盘点保存系统库存、实际库存和差异，调整由库存模块完成</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="商品编号、名称或SKU" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增盘点</PermissionButton>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" empty-text="暂无盘点记录" @page-change="changePage" />
    </section>
    <BaseDialog v-model="dialogVisible" title="新增盘点">
      <form class="form-grid">
        <SkuSelector v-model="form.skuId" :products="products" @sku-selected="handleSkuSelected" />
        <label class="form-item full">
          <span class="form-label">实际库存{{ selectedSku?.baseUnit ? `（${selectedSku.baseUnit}）` : '' }}</span>
          <input v-model.number="form.actualQuantity" class="input" type="number" min="0" />
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import BaseDialog from '../../components/BaseDialog.vue'
import BaseTable from '../../components/BaseTable.vue'
import PageToolbar from '../../components/PageToolbar.vue'
import PermissionButton from '../../components/PermissionButton.vue'
import SkuSelector from '../../components/SkuSelector.vue'
import { listProducts } from '../../api/product'
import { createStockcheck, listStockchecks } from '../../api/stockcheck'

const columns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'skuName', title: 'SKU名称' },
  { key: 'systemQuantity', title: '系统库存' },
  { key: 'actualQuantity', title: '实际库存' },
  { key: 'difference', title: '差异' },
  { key: 'checkTime', title: '盘点时间' }
]
const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const products = ref([])
const selectedSku = ref(null)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const message = ref('')
const messageType = ref('success')
const form = reactive({ skuId: null, actualQuantity: 0 })

function showMessage(text, type = 'success') { message.value = text; messageType.value = type }
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
  form.skuId = sku?.id || null
}
function reload() { query.page = 1; loadData() }
function resetQuery() { query.keyword = ''; reload() }
function changePage(page) { query.page = page; loadData() }
function openCreate() {
  selectedSku.value = null
  Object.assign(form, { skuId: null, actualQuantity: 0 })
  dialogVisible.value = true
}
async function submit() {
  if (!form.skuId || form.actualQuantity < 0) {
    showMessage('请选择SKU并填写正确实际库存', 'error')
    return
  }
  submitting.value = true
  try {
    await createStockcheck({ skuId: form.skuId, actualQuantity: form.actualQuantity })
    dialogVisible.value = false
    showMessage('盘点成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}
onMounted(() => { loadProducts(); loadData() })
</script>
