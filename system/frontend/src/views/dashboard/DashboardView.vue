<template>
  <div class="dashboard-workbench-page">
    <div class="page-head">
      <div>
        <h1>{{ greeting }}，{{ displayName }}</h1>
        <div class="greet-sub">
          今日已同步 <b>{{ formatNumber(stockRecordCount) }}</b> 条库存记录 ·
          入库 <b>{{ formatNumber(inboundQuantity) }}</b> 件 ·
          出库 <b>{{ formatNumber(outboundQuantity) }}</b> 件 ·
          预警 <b>{{ formatNumber(warningTotal) }}</b> 项
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" :disabled="loading" @click="loadData">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 2v6h-6" />
            <path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
            <path d="M3 22v-6h6" />
            <path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
          </svg>
          {{ loading ? '同步中' : '刷新' }}
        </button>
        <button class="btn" type="button" @click="go('/reports')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="20" x2="18" y2="10" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="6" y1="20" x2="6" y2="14" />
          </svg>
          报表统计
        </button>
        <button class="btn" type="button" @click="go('/inventory-center')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 21h18" />
            <path d="M5 21V8l7-4 7 4v13" />
            <path d="M9 21v-7h6v7" />
            <path d="M8 10h8" />
          </svg>
          库存中心
        </button>
        <button class="btn primary" type="button" @click="go('/purchase-inbounds')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          采购补货
        </button>
      </div>
    </div>

    <div v-if="message" class="message message-error">{{ message }}</div>

    <section class="kpi-row dashboard-kpi-row">
      <div
        v-for="card in kpiCards"
        :key="card.key"
        class="kpi dashboard-kpi-card"
        :style="{ '--accent': card.color }"
      >
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}<span class="unit">{{ card.unit }}</span></div>
        <div>
          <span class="delta" :class="{ down: card.down }">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
              <polyline :points="card.down ? '6 9 12 15 18 9' : '6 15 12 9 18 15'" />
            </svg>
            {{ card.delta }}
          </span>
          <span class="sub">{{ card.sub }}</span>
        </div>
        <div class="icon-wrap" aria-hidden="true">
          <svg v-if="card.icon === 'product'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
            <path d="M3.27 6.96 12 12.01l8.73-5.05" />
            <path d="M12 22.08V12" />
          </svg>
          <svg v-else-if="card.icon === 'stock'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 21h18" />
            <path d="M5 21V8l7-4 7 4v13" />
            <path d="M9 21v-7h6v7" />
            <path d="M8 10h8" />
          </svg>
          <svg v-else-if="card.icon === 'warning'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <path d="M12 9v4" />
            <path d="M12 17h.01" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1" />
            <circle cx="20" cy="21" r="1" />
            <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
          </svg>
        </div>
      </div>
    </section>

    <section class="dashboard-grid">
      <article class="card dashboard-panel">
        <div class="dashboard-card-head">
          <div class="title-block">
            <h3>库存变化趋势</h3>
            <div class="sub">STOCK FLOW · 最近 30 条聚合记录</div>
          </div>
          <span class="dashboard-sync">{{ syncTime || '待同步' }}</span>
        </div>
        <div v-if="trendBars.length" class="dashboard-trend-bars">
          <div v-for="item in trendBars" :key="item.key" class="dashboard-trend-row">
            <span class="trend-date">{{ item.date }}</span>
            <div class="trend-track">
              <i :class="item.tone" :style="{ width: item.width }"></i>
            </div>
            <span class="trend-type">{{ item.type }}</span>
            <b>{{ formatSignedNumber(item.quantity) }}</b>
          </div>
        </div>
        <div v-else class="dashboard-empty">暂无库存变化记录</div>
      </article>

      <article class="card dashboard-panel">
        <div class="dashboard-card-head">
          <div class="title-block">
            <h3>业务概览</h3>
            <div class="sub">CURRENT OPERATIONS · 现有接口实时汇总</div>
          </div>
          <button class="dashboard-link-btn" type="button" @click="go('/reports')">查看报表</button>
        </div>
        <div class="dashboard-overview-list">
          <div v-for="item in overviewItems" :key="item.key" class="dashboard-overview-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.sub }}</small>
          </div>
        </div>
      </article>
    </section>

    <section class="dashboard-grid dashboard-grid-bottom">
      <article class="card dashboard-panel">
        <div class="dashboard-card-head">
          <div class="title-block">
            <h3>库存预警</h3>
            <div class="sub">LOW & HIGH STOCK · 需要优先处理</div>
          </div>
          <button class="dashboard-link-btn" type="button" @click="go('/inventory-center')">进入库存</button>
        </div>
        <div v-if="prioritizedWarnings.length" class="dashboard-warning-list">
          <button
            v-for="item in prioritizedWarnings"
            :key="item.key"
            class="dashboard-warning-item"
            type="button"
            @click="go('/inventory-center')"
          >
            <span class="warning-main">
              <b>{{ item.productName || '-' }}</b>
              <small>{{ item.skuCode || item.productCode || '-' }} · {{ item.category || '未分类' }}</small>
            </span>
            <span class="warning-stock">
              <strong>{{ formatNumber(item.quantity) }}</strong>
              <small>下限 {{ formatNumber(item.minStock) }} / 上限 {{ formatNumber(item.maxStock) }}</small>
            </span>
            <StatusTag type="warning" :value="item.warningStatus" />
          </button>
        </div>
        <div v-else class="dashboard-empty">暂无库存预警</div>
      </article>

      <article class="card dashboard-panel">
        <div class="dashboard-card-head">
          <div class="title-block">
            <h3>动态与待办</h3>
            <div class="sub">WORKBENCH · 从当前业务数据生成</div>
          </div>
          <span class="dashboard-sync">{{ activeTodoCount }} 项关注</span>
        </div>
        <div class="dashboard-todo-list">
          <button v-for="todo in todoItems" :key="todo.key" class="dashboard-todo-item" type="button" @click="go(todo.path)">
            <span class="todo-icon" :class="todo.tone">
              <svg v-if="todo.icon === 'warning'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <path d="M12 9v4" />
                <path d="M12 17h.01" />
              </svg>
              <svg v-else-if="todo.icon === 'purchase'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </span>
            <span class="todo-copy">
              <b>{{ todo.title }}</b>
              <small>{{ todo.desc }}</small>
            </span>
            <strong>{{ todo.count }}</strong>
          </button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatusTag from '../../components/StatusTag.vue'
