<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">系统信息</h1>
        <p class="page-desc">轻量化 system 模块，仅展示运行信息和使用说明</p>
      </div>
      <button class="btn" type="button" @click="loadData">刷新</button>
    </div>

    <div v-if="message" class="message message-error">{{ message }}</div>

    <section class="card">
      <BaseTable
        :columns="columns"
        :items="items"
        :total="items.length"
        :page="1"
        :page-size="items.length || 10"
        :loading="loading"
        empty-text="暂无系统信息"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import BaseTable from '../../components/BaseTable.vue'
import { getSystemInfo } from '../../api/system'

const loading = ref(false)
const message = ref('')
const info = ref({})
const columns = [
  { key: 'label', title: '项目' },
  { key: 'value', title: '内容' }
]

const labels = {
  systemName: '系统名称',
  version: '系统版本',
  backend: '后端技术',
  database: '数据库',
  profile: '运行环境',
  serverTime: '服务器时间',
  description: '模块说明'
}

const items = computed(() =>
  Object.keys(info.value).map((key) => ({
    label: labels[key] || key,
    value: info.value[key]
  }))
)

async function loadData() {
  loading.value = true
  message.value = ''
  try {
    info.value = await getSystemInfo()
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
