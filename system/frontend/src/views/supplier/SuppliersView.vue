<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">供应商管理</h1>
        <p class="page-desc">维护供应商基础资料和启用状态</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="供应商编码、名称、联系人或电话" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增供应商</PermissionButton>
      </PageToolbar>
      <BaseTable
        :columns="columns"
        :items="items"
        :total="total"
        :page="query.page"
        :page-size="query.pageSize"
        :loading="loading"
        :show-actions="canManageSuppliers"
        empty-text="暂无供应商记录"
        @page-change="changePage"
      >
        <template #cell-status="{ item }">
          <StatusTag type="enabled" :value="item.status" />
        </template>
        <template #actions="{ item }">
          <div class="toolbar-left">
            <button class="btn btn-ghost" type="button" @click="openSupplierSkus(item)">供货SKU</button>
            <PermissionButton :roles="['ADMIN']" @click="openEdit(item)">编辑</PermissionButton>
            <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="remove(item)">删除</PermissionButton>
          </div>
        </template>
      </BaseTable>
    </section>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑供应商' : '新增供应商'">
      <form class="form-grid">
        <label v-if="editingId" class="form-item">
          <span class="form-label">供应商编码</span>
          <input v-model.trim="form.supplierCode" class="input" disabled />
        </label>
        <label class="form-item">
          <span class="form-label">供应商名称</span>
          <input v-model.trim="form.supplierName" class="input" maxlength="100" />
        </label>
        <label class="form-item">
          <span class="form-label">联系人</span>
          <input v-model.trim="form.contactPerson" class="input" maxlength="50" />
        </label>
        <label class="form-item">
          <span class="form-label">联系电话</span>
          <input
            v-model.trim="form.contactPhone"
            class="input"
            type="tel"
            inputmode="tel"
            maxlength="30"
            pattern="[0-9+\-\s]*"
            placeholder="如 13800000000"
            @input="sanitizeContactPhone"
          />
        </label>
        <label class="form-item full">
          <span class="form-label">地址</span>
          <input v-model.trim="form.address" class="input" maxlength="200" />
        </label>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <input v-model.trim="form.remark" class="input" maxlength="200" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="form.status" class="select">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="skuDialogVisible" :title="`${currentSupplier?.supplierName || ''} - 供货SKU`">
      <div class="toolbar-left" style="margin-bottom: 12px;">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" @click="openCreateBinding">新增绑定</PermissionButton>
      </div>
      <BaseTable
        :columns="supplierSkuColumns"
        :items="supplierSkuItems"
        :total="supplierSkuItems.length"
        :page="1"
        :page-size="supplierSkuItems.length || 1"
        :loading="supplierSkuLoading"
        :show-actions="canManageSuppliers"
        empty-text="暂无供货SKU"
      >
        <template #cell-defaultPurchasePrice="{ item }">{{ formatMoney(item.defaultPurchasePrice) }}</template>
        <template #cell-status="{ item }"><StatusTag type="enabled" :value="item.status" /></template>
        <template #actions="{ item }">
          <div class="toolbar-left">
            <PermissionButton :roles="['ADMIN']" @click="openEditBinding(item)">编辑</PermissionButton>
            <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="removeBinding(item)">删除</PermissionButton>
          </div>
        </template>
      </BaseTable>
      <template #footer>
        <button class="btn btn-primary" type="button" @click="skuDialogVisible = false">关闭</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="skuBindingDialogVisible" :title="editingBindingId ? '编辑供货SKU' : '新增供货SKU'">
      <form class="form-grid">
        <SkuSelector
          v-if="!editingBindingId"
          v-model="supplierSkuForm.skuId"
          :products="products"
          @sku-selected="handleBindingSkuSelected"
        />
        <label class="form-item">
          <span class="form-label">供应商商品编码</span>
          <input v-model.trim="supplierSkuForm.supplierSkuCode" class="input" maxlength="80" />
        </label>
        <label class="form-item">
          <span class="form-label">供应商商品名称</span>
          <input v-model.trim="supplierSkuForm.supplierSkuName" class="input" maxlength="120" />
        </label>
        <label class="form-item">
          <span class="form-label">供应商规格</span>
          <input v-model.trim="supplierSkuForm.supplierSpec" class="input" maxlength="120" />
        </label>
        <label class="form-item">
          <span class="form-label">默认采购价</span>
          <input v-model.number="supplierSkuForm.defaultPurchasePrice" class="input" type="number" min="0" step="0.01" />
        </label>
        <label class="form-item">
          <span class="form-label">最小采购量</span>
          <input v-model.number="supplierSkuForm.minPurchaseQuantity" class="input" type="number" min="1" step="1" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="supplierSkuForm.status" class="select">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="skuBindingDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="supplierSkuSubmitting" @click="submitBinding">{{ supplierSkuSubmitting ? '提交中...' : '保存' }}</button>
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
import SkuSelector from '../../components/SkuSelector.vue'
import { listProducts } from '../../api/product'
import {
  createSupplier,
  createSupplierSku,
  deleteSupplier,
  deleteSupplierSku,
  listSupplierSkus,
  listSuppliers,
  updateSupplier,
  updateSupplierSku
} from '../../api/supplier'
import { useAuthStore } from '../../stores/auth'

