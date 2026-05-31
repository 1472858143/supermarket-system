<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">分类管理</h1>
        <p class="page-desc">维护商品分类，支持一级和二级分类</p>
      </div>
    </div>
    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>
    <section class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreateRoot">新增一级分类</PermissionButton>
        </div>
      </div>
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="tree.length === 0" class="empty-state">暂无分类数据</div>
      <div v-else class="category-tree">
        <div v-for="parent in tree" :key="parent.id" class="category-group">
          <div class="category-row category-row-parent">
            <span class="category-name">{{ parent.name }}</span>
            <span class="category-sort">排序: {{ parent.sortOrder }}</span>
            <div class="toolbar-left">
              <PermissionButton :roles="['ADMIN']" @click="openCreateChild(parent)">新增子分类</PermissionButton>
              <PermissionButton :roles="['ADMIN']" @click="openEdit(parent)">编辑</PermissionButton>
              <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="remove(parent)">删除</PermissionButton>
            </div>
          </div>
          <div v-for="child in parent.children" :key="child.id" class="category-row category-row-child">
            <span class="category-indent">└─</span>
            <span class="category-name">{{ child.name }}</span>
            <span class="category-sort">排序: {{ child.sortOrder }}</span>
            <div class="toolbar-left">
              <PermissionButton :roles="['ADMIN']" @click="openEdit(child)">编辑</PermissionButton>
              <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="remove(child)">删除</PermissionButton>
            </div>
          </div>
        </div>
      </div>
    </section>

    <BaseDialog v-model="dialogVisible" :title="dialogTitle">
      <form class="form-grid">
        <label v-if="editingParentName" class="form-item">
          <span class="form-label">父分类</span>
          <input :value="editingParentName" class="input" disabled />
        </label>
        <label class="form-item">
          <span class="form-label">分类名称</span>
          <input v-model.trim="form.name" class="input" />
        </label>
        <label v-if="editingId" class="form-item">
          <span class="form-label">排序序号</span>
          <input v-model.number="form.sortOrder" class="input" type="number" min="0" />
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
import PermissionButton from '../../components/PermissionButton.vue'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '../../api/category'

const tree = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const editingParentId = ref(null)
const editingParentName = ref('')
const message = ref('')
const messageType = ref('success')
const form = reactive({ name: '', sortOrder: 0 })

const dialogTitle = computed(() => {
  if (editingId.value) return '编辑分类'
  if (editingParentId.value) return '新增二级分类'
  return '新增一级分类'
})

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

async function loadData() {
  loading.value = true
  try {
    tree.value = await getCategoryTree()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { name: '', sortOrder: 0 })
  editingId.value = null
  editingParentId.value = null
  editingParentName.value = ''
}

function openCreateRoot() {
  resetForm()
  dialogVisible.value = true
}

function openCreateChild(parent) {
  resetForm()
  editingParentId.value = parent.id
  editingParentName.value = parent.name
  dialogVisible.value = true
}

function openEdit(item) {
  resetForm()
  editingId.value = item.id
  form.name = item.name
  form.sortOrder = item.sortOrder
  dialogVisible.value = true
}

async function submit() {
  if (!form.name) {
    showMessage('请填写分类名称', 'error')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { name: form.name, sortOrder: form.sortOrder })
    } else {
      await createCategory({ name: form.name, parentId: editingParentId.value })
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
  if (!window.confirm(`确认删除分类「${item.name}」？`)) return
  try {
    await deleteCategory(item.id)
    showMessage('删除成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

onMounted(loadData)
</script>

<style scoped>
.category-tree {
  padding: 0.5rem 0;
}
.category-group {
  border-bottom: 1px solid var(--border);
}
.category-group:last-child {
  border-bottom: none;
}
.category-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.6rem 1rem;
}
.category-row-parent {
  font-weight: 600;
}
.category-row-child {
  padding-left: 2.5rem;
}
.category-indent {
  color: var(--text-secondary);
  user-select: none;
}
.category-name {
  flex: 1;
}
.category-sort {
  color: var(--text-secondary);
  font-size: 0.85rem;
  min-width: 5rem;
}
</style>
