<template>
  <span class="tag" :class="className">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'status'
  },
  value: {
    type: [String, Number],
    default: ''
  }
})

const label = computed(() => {
  if (props.type === 'purchase') {
    const purchaseStatusMap = {
      DRAFT: { text: '草稿', className: 'tag-muted' },
      SUBMITTED: { text: '待审批', className: 'tag-warning' },
      RETURNED: { text: '退回修改', className: 'tag-danger' },
      APPROVED: { text: '已审批', className: 'tag-success' },
      PARTIALLY_INBOUNDED: { text: '部分入库', className: 'tag-warning' },
      INBOUNDED: { text: '已入库', className: 'tag-success' },
      CANCELLED: { text: '已取消', className: 'tag-muted' },
      CLOSED: { text: '已关闭', className: 'tag-muted' }
    }
    return purchaseStatusMap[props.value]?.text || props.value || '-'
  }
  if (props.type === 'batch') {
    const labels = {
      AVAILABLE: '可用',
      DEPLETED: '已耗尽',
      EXPIRED: '已过期',
      LOCKED: '已冻结',
      DAMAGED: '已报损',
      CLOSED: '已关闭'
    }
    return labels[props.value] || props.value || '-'
  }
  if (props.type === 'enabled') {
    return Number(props.value) === 1 ? '启用' : '禁用'
  }
  if (props.type === 'product') {
    return Number(props.value) === 1 ? '上架' : '下架'
  }
  if (props.type === 'warning') {
    return props.value === 'LOW' ? '低库存' : props.value === 'HIGH' ? '超上限' : '正常'
  }
  if (props.type === 'batch') {
    return props.value === 'DEPLETED' ? '已扣完' : '可用'
  }
  return props.value || '-'
})

const className = computed(() => {
  if (props.type === 'purchase') {
    const purchaseStatusMap = {
      DRAFT: { text: '草稿', className: 'tag-muted' },
      SUBMITTED: { text: '待审批', className: 'tag-warning' },
      RETURNED: { text: '退回修改', className: 'tag-danger' },
      APPROVED: { text: '已审批', className: 'tag-success' },
      PARTIALLY_INBOUNDED: { text: '部分入库', className: 'tag-warning' },
      INBOUNDED: { text: '已入库', className: 'tag-success' },
      CANCELLED: { text: '已取消', className: 'tag-muted' },
      CLOSED: { text: '已关闭', className: 'tag-muted' }
    }
    return purchaseStatusMap[props.value]?.className || 'tag-muted'
  }
  if (props.type === 'batch') {
    const classes = {
      AVAILABLE: 'tag-success',
      DEPLETED: 'tag-muted',
      EXPIRED: 'tag-warning',
      LOCKED: 'tag-warning',
      DAMAGED: 'tag-danger',
      CLOSED: 'tag-muted'
    }
    return classes[props.value] || 'tag-muted'
  }
  if (props.type === 'warning') {
    return props.value === 'NORMAL' ? 'tag-success' : 'tag-warning'
  }
  if (props.type === 'batch') {
    return props.value === 'DEPLETED' ? 'tag-muted' : 'tag-success'
  }
  return Number(props.value) === 1 ? 'tag-success' : 'tag-muted'
})
</script>
