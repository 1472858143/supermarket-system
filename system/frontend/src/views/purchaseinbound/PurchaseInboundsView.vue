<template>
  <div class="purchase-dock-page">
    <div class="page-head">
      <div>
        <h1>{{ pageTitle }}</h1>
        <div v-if="activeModule === 'inbound'" class="greet-sub">
          当前待入库 <b>{{ formatNumber(statusTotals.APPROVED) }}</b> 单 ·
          已完成 <b class="ok">{{ formatNumber(statusTotals.INBOUNDED) }}</b> 单 ·
          部分入库 <b class="warn">{{ formatNumber(statusTotals.PARTIALLY_INBOUNDED) }}</b> 单
        </div>
        <div v-else class="greet-sub">
          当前采购单 <b>{{ formatNumber(statusTotals.all) }}</b> 单 ·
          待审批 <b class="warn">{{ formatNumber(statusTotals.SUBMITTED) }}</b> 单 ·
          待入库 <b>{{ formatNumber(statusTotals.APPROVED) }}</b> 单
        </div>
      </div>
      <div class="quick-actions">
        <button v-if="activeModule === 'inbound'" class="btn" type="button" @click="safeAction('扫码收货设备接口暂未接入，当前请使用确认入库')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" />
            <rect x="14" y="3" width="7" height="7" />
            <rect x="3" y="14" width="7" height="7" />
            <line x1="14" y1="14" x2="14" y2="21" />
            <line x1="18" y1="14" x2="18" y2="18" />
          </svg>
          扫码收货
        </button>
        <button v-if="activeModule === 'inbound'" class="btn" type="button" @click="safeAction('今日收货报告导出接口暂未接入', 'error')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="8" y1="13" x2="16" y2="13" />
            <line x1="8" y1="17" x2="14" y2="17" />
          </svg>
          收货报告
        </button>
        <PermissionButton :roles="['ADMIN']" button-class="primary" @click="openPlanDialog()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新建采购单
        </PermissionButton>
      </div>
    </div>

    <nav class="sub-tabs">
      <a :class="{ on: activeModule === 'orders' }" href="#" @click.prevent="setActiveModule('orders')">采购订单<span class="ct">{{ formatNumber(statusTotals.all) }}</span></a>
      <a :class="{ on: activeModule === 'inbound' }" href="#" @click.prevent="setActiveModule('inbound')">采购入库<span class="ct">{{ formatNumber(inboundTotalCount) }}</span></a>
      <a href="#" @click.prevent="safeAction('采购绩效页面尚未接入前端路由', 'error')">采购绩效</a>
    </nav>

    <section class="kpi-row purchase-kpi-row">
      <button
        v-for="card in kpiCards"
        :key="card.key"
        class="kpi purchase-kpi-card"
        type="button"
        :style="{ '--accent': card.color }"
        @click="card.status ? setStatus(card.status) : setStatus('all')"
      >
        <div class="label">{{ card.label }}</div>
        <div class="value">{{ card.value }}<span class="unit">{{ card.unit }}</span></div>
        <div>
          <span class="delta" :class="{ down: card.down }">
            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <polyline :points="card.down ? '6 9 12 15 18 9' : '6 15 12 9 18 15'" />
            </svg>
            {{ card.delta }}
          </span>
          <span class="sub">{{ card.sub }}</span>
        </div>
        <div class="icon-wrap" aria-hidden="true">
          <svg v-if="card.icon === 'pending'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <svg v-else-if="card.icon === 'done'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 6L9 17l-5-5" />
          </svg>
          <svg v-else-if="card.icon === 'partial'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 7h11v10H3z" />
            <path d="M14 10h4l3 3v4h-7z" />
            <circle cx="7" cy="18" r="1.5" />
            <circle cx="18" cy="18" r="1.5" />
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="7" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        </div>
      </button>
    </section>

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">
      {{ message }}
    </div>

    <section class="purchase-filter-bar">
      <label class="purchase-filter-search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input v-model.trim="query.keyword" :placeholder="searchPlaceholder" @keyup.enter="reload" />
      </label>
      <button class="btn sm" type="button" @click="resetQuery">重置</button>
      <button class="btn primary sm" type="button" :disabled="loading" @click="reload">
        {{ loading ? '查询中...' : '查询' }}
      </button>
      <div v-if="activeModule === 'inbound'" class="view-tools">
        <span>视图</span>
        <div class="view-switch" aria-label="视图切换">
          <button type="button" :class="{ on: viewMode === 'board' }" title="看板" @click="viewMode = 'board'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7" />
              <rect x="14" y="3" width="7" height="7" />
              <rect x="3" y="14" width="7" height="7" />
              <rect x="14" y="14" width="7" height="7" />
            </svg>
          </button>
          <button type="button" :class="{ on: viewMode === 'table' }" title="表格" @click="viewMode = 'table'">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="8" y1="6" x2="21" y2="6" />
              <line x1="8" y1="12" x2="21" y2="12" />
              <line x1="8" y1="18" x2="21" y2="18" />
              <line x1="3" y1="6" x2="3.01" y2="6" />
              <line x1="3" y1="12" x2="3.01" y2="12" />
              <line x1="3" y1="18" x2="3.01" y2="18" />
            </svg>
          </button>
        </div>
      </div>
    </section>

    <article class="card purchase-work-card">
      <div class="purchase-status-tabs">
        <button
          v-for="tab in statusTabs"
          :key="tab.key"
          type="button"
          :class="{ on: activeStatus === tab.key }"
          @click="setStatus(tab.key)"
        >
          {{ tab.label }}<span class="ct">{{ formatNumber(tab.count) }}</span>
        </button>
      </div>

      <div v-if="activeModule === 'orders'" class="purchase-table purchase-order-table">
        <table>
          <thead>
            <tr>
              <th>采购单号</th>
              <th>供应商</th>
              <th>计划 / 实收</th>
              <th>采购金额</th>
              <th>创建人</th>
              <th>最近节点</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="items.length">
            <tr v-for="item in items" :key="item.id">
              <td class="mono strong">{{ item.orderNo }}</td>
              <td>
                <b>{{ item.supplierName || '-' }}</b>
                <div class="muted">{{ item.supplierCode || '-' }}</div>
              </td>
              <td>
                <b>{{ formatNumber(item.plannedTotalQuantity) }}</b>
                <span class="muted"> / 已收 {{ formatNumber(item.inboundTotalQuantity) }}</span>
                <div class="mini-progress"><i :style="{ width: `${receivePercent(item)}%` }"></i></div>
              </td>
              <td>
                <b>{{ formatMoney(item.plannedTotalAmount) }}</b>
                <div class="muted">实收 {{ formatMoney(item.inboundTotalAmount, 6) }}</div>
              </td>
              <td>{{ item.creatorUsername || item.operator || '-' }}</td>
              <td class="mono">{{ formatDateTime(nodeTime(item)) }}</td>
              <td><span class="pill" :class="statusMeta(item.status).className"><span class="dot"></span>{{ statusMeta(item.status).label }}</span></td>
              <td>
                <div class="row-actions">
                  <button type="button" title="详情" @click="openDetail(item)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button v-if="canEditPlan(item)" type="button" title="编辑" @click="openPlanDialog(item)">改</button>
                  <button v-if="canSubmit(item)" type="button" title="提交" :disabled="actioningId === item.id" @click="submitOrder(item)">提</button>
                  <button v-if="canApprove(item)" type="button" title="审批" :disabled="actioningId === item.id" @click="approveOrder(item)">审</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="modern-loading">正在加载采购订单...</div>
        <div v-else-if="!items.length" class="modern-empty">
          <h4>暂无采购订单</h4>
          <p>可新建采购单，或切换状态查看历史单据</p>
        </div>
      </div>

      <div v-else-if="viewMode === 'board'" class="dock-board">
        <article
          v-for="item in items"
          :key="item.id"
          class="dock-card"
          :style="{ '--accent': statusMeta(item.status).color }"
        >
          <div class="dock-head">
            <div>
              <div class="dock-id">{{ item.orderNo }}</div>
              <div class="dock-supplier">{{ item.supplierName || '-' }}</div>
              <div class="dock-sub">{{ item.supplierCode || '供应商' }} · {{ formatDateTime(item.createTime) }}</div>
            </div>
            <span class="pill" :class="statusMeta(item.status).className"><span class="dot"></span>{{ statusMeta(item.status).label }}</span>
          </div>
          <div class="dock-meta">
            <div class="row"><span>计划数量</span><b>{{ formatNumber(item.plannedTotalQuantity) }}</b></div>
            <div class="row"><span>已收数量</span><b>{{ formatNumber(item.inboundTotalQuantity) }}</b></div>
            <div class="row"><span>采购金额</span><b>{{ formatMoney(item.plannedTotalAmount) }}</b></div>
            <div class="row"><span>经办人</span><b>{{ item.operator || item.creatorUsername || '-' }}</b></div>
          </div>
          <div class="progress"><i :class="{ full: receivePercent(item) >= 100 }" :style="{ width: `${receivePercent(item)}%` }"></i></div>
          <div class="progress-label">
            <span>实收 <b>{{ formatNumber(item.inboundTotalQuantity) }}</b> / 计划 {{ formatNumber(item.plannedTotalQuantity) }}</span>
            <span>{{ receivePercent(item) }}%</span>
          </div>
          <div class="dock-actions">
            <button type="button" @click="openDetail(item)">查看</button>
            <button v-if="canEditPlan(item)" type="button" @click="openPlanDialog(item)">编辑</button>
            <button v-if="canSubmit(item)" type="button" :disabled="actioningId === item.id" @click="submitOrder(item)">提交</button>
            <button v-if="canApprove(item)" type="button" :disabled="actioningId === item.id" @click="approveOrder(item)">审批</button>
            <button v-if="canReturn(item)" type="button" :disabled="actioningId === item.id" @click="returnOrder(item)">退回</button>
            <button v-if="canCancel(item)" type="button" :disabled="actioningId === item.id" @click="cancelOrder(item)">取消</button>
            <button v-if="canReceive(item)" class="primary" type="button" :disabled="actioningId === item.id" @click="openReceiptDialog(item)">确认入库</button>
            <button v-if="canClose(item)" type="button" :disabled="actioningId === item.id" @click="closeOrder(item)">关闭</button>
          </div>
        </article>
        <div v-if="loading" class="modern-loading">正在加载采购入库单...</div>
        <div v-else-if="!items.length" class="modern-empty">
          <h4>暂无采购入库单</h4>
          <p>可新建采购单，或切换状态查看历史收货记录</p>
        </div>
      </div>

      <div v-else class="purchase-table">
        <table>
          <thead>
            <tr>
              <th>采购单号</th>
              <th>供应商</th>
              <th>计划 / 实收</th>
              <th>金额</th>
              <th>最近节点</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody v-if="items.length">
            <tr v-for="item in items" :key="item.id">
              <td class="mono strong">{{ item.orderNo }}</td>
              <td>
                <b>{{ item.supplierName || '-' }}</b>
                <div class="muted">{{ item.supplierCode || '-' }}</div>
              </td>
              <td>
                <b>{{ formatNumber(item.inboundTotalQuantity) }}</b>
                <span class="muted"> / {{ formatNumber(item.plannedTotalQuantity) }}</span>
                <div class="mini-progress"><i :style="{ width: `${receivePercent(item)}%` }"></i></div>
              </td>
              <td>
                <b>{{ formatMoney(item.inboundTotalAmount, 6) }}</b>
                <div class="muted">计划 {{ formatMoney(item.plannedTotalAmount) }}</div>
              </td>
              <td class="mono">{{ formatDateTime(nodeTime(item)) }}</td>
              <td><span class="pill" :class="statusMeta(item.status).className"><span class="dot"></span>{{ statusMeta(item.status).label }}</span></td>
              <td>
                <div class="row-actions">
                  <button type="button" title="详情" @click="openDetail(item)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button v-if="canEditPlan(item)" type="button" title="编辑" @click="openPlanDialog(item)">改</button>
                  <button v-if="canSubmit(item)" type="button" title="提交" :disabled="actioningId === item.id" @click="submitOrder(item)">提</button>
                  <button v-if="canApprove(item)" type="button" title="审批" :disabled="actioningId === item.id" @click="approveOrder(item)">审</button>
                  <button v-if="canReceive(item)" class="primary-icon" type="button" title="确认入库" :disabled="actioningId === item.id" @click="openReceiptDialog(item)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="loading" class="modern-loading">正在加载采购入库单...</div>
        <div v-else-if="!items.length" class="modern-empty">
          <h4>暂无采购入库单</h4>
          <p>可新建采购单，或切换状态查看历史收货记录</p>
        </div>
      </div>

      <footer class="page-foot">
        <span>共 {{ formatNumber(total) }} 条</span>
        <button class="btn sm" type="button" :disabled="query.page <= 1 || loading" @click="changePage(query.page - 1)">上一页</button>
        <button
          v-for="page in pageButtons"
          :key="page"
          class="page-btn"
          type="button"
          :class="{ on: query.page === page }"
          @click="changePage(page)"
        >
          {{ page }}
        </button>
        <button class="btn sm" type="button" :disabled="query.page >= pageCount || loading" @click="changePage(query.page + 1)">下一页</button>
      </footer>
    </article>

    <BaseDialog v-model="planDialogVisible" :title="planDialogTitle">
      <label class="form-item full">
        <span class="form-label">供应商</span>
        <select v-model.number="form.supplierId" class="select">
          <option :value="null">请选择供应商</option>
          <option v-for="supplier in suppliers" :key="supplier.id" :value="supplier.id">
            {{ supplier.supplierCode }} / {{ supplier.supplierName }}
          </option>
        </select>
      </label>

      <div class="detail-lines">
        <div v-for="(line, index) in form.items" :key="line.key" class="line-panel">
          <div class="line-header">
            <strong>计划明细 {{ index + 1 }}</strong>
            <button v-if="form.items.length > 1" class="btn btn-ghost btn-small" type="button" @click="removeLine(index)">删除</button>
          </div>
          <div class="form-grid">
            <label class="form-item">
              <span class="form-label">供应商SKU</span>
              <select
                v-model.number="line.bindingId"
                class="select"
                :disabled="!form.supplierId || supplierSkuLoading"
                @change="handleSupplierSkuSelected(line)"
              >
                <option :value="null">{{ supplierSkuLoading ? '加载中...' : '请选择SKU' }}</option>
                <option v-for="binding in supplierSkus" :key="binding.id" :value="binding.id">
                  {{ binding.supplierSkuCode || binding.skuCode }} / {{ binding.supplierSkuName || binding.skuName }}
                </option>
              </select>
            </label>
            <UnitSelector v-model="line.unit" :base-unit="line.selectedSku?.baseUnit || ''" :units="line.units" @rate-changed="(rate) => setConversionRate(line, rate)" />
            <label class="form-item">
              <span class="form-label">计划数量</span>
              <input v-model.number="line.quantity" class="input" type="number" min="1" />
            </label>
            <label class="form-item">
              <span class="form-label">审批采购价</span>
              <input v-model.number="line.purchasePrice" class="input" type="number" min="0" step="0.01" />
            </label>
            <div v-if="linePreviews[line.key]" class="form-hint full">{{ linePreviews[line.key] }}</div>
          </div>
        </div>
        <button class="btn btn-ghost add-line-button" type="button" @click="addLine">+ 添加明细</button>
      </div>

      <div class="summary-bar">
        <span>计划基础数量：{{ formatNumber(totalBaseQuantity) }}</span>
        <span>计划金额：{{ formatMoney(totalAmount) }}</span>
      </div>
      <label class="form-item full">
        <span class="form-label">备注</span>
        <input v-model.trim="form.remark" class="input" maxlength="200" placeholder="可选" />
      </label>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="planDialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="savePlan">{{ submitting ? '保存中...' : '保存草稿' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="receiptVisible" title="登记实际入库">
      <div v-if="receiptOrder" class="detail-block">
        <div class="detail-grid">
          <span>单号</span><strong>{{ receiptOrder.orderNo }}</strong>
          <span>状态</span><strong><StatusTag type="purchase" :value="receiptOrder.status" /></strong>
          <span>计划基础数量</span><strong>{{ receiptOrder.plannedTotalQuantity ?? '-' }}</strong>
          <span>已入库基础数量</span><strong>{{ receiptOrder.inboundTotalQuantity ?? 0 }}</strong>
        </div>

        <div class="detail-lines">
          <div v-for="receiptItem in receiptForm.items" :key="receiptItem.key" class="line-panel">
            <div class="line-header">
              <div>
                <strong>{{ receiptItem.item.supplierSkuCodeSnapshot || receiptItem.item.skuCode }}</strong>
                <div class="muted">{{ receiptItem.item.supplierSkuNameSnapshot || receiptItem.item.productName }}</div>
              </div>
              <span class="muted">剩余 {{ formatNumber(remainingBaseQuantity(receiptItem.item)) }} 基础单位</span>
            </div>
            <div v-for="(batch, batchIndex) in receiptItem.batches" :key="batch.key" class="form-grid receipt-batch-grid">
              <label class="form-item">
                <span class="form-label">实收数量</span>
                <input v-model.number="batch.quantity" class="input" type="number" min="1" />
              </label>
              <label class="form-item">
                <span class="form-label">生产日期</span>
                <input v-model="batch.productionDate" class="input" type="date" />
              </label>
              <label class="form-item">
                <span class="form-label">保质期天数</span>
                <input v-model.number="batch.shelfLifeDays" class="input" type="number" min="1" step="1" />
              </label>
              <div class="form-item batch-actions">
                <span class="form-label">批次</span>
                <button v-if="receiptItem.batches.length > 1" class="btn btn-ghost btn-small" type="button" @click="removeReceiptBatch(receiptItem, batchIndex)">删除</button>
              </div>
              <div class="form-hint full">
                {{ batch.quantity || 0 }} {{ receiptItem.item.unit }} = {{ formatNumber(receiptBatchBaseQuantity(receiptItem.item, batch)) }} 基础单位
              </div>
            </div>
            <button class="btn btn-ghost btn-small" type="button" @click="addReceiptBatch(receiptItem)">+ 添加批次</button>
          </div>
        </div>

        <div class="summary-bar">
          <span>本次实收基础数量：{{ formatNumber(totalReceiptBaseQuantity) }}</span>
          <span>本次实收金额：{{ formatMoney(totalReceiptAmount, 6) }}</span>
        </div>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <input v-model.trim="receiptForm.remark" class="input" maxlength="200" placeholder="可选" />
        </label>
      </div>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="receiptVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submitReceipt">{{ submitting ? '保存中...' : '确认入库' }}</button>
      </template>
    </BaseDialog>

    <BaseDialog v-model="detailVisible" title="采购入库详情">
      <div v-if="detail" class="detail-block">
        <div class="detail-grid">
          <span>单号</span><strong>{{ detail.orderNo }}</strong>
          <span>供应商</span><strong>{{ detail.supplierCode }} / {{ detail.supplierName }}</strong>
          <span>状态</span><strong><StatusTag type="purchase" :value="detail.status" /></strong>
          <span>计划基础数量</span><strong>{{ detail.plannedTotalQuantity ?? '-' }}</strong>
          <span>已入库基础数量</span><strong>{{ detail.inboundTotalQuantity ?? 0 }}</strong>
          <span>计划金额</span><strong>{{ formatMoney(detail.plannedTotalAmount) }}</strong>
          <span>入库金额</span><strong>{{ formatMoney(detail.inboundTotalAmount, 6) }}</strong>
          <span>创建人</span><strong>{{ detail.creatorUsername || detail.operator || '-' }}</strong>
          <span>创建时间</span><strong>{{ formatDateTime(detail.createTime) }}</strong>
          <span>备注</span><strong>{{ detail.remark || '-' }}</strong>
        </div>

        <h2 class="subsection-title">计划明细</h2>
        <div class="compact-table">
          <BaseTable :columns="detailColumns" :items="detail.items || []" :total="detail.items?.length || 0" :page="1" :page-size="detail.items?.length || 1" :show-actions="false" empty-text="暂无明细">
            <template #cell-purchasePrice="{ item }">{{ formatMoney(item.purchasePrice) }}</template>
            <template #cell-costPrice="{ item }">{{ formatMoney(item.costPrice, 8) }}</template>
            <template #cell-plannedAmount="{ item }">{{ formatMoney(item.plannedAmount) }}</template>
          </BaseTable>
        </div>

        <h2 class="subsection-title">审批日志</h2>
        <div class="compact-table">
          <BaseTable :columns="approvalLogColumns" :items="detail.approvalLogs || []" :total="detail.approvalLogs?.length || 0" :page="1" :page-size="detail.approvalLogs?.length || 1" :show-actions="false" empty-text="暂无审批日志">
            <template #cell-fromStatus="{ item }"><StatusTag type="purchase" :value="item.fromStatus" /></template>
            <template #cell-toStatus="{ item }"><StatusTag type="purchase" :value="item.toStatus" /></template>
          </BaseTable>
        </div>

        <h2 class="subsection-title">实际入库</h2>
        <div v-if="detail.receipts?.length" class="detail-lines">
          <div v-for="receipt in detail.receipts" :key="receipt.id" class="line-panel">
            <div class="line-header">
              <strong>{{ receipt.receiptNo }}</strong>
              <span class="muted">{{ receipt.operatorUsername }} / {{ formatDateTime(receipt.createTime) }}</span>
            </div>
            <div class="meta-row">
              <span>基础数量 {{ formatNumber(receipt.totalBaseQuantity) }}</span>
              <span>金额 {{ formatMoney(receipt.totalAmount, 6) }}</span>
              <span>{{ receipt.remark || '-' }}</span>
            </div>
            <BaseTable :columns="receiptBatchColumns" :items="receipt.batches || []" :total="receipt.batches?.length || 0" :page="1" :page-size="receipt.batches?.length || 1" :show-actions="false" empty-text="暂无入库批次">
              <template #cell-amount="{ item }">{{ formatMoney(item.amount, 6) }}</template>
              <template #cell-costPriceSnapshot="{ item }">{{ formatMoney(item.costPriceSnapshot, 8) }}</template>
            </BaseTable>
          </div>
        </div>
        <div v-else class="empty-note">暂无实际入库记录</div>
      </div>
      <template #footer>
        <button class="btn btn-primary" type="button" @click="detailVisible = false">关闭</button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BaseDialog from '../../components/BaseDialog.vue'