import { getInboundReport, getOutboundReport, getStockReport, getWarningReport } from '../../api/report'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const message = ref('')
const stock = ref({})
const inbound = ref({})
const outbound = ref({})
const warnings = ref([])
const syncTime = ref('')
const now = ref(new Date())
let clockTimer = null

const displayName = computed(() => authStore.username || '管理员')
const greeting = computed(() => {
  const hour = now.value.getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})
const stockRecordCount = computed(() => toNumber(stock.value.stockCount ?? stock.value.productCount))
const productCount = computed(() => toNumber(stock.value.productCount))
const stockQuantity = computed(() => toNumber(stock.value.totalQuantity))
const inboundQuantity = computed(() => toNumber(inbound.value.totalQuantity))
const outboundQuantity = computed(() => toNumber(outbound.value.totalQuantity))
const warningTotal = computed(() => warnings.value.length || toNumber(stock.value.lowWarningCount) + toNumber(stock.value.highWarningCount))
const activeTodoCount = computed(() => todoItems.value.filter((item) => item.countNumber > 0).length)

const kpiCards = computed(() => [
  {
    key: 'product',
    label: '商品档案',
    value: formatNumber(productCount.value),
    unit: '件',
    delta: `库存项 ${formatNumber(stockRecordCount.value)}`,
    sub: '商品与 SKU 基础盘',
    color: '#4d9bff',
    icon: 'product'
  },
  {
    key: 'stock',
    label: '库存总量',
    value: formatNumber(stockQuantity.value),
    unit: '件',
    delta: '实时汇总',
    sub: '当前在库数量',
    color: '#34d399',
    icon: 'stock'
  },
  {
    key: 'warning',
    label: '库存预警',
    value: formatNumber(warningTotal.value),
    unit: '项',
    delta: warningTotal.value ? `需处理 ${formatNumber(warningTotal.value)}` : '状态稳定',
    sub: '低库存或超上限',
    color: '#fbbf24',
    icon: 'warning',
    down: warningTotal.value > 0
  },
  {
    key: 'purchase',
    label: '采购入库',
    value: formatNumber(inboundQuantity.value),
    unit: '件',
    delta: `单据 ${formatNumber(inbound.value.orderCount)}`,
    sub: '累计采购入库',
    color: '#c084fc',
    icon: 'purchase'
  }
])

