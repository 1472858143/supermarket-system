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
        <select v-model="query.brandId" class="select" @change="reload">
          <option value="">全部品牌</option>
          <option v-for="brand in brandOptions" :key="brand.id" :value="String(brand.id)">
            {{ brand.brandName }}
          </option>
        </select>
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增商品</PermissionButton>
      </PageToolbar>
      <BaseTable :columns="columns" :items="items" :total="total" :page="query.page" :page-size="query.pageSize" :loading="loading" :show-actions="canManageProducts" empty-text="暂无商品记录" @page-change="changePage">
        <template #cell-status="{ item }">
          <StatusTag type="product" :value="item.status" />
        </template>
        <template #actions="{ item }">
          <div class="toolbar-left">
            <PermissionButton :roles="['ADMIN']" @click="openSkuManager(item)">SKU管理</PermissionButton>
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
        <CategoryCascader v-model="form.categoryId" :tree="categoryTree" />
        <label class="form-item">
          <span class="form-label">品牌</span>
          <select v-model.number="form.brandId" class="select">
            <option :value="null">请选择品牌</option>
            <option v-for="brand in brandOptions" :key="brand.id" :value="brand.id">
              {{ brand.brandName }}
            </option>
          </select>
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="form.status" class="select">
            <option :value="1">上架</option>
            <option :value="0">下架</option>
          </select>
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="skuDialogVisible" :title="selectedProduct ? `SKU管理 - ${selectedProduct.productName}` : 'SKU管理'">
      <div class="toolbar">
        <div class="toolbar-left">
          <button class="btn btn-primary" type="button" @click="openSkuCreate">新增SKU</button>
        </div>
      </div>
      <div class="compact-table">
        <BaseTable
          :columns="skuColumns"
          :items="skuItems"
          :total="skuItems.length"
          :page="1"
          :page-size="skuItems.length || 10"
          :loading="skuLoading"
          empty-text="暂无SKU记录"
        >
          <template #cell-status="{ item }">
            <StatusTag type="product" :value="item.status" />
          </template>
          <template #cell-isDefault="{ item }">
            {{ item.isDefault === 1 ? '是' : '否' }}
          </template>
          <template #actions="{ item }">
            <div class="toolbar-left">
              <button class="btn" type="button" @click="openSkuEdit(item)">编辑</button>
              <button class="btn" type="button" @click="selectSku(item)">单位换算</button>
              <button class="btn btn-danger" type="button" :disabled="item.isDefault === 1" @click="removeSku(item)">删除</button>
            </div>
          </template>
        </BaseTable>
      </div>

      <section v-if="selectedSku" class="sku-unit-panel">
        <div class="toolbar">
          <div>
            <div class="form-label">单位换算：{{ selectedSku.skuName }}</div>
            <p class="page-desc">示例：1 箱 = 24 {{ selectedSku.baseUnit || '瓶' }}</p>
          </div>
          <button class="btn btn-primary" type="button" @click="openUnitCreate">新增单位</button>
        </div>
        <div class="compact-table">
          <BaseTable
            :columns="unitColumns"
            :items="selectedSku.units || []"
            :total="(selectedSku.units || []).length"
            :page="1"
            :page-size="(selectedSku.units || []).length || 10"
            empty-text="暂无单位换算"
          >
            <template #cell-conversionRate="{ item }">
              1 {{ item.unitName }} = {{ item.conversionRate }} {{ selectedSku.baseUnit }}
            </template>
            <template #actions="{ item }">
              <div class="toolbar-left">
                <button class="btn" type="button" @click="openUnitEdit(item)">编辑</button>
                <button class="btn btn-danger" type="button" @click="removeUnit(item)">删除</button>
              </div>
            </template>
          </BaseTable>
        </div>
      </section>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="skuDialogVisible = false">关闭</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="skuFormVisible" :title="editingSkuId ? '编辑SKU' : '新增SKU'">
      <form class="form-grid">
        <label class="form-item">
          <span class="form-label">SKU名称</span>
          <input v-model.trim="skuForm.skuName" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">规格描述</span>
          <input v-model.trim="skuForm.spec" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">条码</span>
          <input v-model.trim="skuForm.barcode" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">基础单位</span>
          <input v-model.trim="skuForm.baseUnit" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">进价</span>
          <input v-model.number="skuForm.purchasePrice" class="input" type="number" min="0" step="0.01" />
        </label>
        <label class="form-item">
          <span class="form-label">售价</span>
          <input v-model.number="skuForm.salePrice" class="input" type="number" min="0" step="0.01" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="skuForm.status" class="select">
            <option :value="1">上架</option>
            <option :value="0">下架</option>
          </select>
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="skuFormVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="skuSubmitting" @click="submitSku">{{ skuSubmitting ? '提交中...' : '保存' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="unitFormVisible" :title="editingUnitId ? '编辑单位换算' : '新增单位换算'">
      <form class="form-grid">
        <label class="form-item">
          <span class="form-label">单位名称</span>
          <input v-model.trim="unitForm.unitName" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">换算比例</span>
          <input v-model.number="unitForm.conversionRate" class="input" type="number" min="1" step="1" />
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="unitFormVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="unitSubmitting" @click="submitUnit">{{ unitSubmitting ? '提交中...' : '保存' }}</button>
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
import CategoryCascader from '../../components/CategoryCascader.vue'
import { createProduct, deleteProduct, listProducts, updateProduct } from '../../api/product'
import { createSku, createUnit, deleteSku, deleteUnit, listSkus, updateSku, updateUnit } from '../../api/sku'
import { getCategoryTree } from '../../api/category'
import { listBrandOptions } from '../../api/brand'
import { useAuthStore } from '../../stores/auth'

