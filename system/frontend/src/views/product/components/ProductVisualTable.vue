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
            <th style="min-width: 280px">商品 / SKU</th>
            <th>品类 · 条码</th>
            <th>售价 / 成本</th>
            <th>毛利</th>
            <th>库存</th>
            <th>SKU数</th>
            <th>状态</th>
            <th>更新时间</th>
            <th style="width: 132px">操作</th>
          </tr>
        </thead>
        <tbody v-if="pagedRows.length">
          <tr
            v-for="row in pagedRows"
            :key="row.id"
            :class="{ checked: selectedIds.has(row.id) }"
            @click="$emit('action', `${row.productName} 详情面板待接入`)"
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
                    <span v-else-if="row.isLow" class="tag tag-promo">预警</span>
                  </div>
                  <div class="sub-sku">
                    <span class="bar">{{ row.skuCode || row.productCode }}</span>
                    <span>{{ row.skuName || '默认规格' }}</span>
                  </div>
                </div>
              </div>
            </td>
            <td>
              <b>{{ row.categoryName || '未分类' }}</b>
              <div class="modern-muted">{{ row.brandName || '未设置品牌' }}</div>
              <div class="modern-muted">{{ row.barcode || '暂无条码' }}</div>
            </td>
            <td>
              <div class="price-block">
                <div class="sell">{{ money(row.salePrice) }}</div>
                <div class="cost">成本 {{ money(row.purchasePrice) }}</div>
              </div>
            </td>
            <td>
              <div class="modern-margin-ring" :style="{ '--mgn': row.margin, '--mgn-c': row.marginColor }">
                <div class="ring"></div>
                <div class="val" :style="{ color: row.marginColor }">{{ row.margin }}%</div>
              </div>
            </td>
            <td>
              <div class="stock-text">
                <span class="cur">{{ formatNumber(row.quantity) }}</span>
                <span class="sep">/</span>
                <span class="max">{{ formatNumber(row.safeStock) }}</span>
              </div>
              <div class="stock-bar"><i :class="row.stockClass" :style="{ width: `${row.stockPercent}%` }"></i></div>
            </td>
            <td class="num">{{ row.skuCount }}</td>
            <td><span class="pill" :class="row.statusClass"><span class="dot"></span>{{ row.statusLabel }}</span></td>
            <td class="modern-time">{{ row.updateTime }}</td>
            <td @click.stop>
              <div class="row-actions">
                <button type="button" title="查看" @click="$emit('action', `${row.productName} 详情面板待接入`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                    <circle cx="12" cy="12" r="3" />
                  </svg>
                </button>
                <button type="button" title="编辑" @click="$emit('edit')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                  </svg>
                </button>
                <button type="button" title="补货" @click="$emit('action', `${row.productName} 已加入补货清单`)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="9" cy="21" r="1" />
                    <circle cx="20" cy="21" r="1" />
                    <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
                  </svg>
                </button>
                <button class="danger" type="button" title="下架" @click="$emit('action', `${row.productName} 下架动作需在旧版维护页确认`, 'error')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
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
        <button type="button" :disabled="query.page >= pageCount" @click="$emit('change-page', query.page + 1)">›</button>
      </div>
    </div>
  </article>
</template>

<script setup>
import ProductStatusTabs from './ProductStatusTabs.vue'

defineProps({
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
  'reset',
  'reset-page',
  'change-page'
])

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function money(value) {
  return `￥ ${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
</script>
