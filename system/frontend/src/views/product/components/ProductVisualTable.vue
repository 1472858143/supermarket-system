<template>
  <article class="card modern-table-card">
    <ProductStatusTabs :tabs="statusTabs" :active-status="query.status" @set-status="$emit('set-status', $event)" />

    <div class="table-host modern-product-table">
      <table class="t">
        <thead>
          <tr>
            <th class="col-check">
              <input type="checkbox" class="check" :checked="allPageSelected" @change="$emit('toggle-page')" />
            </th>
            <th style="min-width: 300px">商品名称 / SPU 编码</th>
            <th>品牌</th>
            <th>基础品类</th>
            <th style="min-width: 240px">基础商品属性</th>
            <th>关联 SKU</th>
            <th>状态</th>
            <th>更新时间</th>
            <th style="width: 120px">操作</th>
          </tr>
        </thead>
        <tbody v-if="pagedRows.length">
          <tr
            v-for="row in pagedRows"
            :key="row.id"
            :class="{ checked: selectedIds.has(row.id) }"
            @click="$emit('action', `${row.productName} 档案详情待接入`)"
          >
            <td class="col-check" @click.stop>
              <input type="checkbox" class="check row-check" :checked="selectedIds.has(row.id)" @change="$emit('toggle-row', row.id)" />
            </td>
            <td>
              <div class="prod-cell">
                <div class="prod-thumb" :style="row.thumbStyle"><span>{{ row.thumbText }}</span></div>
                <div class="prod-info">
                  <div class="name">
                    {{ row.productName }}
                    <span v-if="row.isNew" class="tag tag-new">NEW</span>
                  </div>
                  <div class="sub-sku">
                    <span class="bar">{{ row.spuCode || '保存后生成' }}</span>
                  </div>
                </div>
              </div>
            </td>
            <td>
              <b class="archive-brand">{{ row.brandName || '未设置品牌' }}</b>
            </td>
            <td>
              <span class="cat-tag">
                <span class="swatch" :style="{ background: row.categoryColor }"></span>
                {{ row.categoryName || '未分类' }}
              </span>
            </td>
            <td>
              <div class="archive-attrs">
                <span v-for="attr in row.attrTags" :key="`${attr.key}-${attr.value}`" class="attr">
                  <span class="k">{{ attr.key }}</span>
                  <span class="v">{{ attr.value }}</span>
                </span>
              </div>
            </td>
            <td>
              <button class="sku-link" type="button" title="查看该 SPU 下的 SKU" @click.stop="$emit('manage-sku', row)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="12 2 2 7 12 12 22 7 12 2" />
                  <polyline points="2 17 12 22 22 17" />
                  <polyline points="2 12 12 17 22 12" />
                </svg>
                <b>{{ formatNumber(row.skuCount) }}</b> 个
              </button>
            </td>
            <td><span class="pill" :class="row.statusClass"><span class="dot"></span>{{ row.statusLabel }}</span></td>
            <td class="modern-time">{{ row.updateTime }}</td>
            <td @click.stop>
              <div class="row-actions">
                <button type="button" title="查看档案" @click="$emit('action', `${row.productName} 档案详情待接入`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                </button>
                <button type="button" title="编辑档案" @click="$emit('edit', row)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                  </svg>
                </button>
                <button type="button" title="管理 SKU" @click="$emit('manage-sku', row)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polygon points="12 2 2 7 12 12 22 7 12 2" />
                    <polyline points="2 17 12 22 22 17" />
                    <polyline points="2 12 12 17 22 12" />
                  </svg>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && !pagedRows.length" class="modern-empty">
        <div class="icon-circle">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </div>
        <h4>没有找到匹配的商品</h4>
        <p>调整筛选条件后重新查询</p>
        <button class="btn primary sm" type="button" @click="$emit('reset')">重置筛选</button>
      </div>
      <div v-if="loading" class="modern-loading">正在加载商品数据...</div>
    </div>

    <div class="page-foot">
      <div class="total">共 <b>{{ formatNumber(filteredCount) }}</b> 条记录</div>
      <div class="row">
        <span>每页</span>
        <select v-model.number="query.pageSize" class="select" @change="$emit('reset-page')">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
        <span>条</span>
      </div>
      <div class="pager">
        <button type="button" :disabled="query.page <= 1" @click="$emit('change-page', 1)">«</button>
        <button type="button" :disabled="query.page <= 1" @click="$emit('change-page', query.page - 1)">‹</button>
        <button
          v-for="page in pageButtons"
          :key="page"
          type="button"
          :class="{ on: page === query.page }"
          @click="$emit('change-page', page)"
        >
          {{ page }}
        </button>
        <span v-if="showLastGap" class="gap">…</span>
        <button v-if="showLastShortcut" type="button" @click="$emit('change-page', pageCount)">{{ pageCount }}</button>
        <button type="button" :disabled="query.page >= pageCount" @click="$emit('change-page', query.page + 1)">›</button>
        <button type="button" :disabled="query.page >= pageCount" @click="$emit('change-page', pageCount)">»</button>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import ProductStatusTabs from './ProductStatusTabs.vue'

const props = defineProps({
  query: {
    type: Object,
    required: true
  },
  statusTabs: {
    type: Array,
    default: () => []
  },
  pagedRows: {
    type: Array,
    default: () => []
  },
  selectedIds: {
    type: Set,
    required: true
  },
  allPageSelected: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  filteredCount: {
    type: Number,
    default: 0
  },
  pageButtons: {
    type: Array,
    default: () => []
  },
  pageCount: {
    type: Number,
    default: 1
  }
})

defineEmits([
  'set-status',
  'toggle-page',
  'toggle-row',
  'action',
  'edit',
  'manage-sku',
  'reset',
  'reset-page',
  'change-page'
])

const showLastShortcut = computed(() => {
  return props.pageCount > 0 && !props.pageButtons.includes(props.pageCount)
})

const showLastGap = computed(() => {
  if (props.pageButtons.length === 0) return false
  const lastButton = props.pageButtons[props.pageButtons.length - 1]
  return props.pageCount > lastButton + 1
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value) {
  return `￥ ${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
</script>