const columns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'categoryName', title: '分类' },
  { key: 'brandName', title: '品牌' },
  { key: 'status', title: '状态' },
  { key: 'createTime', title: '创建时间' }
]
const skuColumns = [
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'skuName', title: 'SKU名称' },
  { key: 'spec', title: '规格描述' },
  { key: 'barcode', title: '条码' },
  { key: 'baseUnit', title: '基础单位' },
  { key: 'purchasePrice', title: '进价' },
  { key: 'salePrice', title: '售价' },
  { key: 'status', title: '状态' },
  { key: 'isDefault', title: '默认' }
]
const unitColumns = [
  { key: 'unitName', title: '单位名称' },
  { key: 'conversionRate', title: '换算关系' }
]
const query = reactive({ keyword: '', brandId: '', page: 1, pageSize: 10 })
const items = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const message = ref('')
const messageType = ref('success')
const form = reactive({ productCode: '', productName: '', categoryId: null, brandId: null, status: 1 })
const skuDialogVisible = ref(false)
const skuFormVisible = ref(false)
const unitFormVisible = ref(false)
const skuLoading = ref(false)
const skuSubmitting = ref(false)
const unitSubmitting = ref(false)
const skuItems = ref([])
const selectedProduct = ref(null)
const selectedSku = ref(null)
const editingSkuId = ref(null)
const editingUnitId = ref(null)
const skuForm = reactive({ skuName: '', spec: '', barcode: '', baseUnit: '个', purchasePrice: 0, salePrice: 0, status: 1 })
const unitForm = reactive({ unitName: '', conversionRate: 1 })
const categoryTree = ref([])
const brandOptions = ref([])
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
function resetQuery() { query.keyword = ''; query.brandId = ''; reload() }
function changePage(page) { query.page = page; loadData() }
function resetForm() { Object.assign(form, { productCode: '', productName: '', categoryId: null, brandId: null, status: 1 }); editingId.value = null }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    productCode: item.productCode,
    productName: item.productName,
    categoryId: item.categoryId,
    brandId: item.brandId,
    status: item.status
  })
  dialogVisible.value = true
}
function resetSkuForm() { Object.assign(skuForm, { skuName: '', spec: '', barcode: '', baseUnit: '个', purchasePrice: 0, salePrice: 0, status: 1 }); editingSkuId.value = null }
function resetUnitForm() { Object.assign(unitForm, { unitName: '', conversionRate: 1 }); editingUnitId.value = null }

async function loadSkus() {
  if (!selectedProduct.value) {
    return
  }
  skuLoading.value = true
  try {
    skuItems.value = await listSkus(selectedProduct.value.id)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    skuLoading.value = false
  }
}

async function refreshSkusAndSelection(skuId = selectedSku.value?.id) {
  await loadSkus()
  selectedSku.value = skuItems.value.find((sku) => sku.id === skuId) || null
}

async function openSkuManager(item) {
  selectedProduct.value = item
  selectedSku.value = null
  skuDialogVisible.value = true
  await loadSkus()
}

