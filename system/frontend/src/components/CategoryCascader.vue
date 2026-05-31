<template>
  <div class="cascader">
    <label class="form-item">
      <span class="form-label">一级分类</span>
      <select v-model.number="selectedParentId" class="select" @change="onParentChange">
        <option :value="null" disabled>请选择一级分类</option>
        <option v-for="p in tree" :key="p.id" :value="p.id">{{ p.name }}</option>
      </select>
    </label>
    <label class="form-item">
      <span class="form-label">二级分类</span>
      <select :value="modelValue" class="select" @change="$emit('update:modelValue', Number($event.target.value))">
        <option :value="null" disabled>请选择二级分类</option>
        <option v-for="c in childOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
    </label>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: null },
  tree: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue'])

const selectedParentId = ref(null)

const childOptions = computed(() => {
  const parent = props.tree.find(p => p.id === selectedParentId.value)
  return parent ? parent.children : []
})

function onParentChange() {
  emit('update:modelValue', null)
}

watch(() => props.modelValue, (newVal) => {
  if (newVal == null) return
  for (const parent of props.tree) {
    const found = parent.children.find(c => c.id === newVal)
    if (found) {
      selectedParentId.value = parent.id
      return
    }
  }
}, { immediate: true })
</script>
