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
          <h2 class="page-title">库存预警</h2>
          <p class="page-desc">库存低于下限或超过上限的 SKU</p>
        </div>
      </div>
      <BaseTable
        :columns="warningColumns"
        :items="warnings"
        :total="warnings.length"
        :page="1"
        :page-size="warnings.length || 10"
        :loading="loading"
        id-key="skuCode"
        empty-text="暂无库存预警"
      >
        <template #cell-warningStatus="{ item }">
          <StatusTag type="warning" :value="item.warningStatus" />
        </template>
      </BaseTable>
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
import StatusTag from '../../components/StatusTag.vue'
import { getInboundReport, getOutboundReport, getStockReport, getWarningReport } from '../../api/report'

const loading = ref(false)
const message = ref('')
const stock = ref({})
const inbound = ref({})
const outbound = ref({})
const warnings = ref([])
const warningColumns = [
  { key: 'productCode', title: '商品编号' },
  { key: 'productName', title: '商品名称' },
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'skuName', title: 'SKU名称' },
  { key: 'category', title: '分类' },
  { key: 'quantity', title: '当前库存' },
  { key: 'minStock', title: '下限' },
  { key: 'maxStock', title: '上限' },
  { key: 'warningStatus', title: '预警' }
]
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
  } catch (error) {
    message.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