const overviewItems = computed(() => [
  {
    key: 'inbound',
    label: '入库单据',
    value: `${formatNumber(inbound.value.orderCount)} 单`,
    sub: `累计入库 ${formatNumber(inboundQuantity.value)} 件`
  },
  {
    key: 'outbound',
    label: '出库单据',
    value: `${formatNumber(outbound.value.orderCount)} 单`,
    sub: `累计出库 ${formatNumber(outboundQuantity.value)} 件`
  },
  {
    key: 'low',
    label: '低库存预警',
    value: `${formatNumber(lowWarningCount.value)} 项`,
    sub: '建议优先补货'
  },
  {
    key: 'high',
    label: '超上限预警',
    value: `${formatNumber(highWarningCount.value)} 项`,
    sub: '关注滞销与占仓'
  }
])

const lowWarningCount = computed(() =>
  warnings.value.filter((item) => item.warningStatus === 'LOW').length || toNumber(stock.value.lowWarningCount)
)
const highWarningCount = computed(() =>
  warnings.value.filter((item) => item.warningStatus === 'HIGH').length || toNumber(stock.value.highWarningCount)
)

const prioritizedWarnings = computed(() =>
  warnings.value
    .map((item, index) => ({
      ...item,
      key: `${item.skuCode || item.productCode || index}-${item.warningStatus || 'NORMAL'}`
    }))
    .sort((a, b) => warningPriority(a.warningStatus) - warningPriority(b.warningStatus))
    .slice(0, 6)
)

const trendBars = computed(() => {
  const rows = Array.isArray(stock.value.trend) ? stock.value.trend.slice(0, 10).reverse() : []
  const max = Math.max(1, ...rows.map((item) => Math.abs(toNumber(item.changeQuantity))))
  return rows.map((item, index) => {
    const quantity = toNumber(item.changeQuantity)
    const type = formatChangeType(item.changeType)
    const outflow = quantity < 0 || ['OUTBOUND', 'DAMAGE'].includes(item.changeType)
    return {
      key: `${item.statDate || index}-${item.changeType || 'CHANGE'}-${index}`,
      date: formatShortDate(item.statDate),
      type,
      quantity,
      tone: outflow ? 'out' : 'in',
      width: `${Math.max(8, Math.round((Math.abs(quantity) / max) * 100))}%`
    }
  })
})

