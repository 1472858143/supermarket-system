<template>
  <nav class="sub-tabs">
    <RouterLink :class="{ on: active === 'list' }" to="/products-modern">
      <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="8" y1="6" x2="21" y2="6" />
        <line x1="8" y1="12" x2="21" y2="12" />
        <line x1="8" y1="18" x2="21" y2="18" />
        <line x1="3" y1="6" x2="3.01" y2="6" />
        <line x1="3" y1="12" x2="3.01" y2="12" />
        <line x1="3" y1="18" x2="3.01" y2="18" />
      </svg>
      <span>商品档案</span>
      <span class="ct">{{ formatNumber(totalCount) }}</span>
    </RouterLink>
    <RouterLink :class="{ on: active === 'sku' }" to="/product-skus-modern">
      <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polygon points="12 2 2 7 12 12 22 7 12 2" />
        <polyline points="2 17 12 22 22 17" />
        <polyline points="2 12 12 17 22 12" />
      </svg>
      <span>SKU 管理</span>
    </RouterLink>
    <RouterLink :class="{ on: active === 'category' }" to="/product-categories-modern">
      <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      </svg>
      <span>分类管理</span>
    </RouterLink>
    <RouterLink to="/stocks">
      <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
        <polyline points="9 22 9 12 15 12 15 22" />
      </svg>
      <span>库存视图</span>
    </RouterLink>
    <RouterLink :class="{ on: active === 'brand' }" to="/brands">
      <svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z" />
        <line x1="7" y1="7" x2="7.01" y2="7" />
      </svg>
      <span>品牌管理</span>
    </RouterLink>
  </nav>
</template>

<script setup>
defineProps({
  active: {
    type: String,
    default: 'list'
  },
  totalCount: {
    type: Number,
    default: 0
  }
})

defineEmits(['action'])

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}
</script>
