<template>
  <section class="modern-filter-bar">
    <label class="modern-filter-search">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      <input
        v-model.trim="query.keyword"
        placeholder="按商品名 / SKU / 条码搜索..."
        @keydown.enter="$emit('reload')"
      />
    </label>
    <div class="modern-filter-divider"></div>
    <label class="modern-field">
      <span class="modern-field-label">分类</span>
      <select v-model="query.categoryId" class="select" @change="$emit('reset-page')">
        <option value="">全部</option>
        <option v-for="category in categories" :key="category.id" :value="String(category.id)">
          {{ category.label }}
        </option>
      </select>
    </label>
    <label class="modern-field">
      <span class="modern-field-label">品牌</span>
      <select v-model="query.brandId" class="select" @change="$emit('reset-page')">
        <option value="">全部</option>
        <option v-for="brand in brands" :key="brand.id" :value="String(brand.id)">
          {{ brand.brandName }}
        </option>
      </select>
    </label>
    <label class="modern-field">
      <span class="modern-field-label">价格</span>
      <input
        v-model.number="query.priceMin"
        class="input compact-input"
        type="number"
        min="0"
        placeholder="最低"
        @input="$emit('reset-page')"
      />
      <span class="modern-range-sep">-</span>
      <input
        v-model.number="query.priceMax"
        class="input compact-input"
        type="number"
        min="0"
        placeholder="最高"
        @input="$emit('reset-page')"
      />
    </label>
    <button class="btn ghost sm" type="button" @click="$emit('reset')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="1 4 1 10 7 10" />
        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
      </svg>
      重置
    </button>
    <button class="btn primary sm" type="button" :disabled="loading" @click="$emit('reload')">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
      查询
    </button>
  </section>
</template>

<script setup>
defineProps({
  query: {
    type: Object,
    required: true
  },
  categories: {
    type: Array,
    default: () => []
  },
  brands: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['reload', 'reset', 'reset-page'])
</script>
