<template>
  <div v-if="visible" class="category-modal-mask" @click.self="$emit('close')">
    <section class="category-modal-card">
      <header class="modal-head">
        <h3>{{ title }}</h3>
        <button class="close-x" type="button" aria-label="关闭弹窗" @click="$emit('close')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </header>
      <div class="modal-body">
        <div v-if="mode !== 'edit'" class="field-row">
          <div class="label">分类层级</div>
          <div class="input-wrap">
            <div class="radio-chips">
              <label>
                <input v-model="form.level" type="radio" name="catLevelModern" value="1" />
                <span>一级分类</span>
              </label>
              <label>
                <input v-model="form.level" type="radio" name="catLevelModern" value="2" />
                <span>二级分类</span>
              </label>
            </div>
          </div>
        </div>
        <div v-if="mode !== 'edit'" class="field-row">
          <div class="label"><span class="req">*</span>父级分类</div>
          <div class="input-wrap">
            <select v-model="form.parentId" class="select" :disabled="form.level === '1'">
              <option v-if="form.level === '1'" :value="null">一级分类无需父级</option>
              <option v-for="parent in parentOptions" v-else :key="parent.id" :value="parent.id">
                {{ parent.name }}
              </option>
            </select>
          </div>
        </div>
        <div class="field-row">
          <div class="label"><span class="req">*</span>名称</div>
          <div class="input-wrap">
            <input v-model.trim="form.name" class="input" placeholder="例：进口饮料" />
          </div>
        </div>
        <div class="field-row">
          <div class="label">英文标识</div>
          <div class="input-wrap">
            <input v-model.trim="form.code" class="input mono" placeholder="import-drink" />
          </div>
        </div>
        <div class="field-row last">
          <div class="label">显示顺序</div>
          <div class="input-wrap narrow">
            <input v-model.number="form.sortOrder" class="input" type="number" min="0" />
          </div>
        </div>
      </div>
      <footer class="modal-foot">
        <button class="btn" type="button" @click="$emit('close')">取消</button>
        <button class="btn primary" type="button" :disabled="submitting" @click="$emit('submit')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12" />
          </svg>
          {{ submitting ? '提交中...' : mode === 'edit' ? '保存' : '创建' }}
        </button>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  mode: {
    type: String,
    default: 'create'
  },
  form: {
    type: Object,
    required: true
  },
  parents: {
    type: Array,
    default: () => []
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

defineEmits(['close', 'submit'])

const title = computed(() => {
  if (props.mode === 'edit') return '编辑分类'
  if (props.form.level === '2') return '新建二级分类'
  return '新建分类'
})

const parentOptions = computed(() => props.parents.filter((parent) => !parent.parentId))

watch(
  () => props.form.level,
  (level) => {
    if (props.mode === 'edit') return
    if (level === '1') {
      props.form.parentId = null
      return
    }
    if (!props.form.parentId && parentOptions.value[0]) {
      props.form.parentId = parentOptions.value[0].id
    }
  },
  { immediate: true }
)

watch(parentOptions, (options) => {
  if (props.mode === 'edit' || props.form.level !== '2') return
  const hasSelectedParent = options.some((parent) => String(parent.id) === String(props.form.parentId))
  if (!hasSelectedParent) {
    props.form.parentId = options[0]?.id ?? null
  }
})
</script>
