<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">商品管理</h1>
        <p class="page-desc">维护商品基础资料，不在商品页面维护库存数量</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="商品编号、名称或分类" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增商品</PermissionButton>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" :show-actions="canManageProducts" empty-text="暂无商品记录" @page-change="changePage">
        <template #cell-status="{ item }">
          <StatusTag type="product" :value="item.status" />
        </template>
        <template #actions="{ item }">
          <div class="toolbar-left">
            <PermissionButton :roles="['ADMIN']" @click="openEdit(item)">编辑</PermissionButton>
            <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="remove(item)">删除</PermissionButton>
          </div>
        </template>
      </BaseTable>
    </section>
    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'">
      <form class="form-grid">
        <label class="form-item">
          <span class="form-label">商品编号</span>
          <input v-model.trim="form.productCode" class="input" :disabled="Boolean(editingId)" />
        </label>
        <label class="form-item">
          <span class="form-label">商品名称</span>
          <input v-model.trim="form.productName" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">分类</span>
          <input v-model.trim="form.category" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="form.status" class="select">
            <option :value="1">上架</option>
            <option :value="0">下架</option>
          </select>
        </label>
        <label class="form-item">
          <span class="form-label">进价</span>
          <input v-model.number="form.purchasePrice" class="input" type="number" min="0" step="0.01" />
        </label>
        <label class="form-item">
          <span class="form-label">售价</span>
          <input v-model.number="form.salePrice" class="input" type="number" min="0" step="0.01" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import BaseDialog from '../../components/BaseDialog.vue'
import BaseTable from '../../components/BaseTable.vue'
import PageToolbar from '../../components/PageToolbar.vue'
import PermissionButton from '../../components/PermissionButton.vue'
import StatusTag from '../../components/StatusTag.vue'
import { createProduct, deleteProduct, listProducts, updateProduct } from '../../api/product'
import { useAuthStore } from '../../stores/auth'

const columns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'category', title: '分类' },
  { key: 'purchasePrice', title: '进价' },
  { key: 'salePrice', title: '售价' },
  { key: 'status', title: '状态' },
  { key: 'createTime', title: '创建时间' }
]
const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const message = ref('')
const messageType = ref('success')
const form = reactive({ productCode: '', productName: '', category: '', purchasePrice: 0, salePrice: 0, status: 1 })
const authStore = useAuthStore()
const canManageProducts = computed(() => authStore.hasRole('ADMIN'))

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

async function loadData() {
  loading.value = true
  try {
    const data = await listProducts(query)
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
function resetForm() { Object.assign(form, { productCode: '', productName: '', category: '', purchasePrice: 0, salePrice: 0, status: 1 }); editingId.value = null }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(item) { editingId.value = item.id; Object.assign(form, item); dialogVisible.value = true }

async function submit() {
  if (!form.productCode || !form.productName || !form.category) {
    showMessage('请填写商品编号、名称和分类', 'error')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, form)
    } else {
      await createProduct(form)
    }
    dialogVisible.value = false
    showMessage('保存成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function remove(item) {
  if (!window.confirm(`确认删除商品 ${item.productName}？已有业务记录的商品不能删除。`)) {
    return
  }
  try {
    await deleteProduct(item.id)
    showMessage('删除成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

onMounted(loadData)
</script>