import BaseTable from '../../components/BaseTable.vue'
import PermissionButton from '../../components/PermissionButton.vue'
import StatusTag from '../../components/StatusTag.vue'
import UnitSelector from '../../components/UnitSelector.vue'
import { listEnabledSupplierSkus, listSuppliers } from '../../api/supplier'
import {
  approvePurchaseInbound,
  cancelPurchaseInbound,
  closePurchaseInbound,
  createPurchaseInboundDraft,
  getPurchaseInbound,
  listPurchaseInbounds,
  receivePurchaseInbound,
  returnPurchaseInbound,
  submitPurchaseInbound,
  updatePurchaseInboundPlan
} from '../../api/purchaseInbound'

const route = useRoute()

const inboundStatusFilters = {
  all: '',
  pending: 'APPROVED',
  partial: 'PARTIALLY_INBOUNDED',
  done: 'INBOUNDED'
}

const orderStatusFilters = {
  all: '',
  draft: 'DRAFT',
  submitted: 'SUBMITTED',
  returned: 'RETURNED',
  approved: 'APPROVED',
  partial: 'PARTIALLY_INBOUNDED',
  done: 'INBOUNDED',
  cancelled: 'CANCELLED',
  closed: 'CLOSED'
}

const statusViewMap = {
  DRAFT: { label: '草稿', className: 'muted', color: '#94a3b8' },
  SUBMITTED: { label: '待审批', className: 'warn', color: '#fbbf24' },
  RETURNED: { label: '退回修改', className: 'danger', color: '#ff6b6b' },
  APPROVED: { label: '待入库', className: 'warn', color: '#fbbf24' },
  PARTIALLY_INBOUNDED: { label: '部分入库', className: 'info', color: '#4d9bff' },
  INBOUNDED: { label: '已完成', className: 'ok', color: '#34d399' },
  CANCELLED: { label: '已取消', className: 'muted', color: '#94a3b8' },
  CLOSED: { label: '已关闭', className: 'muted', color: '#64748b' }
}

