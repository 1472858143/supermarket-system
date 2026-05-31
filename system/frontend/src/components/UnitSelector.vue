<template>
  <label class="form-item">
    <span class="form-label">单位</span>
    <select v-model="selectedUnit" class="select" :disabled="!options.length">
      <option v-for="option in options" :key="option.unitName" :value="option.unitName">
        {{ option.unitName }}
      </option>
    </select>
  </label>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  baseUnit: {
    type: String,
    default: ''
  },
  units: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'rate-changed'])

const selectedUnit = ref('')
const options = computed(() => {
  const baseUnit = props.baseUnit?.trim()
  const baseOption = baseUnit ? [{ unitName: baseUnit, conversionRate: 1 }] : []
  const conversionOptions = props.units
    .filter((unit) => unit.unitName && unit.unitName !== baseUnit)
    .map((unit) => ({ unitName: unit.unitName, conversionRate: unit.conversionRate }))
  return [...baseOption, ...conversionOptions]
})

watch(
  options,
  (items) => {
    if (!items.length) {
      selectedUnit.value = ''
      emit('update:modelValue', '')
      emit('rate-changed', 1)
      return
    }
    const next = items.find((item) => item.unitName === props.modelValue) || items[0]
    selectedUnit.value = next.unitName
    emit('update:modelValue', next.unitName)
    emit('rate-changed', next.conversionRate)
  },
  { immediate: true }
)

watch(
  () => props.modelValue,
  (unit) => {
    if (unit !== selectedUnit.value && options.value.some((item) => item.unitName === unit)) {
      selectedUnit.value = unit
    }
  }
)

watch(selectedUnit, (unit) => {
  const option = options.value.find((item) => item.unitName === unit)
  emit('update:modelValue', option?.unitName || '')
  emit('rate-changed', option?.conversionRate || 1)
})
</script>
