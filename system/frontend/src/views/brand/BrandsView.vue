<template>
  <div class="product-modern-page product-brand-modern-page">
    <div class="page-head">
      <div>
        <h1>品牌管理</h1>
        <div class="greet-sub">
          品牌库共 <b>{{ formatNumber(brandMetrics.total) }}</b> 个 · 启用 <b>{{ formatNumber(brandMetrics.enabled) }}</b> 个 ·
          停用 <b>{{ formatNumber(brandMetrics.disabled) }}</b> 个 · 最近同步 {{ syncTime || '--:--' }}
        </div>
      </div>
      <div class="quick-actions">
        <button class="btn" type="button" @click="safeAction('品牌导入模板已准备')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="17 8 12 3 7 8" />
            <line x1="12" y1="3" x2="12" y2="15" />
          </svg>
          批量导入
        </button>
        <button class="btn" type="button" @click="safeAction('正在按当前筛选导出品牌列表')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
            <polyline points="7 10 12 15 17 10" />
            <line x1="12" y1="15" x2="12" y2="3" />
          </svg>
          导出
        </button>
        <button class="btn primary" type="button" @click="openBrandModal()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          新建品牌
        </button>
      </div>
    </div>

    <ProductModuleTabs active="brand" :total-count="brandMetrics.total" @action="safeAction" />

    <div v-if="message" class="message" :class="messageType === 'error' ? 'message-error' : 'message-success'">
      {{ message }}
    </div>

    <section class="brand-toolbar">
      <label class="modern-filter-search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input v-model.trim="query.keyword" placeholder="按品牌编码 / 品牌名称搜索" @keyup.enter="reload" />
      </label>
      <select v-model="query.status" class="select" @change="reload">
        <option value="">全部状态</option>
        <option value="1">启用</option>
        <option value="0">停用</option>
      </select>
      <select v-model="query.sort" class="select" @change="resetLocalPage">
        <option value="new">最新创建</option>
        <option value="name">名称 A → Z</option>
        <option value="code">编码升序</option>
        <option value="status">启用优先</option>
      </select>
      <div class="brand-view-switch" aria-label="品牌视图切换">
        <button
          type="button"
          :class="{ on: viewMode === 'grid' }"
          title="卡片视图"
          @click="viewMode = 'grid'"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" />
            <rect x="14" y="3" width="7" height="7" />
            <rect x="14" y="14" width="7" height="7" />
            <rect x="3" y="14" width="7" height="7" />
          </svg>
        </button>
        <button
          type="button"
          :class="{ on: viewMode === 'list' }"
          title="表格视图"
          @click="viewMode = 'list'"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
      </div>
      <button class="btn" type="button" @click="resetQuery">重置</button>
      <button class="btn primary" type="button" :disabled="loading" @click="reload">查询</button>
    </section>

    <div v-if="loading" class="modern-loading">品牌数据加载中...</div>

    <section v-else-if="viewMode === 'grid'" class="brand-grid">
      <article
        v-for="brand in pagedBrands"
        :key="brand.id"
        class="brand-card"
        :style="{ '--accent': brand.color }"
        @click="openBrandModal(brand)"
      >
        <div class="brand-card-head">
          <div class="brand-logo" :style="{ background: brand.color }">
            <span>{{ brand.logoText }}</span>
          </div>
          <div class="brand-card-title">
            <h4>{{ brand.brandName }}</h4>
            <p>{{ brand.brandCode || brand.enName }}</p>
            <span class="brand-area">{{ brand.area }}</span>
          </div>
        </div>
        <div class="brand-stats">
          <div>
            <span>品牌编码</span>
            <strong>{{ brand.brandCode || '-' }}</strong>
          </div>
          <div>
            <span>状态</span>
            <strong>{{ brand.statusLabel }}</strong>
          </div>
        </div>
        <div class="brand-spark" :style="{ '--accent': brand.color }">
          <i v-for="height in brand.spark" :key="height" :style="{ height: `${height}%` }"></i>
        </div>
        <footer class="brand-card-foot">
          <button type="button" @click.stop="filterProductsByBrand(brand)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            商品
          </button>
          <button type="button" @click.stop="openBrandModal(brand)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
            </svg>
            编辑
          </button>
          <button class="danger" type="button" @click.stop="confirmRemove(brand)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6" />
            </svg>
          </button>
        </footer>
      </article>
      <div v-if="!pagedBrands.length" class="modern-empty brand-empty">
        <h4>暂无品牌记录</h4>
        <p>调整筛选条件或新建品牌后再查看。</p>
        <button class="btn primary" type="button" @click="openBrandModal()">新建品牌</button>
      </div>
    </section>

    <article v-else class="card brand-list">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>品牌</th>
              <th>品牌编码</th>
              <th>状态</th>
              <th>备注</th>
              <th>创建时间</th>
              <th class="actions-column">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="brand in pagedBrands" :key="brand.id">
              <td>
                <div class="brand-table-cell">
                  <div class="brand-logo small" :style="{ background: brand.color }">
                    <span>{{ brand.logoText }}</span>
                  </div>
                  <div>
                    <strong>{{ brand.brandName }}</strong>
                    <span>{{ brand.enName }}</span>
                  </div>
                </div>
              </td>
              <td>{{ brand.brandCode || '-' }}</td>
              <td><span class="pill" :class="brand.status === 1 ? 'ok' : 'mute'"><span class="dot"></span>{{ brand.statusLabel }}</span></td>
              <td>{{ brand.remark || '-' }}</td>
              <td>{{ formatDate(brand.createTime) }}</td>
              <td>
                <div class="row-actions">
                  <button type="button" title="查看商品" @click="filterProductsByBrand(brand)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button type="button" title="编辑" @click="openBrandModal(brand)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                    </svg>
                  </button>
                  <button class="danger" type="button" title="删除" @click="confirmRemove(brand)">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6" />
                      <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!pagedBrands.length">
              <td colspan="6" class="brand-list-empty">暂无品牌记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>

    <div class="page-foot">
      <span class="total">共 <b>{{ formatNumber(filteredBrands.length) }}</b> 条</span>
      <span class="row">
        每页
        <select v-model.number="query.pageSize" class="select" @change="resetLocalPage">
          <option :value="8">8</option>
          <option :value="12">12</option>
          <option :value="20">20</option>
        </select>
      </span>
      <div class="pager">
        <button type="button" :disabled="query.page <= 1" @click="changePage(query.page - 1)">上一页</button>
        <button
          v-for="page in pageButtons"
          :key="page"
          type="button"
          :class="{ on: page === query.page }"
          @click="changePage(page)"
        >
          {{ page }}
        </button>
        <button type="button" :disabled="query.page >= pageCount" @click="changePage(query.page + 1)">下一页</button>
      </div>
    </div>

    <BaseDialog v-model="dialogVisible" :title="editingId ? '编辑品牌' : '新建品牌'">
      <div class="brand-modal-preview" :style="{ '--accent': previewColor }">
        <div class="brand-logo large" :style="{ background: previewColor }">
          <span>{{ previewLogoText }}</span>
        </div>
        <div>
          <span>实时预览</span>
          <strong>{{ form.brandName || '品牌名称' }}</strong>
          <p>{{ previewEnName }}</p>
        </div>
      </div>
      <form class="form-grid">
        <label v-if="editingId" class="form-item">
          <span class="form-label">品牌编码</span>
          <input v-model.trim="form.brandCode" class="input" disabled />
        </label>
        <label class="form-item">
          <span class="form-label">品牌名称</span>
          <input v-model.trim="form.brandName" class="input" maxlength="100" placeholder="例如：伊利" />
        </label>
        <label class="form-item">
          <span class="form-label">状态</span>
          <select v-model.number="form.status" class="select">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </label>
        <label class="form-item full">
          <span class="form-label">备注</span>
          <input v-model.trim="form.remark" class="input" maxlength="200" placeholder="一句话品牌定位或维护说明" />
        </label>
      </form>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="dialogVisible = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : '保存' }}
        </button>
      </template>
    </BaseDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseDialog from '../../components/BaseDialog.vue'
