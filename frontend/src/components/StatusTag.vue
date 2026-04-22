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
  if (props.type === 'enabled') {
    return Number(props.value) === 1 ? '启用' : '禁用'
  }
  if (props.type === 'product') {
    return Number(props.value) === 1 ? '上架' : '下架'
  }
  if (props.type === 'warning') {
    return props.value === 'LOW' ? '低库存' : props.value === 'HIGH' ? '超上限' : '正常'
  }
  return props.value || '-'
})

const className = computed(() => {
  if (props.type === 'warning') {
    return props.value === 'NORMAL' ? 'tag-success' : 'tag-warning'
  }
  return Number(props.value) === 1 ? 'tag-success' : 'tag-muted'
})
</script>
