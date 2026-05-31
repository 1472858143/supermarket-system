<template>
  <div class="selector-grid full">
    <label class="form-item">
      <span class="form-label">商品</span>
      <select v-model.number="selectedProductId" class="select">
        <option :value="null">请选择商品</option>
        <option v-for="product in products" :key="product.id" :value="product.id">
          {{ product.productCode }} / {{ product.productName }}
        </option>
      </select>
    </label>
    <label class="form-item">
      <span class="form-label">SKU</span>
      <select v-model.number="selectedSkuId" class="select" :disabled="!selectedProductId || loading">
        <option :value="null">{{ loading ? '加载中...' : '请选择SKU' }}</option>
        <option v-for="sku in skus" :key="sku.id" :value="sku.id">
          {{ sku.skuCode }} / {{ sku.skuName }}
        </option>
      </select>
    </label>
    <div v-if="errorMessage" class="form-hint full">{{ errorMessage }}</div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { listSkus } from '../api/sku'

const props = defineProps({
  modelValue: {
    type: Number,
    default: null
  },
  products: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'sku-selected'])

const selectedProductId = ref(null)
const selectedSkuId = ref(null)
const skus = ref([])
const loading = ref(false)
const errorMessage = ref('')

watch(selectedProductId, async (productId) => {
  selectedSkuId.value = null
  skus.value = []
  errorMessage.value = ''
  emit('update:modelValue', null)
  emit('sku-selected', null)
  if (!productId) {
    return
  }
  loading.value = true
  try {
    skus.value = await listSkus(productId)
    if (skus.value.length === 1) {
      selectedSkuId.value = skus.value[0].id
    }
  } catch (error) {
    errorMessage.value = error.message || 'SKU加载失败'
  } finally {
    loading.value = false
  }
})

watch(selectedSkuId, (skuId) => {
  const sku = skus.value.find((item) => item.id === skuId) || null
  emit('update:modelValue', sku?.id || null)
  emit('sku-selected', sku)
})

watch(
  () => props.modelValue,
  (skuId) => {
    if (!skuId) {
      selectedSkuId.value = null
    }
  }
)
</script>