import { createBrand, deleteBrand, listBrandOptions, listBrands, updateBrand } from '../../api/brand'
import ProductModuleTabs from '../product/components/ProductModuleTabs.vue'

const router = useRouter()
const colorPalette = ['#2f7cff', '#16a34a', '#dc2626', '#f59e0b', '#7c3aed', '#0e7490', '#be185d', '#0f766e']
const areaOptions = ['中国', '美国', '瑞士', '日本', '韩国', '欧洲', '东南亚', '其它']

const query = reactive({ keyword: '', status: '', sort: 'new', page: 1, pageSize: 8 })
const items = ref([])
const optionBrands = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const message = ref('')
const messageType = ref('success')
const syncTime = ref('')
const viewMode = ref('grid')
const form = reactive({ brandCode: '', brandName: '', status: 1, remark: '' })

const brandMetrics = computed(() => {
  const total = items.value.length
  const enabled = items.value.filter((item) => Number(item.status) === 1).length
  return {
    total,
    enabled,
    disabled: total - enabled
  }
})

const derivedBrands = computed(() =>
  items.value.map((item, index) => {
    const hash = hashText(`${item.brandCode || ''}${item.brandName || ''}`)
    const color = colorPalette[hash % colorPalette.length]
    return {
      ...item,
      status: Number(item.status),
      statusLabel: Number(item.status) === 1 ? '启用' : '停用',
      color,
      logoText: logoText(item.brandName),
      enName: toBrandAlias(item),
      area: areaOptions[(hash + index) % areaOptions.length],
      spark: sparkBars(hash)
    }
  })
)