const columns = [
  { key: 'supplierCode', title: '供应商编码' },
  { key: 'supplierName', title: '供应商名称' },
  { key: 'contactPerson', title: '联系人' },
  { key: 'contactPhone', title: '联系电话' },
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
const form = reactive({
  supplierCode: '',
  supplierName: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  remark: '',
  status: 1
})
const authStore = useAuthStore()
const canManageSuppliers = computed(() => authStore.hasRole('ADMIN'))

const products = ref([])
const skuDialogVisible = ref(false)
const skuBindingDialogVisible = ref(false)
const currentSupplier = ref(null)
const supplierSkuItems = ref([])
const supplierSkuLoading = ref(false)
const supplierSkuSubmitting = ref(false)
const editingBindingId = ref(null)
const supplierSkuForm = reactive({
  skuId: null,
  selectedSku: null,
  supplierSkuCode: '',
  supplierSkuName: '',
  supplierSpec: '',
  defaultPurchasePrice: 0,
  minPurchaseQuantity: 1,
  status: 1
})
const supplierSkuColumns = [
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'skuName', title: 'SKU名称' },
  { key: 'productName', title: '商品名称' },
  { key: 'supplierSkuCode', title: '供应商商品编码' },
  { key: 'supplierSkuName', title: '供应商商品名称' },
  { key: 'supplierSpec', title: '供应商规格' },
  { key: 'defaultPurchasePrice', title: '默认采购价' },
  { key: 'minPurchaseQuantity', title: '最小采购量' },
  { key: 'status', title: '状态' }
]

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

function isValidContactPhone(value) {
  return !value || /^[0-9+\-\s]+$/.test(value)
}

function sanitizeContactPhone(event) {
  const sanitized = event.target.value.replace(/[^0-9+\-\s]/g, '')
  if (event.target.value !== sanitized) {
    event.target.value = sanitized
  }
  form.contactPhone = sanitized.trim()
}

