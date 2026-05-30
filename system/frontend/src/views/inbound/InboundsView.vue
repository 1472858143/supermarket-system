<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">入库管理</h1>
        <p class="page-desc">入库记录库存增加原因，库存变更由后端库存模块统一处理</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="商品或操作人" @search="reload" @reset="resetQuery">
        <button class="btn btn-primary" type="button" @click="openCreate">+ 新增入库</button>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" empty-text="暂无入库记录" @page-change="changePage" />
    </section>
    <BaseDialog v-model="dialogVisible" title="新增入库">
      <form class="form-grid">
        <label class="form-item full">
          <span class="form-label">商品</span>
          <select v-model.number="form.productId" class="select">
            <option :value="null">请选择商品</option>
            <option v-for="product in products" :key="product.id" :value="product.id">
              {{ product.productCode }} / {{ product.productName }}
            </option>
          </select>
        </label>
        <label class="form-item">
          <span class="form-label">数量</span>
          <input v-model.number="form.quantity" class="input" type="number" min="1" />
        </label>
        <label class="form-item">
          <span class="form-label">操作人</span>
          <input v-model.trim="form.operator" class="input" placeholder="默认当前用户" />
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
import { listProducts } from '../../api/product'
import { createInbound, listInbounds } from '../../api/inbound'

const columns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'quantity', title: '入库数量' },
  { key: 'operator', title: '操作人' },
  { key: 'createTime', title: '入库时间' }
]
const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const products = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const message = ref('')
const messageType = ref('success')
const form = reactive({ productId: null, quantity: 1, operator: '' })

function showMessage(text, type = 'success') { message.value = text; messageType.value = type }
async function loadData() {
  loading.value = true
  try {
    const data = await listInbounds(query)
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
}
async function loadProducts() {
  const data = await listProducts({ page: 1, pageSize: 100 })
  products.value = data.items || []
}
function reload() { query.page = 1; loadData() }
function resetQuery() { query.keyword = ''; reload() }
function changePage(page) { query.page = page; loadData() }
function openCreate() { Object.assign(form, { productId: null, quantity: 1, operator: '' }); dialogVisible.value = true }
async function submit() {
  if (!form.productId || !form.quantity || form.quantity <= 0) {
    showMessage('请选择商品并填写正确数量', 'error')
    return
  }
  submitting.value = true
  try {
    await createInbound(form)
    dialogVisible.value = false
    showMessage('入库成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}
onMounted(async () => { await loadProducts(); await loadData() })
</script>