const filteredBrands = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  const list = derivedBrands.value.filter((item) => {
    if (query.status !== '' && String(item.status) !== query.status) return false
    if (!keyword) return true
    return [item.brandCode, item.brandName, item.enName, item.remark]
      .some((value) => String(value || '').toLowerCase().includes(keyword))
  })
  return [...list].sort((a, b) => {
    if (query.sort === 'name') return a.brandName.localeCompare(b.brandName, 'zh-CN')
    if (query.sort === 'code') return String(a.brandCode || '').localeCompare(String(b.brandCode || ''))
    if (query.sort === 'status') return b.status - a.status || a.brandName.localeCompare(b.brandName, 'zh-CN')
    return String(b.createTime || '').localeCompare(String(a.createTime || '')) || Number(b.id || 0) - Number(a.id || 0)
  })
})

const pageCount = computed(() => Math.max(1, Math.ceil(filteredBrands.value.length / query.pageSize)))
const pagedBrands = computed(() => {
  const safePage = Math.min(query.page, pageCount.value)
  const start = (safePage - 1) * query.pageSize
  return filteredBrands.value.slice(start, start + query.pageSize)
})
const pageButtons = computed(() => {
  const pages = []
  const start = Math.max(1, query.page - 2)
  const end = Math.min(pageCount.value, start + 4)
  for (let page = start; page <= end; page += 1) pages.push(page)
  return pages
})
const previewColor = computed(() => {
  const hash = hashText(form.brandName || form.brandCode || 'brand')
  return colorPalette[hash % colorPalette.length]
})
const previewLogoText = computed(() => logoText(form.brandName || '品牌'))
const previewEnName = computed(() => toBrandAlias(form))

function hashText(text) {
  return [...String(text || '')].reduce((sum, char) => sum + char.charCodeAt(0), 0)
}

function logoText(name) {
  return [...String(name || '品牌')].slice(0, 2).join('')
}

function toBrandAlias(brand) {
  const source = brand.brandCode || brand.brandName || 'BRAND'
  return String(source)
    .replace(/[^a-zA-Z0-9]/g, '')
    .slice(0, 12)
    .toUpperCase() || 'BRAND'
}

function sparkBars(seed) {
  const bars = []
  let value = seed || 37
  for (let index = 0; index < 10; index += 1) {
    value = (value * 31 + 7) % 100
    bars.push(22 + (value % 72))
  }
  return bars
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

function resetLocalPage() {
  query.page = 1
}

function changePage(page) {
  query.page = Math.min(Math.max(1, page), pageCount.value)
}

function resetQuery() {
  Object.assign(query, {
    keyword: '',
    status: '',
    sort: 'new',
    page: 1,
    pageSize: query.pageSize
  })
  loadData()
}

function resetForm() {
  Object.assign(form, { brandCode: '', brandName: '', status: 1, remark: '' })
  editingId.value = null
}

function openBrandModal(brand) {
  if (brand) {
    editingId.value = brand.id
    Object.assign(form, {
      brandCode: brand.brandCode || '',
      brandName: brand.brandName || '',
      status: Number(brand.status) === 0 ? 0 : 1,
      remark: brand.remark || ''
    })
  } else {
    resetForm()
  }
  dialogVisible.value = true
}

async function submit() {
  if (!form.brandName.trim()) {
    showMessage('请填写品牌名称', 'error')
    return
  }
  submitting.value = true
  try {
    const payload = {
      brandName: form.brandName,
      status: form.status,
      remark: form.remark
    }
    if (editingId.value) {
      await updateBrand(editingId.value, payload)
    } else {
      await createBrand(payload)
    }
    dialogVisible.value = false
    showMessage('品牌已保存')
    await loadData()
  } catch (error) {
    showMessage(error.message || '品牌保存失败', 'error')
  } finally {
    submitting.value = false
  }
}

async function confirmRemove(brand) {
  if (!window.confirm(`确认删除品牌“${brand.brandName}”？已绑定商品的品牌会被后端拒绝删除。`)) {
    return
  }
  try {
    await deleteBrand(brand.id)
    showMessage('品牌已删除')
    await loadData()
  } catch (error) {
    showMessage(error.message || '品牌删除失败', 'error')
  }
}

function filterProductsByBrand(brand) {
  router.push({ path: '/products-modern', query: { brandId: brand.id } })
}

async function reload() {
  query.page = 1
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [pageData, optionData] = await Promise.all([
      listBrands({
        keyword: query.keyword,
        status: query.status === '' ? undefined : Number(query.status),
        page: 1,
        pageSize: 200
      }),
      listBrandOptions()
    ])
    items.value = pageData.items || []
    optionBrands.value = optionData || []
    syncTime.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    changePage(query.page)
  } catch (error) {
    showMessage(error.message || '品牌数据加载失败', 'error')
  } finally {
    loading.value = false
  }
}

function formatDate(value) {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>
