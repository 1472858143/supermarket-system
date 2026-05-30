<template>
  <div class="toolbar">
    <div class="toolbar-left">
      <input
        class="input"
        :value="keyword"
        :placeholder="placeholder"
        @input="$emit('update:keyword', $event.target.value)"
        @keyup.enter="$emit('search')"
      />
      <button class="btn" type="button" @click="$emit('search')">查询</button>
      <button class="btn btn-ghost" type="button" @click="$emit('reset')">重置</button>
    </div>
    <div class="toolbar-right">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  keyword: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入关键词'
  }
})

defineEmits(['update:keyword', 'search', 'reset'])
</script>
