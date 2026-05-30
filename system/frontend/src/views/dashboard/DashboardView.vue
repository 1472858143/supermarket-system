<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">首页概览</h1>
        <p class="page-desc">库存业务核心指标和预警状态</p>
      </div>
      <button class="btn" type="button" @click="loadData">刷新</button>
    </div>

    <div v-if="message" class="message message-error">{{ message }}</div>

    <section class="grid-cards">
      <div class="card metric-card">
        <span class="metric-label">商品数量</span>
        <strong class="metric-value">{{ stock.productCount ?? 0 }}</strong>
      </div>
      <div class="card metric-card">
        <span class="metric-label">库存总量</span>
        <strong class="metric-value">{{ stock.totalQuantity ?? 0 }}</strong>
      </div>
      <div class="card metric-card">
        <span class="metric-label">入库总量</span>
        <strong class="metric-value">{{ inbound.totalQuantity ?? 0 }}</strong>
      </div>
      <div class="card metric-card">
        <span class="metric-label">出库总量</span>
        <strong class="metric-value">{{ outbound.totalQuantity ?? 0 }}</strong>
      </div>
    </section>

    <section class="card">
      <div class="page-header">
        <div>
          <h2 class="page-title">库存预警</h2>
          <p class="page-desc">库存低于下限或超过上限的商品</p>
        </div>
      </div>
      <BaseTable
        :columns="warningColumns"
        :items="warnings"
        :total="warnings.length"
        :page="1"
        :page-size="warnings.length || 10"
        :loading="loading"
        empty-text="暂无库存预警"
      >
        <template #cell-warningStatus="{ item }">
          <StatusTag type="warning" :value="item.warningStatus" />
        </template>
      </BaseTable>
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
  { key: 'category', title: '分类' },
  { key: 'quantity', title: '当前库存' },
  { key: 'minStock', title: '下限' },
  { key: 'maxStock', title: '上限' },
  { key: 'warningStatus', title: '状态' }
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
