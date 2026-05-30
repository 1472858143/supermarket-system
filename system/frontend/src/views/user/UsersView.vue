<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-desc">仅管理员可维护用户和角色，不展示任何密码字段</p>
      </div>
    </div>

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">{{ message }}</div>

    <section class="card">
      <PageToolbar v-model:keyword="query.keyword" placeholder="用户名或姓名" @search="reload" @reset="resetQuery">
        <PermissionButton :roles="['ADMIN']" button-class="btn-primary" icon="+" @click="openCreate">新增用户</PermissionButton>
      </PageToolbar>
      <BaseTable
        :columns="columns"
        :items="items"
        :total="total"
        :page="query.page"
        :page-size="query.pageSize"
        :loading="loading"
        empty-text="暂无用户记录"
        @page-change="changePage"
      >
        <template #cell-status="{ item }">
          <StatusTag type="enabled" :value="item.status" />
        </template>
        <template #cell-roles="{ item }">
          {{ (item.roles || []).map((role) => role.roleCode).join(', ') || '-' }}
        </template>
        <template #actions="{ item }">
          <div class="toolbar-left">
            <PermissionButton :roles="['ADMIN']" @click="openEdit(item)">编辑</PermissionButton>
            <PermissionButton :roles="['ADMIN']" button-class="btn-danger" @click="remove(item)">删除</PermissionButton>
          </div>
        </template>
      </BaseTable>
    </section>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑用户' : '新增用户'">
      <form class="form-grid">
        <label class="form-item">
          <span class="form-label">用户名</span>
          <input v-model.trim="form.username" class="input" :disabled="Boolean(editingId)" />
        </label>
        <label class="form-item">
          <span class="form-label">{{ editingId ? '重置密码' : '密码' }}</span>
          <input v-model="form.password" class="input" type="password" />
        </label>
        <label class="form-item">
          <span class="form-label">姓名</span>
          <input v-model.trim="form.realName" class="input" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="form.status" class="select">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </label>
        <div class="form-item full">
          <span class="form-label">角色</span>
          <label v-for="role in roles" :key="role.id">
            <input v-model="form.roleIds" type="checkbox" :value="role.id" />
            {{ role.roleName }}（{{ role.roleCode }}）
          </label>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '保存' }}
        </button>
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
import StatusTag from '../../components/StatusTag.vue'
import { createUser, deleteUser, listRoles, listUsers, updateUser } from '../../api/user'

const columns = [
  { key: 'username', title: '用户名' },
  { key: 'realName', title: '姓名' },
  { key: 'roles', title: '角色' },
  { key: 'status', title: '状态' },
  { key: 'createTime', title: '创建时间' }
]
const query = reactive({ keyword: '', page: 1, pageSize: 10 })
const items = ref([])
const roles = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const message = ref('')
const messageType = ref('success')
const form = reactive({ username: '', password: '', realName: '', status: 1, roleIds: [] })

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
}

async function loadData() {
  loading.value = true
  try {
    const data = await listUsers(query)
    items.value = data.items || []
    total.value = data.total || 0
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  roles.value = await listRoles()
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
  Object.assign(form, { username: '', password: '', realName: '', status: 1, roleIds: [] })
  editingId.value = null
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  Object.assign(form, {
    username: item.username,
    password: '',
    realName: item.realName,
    status: item.status,
    roleIds: (item.roles || []).map((role) => role.id)
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.username || (!editingId.value && !form.password) || form.roleIds.length === 0) {
    showMessage('请填写用户名、密码并选择角色', 'error')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        realName: form.realName,
        status: form.status,
        roleIds: form.roleIds,
        newPassword: form.password || null
      })
    } else {
      await createUser(form)
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
  if (!window.confirm(`确认删除用户 ${item.username}？`)) {
    return
  }
  try {
    await deleteUser(item.id)
    showMessage('删除成功')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

onMounted(async () => {
  await loadRoles()
  await loadData()
})
</script>
