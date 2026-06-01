<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">品牌管理</h1>
        <p class="page-desc">维护品牌主档，并绑定到商品SPU</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="品牌编码或品牌名称" @search="reload" @reset="resetQuery">
        <select v-model="query.status" class="select" @change="reload">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </select>
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增品牌</PermissionButton>
      </PageToolbar>
      <BaseTable
        :columns="columns"
        :items="items"
        :total="total"
        :page="query.page"
        :page-size="query.pageSize"
        :loading="loading"
        :show-actions="canManageBrands"
        empty-text="暂无品牌记录"
        @page-change="changePage"
      >
        <template #cell-status="{ item }">
          <StatusTag type="enabled" :value="item.status" />
        </template>
        <template #actions="{ item }">
          <div class="toolbar-left">
            <PermissionButton :roles="['ADMIN']" @click="openEdit(item)">编辑</PermissionButton>
            <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="remove(item)">删除</PermissionButton>
          </div>
        </template>
      </BaseTable>
    </section>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑品牌' : '新增品牌'">
      <form class="form-grid">
        <label v-if="editingId" class="form-item">
          <span class="form-label">品牌编码</span>
          <input v-model.trim="form.brandCode" class="input" disabled />
        </label>
        <label class="form-item">
          <span class="form-label">品牌名称</span>
          <input v-model.trim="form.brandName" class="input" maxlength="100" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="form.status" class="select">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </label>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <input v-model.trim="form.remark" class="input" maxlength="200" />
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
import { createBrand, deleteBrand, listBrands, updateBrand } from '../../api/brand'
import { useAuthStore } from '../../stores/auth'

const columns = [
  { key: 'brandCode', title: '品牌编码' },
  { key: 'brandName', title: '品牌名称' },
  { key: 'status', title: '状态' },
  { key: 'remark', title: '备注' },
  { key: 'createTime', title: '创建时间' }
]

const query = reactive({ keyword: '', status: '', page: 1, pageSize: 10 })
const items = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const message = ref('')
const messageType = ref('success')
const form = reactive({ brandCode: '', brandName: '', status: 1, remark: '' })
const authStore = useAuthStore()
const canManageBrands = computed(() => authStore.hasRole('ADMIN'))

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

async function loadData() {
  loading.value = true
  try {
    const data = await listBrands({
      keyword: query.keyword,
      status: query.status === '' ? undefined : Number(query.status),
      page: query.page,
      pageSize: query.pageSize
    })
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
  query.status = ''
  reload()
}

function changePage(page) {
  query.page = page
  loadData()
}

function resetForm() {
  Object.assign(form, { brandCode: '', brandName: '', status: 1, remark: '' })
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    brandCode: item.brandCode,
    brandName: item.brandName,
    status: item.status,
    remark: item.remark || ''
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.brandName) {
    showMessage('请填写品牌名称', 'error')
    return
  }
  submitting.value = true
  try {
    const payload = {
      brandName: form.brandName,
      status: form.status,
      remark: form.remark
    }
    if (editingId.value) {
      await updateBrand(editingId.value, payload)
    } else {
      await createBrand(payload)
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
  if (!window.confirm(`确认删除品牌 ${item.brandName}？已绑定商品的品牌不能删除。`)) {
    return
  }
  try {
    await deleteBrand(item.id)
    showMessage('删除成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

onMounted(loadData)
</script>