const todoItems = computed(() => [
  {
    key: 'warning',
    title: '库存预警处理',
    desc: `${formatNumber(lowWarningCount.value)} 项低库存，${formatNumber(highWarningCount.value)} 项超上限`,
    count: formatNumber(warningTotal.value),
    countNumber: warningTotal.value,
    path: '/inventory-center',
    icon: 'warning',
    tone: warningTotal.value ? 'warn' : 'ok'
  },
  {
    key: 'purchase',
    title: '采购入库跟进',
    desc: `累计 ${formatNumber(inbound.value.orderCount)} 张采购入库单`,
    count: formatNumber(inbound.value.orderCount),
    countNumber: toNumber(inbound.value.orderCount),
    path: '/purchase-inbounds',
    icon: 'purchase',
    tone: 'info'
  },
  {
    key: 'report',
    title: '经营报表复盘',
    desc: `出库 ${formatNumber(outboundQuantity.value)} 件，库存变化 ${formatNumber((stock.value.trend || []).length)} 条`,
    count: formatNumber((stock.value.trend || []).length),
    countNumber: (stock.value.trend || []).length,
    path: '/reports',
    icon: 'report',
    tone: 'ok'
  }
])

async function loadData() {
  loading.value = true
  message.value = ''
  try {
    const [stockData, inboundData, outboundData, warningData] = await Promise.all([
      getStockReport(),
      getInboundReport(),
      getOutboundReport(),
      getWarningReport()
    ])
    stock.value = stockData || {}
    inbound.value = inboundData || {}
    outbound.value = outboundData || {}
    warnings.value = warningData || []
    syncTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch (error) {
    message.value = error.message || '首页数据加载失败'
  } finally {
    loading.value = false
  }
}

function go(path) {
  router.push(path)
}

function warningPriority(status) {
  return status === 'LOW' ? 0 : status === 'HIGH' ? 1 : 2
}

function toNumber(value) {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number : 0
}

function formatNumber(value) {
  return toNumber(value).toLocaleString('zh-CN')
}

function formatSignedNumber(value) {
  const number = toNumber(value)
  return `${number > 0 ? '+' : ''}${formatNumber(number)}`
}

function formatShortDate(value) {
  return value ? String(value).slice(5, 10) : '--'
}

function formatChangeType(value) {
  const labels = {
    PURCHASE_INBOUND: '采购入库',
    OUTBOUND: '销售出库',
    CHECK: '盘点调整',
    DAMAGE: '批次报损',
    BATCH_STATUS: '批次状态'
  }
  return labels[value] || value || '库存变动'
}

function tick() {
  now.value = new Date()
}

onMounted(() => {
  tick()
  clockTimer = window.setInterval(tick, 60 * 1000)
  loadData()
})

onBeforeUnmount(() => {
  if (clockTimer) window.clearInterval(clockTimer)
})
</script>

<style scoped>
.dashboard-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dashboard-workbench-page .page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.dashboard-workbench-page .page-head h1 {
  margin: 0;
  color: var(--text);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0;
}

.dashboard-workbench-page .greet-sub {
  margin-top: 8px;
  color: var(--text-dim);
  font-size: 13px;
  line-height: 1.7;
}

.dashboard-workbench-page .greet-sub b {
  color: var(--brand);
  font-weight: 700;
}

.dashboard-workbench-page .quick-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.dashboard-workbench-page .quick-actions svg,
.dashboard-link-btn svg {
  width: 16px;
  height: 16px;
}

.dashboard-kpi-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
  gap: 18px;
}

.dashboard-grid-bottom {
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
}

.dashboard-panel {
  padding: 0;
  overflow: hidden;
}

.dashboard-card-head {
  min-height: 72px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.dashboard-card-head h3 {
  margin: 0;
  color: var(--text);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
}

.dashboard-card-head .sub {
  margin-top: 5px;
  color: var(--text-faint);
  font-size: 11px;
  letter-spacing: 1.2px;
}

.dashboard-sync {
  flex-shrink: 0;
  color: var(--text-mute);
  font-size: 12px;
  line-height: 28px;
}

.dashboard-link-btn {
  border: 0;
  background: transparent;
  color: var(--brand);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.dashboard-trend-bars {
  display: grid;
  gap: 12px;
  padding: 18px 20px 20px;
}

.dashboard-trend-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 76px 70px;
  align-items: center;
  gap: 12px;
  color: var(--text-dim);
  font-size: 12px;
}

.trend-date,
.trend-type {
  color: var(--text-mute);
}

.dashboard-trend-row b {
  color: var(--text);
  font-family: "Orbitron", "DM Mono", Consolas, monospace;
  font-size: 13px;
  text-align: right;
}

.trend-track {
  height: 9px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--bg-elev);
}