function openSkuCreate() {
  resetSkuForm()
  skuFormVisible.value = true
}

function openSkuEdit(item) {
  editingSkuId.value = item.id
  Object.assign(skuForm, {
    skuName: item.skuName,
    spec: item.spec,
    barcode: item.barcode || '',
    baseUnit: item.baseUnit,
    purchasePrice: item.purchasePrice,
    salePrice: item.salePrice,
    status: item.status
  })
  skuFormVisible.value = true
}

function selectSku(item) {
  selectedSku.value = item
}

function openUnitCreate() {
  resetUnitForm()
  unitFormVisible.value = true
}

function openUnitEdit(item) {
  editingUnitId.value = item.id
  Object.assign(unitForm, {
    unitName: item.unitName,
    conversionRate: item.conversionRate
  })
  unitFormVisible.value = true
}

async function submit() {
  if (!form.productCode || !form.productName || !form.categoryId || !form.brandId) {
    showMessage('请填写商品编号、名称、分类和品牌', 'error')
    return
  }
  submitting.value = true
  try {
    const isEditing = Boolean(editingId.value)
    if (editingId.value) {
      await updateProduct(editingId.value, form)
    } else {
      await createProduct(form)
    }
    dialogVisible.value = false
    showMessage(isEditing ? '保存成功' : '商品保存成功，请在SKU管理中新增规格')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function submitSku() {
  const purchasePrice = Number(skuForm.purchasePrice)
  const salePrice = Number(skuForm.salePrice)
  if (!skuForm.skuName || !skuForm.spec || !skuForm.baseUnit || Number.isNaN(purchasePrice) || Number.isNaN(salePrice)) {
    showMessage('请填写SKU名称、规格、基础单位、进价和售价', 'error')
    return
  }
  if (purchasePrice < 0 || salePrice < 0) {
    showMessage('进价和售价不能小于0', 'error')
    return
  }
  if (salePrice < purchasePrice) {
    showMessage('售价不能小于进价', 'error')
    return
  }
  skuSubmitting.value = true
  try {
    if (editingSkuId.value) {
      await updateSku(selectedProduct.value.id, editingSkuId.value, skuForm)
    } else {
      await createSku(selectedProduct.value.id, skuForm)
    }
    const skuId = editingSkuId.value
    skuFormVisible.value = false
    showMessage('SKU保存成功')
    await refreshSkusAndSelection(skuId)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    skuSubmitting.value = false
  }
}

async function removeSku(item) {
  if (item.isDefault === 1 || !window.confirm(`确认删除SKU ${item.skuName}？`)) {
    return
  }
  try {
    await deleteSku(selectedProduct.value.id, item.id)
    showMessage('SKU删除成功')
    await refreshSkusAndSelection()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function submitUnit() {
  const conversionRate = Number(unitForm.conversionRate)
  if (!unitForm.unitName || Number.isNaN(conversionRate) || conversionRate < 1) {
    showMessage('请填写单位名称，且换算比例不能小于1', 'error')
    return
  }
  unitSubmitting.value = true
  try {
    const skuId = selectedSku.value.id
    if (editingUnitId.value) {
      await updateUnit(selectedProduct.value.id, skuId, editingUnitId.value, unitForm)
    } else {
      await createUnit(selectedProduct.value.id, skuId, unitForm)
    }
    unitFormVisible.value = false
    showMessage('单位换算保存成功')
    await refreshSkusAndSelection(skuId)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    unitSubmitting.value = false
  }
}

async function removeUnit(item) {
  if (!window.confirm(`确认删除单位 ${item.unitName}？`)) {
    return
  }
  try {
    const skuId = selectedSku.value.id
    await deleteUnit(selectedProduct.value.id, skuId, item.id)
    showMessage('单位换算删除成功')
    await refreshSkusAndSelection(skuId)
  } catch (error) {
    showMessage(error.message, 'error')
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

async function loadCategories() {
  try {
    categoryTree.value = await getCategoryTree()
  } catch (error) {
    showMessage('加载分类失败', 'error')
  }
}

async function loadBrands() {
  try {
    brandOptions.value = await listBrandOptions()
  } catch (error) {
    showMessage('加载品牌失败', 'error')
  }
}

onMounted(() => {
  loadData()
  loadCategories()
  loadBrands()
})
</script>