const detailColumns = [
  { key: 'supplierSkuCodeSnapshot', title: '供应商SKU' },
  { key: 'supplierSkuNameSnapshot', title: '供应商品名' },
  { key: 'skuCode', title: 'SKU编码' },
  { key: 'productName', title: '商品名称' },
  { key: 'plannedQuantity', title: '计划数量' },
  { key: 'unit', title: '单位' },
  { key: 'conversionRate', title: '换算率' },
  { key: 'plannedBaseQuantity', title: '计划基础数量' },
  { key: 'inboundedBaseQuantity', title: '已入库基础数量' },
  { key: 'purchasePrice', title: '审批采购价' },
  { key: 'costPrice', title: '基础成本价' },
  { key: 'plannedAmount', title: '计划小计' }
]
const approvalLogColumns = [
  { key: 'action', title: '动作' },
  { key: 'fromStatus', title: '原状态' },
  { key: 'toStatus', title: '新状态' },
  { key: 'operatorUsername', title: '操作人' },
  { key: 'reason', title: '原因' },
  { key: 'createTime', title: '时间' }
]
const receiptBatchColumns = [
  { key: 'supplierSkuCodeSnapshot', title: '供应商SKU' },
  { key: 'supplierSkuNameSnapshot', title: '供应商品名' },
  { key: 'quantity', title: '实收数量' },
  { key: 'baseQuantity', title: '基础数量' },
  { key: 'productionDate', title: '生产日期' },
  { key: 'shelfLifeDays', title: '保质期' },
  { key: 'expireDate', title: '到期日期' },
  { key: 'costPriceSnapshot', title: '基础成本价' },
  { key: 'amount', title: '金额' }
]

