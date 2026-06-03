<template>
  <div class="inventory-center-page">
    <div class="page-head">
      <div>
        <h1>库存中心</h1>
        <div class="greet-sub">
          实时库存 · 共 <b>{{ formatNumber(total) }}</b> 条记录 ·
          预警 <b>{{ formatNumber(statusCounts.low + statusCounts.high + statusCounts.out) }}</b> ·
          最近同步 {{ syncTime || '--:--' }}
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" :disabled="loading" @click="reload">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 2v6h-6" />
            <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
            <path d="M3 22v-6h6" />
            <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
          </svg>
          刷新库存
        </button>
        <button class="btn" type="button" @click="goStockcheck">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
            <rect x="8" y="2" width="8" height="4" rx="1" />
            <path d="M9 14l2 2 4-4" />
          </svg>
          盘点入口
        </button>
        <button class="btn primary" type="button" @click="goPurchaseInbound">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          采购补货
        </button>
      </div>
    </div>

    <section class="inventory-kpi-row">
      <div v-for="card in kpiCards" :key="card.key" class="inventory-kpi" :style="{ '--accent': card.color }">
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}<span class="unit">{{ card.unit }}</span></div>
        <div class="sub">{{ card.sub }}</div>
      </div>
    </section>

    <nav class="sub-tabs">
      <a class="on" href="#" @click.prevent>实时库存<span class="ct">{{ formatNumber(filteredRows.length) }}</span></a>
      <a href="#" @click.prevent="goStockcheck">盘点管理</a>
      <a href="#" @click.prevent="safeAction('库存流水需后端提供变更日志分页接口', 'error')">库存流水</a>
      <a href="#" @click.prevent="safeAction('调拨功能当前未接入仓库接口', 'error')">调拨管理</a>
    </nav>

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">
      {{ message }}
    </div>

    <section class="inventory-filter-bar">
      <label class="inventory-filter-search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input v-model.trim="query.keyword" placeholder="按商品编号 / 名称 / SKU / 分类搜索..." @keyup.enter="reload" />
      </label>
      <div class="modern-field">
        <span class="modern-field-label">分类</span>
        <select v-model="query.category" class="select" @change="resetLocalPage">
          <option value="">全部</option>
          <option v-for="category in categoryOptions" :key="category" :value="category">{{ category }}</option>
        </select>
      </div>
      <div class="modern-field">
        <span class="modern-field-label">库存状态</span>
        <select v-model="query.status" class="select" @change="resetLocalPage">
          <option value="all">全部</option>
          <option value="normal">正常</option>
          <option value="low">低库存</option>
          <option value="out">已售罄</option>
          <option value="high">超上限</option>
        </select>
      </div>
      <button class="btn sm" type="button" @click="resetQuery">重置</button>
      <button class="btn primary sm" type="button" :disabled="loading" @click="reload">
        {{ loading ? '查询中...' : '查询' }}
      </button>
    </section>

    <article class="card inventory-table-card">
      <div class="inventory-status-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.key"
          type="button"
          :class="{ on: query.status === tab.key }"
          @click="setStatus(tab.key)"
        >
          {{ tab.label }}<span class="ct">{{ formatNumber(tab.count) }}</span>
        </button>
      </div>

      <div class="inventory-stock-table">
        <table>
          <thead>
            <tr>
              <th style="min-width: 280px">商品 / SKU</th>
              <th>分类</th>
              <th>当前库存 / 下限</th>
              <th>上限</th>
              <th>库存状态</th>
              <th>最近变动</th>
              <th style="width: 138px">操作</th>
            </tr>
          </thead>
          <tbody v-if="pagedRows.length">
            <tr v-for="row in pagedRows" :key="row.skuId || row.id">
              <td>
                <div class="prod-cell">
                  <div class="prod-thumb" :style="thumbStyle(row.productName, row.skuId)">
                    <span>{{ thumbText(row.productName) }}</span>
                  </div>
                  <div class="prod-info">
                    <div class="name">{{ row.productName || '-' }}</div>
                    <div class="sub-sku">
                      <span class="bar">{{ row.productCode || '-' }}</span>
                      <span>{{ row.skuCode || '-' }}</span>
                      <span>{{ row.spec || row.skuName || '默认规格' }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td>
                <b>{{ row.category || '未分类' }}</b>
                <div class="modern-muted">{{ row.baseUnit || '-' }}</div>
              </td>
              <td>
                <div class="stock-text">
                  <span class="cur">{{ formatNumber(row.quantity) }}</span>
                  <span class="sep">/</span>
                  <span class="max">{{ formatNumber(row.minStock) }}</span>
                </div>
                <div class="stock-bar"><i :class="row.stockClass" :style="{ width: `${row.stockPercent}%` }"></i></div>
              </td>
              <td class="num">{{ formatNumber(row.maxStock) }}</td>
              <td><span class="pill" :class="row.statusClass"><span class="dot"></span>{{ row.statusLabel }}</span></td>
              <td class="modern-time">{{ row.updateTimeText }}</td>
              <td>
                <div class="row-actions">
                  <button type="button" title="查看批次" @click="openBatches(row)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                      <path d="M3.27 6.96 12 12.01l8.73-5.05" />
                      <path d="M12 22.08V12" />
                    </svg>
                  </button>
                  <PermissionButton :roles="['ADMIN']" button-class="icon-only" @click="openLimit(row)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                    </svg>
                  </PermissionButton>
                  <button type="button" title="补货" @click="goPurchaseInbound">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="9" cy="21" r="1" />
                      <circle cx="20" cy="21" r="1" />
                      <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="modern-loading">正在加载实时库存...</div>
        <div v-else-if="!pagedRows.length" class="modern-empty">
          <div class="icon-circle">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>
          <h4>暂无匹配库存</h4>
          <p>调整关键词、分类或库存状态后再查询</p>
          <button class="btn primary sm" type="button" @click="resetQuery">重置筛选</button>
        </div>
      </div>

      <div class="page-foot">
        <div class="total">共 <b>{{ formatNumber(filteredRows.length) }}</b> 条记录</div>
        <div class="row">
          <span>每页</span>
          <select v-model.number="query.pageSize" class="select" @change="resetLocalPage">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
          <span>条</span>
        </div>
        <div class="pager">
          <button type="button" :disabled="query.page <= 1" @click="changePage(query.page - 1)">‹</button>
          <button
            v-for="page in pageButtons"
            :key="page"
            type="button"
            :class="{ on: page === query.page }"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <button type="button" :disabled="query.page >= pageCount" @click="changePage(query.page + 1)">›</button>
        </div>
      </div>
    </article>

    <BaseDialog v-model="batchDialogVisible" title="库存批次">
      <div v-if="batchStock" class="inventory-batch-summary">
        <div>
          <span>商品</span>
          <strong>{{ batchStock.productName || '-' }}</strong>
        </div>
        <div>
          <span>SKU</span>
          <strong>{{ batchStock.skuCode || '-' }}</strong>
        </div>
        <div>
          <span>当前库存</span>
          <strong>{{ formatNumber(batchStock.quantity) }}</strong>
        </div>
      </div>
      <InventoryBatchDialog
        :batches="batches"
        :loading="batchLoading"
        :action-loading="batchActionLoading"
        @lock="handleLockBatch"
        @unlock="handleUnlockBatch"
        @damage="openDamageDialog"
        @close="handleCloseBatch"
      />
      <template #footer>
        <button class="btn btn-primary" type="button" @click="batchDialogVisible = false">关闭</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="limitDialogVisible" title="维护上下限">
      <form class="form-grid">
        <label class="form-item full">
          <span class="form-label">商品</span>
          <input class="input" :value="currentStockLabel" disabled />
        </label>
        <label class="form-item">
          <span class="form-label">库存下限</span>
          <input v-model.number="limitForm.minStock" class="input" type="number" min="0" />
        </label>
        <label class="form-item">
          <span class="form-label">库存上限</span>
          <input v-model.number="limitForm.maxStock" class="input" type="number" min="0" />
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="limitDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submitLimit">
          {{ submitting ? '提交中...' : '保存' }}
        </button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="damageDialogVisible" title="批次报损">
      <form class="form-grid">
        <label class="form-item">
          <span class="form-label">数量</span>
          <input v-model.number="damageForm.quantity" class="input" type="number" min="1" :max="currentDamageMax" />
        </label>
        <label class="form-item">
          <span class="form-label">原因</span>
          <input v-model.trim="damageForm.reason" class="input" maxlength="50" />
        </label>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <textarea v-model.trim="damageForm.remark" class="input" rows="3" maxlength="200"></textarea>
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="damageDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="batchActionLoading" @click="submitDamage">
          {{ batchActionLoading ? '提交中...' : '确认' }}
        </button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseDialog from '../../components/BaseDialog.vue'
import PermissionButton from '../../components/PermissionButton.vue'
import StatusTag from '../../components/StatusTag.vue'
import { listStocks, updateStockLimit } from '../../api/stock'
import {
  closeStockBatch,
  damageStockBatch,
  listStockBatches,
  lockStockBatch,
  unlockStockBatch
} from '../../api/stockBatch'

const InventoryBatchDialog = defineComponent({
  name: 'InventoryBatchDialog',
  props: {
    batches: { type: Array, default: () => [] },
    loading: { type: Boolean, default: false },
    actionLoading: { type: Boolean, default: false }
  },
  emits: ['lock', 'unlock', 'damage', 'close'],
  setup(props, { emit }) {
    const canLockBatch = (item) => ['AVAILABLE', 'EXPIRED'].includes(item.status)
    const canUnlockBatch = (item) => item.status === 'LOCKED'
    const canDamageBatch = (item) => ['AVAILABLE', 'EXPIRED', 'LOCKED'].includes(item.status) && Number(item.quantity) > 0
    const canCloseBatch = (item) => Number(item.quantity) === 0 && item.status !== 'CLOSED'
    const money = (value) => formatMoney(value)
    return () => h('div', { class: 'inventory-batch-table' }, [
      props.loading
        ? h('div', { class: 'modern-loading' }, '正在加载批次...')
        : props.batches.length
          ? h('table', [
              h('thead', [
                h('tr', [
                  h('th', '批次号'),
                  h('th', '状态'),
                  h('th', '剩余 / 初始'),
                  h('th', '进价'),
                  h('th', '生产日期'),
                  h('th', '到期日期'),
                  h('th', '操作')
                ])
              ]),
              h('tbody', props.batches.map((batch) =>
                h('tr', { key: batch.id }, [
                  h('td', batch.batchNo || '-'),
                  h('td', [h(StatusTag, { type: 'batch', value: batch.status })]),
                  h('td', `${formatNumber(batch.quantity)} / ${formatNumber(batch.initialQuantity)}`),
                  h('td', money(batch.purchasePrice)),
                  h('td', formatDate(batch.productionDate)),
                  h('td', formatDate(batch.expireDate)),
                  h('td', [
                    h('div', { class: 'row-actions' }, [
                      canLockBatch(batch) && h('button', {
                        type: 'button',
                        title: '冻结',
                        disabled: props.actionLoading,
                        onClick: () => emit('lock', batch)
                      }, '冻'),
                      canUnlockBatch(batch) && h('button', {
                        type: 'button',
                        title: '解冻',
                        disabled: props.actionLoading,
                        onClick: () => emit('unlock', batch)
                      }, '解'),
                      canDamageBatch(batch) && h('button', {
                        type: 'button',
                        title: '报损',
                        disabled: props.actionLoading,
                        onClick: () => emit('damage', batch)
                      }, '损'),
                      canCloseBatch(batch) && h('button', {
                        type: 'button',
                        title: '关闭',
                        disabled: props.actionLoading,
                        onClick: () => emit('close', batch)
                      }, '关')
                    ])
                  ])
                ])
              ))
            ])
          : h('div', { class: 'modern-empty' }, [
              h('h4', '暂无批次'),
              h('p', '该 SKU 当前没有可展示的库存批次')
            ])
    ])
  }
})

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const syncTime = ref('')
const message = ref('')
const messageType = ref('success')
const batchDialogVisible = ref(false)
const limitDialogVisible = ref(false)
const damageDialogVisible = ref(false)
const batchLoading = ref(false)
const batchActionLoading = ref(false)
const submitting = ref(false)
const batchStock = ref(null)
const editingStock = ref(null)
const damageBatch = ref(null)
const batches = ref([])
const query = reactive({ keyword: '', category: '', status: 'all', page: 1, pageSize: 10 })
const limitForm = reactive({ minStock: 0, maxStock: 100 })
const damageForm = reactive({ quantity: 1, reason: '', remark: '' })
let batchRequestToken = 0

const palettes = [
  ['#4d9bff', '#1e63e0'],
  ['#34d399', '#0f766e'],
  ['#fbbf24', '#d97706'],
  ['#f472b6', '#be185d'],
  ['#c084fc', '#7c3aed'],
  ['#22d3ee', '#0e7490']
]

const normalizedRows = computed(() =>
  rows.value.map((item) => {
    const quantity = Number(item.quantity || 0)
    const minStock = Number(item.minStock || 0)
    const maxStock = Number(item.maxStock || 0)
    const statusKey = resolveStatusKey(item.warningStatus, quantity, minStock, maxStock)
    return {
      ...item,
      quantity,
      minStock,
      maxStock,
      statusKey,
      statusLabel: statusText(statusKey),
      statusClass: statusClass(statusKey),
      stockClass: stockClass(statusKey),
      stockPercent: Math.min(100, Math.round((quantity / Math.max(maxStock, minStock, quantity, 1)) * 100)),
      updateTimeText: formatDateTime(item.updateTime)
    }
  })
)

const categoryOptions = computed(() => {
  const set = new Set()
  for (const row of normalizedRows.value) {
    if (row.category) set.add(row.category)
  }
  return Array.from(set).sort((a, b) => a.localeCompare(b, 'zh-CN'))
})

const statusCounts = computed(() => {
  const counts = { all: normalizedRows.value.length, normal: 0, low: 0, high: 0, out: 0 }
  for (const row of normalizedRows.value) {
    counts[row.statusKey] = (counts[row.statusKey] || 0) + 1
  }
  return counts
})

const statusTabs = computed(() => [
  { key: 'all', label: '全部', count: statusCounts.value.all },
  { key: 'normal', label: '正常', count: statusCounts.value.normal },
  { key: 'low', label: '低库存', count: statusCounts.value.low },
  { key: 'out', label: '已售罄', count: statusCounts.value.out },
  { key: 'high', label: '超上限', count: statusCounts.value.high }
])

const filteredRows = computed(() =>
  normalizedRows.value.filter((row) => {
    if (query.category && row.category !== query.category) return false
    if (query.status !== 'all' && row.statusKey !== query.status) return false
    return true
  })
)

const pageCount = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / query.pageSize)))
const pagedRows = computed(() => {
  const safePage = Math.min(query.page, pageCount.value)
  const start = (safePage - 1) * query.pageSize
  return filteredRows.value.slice(start, start + query.pageSize)
})
const pageButtons = computed(() => {
  const pages = []
  const start = Math.max(1, Math.min(query.page - 2, pageCount.value - 4))
  const end = Math.min(pageCount.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})

const kpiCards = computed(() => [
  { key: 'total', label: 'SKU库存项', value: formatNumber(total.value || rows.value.length), unit: '项', sub: '来自实时库存接口', color: '#4d9bff' },
  { key: 'quantity', label: '库存总量', value: formatNumber(sumBy(normalizedRows.value, 'quantity')), unit: '件', sub: '按基础单位汇总', color: '#34d399' },
  { key: 'warning', label: '库存预警', value: formatNumber(statusCounts.value.low + statusCounts.value.high + statusCounts.value.out), unit: '项', sub: '低库存、售罄或超上限', color: '#fbbf24' },
  { key: 'normal', label: '状态正常', value: formatNumber(statusCounts.value.normal), unit: '项', sub: '处于上下限范围内', color: '#c084fc' }
])

const currentStockLabel = computed(() => {
  if (!editingStock.value) return ''
  const { productCode, productName, skuCode, spec } = editingStock.value
  return [productCode, productName, skuCode, spec].filter(Boolean).join(' / ')
})
const currentDamageMax = computed(() => Number(damageBatch.value?.quantity) || 1)

function resolveStatusKey(warningStatus, quantity, minStock, maxStock) {
  if (quantity <= 0) return 'out'
  if (warningStatus === 'LOW' || quantity < minStock) return 'low'
  if (warningStatus === 'HIGH' || (maxStock > 0 && quantity > maxStock)) return 'high'
  return 'normal'
}

function statusText(key) {
  return { normal: '正常', low: '库存低', out: '已售罄', high: '超上限' }[key] || '正常'
}

function statusClass(key) {
  return { normal: 'ok', low: 'warn', out: 'danger', high: 'info' }[key] || 'ok'
}

function stockClass(key) {
  return key === 'out' ? 'danger' : key === 'low' || key === 'high' ? 'warn' : ''
}

function sumBy(list, key) {
  return list.reduce((sum, item) => sum + Number(item[key] || 0), 0)
}

function showMessage(text, type = 'success') {
  message.value = text
  messageType.value = type
  window.clearTimeout(showMessage.timer)
  showMessage.timer = window.setTimeout(() => {
    message.value = ''
  }, 2600)
}

function safeAction(text, type = 'success') {
  showMessage(text, type)
}

async function loadData() {
  loading.value = true
  try {
    const data = await listStocks({ keyword: query.keyword, page: 1, pageSize: 500 })
    rows.value = data.items || []
    total.value = data.total || rows.value.length
    syncTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch (error) {
    showMessage(error.message || '库存数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function reload() {
  resetLocalPage()
  await loadData()
}

function resetQuery() {
  Object.assign(query, { keyword: '', category: '', status: 'all', page: 1, pageSize: query.pageSize })
  loadData()
}

function resetLocalPage() {
  query.page = 1
}

function setStatus(status) {
  query.status = status
  resetLocalPage()
}

function changePage(page) {
  query.page = Math.min(Math.max(1, page), pageCount.value)
}

function openLimit(row) {
  editingStock.value = row
  limitForm.minStock = row.minStock
  limitForm.maxStock = row.maxStock
  limitDialogVisible.value = true
}

async function submitLimit() {
  if (!editingStock.value?.skuId) return
  submitting.value = true
  try {
    await updateStockLimit(editingStock.value.skuId, {
      minStock: Number(limitForm.minStock),
      maxStock: Number(limitForm.maxStock)
    })
    limitDialogVisible.value = false
    showMessage('库存上下限已更新')
    await loadData()
  } catch (error) {
    showMessage(error.message || '库存上下限更新失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function openBatches(row) {
  await loadBatches(row, true)
}

async function loadBatches(row = batchStock.value, openDialog = false) {
  if (!row?.skuId) return
  const requestToken = ++batchRequestToken
  batchStock.value = row
  if (openDialog) {
    batches.value = []
    batchDialogVisible.value = true
  }
  batchLoading.value = true
  try {
    const data = await listStockBatches(row.skuId)
    if (requestToken === batchRequestToken) {
      batches.value = Array.isArray(data) ? data : (data.items || [])
    }
  } catch (error) {
    if (requestToken === batchRequestToken) showMessage(error.message || '批次加载失败', 'error')
  } finally {
    if (requestToken === batchRequestToken) batchLoading.value = false
  }
}

async function refreshAfterBatchAction() {
  await Promise.all([loadBatches(), loadData()])
}

async function runBatchAction(action, successText) {
  if (!batchStock.value) return
  batchActionLoading.value = true
  try {
    await action()
    showMessage(successText)
    await refreshAfterBatchAction()
  } catch (error) {
    showMessage(error.message || '批次操作失败', 'error')
  } finally {
    batchActionLoading.value = false
  }
}

function handleLockBatch(batch) {
  runBatchAction(() => lockStockBatch(batchStock.value.skuId, batch.id), '批次已冻结')
}

function handleUnlockBatch(batch) {
  runBatchAction(() => unlockStockBatch(batchStock.value.skuId, batch.id), '批次已解冻')
}

function handleCloseBatch(batch) {
  runBatchAction(() => closeStockBatch(batchStock.value.skuId, batch.id), '批次已关闭')
}

function openDamageDialog(batch) {
  damageBatch.value = batch
  damageForm.quantity = Math.min(1, currentDamageMax.value)
  damageForm.reason = ''
  damageForm.remark = ''
  damageDialogVisible.value = true
}

async function submitDamage() {
  const quantity = Number(damageForm.quantity)
  if (!damageBatch.value || !batchStock.value) return
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > currentDamageMax.value) {
    showMessage('数量不合法', 'error')
    return
  }
  if (!damageForm.reason) {
    showMessage('原因必填', 'error')
    return
  }
  await runBatchAction(
    () => damageStockBatch(batchStock.value.skuId, damageBatch.value.id, {
      quantity,
      reason: damageForm.reason,
      remark: damageForm.remark
    }),
    '批次已报损'
  )
  if (messageType.value !== 'error') {
    damageDialogVisible.value = false
  }
}

function goPurchaseInbound() {
  router.push('/purchase-inbounds')
}

function goStockcheck() {
  router.push('/stockchecks')
}

function thumbText(name) {
  return [...(name || '库存')].slice(0, 2).join('')
}

function thumbStyle(name, key) {
  const index = Number(key || hashText(name)) % palettes.length
  const [from, to] = palettes[index]
  return { background: `linear-gradient(135deg, ${from}, ${to})` }
}

function hashText(text) {
  return [...(text || '')].reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatMoney(value, digits = 2) {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return `￥ ${number.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })}`
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '-'
}

function formatDateTime(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

onMounted(loadData)
</script>
