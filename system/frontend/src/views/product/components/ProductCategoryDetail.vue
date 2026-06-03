<template>
  <article class="card cat-detail-card">
    <div v-if="!category" class="cat-empty-detail">请选择左侧分类</div>
    <template v-else>
      <div class="cat-detail-head">
        <div class="cat-icon-circle" :style="themeStyle">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="iconPath" />
        </div>
        <div class="cat-detail-copy">
          <h2>{{ category.name }}</h2>
          <div class="sub">
            <span>分类ID <b>{{ category.id }}</b></span>
            <span>{{ parentName ? `父级：${parentName}` : '一级分类（顶级）' }}</span>
            <span class="pill ok"><span class="dot"></span>已启用</span>
          </div>
        </div>
      </div>

      <div class="stat-grid">
        <div class="item" style="--accent: var(--brand)">
          <div class="lbl">商品数</div>
          <div class="val">{{ formatNumber(stats.total) }}</div>
        </div>
        <div class="item" style="--accent: #34d399">
          <div class="lbl">在售</div>
          <div class="val">{{ formatNumber(stats.onSale) }}</div>
        </div>
        <div class="item" style="--accent: #fbbf24">
          <div class="lbl">库存预警</div>
          <div class="val">{{ formatNumber(stats.warning) }}</div>
        </div>
        <div class="item" style="--accent: #c084fc">
          <div class="lbl">本月销量</div>
          <div class="val">{{ formatNumber(stats.monthSales) }}<span> 件</span></div>
        </div>
      </div>

      <div class="card-head cat-settings-head">
        <div class="title-block">
          <h3>分类设置</h3>
          <div class="sub">{{ isEditing ? 'EDITING' : '查看模式' }}</div>
        </div>
        <button v-if="!isEditing" class="btn cat-detail-edit" type="button" @click="$emit('edit')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
          </svg>
          编辑
        </button>
      </div>

      <div class="field-row">
        <div class="label">分类名称</div>
        <div class="input-wrap">
          <input v-if="isEditing" v-model.trim="form.name" class="input" />
          <div v-else class="cat-readonly-field">{{ form.name || '--' }}</div>
        </div>
      </div>
      <div class="field-row">
        <div class="label">
          英文标识
          <span class="hint">用于 API / 报表展示</span>
        </div>
        <div class="input-wrap">
          <input v-if="isEditing" v-model.trim="form.code" class="input mono" />
          <div v-else class="cat-readonly-field mono">{{ form.code || '--' }}</div>
        </div>
      </div>
      <div class="field-row">
        <div class="label">分类图标</div>
        <div class="input-wrap">
          <div v-if="isEditing" class="radio-chips">
            <label v-for="icon in icons" :key="icon.key">
              <input v-model="form.icon" type="radio" name="categoryIcon" :value="icon.key" />
              <span>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" v-html="icon.path" />
                {{ icon.label }}
              </span>
            </label>
          </div>
          <div v-else class="cat-readonly-field">{{ iconLabel }}</div>
        </div>
      </div>
      <div class="field-row">
        <div class="label">是否启用</div>
        <div class="input-wrap switch-line">
          <label class="switch" :class="{ disabled: !isEditing }">
            <input v-model="form.enabled" type="checkbox" :disabled="!isEditing" />
            <span class="slider"></span>
          </label>
          <span>关闭后此分类不在商品创建时可选</span>
        </div>
      </div>
      <div class="field-row">
        <div class="label">显示顺序</div>
        <div class="input-wrap narrow">
          <input v-if="isEditing" v-model.number="form.sortOrder" class="input" type="number" min="0" />
          <div v-else class="cat-readonly-field">{{ form.sortOrder ?? '--' }}</div>
        </div>
      </div>

      <div v-if="isEditing" class="cat-save-row" :class="{ 'is-editing': isEditing }">
        <button class="btn danger" type="button" @click="$emit('delete', category)">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6" />
          </svg>
          删除分类
        </button>
        <button class="btn" type="button" :disabled="!isEditing" @click="$emit('cancel')">取消</button>
        <button class="btn primary" type="button" :disabled="!isEditing || saving" @click="$emit('save')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          {{ saving ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </template>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  category: {
    type: Object,
    default: null
  },
  parentName: {
    type: String,
    default: ''
  },
  theme: {
    type: Object,
    required: true
  },
  form: {
    type: Object,
    required: true
  },
  stats: {
    type: Object,
    required: true
  },
  icons: {
    type: Array,
    default: () => []
  },
  isEditing: {
    type: Boolean,
    default: false
  },
  saving: {
    type: Boolean,
    default: false
  }
})

defineEmits(['edit', 'save', 'cancel', 'delete', 'action'])

const themeStyle = computed(() => ({ '--c1': props.theme.c1, '--c2': props.theme.c2 }))
const iconPath = computed(() => props.theme.path || props.icons[0]?.path || '')
const iconLabel = computed(() => props.icons.find((icon) => icon.key === props.form.icon)?.label || '文件夹')

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}
</script>
