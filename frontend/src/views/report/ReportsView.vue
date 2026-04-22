<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">报表统计</h1>
        <p class="page-desc">报表模块只读，不提供任何写业务数据入口</p>
      </div>
      <button class="btn" type="button" @click="loadData">刷新</button>
    </div>

    <div v-if="message" class="message message-error">{{ message }}</div>

    <section class="grid-cards">
      <div class="card metric-card">
        <span class="metric-label">库存记录数</span>
        <strong class="metric-value">{{ stock.stockCount ?? 0 }}</strong>
      </div>
      <div class="card metric-card">
        <span class="metric-label">低库存预警</span>
        <strong class="metric-value">{{ stock.lowWarningCount ?? 0 }}</strong>
      </div>
      <div class="card metric-card">
        <span class="metric-label">入库单数</span>
        <strong class="metric-value">{{ inbound.orderCount ?? 0 }}</strong>
      </div>
      <div class="card metric-card">
        <span class="metric-label">出库单数</span>
        <strong class="metric-value">{{ outbound.orderCount ?? 0 }}</strong>
      </div>
    </section>

    <section class="card">
      <div class="page-header">
        <div>
          <h2 class="page-title">库存变化趋势</h2>
          <p class="page-desc">按日期和库存变化类型聚合展示</p>
        </div>
      </div>
      <BaseTable
        :columns="trendColumns"
        :items="stock.trend || []"
        :total="(stock.trend || []).length"
        :page="1"
        :page-size="30"
        :loading="loading"
        empty-text="暂无库存变化记录"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import BaseTable from '../../components/BaseTable.vue'
import { getInboundReport, getOutboundReport, getStockReport } from '../../api/report'

const loading = ref(false)
const message = ref('')
const stock = ref({})
const inbound = ref({})
const outbound = ref({})
const trendColumns = [
  { key: 'statDate', title: '日期' },
  { key: 'changeType', title: '类型' },
  { key: 'changeCount', title: '次数' },
  { key: 'changeQuantity', title: '变化数量' }
]

async function loadData() {
  loading.value = true
  message.value = ''
  try {
    const [stockData, inboundData, outboundData] = await Promise.all([
      getStockReport(),
      getInboundReport(),
      getOutboundReport()
    ])
    stock.value = stockData || {}
    inbound.value = inboundData || {}
    outbound.value = outboundData || {}
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
