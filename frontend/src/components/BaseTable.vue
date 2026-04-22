<template>
  <div>
    <div class="table-wrap">
      <div v-if="loading" class="table-state">加载中...</div>
      <div v-else-if="!items.length" class="table-state">{{ emptyText }}</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th v-for="column in columns" :key="column.key">{{ column.title }}</th>
            <th v-if="$slots.actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item[idKey] || item.id">
            <td v-for="column in columns" :key="column.key">
              <slot :name="`cell-${column.key}`" :item="item">
                {{ item[column.key] ?? '-' }}
              </slot>
            </td>
            <td v-if="$slots.actions">
              <slot name="actions" :item="item" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination">
      <span>共 {{ total }} 条</span>
      <button class="btn btn-ghost" type="button" :disabled="page <= 1" @click="$emit('page-change', page - 1)">上一页</button>
      <span>第 {{ page }} 页</span>
      <button class="btn btn-ghost" type="button" :disabled="page * pageSize >= total" @click="$emit('page-change', page + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  columns: {
    type: Array,
    required: true
  },
  items: {
    type: Array,
    default: () => []
  },
  total: {
    type: Number,
    default: 0
  },
  page: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  loading: {
    type: Boolean,
    default: false
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  idKey: {
    type: String,
    default: 'id'
  }
})

defineEmits(['page-change'])
</script>