const query = reactive({
  keyword: route.query.keyword ? String(route.query.keyword) : '',
  module: route.query.module ? String(route.query.module) : '',
  page: 1,
  pageSize: 12
})
const statusTotals = reactive({
  all: 0,
  DRAFT: 0,
  SUBMITTED: 0,
  RETURNED: 0,
  APPROVED: 0,
  PARTIALLY_INBOUNDED: 0,
  INBOUNDED: 0,
  CANCELLED: 0,
  CLOSED: 0
})
const items = ref([])
const suppliers = ref([])
const supplierSkus = ref([])
const supplierSkuLoading = ref(false)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const actioningId = ref(null)
const planDialogVisible = ref(false)
const planMode = ref('create')
const editingId = ref(null)
const receiptVisible = ref(false)
const receiptOrder = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const message = ref('')
const messageType = ref('success')
const suppressSupplierWatch = ref(false)
const activeModule = ref(query.module === 'orders' ? 'orders' : 'inbound')
const activeStatus = ref('all')
const viewMode = ref('board')
const form = reactive({ supplierId: null, items: [], remark: '' })
const receiptForm = reactive({ items: [], remark: '' })
let countRequestToken = 0

const pageTitle = computed(() => (activeModule.value === 'orders' ? '采购订单 · 计划中心' : '采购入库 · 收货中心'))
const searchPlaceholder = computed(() => (
  activeModule.value === 'orders'
    ? '按采购单号 / 供应商 / 商品 / SKU 搜索...'
    : '按采购单号 / 供应商 / 商品 / SKU 搜索...'
))
const inboundTotalCount = computed(() => (
  statusTotals.APPROVED + statusTotals.PARTIALLY_INBOUNDED + statusTotals.INBOUNDED
))
const planDialogTitle = computed(() => (planMode.value === 'edit' ? '编辑采购计划' : '新增采购计划'))
const totalBaseQuantity = computed(() => form.items.reduce((sum, line) => sum + baseQuantity(line), 0))
const totalAmount = computed(() => form.items.reduce((sum, line) => sum + lineAmount(line), 0))
const totalReceiptBaseQuantity = computed(() => receiptForm.items.reduce((sum, receiptItem) => (
  sum + receiptItem.batches.reduce((batchSum, batch) => batchSum + receiptBatchBaseQuantity(receiptItem.item, batch), 0)
), 0))
const totalReceiptAmount = computed(() => receiptForm.items.reduce((sum, receiptItem) => {
  const costPrice = Number(receiptItem.item.costPrice || 0)
  return sum + receiptItem.batches.reduce((batchSum, batch) => (
    batchSum + receiptBatchBaseQuantity(receiptItem.item, batch) * costPrice
  ), 0)
}, 0))
const linePreviews = computed(() => {
  const previews = {}
  form.items.forEach((line) => {
    previews[line.key] = buildLinePreview(line)
  })
  return previews
})
const inboundStatusTabs = computed(() => [
  { key: 'all', label: '全部', count: statusTotals.all },
  { key: 'pending', label: '待入库', count: statusTotals.APPROVED },
  { key: 'partial', label: '部分入库', count: statusTotals.PARTIALLY_INBOUNDED },
  { key: 'done', label: '已完成', count: statusTotals.INBOUNDED }
])
const orderStatusTabs = computed(() => [
  { key: 'all', label: '全部', count: statusTotals.all },
  { key: 'draft', label: '草稿', count: statusTotals.DRAFT },
  { key: 'submitted', label: '待审批', count: statusTotals.SUBMITTED },
  { key: 'returned', label: '退回修改', count: statusTotals.RETURNED },
  { key: 'approved', label: '待入库', count: statusTotals.APPROVED },
  { key: 'partial', label: '部分入库', count: statusTotals.PARTIALLY_INBOUNDED },
  { key: 'done', label: '已完成', count: statusTotals.INBOUNDED },
  { key: 'cancelled', label: '已取消', count: statusTotals.CANCELLED },
  { key: 'closed', label: '已关闭', count: statusTotals.CLOSED }
])
const statusTabs = computed(() => (
  activeModule.value === 'orders' ? orderStatusTabs.value : inboundStatusTabs.value
))
const kpiCards = computed(() => [
  ...(activeModule.value === 'orders'
    ? [
      {
        key: 'all',
        label: '采购单总数',
        value: formatNumber(statusTotals.all),
        unit: '单',
        delta: `待审批 ${formatNumber(statusTotals.SUBMITTED)}`,
        sub: '现有采购工作流单据',
        color: '#4d9bff',
        status: 'all',
        icon: 'pending'
      },
      {
        key: 'submitted',
        label: '待审批',
        value: formatNumber(statusTotals.SUBMITTED),
        unit: '单',
        delta: `草稿 ${formatNumber(statusTotals.DRAFT)}`,
        sub: '已提交待审批',
        color: '#fbbf24',
        status: 'submitted',
        icon: 'pending'
      },
      {
        key: 'approved',
        label: '待入库',
        value: formatNumber(statusTotals.APPROVED),
        unit: '单',
        delta: `部分入库 ${formatNumber(statusTotals.PARTIALLY_INBOUNDED)}`,
        sub: '已审批待收货',
        color: '#4d9bff',
        status: 'approved',
        icon: 'partial'
      },
      {
        key: 'done',
        label: '已完成',
        value: formatNumber(statusTotals.INBOUNDED),
        unit: '单',
        delta: `关闭 ${formatNumber(statusTotals.CLOSED)}`,
        sub: '已完成采购入库',
        color: '#34d399',
        status: 'done',
        icon: 'done'
      }
    ]
    : [
      {
        key: 'pending',
        label: '待入库',
        value: formatNumber(statusTotals.APPROVED),
        unit: '单',
        delta: `待收 ${formatNumber(statusTotals.APPROVED)}`,
        sub: '已审批待收货',
        color: '#fbbf24',
        status: 'pending',
        icon: 'pending'
      },
      {
        key: 'done',
        label: '已完成',
        value: formatNumber(statusTotals.INBOUNDED),
        unit: '单',
        delta: `完成 ${formatNumber(statusTotals.INBOUNDED)}`,
        sub: '已完成采购入库',
        color: '#34d399',
        status: 'done',
        icon: 'done'
      },
      {
        key: 'partial',
        label: '部分入库',
        value: formatNumber(statusTotals.PARTIALLY_INBOUNDED),
        unit: '单',
        delta: `尾货 ${formatNumber(statusTotals.PARTIALLY_INBOUNDED)}`,
        sub: '仍有尾货待收',
        color: '#4d9bff',
        status: 'partial',
        icon: 'partial'
      },
      {
        key: 'exception',
        label: '异常 / 关闭',
        value: formatNumber(statusTotals.CANCELLED + statusTotals.CLOSED),
        unit: '单',
        delta: `复盘 ${formatNumber(statusTotals.CANCELLED + statusTotals.CLOSED)}`,
        sub: '取消或人工关闭',
        color: '#ff6b6b',
        down: true,
        icon: 'exception'
      }
    ])
])
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))
const pageButtons = computed(() => {
  const pages = []
  const start = Math.max(1, Math.min(query.page - 2, pageCount.value - 4))
  const end = Math.min(pageCount.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})

function canEditPlan(row) {
  return ['DRAFT', 'RETURNED'].includes(row.status)
}

function canSubmit(row) {
  return ['DRAFT', 'RETURNED'].includes(row.status)
}

function canApprove(row) {
  return row.status === 'SUBMITTED'
}

function canReturn(row) {
  return row.status === 'SUBMITTED'
}

function canCancel(row) {
  return ['DRAFT', 'SUBMITTED', 'RETURNED', 'APPROVED'].includes(row.status) && Number(row.inboundTotalQuantity || 0) === 0
}

function canReceive(row) {
  return ['APPROVED', 'PARTIALLY_INBOUNDED'].includes(row.status)
}

function canClose(row) {
  return row.status === 'PARTIALLY_INBOUNDED'
}

function createLine(item = null) {
  const binding = item ? findBinding(item.supplierSkuId, item.skuId) : null
  return {
    key: Date.now() + Math.random(),
    bindingId: item?.supplierSkuId || null,
    skuId: item?.skuId || null,
    selectedSku: binding,
    units: binding?.units || [],
    minPurchaseQuantity: binding?.minPurchaseQuantity || 1,
    quantity: item?.plannedQuantity || 1,
    unit: item?.unit || binding?.baseUnit || '',
    conversionRate: item?.conversionRate || 1,
    purchasePrice: Number(item?.purchasePrice || binding?.defaultPurchasePrice || 0)
  }
}

function createReceiptBatch() {
  return {
    key: Date.now() + Math.random(),
    quantity: 1,
    productionDate: '',
    shelfLifeDays: 180
  }
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

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatMoney(value, digits = 2) {
  if (value === null || value === undefined || value === '') return '-'
  const number = Number(value)
  if (!Number.isFinite(number)) return '-'
  return `￥${number.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })}`
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function baseQuantity(line) {
  const quantity = Number(line.quantity || 0)
  const rate = Number(line.conversionRate || 1)
  return quantity > 0 ? quantity * rate : 0
}

function lineAmount(line) {
  const quantity = Number(line.quantity || 0)
  const price = Number(line.purchasePrice || 0)
  return quantity > 0 && price >= 0 ? quantity * price : 0
}

function remainingBaseQuantity(item) {
  return Math.max(0, Number(item.plannedBaseQuantity || 0) - Number(item.inboundedBaseQuantity || 0))
}

function receiptBatchBaseQuantity(item, batch) {
  return Number(batch.quantity || 0) * Number(item.conversionRate || 1)
}

function buildLinePreview(line) {
  if (!line.selectedSku || !line.unit || !line.quantity || line.quantity <= 0) {
    return ''
  }
  const baseUnit = line.selectedSku.baseUnit || '基础单位'
  return `${line.quantity} ${line.unit} = ${formatNumber(baseQuantity(line))} ${baseUnit}，计划小计 ${formatMoney(lineAmount(line))}`
}

function findBinding(bindingId, skuId) {
  return supplierSkus.value.find((item) => item.id === bindingId || item.skuId === skuId) || null
}

function statusMeta(status) {
  return statusViewMap[status] || { label: status || '-', className: 'muted', color: '#94a3b8' }
}

function receivePercent(item) {
  const planned = Number(item.plannedTotalQuantity || 0)
  if (planned <= 0) return 0
  return Math.min(100, Math.round((Number(item.inboundTotalQuantity || 0) / planned) * 100))
}

function nodeTime(item) {
  return item.closeTime || item.cancelTime || item.approveTime || item.submitTime || item.createTime
}

async function loadData() {
  loading.value = true
  try {
    const filters = activeModule.value === 'orders' ? orderStatusFilters : inboundStatusFilters
    const data = await listPurchaseInbounds({
      keyword: query.keyword,
      status: filters[activeStatus.value] || undefined,
      page: query.page,
      pageSize: query.pageSize
    })
    items.value = data.items || []
    total.value = data.total || 0
    loadStatusCounts()
  } catch (error) {
    showMessage(error.message || '采购入库数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function loadStatusCounts() {
  const token = ++countRequestToken
  try {
    const [all, draft, submitted, returned, approved, partial, inbounded, cancelled, closed] = await Promise.all([
      listPurchaseInbounds({ keyword: query.keyword, page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'DRAFT', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'SUBMITTED', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'RETURNED', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'APPROVED', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'PARTIALLY_INBOUNDED', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'INBOUNDED', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'CANCELLED', page: 1, pageSize: 1 }),
      listPurchaseInbounds({ keyword: query.keyword, status: 'CLOSED', page: 1, pageSize: 1 })
    ])
    if (token !== countRequestToken) return
    statusTotals.all = all.total || 0
    statusTotals.DRAFT = draft.total || 0
    statusTotals.SUBMITTED = submitted.total || 0
    statusTotals.RETURNED = returned.total || 0
    statusTotals.APPROVED = approved.total || 0
    statusTotals.PARTIALLY_INBOUNDED = partial.total || 0
    statusTotals.INBOUNDED = inbounded.total || 0
    statusTotals.CANCELLED = cancelled.total || 0
    statusTotals.CLOSED = closed.total || 0
  } catch {
    // Counts are supporting UI only; the main list already reports request failures.
  }
}

async function loadSuppliers() {
  try {
    const data = await listSuppliers({ page: 1, pageSize: 100 })
    suppliers.value = (data.items || []).filter((supplier) => supplier.status === 1)
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function loadSupplierSkus(supplierId) {
  supplierSkus.value = []
  if (!supplierId) return
  supplierSkuLoading.value = true
  try {
    supplierSkus.value = await listEnabledSupplierSkus(supplierId)
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    supplierSkuLoading.value = false
  }
}

watch(
  () => form.supplierId,
  async (supplierId, previousSupplierId) => {
    if (suppressSupplierWatch.value) return
    if (previousSupplierId !== undefined && supplierId !== previousSupplierId) {
      form.items.splice(0, form.items.length, createLine())
    }
    await loadSupplierSkus(supplierId)
  }
)

function applyRouteSearch(routeSearch) {
  const keyword = routeSearch.keyword ? String(routeSearch.keyword) : ''
  const module = routeSearch.module ? String(routeSearch.module) : ''
  const nextModule = module === 'orders' ? 'orders' : 'inbound'
  if (query.keyword === keyword && query.module === module && activeModule.value === nextModule) return
  query.keyword = keyword
  query.module = module
  activeModule.value = module === 'orders' ? 'orders' : 'inbound'
  activeStatus.value = 'all'
  query.page = 1
  loadData()
}

watch(
  () => ({ keyword: route.query.keyword, module: route.query.module }),
  (routeSearch) => {
    applyRouteSearch(routeSearch)
  }
)

function handleSupplierSkuSelected(line) {
  const binding = findBinding(line.bindingId, null)
  line.selectedSku = binding
  line.skuId = binding?.skuId || null
  line.units = binding?.units || []
  line.unit = binding?.baseUnit || ''
  line.conversionRate = 1
  line.purchasePrice = Number(binding?.defaultPurchasePrice || 0)
  line.minPurchaseQuantity = binding?.minPurchaseQuantity || 1
}

function setConversionRate(line, rate) {
  line.conversionRate = rate || 1
}

async function reload() {
  query.page = 1
  await loadData()
}

function resetQuery() {
  query.keyword = ''
  reload()
}

function setStatus(status) {
  activeStatus.value = status
  query.page = 1
  loadData()
}

function setActiveModule(module) {
  if (activeModule.value === module) return
  activeModule.value = module
  activeStatus.value = 'all'
  query.page = 1
  loadData()
}

function changePage(page) {
  query.page = Math.min(Math.max(1, page), pageCount.value)
  loadData()
}

function addLine() {
  form.items.push(createLine())
}

function removeLine(index) {
  form.items.splice(index, 1)
}

async function openPlanDialog(row = null) {
  planMode.value = row ? 'edit' : 'create'
  editingId.value = row?.id || null
  form.remark = ''
  form.items.splice(0, form.items.length)
  supplierSkus.value = []
  suppressSupplierWatch.value = true
  try {
    if (row) {
      const data = await getPurchaseInbound(row.id)
      form.supplierId = data.supplierId
      form.remark = data.remark || ''
      await loadSupplierSkus(data.supplierId)
      form.items.splice(0, form.items.length, ...(data.items || []).map((item) => createLine(item)))
    } else {
      form.supplierId = null
      form.items.splice(0, form.items.length, createLine())
    }
    planDialogVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    suppressSupplierWatch.value = false
  }
}

async function openDetail(item) {
  try {
    detail.value = await getPurchaseInbound(item.id)
    detailVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

async function openReceiptDialog(row) {
  try {
    const data = await getPurchaseInbound(row.id)
    const receivableItems = (data.items || []).filter((item) => remainingBaseQuantity(item) > 0)
    if (!receivableItems.length) {
      showMessage('当前采购单没有可入库的剩余数量', 'error')
      return
    }
    receiptOrder.value = data
    receiptForm.remark = ''
    receiptForm.items.splice(0, receiptForm.items.length, ...receivableItems.map((item) => ({
      key: item.id,
      item,
      purchaseInboundItemId: item.id,
      batches: [createReceiptBatch()]
    })))
    receiptVisible.value = true
  } catch (error) {
    showMessage(error.message, 'error')
  }
}

function addReceiptBatch(receiptItem) {
  receiptItem.batches.push(createReceiptBatch())
}

function removeReceiptBatch(receiptItem, batchIndex) {
  receiptItem.batches.splice(batchIndex, 1)
}

function validatePlanForm() {
  if (!form.supplierId) {
    return '请选择供应商'
  }
  if (!form.items.length) {
    return '请至少添加一条明细'
  }
  const invalid = form.items.some((line) => (
    !line.skuId ||
    !line.unit ||
    !line.quantity ||
    line.quantity <= 0 ||
    Number(line.quantity) < Number(line.minPurchaseQuantity || 1) ||
    line.purchasePrice === '' ||
    Number(line.purchasePrice) < 0
  ))
  return invalid ? '请选择供应商SKU，并填写正确数量和采购价；数量不能低于最小采购量' : ''
}

function validateReceiptForm() {
  if (!receiptForm.items.length) {
    return '实际入库明细不能为空'
  }
  for (const receiptItem of receiptForm.items) {
    if (!receiptItem.batches.length) {
      return '实际入库批次不能为空'
    }
    const receivedBaseQuantity = receiptItem.batches.reduce((sum, batch) => sum + receiptBatchBaseQuantity(receiptItem.item, batch), 0)
    if (receivedBaseQuantity <= 0) {
      return '实际入库数量必须大于0'
    }
    if (receivedBaseQuantity > remainingBaseQuantity(receiptItem.item)) {
      return '实际入库数量不能超过计划剩余数量'
    }
    const invalidBatch = receiptItem.batches.some((batch) => (
      !batch.quantity ||
      Number(batch.quantity) <= 0 ||
      !batch.productionDate ||
      !batch.shelfLifeDays ||
      !Number.isInteger(Number(batch.shelfLifeDays)) ||
      Number(batch.shelfLifeDays) <= 0
    ))
    if (invalidBatch) {
      return '请填写正确的实收数量、生产日期和保质期'
    }
  }
  return ''
}

async function savePlan() {
  const error = validatePlanForm()
  if (error) {
    showMessage(error, 'error')
    return
  }
  submitting.value = true
  try {
    const payload = {
      supplierId: form.supplierId,
      items: form.items.map((line) => ({
        skuId: line.skuId,
        quantity: Number(line.quantity),
        unit: line.unit,
        purchasePrice: Number(line.purchasePrice)
      })),
      remark: form.remark
    }
    if (planMode.value === 'edit') {
      await updatePurchaseInboundPlan(editingId.value, payload)
      showMessage('采购计划已更新')
    } else {
      await createPurchaseInboundDraft(payload)
      showMessage('采购计划已保存')
    }
    planDialogVisible.value = false
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function submitReceipt() {
  const error = validateReceiptForm()
  if (error) {
    showMessage(error, 'error')
    return
  }
  submitting.value = true
  try {
    await receivePurchaseInbound(receiptOrder.value.id, {
      items: receiptForm.items.map((receiptItem) => ({
        purchaseInboundItemId: receiptItem.purchaseInboundItemId,
        batches: receiptItem.batches.map((batch) => ({
          quantity: Number(batch.quantity),
          productionDate: batch.productionDate,
          shelfLifeDays: Number(batch.shelfLifeDays)
        }))
      })),
      remark: receiptForm.remark
    })
    receiptVisible.value = false
    showMessage('实际入库已保存')
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function submitOrder(row) {
  await runWorkflowAction(row, () => submitPurchaseInbound(row.id), '采购计划已提交')
}

async function approveOrder(row) {
  await runWorkflowAction(row, () => approvePurchaseInbound(row.id), '采购计划已审批')
}

async function returnOrder(row) {
  const reason = window.prompt('请输入退回原因')
  if (reason === null) return
  await runWorkflowAction(row, () => returnPurchaseInbound(row.id, { reason }), '采购计划已退回')
}

async function cancelOrder(row) {
  const reason = window.prompt('请输入取消原因')
  if (reason === null) return
  await runWorkflowAction(row, () => cancelPurchaseInbound(row.id, { reason }), '采购计划已取消')
}

async function closeOrder(row) {
  const reason = window.prompt('请输入关闭原因')
  if (reason === null) return
  await runWorkflowAction(row, () => closePurchaseInbound(row.id, { reason }), '采购计划已关闭')
}

async function runWorkflowAction(row, action, successMessage) {
  actioningId.value = row.id
  try {
    await action()
    showMessage(successMessage)
    loadData()
  } catch (error) {
    showMessage(error.message, 'error')
  } finally {
    actioningId.value = null
  }
}

onMounted(() => {
  loadSuppliers()
  loadData()
})
</script>

<style scoped>
.purchase-dock-page {
  --brand: #1e63e0;
  --brand-bright: #4d9bff;
  --brand-glow: #1e63e0;
  --brand-soft: rgba(77, 155, 255, 0.12);
  --line: #dce5ed;
  --line-strong: #b8cadc;
  --bg-card: #ffffff;
  --bg-elev: #f4f7fb;
  --bg-hover: #eef6ff;
  --text-dim: #4b5b6b;
  --text-mute: #6b7b8c;
  --text-faint: #94a3b8;
  --ok: #15803d;
  --warn: #d97706;
  --danger: #dc2626;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.page-head h1 {
  margin: 0;
  color: var(--text);
  font-size: 24px;
  font-weight: 650;
  letter-spacing: 1px;
}

.greet-sub {
  margin-top: 5px;
  color: var(--text-dim);
  font-size: 13px;
}

.greet-sub b {
  color: var(--brand);
  font-weight: 650;
}

.greet-sub b.ok {
  color: var(--ok);
}

.greet-sub b.warn {
  color: var(--warn);
}

.quick-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.purchase-dock-page .btn svg,
.quick-actions svg {
  width: 15px;
  height: 15px;
}

.purchase-dock-page .btn.primary,
.purchase-dock-page .btn-primary {
  color: #ffffff;
  background: linear-gradient(135deg, var(--brand-bright), var(--brand-glow));
  border: 1px solid transparent;
  box-shadow: 0 6px 18px rgba(47, 124, 255, 0.18);
}

.purchase-dock-page .btn.sm {
  min-height: 32px;
  height: 32px;
  padding: 0 11px;
  border-radius: 7px;
  font-size: 12.5px;
}

.purchase-kpi-row {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.purchase-kpi-card {
  min-height: 128px;
  width: 100%;
  text-align: left;
  color: inherit;
}

.purchase-kpi-card .label,
.purchase-kpi-card .value,
.purchase-kpi-card .delta,
.purchase-kpi-card .sub {
  position: relative;
  z-index: 1;
}

.sub-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 4px;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
  scrollbar-width: none;
}

.sub-tabs::-webkit-scrollbar {
  display: none;
}

.sub-tabs a {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 46px;
  padding: 0 16px;
  border-bottom: 2px solid transparent;
  color: var(--text-mute);
  font-size: 13.5px;
  text-decoration: none;
  white-space: nowrap;
}

.sub-tabs a:hover {
  color: var(--text);
}

.sub-tabs a.on {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 550;
}

.sub-tabs .ct,
.purchase-status-tabs .ct {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--bg-elev);
  color: var(--text-mute);
}

.sub-tabs a.on .ct,
.purchase-status-tabs button.on .ct {
  background: var(--brand-soft);
  color: var(--brand);
}

.purchase-filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 14px;
  background: var(--bg-card);
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(20, 40, 80, 0.04);
}

.purchase-filter-search {
  flex: 1 1 320px;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  background: var(--bg-elev);
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--text-mute);
}

.purchase-filter-search:focus-within {
  border-color: var(--brand-bright);
  box-shadow: 0 0 0 3px var(--brand-soft);
  background: #ffffff;
}

.purchase-filter-search svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
}

.purchase-filter-search input {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: 0;
  outline: 0;
  color: var(--text);
  font-size: 12.5px;
}

.view-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  color: var(--text-mute);
  font-size: 12px;
}

.view-switch {
  display: inline-flex;
  padding: 3px;
  background: var(--bg-elev);
  border: 1px solid var(--line);
  border-radius: 8px;
}

.view-switch button {
  width: 32px;
  height: 28px;
  appearance: none;
  background: transparent;
  border: 0;
  color: var(--text-mute);
  cursor: pointer;
  border-radius: 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.view-switch button.on {
  background: #ffffff;
  color: var(--brand);
  box-shadow: 0 1px 2px rgba(20, 40, 80, 0.06);
}

.view-switch svg {
  width: 14px;
  height: 14px;
}

.purchase-work-card {
  padding: 0;
  overflow: hidden;
}

.purchase-status-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  border-bottom: 1px solid var(--line);
  padding: 0 4px;
  overflow-x: auto;
}

.purchase-status-tabs button {
  appearance: none;
  border: 0;
  background: transparent;
  color: var(--text-mute);
  font-size: 13px;
  padding: 12px 14px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: color 0.15s, border-color 0.15s;
}

.purchase-status-tabs button:hover {
  color: var(--text);
}

.purchase-status-tabs button.on {
  color: var(--brand);
  border-bottom-color: var(--brand);
  font-weight: 550;
}

.dock-board {
  min-height: 280px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  padding: 18px;
}

.dock-card {
  position: relative;
  overflow: hidden;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 8px;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}

.dock-card:hover {
  border-color: color-mix(in oklab, var(--accent, var(--brand)) 44%, var(--line));
  box-shadow: 0 12px 28px rgba(20, 40, 80, 0.08);
  transform: translateY(-2px);
}

.dock-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--accent, var(--brand));
}

.dock-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.dock-id,
.mono {
  font-family: "Consolas", "DM Mono", monospace;
}

.dock-id {
  color: var(--text-mute);
  font-size: 11px;
  letter-spacing: 0.5px;
}

.dock-supplier {
  margin-top: 3px;
  color: var(--text);
  font-size: 15px;
  font-weight: 650;
}

.dock-sub,
.muted {
  color: var(--text-mute);
  font-size: 11.5px;
}

.dock-meta {
  margin-top: 10px;
  color: var(--text-mute);
  font-size: 11.5px;
  line-height: 1.7;
}

.dock-meta .row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.dock-meta b {
  color: var(--text);
  font-weight: 650;
}

.progress,
.mini-progress {
  margin-top: 14px;
  height: 6px;
  background: var(--bg-elev);
  border-radius: 3px;
  overflow: hidden;
}

.mini-progress {
  width: 112px;
  height: 5px;
  margin-top: 5px;
}

.progress > i,
.mini-progress > i {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, var(--brand-bright), var(--brand-glow));
}

.progress > i.full {
  background: linear-gradient(90deg, #1f7a5b, #34d399);
}

.progress-label {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 5px;
  color: var(--text-mute);
  font-family: "Consolas", "DM Mono", monospace;
  font-size: 11px;
}

.progress-label b {
  color: var(--text);
}

.dock-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.dock-actions button {
  flex: 1 1 70px;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #ffffff;
  color: var(--text-dim);
  cursor: pointer;
  font-size: 12px;
  font-weight: 550;
}

.dock-actions button.primary {
  color: #ffffff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--brand-bright), var(--brand-glow));
}

.dock-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.purchase-table {
  min-height: 280px;
  overflow: auto;
}

.purchase-table table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.purchase-table th,
.purchase-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
  white-space: nowrap;
}

.purchase-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: var(--text-dim);
  background: #f8fbff;
  font-size: 12.5px;
  font-weight: 650;
}

.purchase-table tbody tr:hover {
  background: #fbfdff;
}

.strong {
  color: var(--brand);
  font-weight: 700;
}

.pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 9px;
  font-size: 11px;
  border-radius: 999px;
  border: 1px solid currentColor;
  background: color-mix(in oklab, currentColor 14%, #fff);
  white-space: nowrap;
}

.pill.ok {
  color: var(--ok);
}

.pill.warn {
  color: var(--warn);
}

.pill.danger {
  color: var(--danger);
}

.pill.info {
  color: var(--brand);
}

.pill.muted {
  color: var(--text-mute);
}

.pill .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}

.row-actions {
  display: inline-flex;
  gap: 4px;
}

.row-actions button {
  width: 28px;
  height: 28px;
  min-height: 28px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #ffffff;
  color: var(--text-mute);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11px;
  font-weight: 650;
}

.row-actions button:hover {
  color: var(--brand);
  border-color: var(--brand-bright);
  background: var(--brand-soft);
}

.row-actions .primary-icon {
  color: #ffffff;
  border-color: transparent;
  background: var(--brand);
}

.row-actions svg {
  width: 14px;
  height: 14px;
}

.modern-loading,
.modern-empty {
  grid-column: 1 / -1;
  padding: 44px 16px;
  text-align: center;
  color: var(--text-mute);
}

.modern-empty h4 {
  margin: 0;
  color: var(--text);
  font-size: 15px;
}

.modern-empty p {
  margin: 6px 0 0;
  font-size: 12.5px;
}

.page-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--line);
  color: var(--text-mute);
  font-size: 12.5px;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 30px;
  height: 30px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #ffffff;
  color: var(--text-mute);
  cursor: pointer;
}

.page-btn.on {
  color: #ffffff;
  background: var(--brand);
  border-color: var(--brand);
}

.detail-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.line-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
}

.line-header,
.summary-bar,
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.summary-bar {
  margin: 16px 0;
  padding: 12px;
  background: var(--surface-muted);
  border-radius: 8px;
  font-weight: 650;
}

.meta-row {
  justify-content: flex-start;
  color: var(--muted);
  flex-wrap: wrap;
}

.add-line-button {
  align-self: flex-start;
}

.detail-grid {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 10px 16px;
  margin-bottom: 16px;
}

.detail-grid span,
.muted {
  color: var(--muted);
}

.subsection-title {
  margin: 18px 0 8px;
  font-size: 16px;
}

.receipt-batch-grid {
  margin-top: 10px;
}

.batch-actions {
  align-items: flex-start;
}

.empty-note {
  color: var(--muted);
  padding: 12px 0;
}

@media (max-width: 1100px) {
  .purchase-kpi-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
  }

  .quick-actions,
  .quick-actions .btn {
    width: 100%;
  }

  .purchase-kpi-row {
    grid-template-columns: 1fr;
  }

  .view-tools {
    width: 100%;
    margin-left: 0;
    justify-content: space-between;
  }

  .dock-board {
    grid-template-columns: 1fr;
    padding: 12px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
