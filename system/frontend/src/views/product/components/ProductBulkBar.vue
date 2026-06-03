<template>
  <div class="modern-bulk-bar" :class="{ show: selectedCount > 0 }">
    <div class="count"><span class="num">{{ selectedCount }}</span>件商品已选</div>
    <div class="bar-sep"></div>
    <button type="button" @click="$emit('bulk-action', '批量上架动作已记录')">批量上架</button>
    <button type="button" @click="$emit('bulk-action', '批量下架动作已记录', 'error')">批量下架</button>
    <button type="button" @click="$emit('bulk-action', '批量改价向导待接入')">批量改价</button>
    <button type="button" @click="$emit('bulk-action', '正在导出已选商品')">导出</button>
    <div class="bar-sep"></div>
    <button class="danger" type="button" @click="$emit('clear-selection')">取消选择</button>
  </div>
</template>

<script setup>
defineProps({
  selectedCount: {
    type: Number,
    default: 0
  }
})

defineEmits(['bulk-action', 'clear-selection'])
</script>
