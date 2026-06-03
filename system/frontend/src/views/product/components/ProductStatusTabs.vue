<template>
  <div class="modern-status-tabs">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      :class="{ on: activeStatus === tab.key }"
      @click="$emit('set-status', tab.key)"
    >
      {{ tab.label }}<span class="ct">{{ formatNumber(tab.count) }}</span>
    </button>
  </div>
</template>

<script setup>
defineProps({
  tabs: {
    type: Array,
    default: () => []
  },
  activeStatus: {
    type: String,
    default: 'all'
  }
})

defineEmits(['set-status'])

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}
</script>