async function loadData() {
  loading.value = true
  try {
    const data = await listSuppliers(query)
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
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

function resetForm() {
  Object.assign(form, {
    supplierCode: '',
    supplierName: '',
    contactPerson: '',
    contactPhone: '',
    address: '',
    remark: '',
    status: 1
  })
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    supplierCode: item.supplierCode,
    supplierName: item.supplierName,
    contactPerson: item.contactPerson || '',
    contactPhone: item.contactPhone || '',
    address: item.address || '',
    remark: item.remark || '',
    status: item.status
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.supplierName) {
    showMessage('请填写供应商名称', 'error')
    return
  }
  if (!isValidContactPhone(form.contactPhone)) {
    showMessage('联系电话只能包含数字、空格、+或-', 'error')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateSupplier(editingId.value, form)
    } else {
      await createSupplier(form)
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
  if (!window.confirm(`确认删除供应商 ${item.supplierName}？`)) {
    return
  }
  try {
    await deleteSupplier(item.id)
    showMessage('删除成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

async function loadProducts() {
  try {
    const data = await listProducts({ page: 1, pageSize: 100 })
    products.value = data.items || []
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function openSupplierSkus(item) {
  currentSupplier.value = item
  skuDialogVisible.value = true
  await loadSupplierSkus()
}

async function loadSupplierSkus() {
  if (!currentSupplier.value) return
  supplierSkuLoading.value = true
  try {
    supplierSkuItems.value = await listSupplierSkus(currentSupplier.value.id)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    supplierSkuLoading.value = false
  }
}

function resetBindingForm() {
  editingBindingId.value = null
  Object.assign(supplierSkuForm, {
    skuId: null,
    selectedSku: null,
    supplierSkuCode: '',
    supplierSkuName: '',
    supplierSpec: '',
    defaultPurchasePrice: 0,
    minPurchaseQuantity: 1,
    status: 1
  })
}

function openCreateBinding() {
  resetBindingForm()
  skuBindingDialogVisible.value = true
}

function openEditBinding(item) {
  editingBindingId.value = item.id
  Object.assign(supplierSkuForm, {
    skuId: item.skuId,
    selectedSku: item,
    supplierSkuCode: item.supplierSkuCode,
    supplierSkuName: item.supplierSkuName,
    supplierSpec: item.supplierSpec || '',
    defaultPurchasePrice: Number(item.defaultPurchasePrice || 0),
    minPurchaseQuantity: item.minPurchaseQuantity || 1,
    status: item.status
  })
  skuBindingDialogVisible.value = true
}

function handleBindingSkuSelected(sku) {
  supplierSkuForm.selectedSku = sku
  supplierSkuForm.skuId = sku?.id || null
  if (sku && !supplierSkuForm.supplierSkuName) {
    supplierSkuForm.supplierSkuName = sku.skuName
  }
  if (sku && !supplierSkuForm.supplierSpec) {
    supplierSkuForm.supplierSpec = sku.spec || ''
  }
}

function validateBindingForm() {
  if (!editingBindingId.value && !supplierSkuForm.skuId) return '请选择SKU'
  if (!supplierSkuForm.supplierSkuCode) return '请填写供应商商品编码'
  if (!supplierSkuForm.supplierSkuName) return '请填写供应商商品名称'
  if (Number(supplierSkuForm.defaultPurchasePrice) < 0) return '默认采购价不能小于0'
  if (!Number.isInteger(Number(supplierSkuForm.minPurchaseQuantity)) || Number(supplierSkuForm.minPurchaseQuantity) <= 0) return '最小采购量必须大于0'
  return ''
}

async function submitBinding() {
  const error = validateBindingForm()
  if (error) {
    showMessage(error, 'error')
    return
  }
  supplierSkuSubmitting.value = true
  const payload = {
    skuId: supplierSkuForm.skuId,
    supplierSkuCode: supplierSkuForm.supplierSkuCode,
    supplierSkuName: supplierSkuForm.supplierSkuName,
    supplierSpec: supplierSkuForm.supplierSpec,
    defaultPurchasePrice: Number(supplierSkuForm.defaultPurchasePrice),
    minPurchaseQuantity: Number(supplierSkuForm.minPurchaseQuantity),
    status: supplierSkuForm.status
  }
  try {
    if (editingBindingId.value) {
      await updateSupplierSku(currentSupplier.value.id, editingBindingId.value, payload)
    } else {
      await createSupplierSku(currentSupplier.value.id, payload)
    }
    skuBindingDialogVisible.value = false
    showMessage('供货SKU保存成功')
    await loadSupplierSkus()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    supplierSkuSubmitting.value = false
  }
}

async function removeBinding(item) {
  if (!window.confirm(`确认删除供货SKU ${item.supplierSkuName}？`)) return
  try {
    await deleteSupplierSku(currentSupplier.value.id, item.id)
    showMessage('删除成功')
    await loadSupplierSkus()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

onMounted(() => {
  loadData()
  loadProducts()
})
</script>