.trend-track i {
  display: block;
  height: 100%;
  min-width: 8px;
  border-radius: inherit;
}

.trend-track i.in {
  background: linear-gradient(90deg, #34d399, #0f766e);
}

.trend-track i.out {
  background: linear-gradient(90deg, #fbbf24, #d97706);
}

.dashboard-overview-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 18px 20px 20px;
}

.dashboard-overview-item {
  min-height: 92px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fafbfe;
}

.dashboard-overview-item span,
.dashboard-overview-item small {
  display: block;
  color: var(--text-mute);
  font-size: 12px;
}

.dashboard-overview-item strong {
  display: block;
  margin: 8px 0 6px;
  color: var(--text);
  font-family: "Orbitron", "DM Mono", Consolas, monospace;
  font-size: 20px;
  font-weight: 700;
}

.dashboard-warning-list,
.dashboard-todo-list {
  display: grid;
  gap: 10px;
  padding: 16px 20px 20px;
}

.dashboard-warning-item,
.dashboard-todo-item {
  width: 100%;
  min-height: 64px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #ffffff;
  color: inherit;
  cursor: pointer;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
}

.dashboard-warning-item:hover,
.dashboard-todo-item:hover {
  border-color: var(--line-strong);
  box-shadow: 0 10px 24px rgba(20, 40, 80, 0.08);
  transform: translateY(-1px);
}

.dashboard-warning-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px 72px;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  text-align: left;
}

.warning-main b,
.todo-copy b {
  display: block;
  color: var(--text);
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-main small,
.warning-stock small,
.todo-copy small {
  display: block;
  margin-top: 5px;
  color: var(--text-mute);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.warning-stock {
  text-align: right;
}

.warning-stock strong {
  color: var(--text);
  font-family: "Orbitron", "DM Mono", Consolas, monospace;
  font-size: 17px;
}

.dashboard-todo-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  text-align: left;
}

.todo-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-soft);
  color: var(--brand);
}

.todo-icon svg {
  width: 18px;
  height: 18px;
}

.todo-icon.warn {
  background: rgba(251, 191, 36, 0.14);
  color: #b45309;
}

.todo-icon.ok {
  background: rgba(52, 211, 153, 0.14);
  color: #047857;
}

.todo-icon.info {
  background: rgba(77, 155, 255, 0.14);
  color: var(--brand);
}

.dashboard-todo-item > strong {
  color: var(--text);
  font-family: "Orbitron", "DM Mono", Consolas, monospace;
  font-size: 18px;
}

.dashboard-empty {
  min-height: 150px;
  padding: 48px 20px;
  color: var(--text-mute);
  text-align: center;
}

@media (max-width: 1100px) {
  .dashboard-kpi-row,
  .dashboard-grid,
  .dashboard-grid-bottom {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .dashboard-workbench-page .page-head {
    flex-direction: column;
  }

  .dashboard-workbench-page .quick-actions {
    justify-content: flex-start;
  }

  .dashboard-kpi-row,
  .dashboard-grid,
  .dashboard-grid-bottom,
  .dashboard-overview-list {
    grid-template-columns: 1fr;
  }

  .dashboard-trend-row {
    grid-template-columns: 44px minmax(0, 1fr) 60px;
  }

  .trend-type {
    display: none;
  }

  .dashboard-warning-item {
    grid-template-columns: minmax(0, 1fr);
    align-items: flex-start;
  }

  .warning-stock {
    text-align: left;
  }
}
</style>
