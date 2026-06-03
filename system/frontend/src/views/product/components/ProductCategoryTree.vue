<template>
  <article class="card tree-card">
    <div class="tc-head">
      <label class="tc-search">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input v-model.trim="keyword" placeholder="搜索分类..." />
      </label>
      <button class="tc-add" type="button" title="新建分类" @click="$emit('create-root')">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
      <button class="tc-sort" type="button" :class="{ on: sortMode }" :title="sortMode ? '退出排序' : '调整顺序'" @click="$emit('toggle-sort')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M8 6h13" />
          <path d="M8 12h13" />
          <path d="M8 18h13" />
          <path d="M3 6h.01" />
          <path d="M3 12h.01" />
          <path d="M3 18h.01" />
        </svg>
      </button>
    </div>

    <div v-if="sortMode" class="tree-sort-bar">
      <span>拖动同级分类调整顺序</span>
      <button type="button" @click="$emit('cancel-sort')">取消</button>
      <button class="primary" type="button" :disabled="!sortDirty" @click="$emit('save-sort')">保存排序</button>
    </div>

    <div class="tree">
      <div v-if="loading" class="tree-empty">分类加载中...</div>
      <div v-else-if="visibleRows.length === 0" class="tree-empty">暂无匹配分类</div>
      <div
        v-for="row in visibleRows"
        v-else
        :key="row.node.id"
        class="tree-row"
        :class="{ on: String(row.node.id) === String(activeId), open: row.open, leaf: !row.hasChildren, sorting: sortMode, dragging: String(draggingId) === String(row.node.id) }"
        :style="{ paddingLeft: `${10 + row.depth * 18}px` }"
        :data-category-id="row.node.id"
        @pointerdown="startPointerDrag(row, $event)"
        @click="$emit('select', row.node.id)"
      >
        <span v-if="sortMode" class="sort-grip" title="拖动排序">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="9" cy="6" r="1" />
            <circle cx="9" cy="12" r="1" />
            <circle cx="9" cy="18" r="1" />
            <circle cx="15" cy="6" r="1" />
            <circle cx="15" cy="12" r="1" />
            <circle cx="15" cy="18" r="1" />
          </svg>
        </span>
        <button
          v-if="row.hasChildren"
          class="chev-btn"
          type="button"
          @click.stop="$emit('toggle', row.node.id)"
        >
          <svg class="chev" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </button>
        <span v-else class="chev-spacer"></span>
        <span class="leaf-dot"></span>
        <span class="name">{{ row.node.name }}</span>
        <span class="ct">{{ formatNumber(counts[String(row.node.id)] || 0) }}</span>
        <span v-if="!sortMode" class="row-acts">
          <button type="button" title="编辑" @click.stop="$emit('edit', row.node)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 20h9" />
              <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
            </svg>
          </button>
        </span>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="dragPreview.visible"
        class="tree-drag-preview"
        :style="{ left: `${dragPreview.left}px`, top: `${dragPreview.y}px`, width: `${dragPreview.width}px`, paddingLeft: `${dragPreview.paddingLeft}px` }"
      >
        <span class="sort-grip">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="9" cy="6" r="1" />
            <circle cx="9" cy="12" r="1" />
            <circle cx="9" cy="18" r="1" />
            <circle cx="15" cy="6" r="1" />
            <circle cx="15" cy="12" r="1" />
            <circle cx="15" cy="18" r="1" />
          </svg>
        </span>
        <span class="leaf-dot"></span>
        <span class="name">{{ renderDragPreviewContent() }}</span>
      </div>
    </Teleport>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  tree: {
    type: Array,
    default: () => []
  },
  activeId: {
    type: [Number, String],
    default: null
  },
  openIds: {
    type: Array,
    default: () => []
  },
  counts: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  sortMode: {
    type: Boolean,
    default: false
  },
  sortDirty: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select', 'toggle', 'create-root', 'toggle-sort', 'cancel-sort', 'save-sort', 'reorder', 'edit'])

const keyword = ref('')
const draggingId = ref(null)
const dragSourceId = ref(null)
const dragTargetId = ref(null)
const dragPreview = ref({
  visible: false,
  name: '',
  left: 0,
  y: 0,
  width: 0,
  paddingLeft: 10,
  offsetY: 0
})

const visibleRows = computed(() => {
  const rows = []
  collectRows(props.tree, rows)
  return rows
})

function collectRows(nodes, rows, depth = 0) {
  for (const node of nodes || []) {
    const children = Array.isArray(node.children) ? node.children : []
    const hasChildren = children.length > 0
    const matched = matchesNode(node)
    const childMatched = hasChildren && children.some((child) => subtreeMatches(child))
    if (keyword.value && !matched && !childMatched) continue

    const open = keyword.value ? hasChildren : props.openIds.includes(String(node.id))
    rows.push({ node, depth, hasChildren, open, parentId: node.parentId ?? null })
    if (hasChildren && open) collectRows(children, rows, depth + 1)
  }
}

function subtreeMatches(node) {
  if (matchesNode(node)) return true
  return (node.children || []).some((child) => subtreeMatches(child))
}

function matchesNode(node) {
  if (!keyword.value) return true
  return String(node.name || '').toLowerCase().includes(keyword.value.toLowerCase())
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function startPointerDrag(row, event) {
  if (!props.sortMode) return
  if (event.target.closest('button')) return
  event.preventDefault()
  const rowEl = event.currentTarget
  const rect = rowEl.getBoundingClientRect()
  const computedStyle = window.getComputedStyle(rowEl)
  dragSourceId.value = row.node.id
  draggingId.value = row.node.id
  dragTargetId.value = row.node.id
  dragPreview.value = {
    visible: true,
    name: row.node.name || '',
    left: rect.left,
    y: rect.top,
    width: rect.width,
    paddingLeft: Number.parseFloat(computedStyle.paddingLeft) || 10,
    offsetY: event.clientY - rect.top
  }
  document.addEventListener('pointermove', handlePointerMove)
  document.addEventListener('pointerup', finishPointerDrag, { once: true })
}

function handlePointerMove(event) {
  if (!dragPreview.value.visible) return
  dragPreview.value = {
    ...dragPreview.value,
    y: event.clientY - dragPreview.value.offsetY
  }
  const targetRow = document.elementFromPoint(event.clientX, event.clientY)?.closest?.('.tree-row')
  dragTargetId.value = targetRow?.dataset?.categoryId || null
}

function finishPointerDrag() {
  document.removeEventListener('pointermove', handlePointerMove)
  if (dragSourceId.value && dragTargetId.value && String(dragSourceId.value) !== String(dragTargetId.value)) {
    const nextTree = moveNodeWithinParent(props.tree, dragSourceId.value, dragTargetId.value)
    if (nextTree) emit('reorder', nextTree)
  }
  dragSourceId.value = null
  dragTargetId.value = null
  draggingId.value = null
  dragPreview.value = {
    visible: false,
    name: '',
    left: 0,
    y: 0,
    width: 0,
    paddingLeft: 10,
    offsetY: 0
  }
}

function renderDragPreviewContent() {
  return dragPreview.value.name
}

function moveNodeWithinParent(tree, sourceId, targetId) {
  const sourcePath = findNodePath(tree, sourceId)
  const targetPath = findNodePath(tree, targetId)
  if (!sourcePath || !targetPath) return null
  if (sourcePath.parentKey !== targetPath.parentKey) return null

  const nextTree = cloneTree(tree)
  const nextSourcePath = findNodePath(nextTree, sourceId)
  const nextTargetPath = findNodePath(nextTree, targetId)
  const siblings = nextSourcePath.siblings
  const [moved] = siblings.splice(nextSourcePath.index, 1)
  const targetIndex = siblings.findIndex((item) => String(item.id) === String(targetId))
  siblings.splice(targetIndex < 0 ? nextTargetPath.index : targetIndex, 0, moved)
  return nextTree
}

function findNodePath(nodes, id, parentId = null) {
  for (let index = 0; index < (nodes || []).length; index += 1) {
    const node = nodes[index]
    if (String(node.id) === String(id)) {
      return { node, siblings: nodes, index, parentKey: String(parentId ?? 'root') }
    }
    const childPath = findNodePath(node.children || [], id, node.id)
    if (childPath) return childPath
  }
  return null
}

function cloneTree(nodes) {
  return (nodes || []).map((node) => ({
    ...node,
    children: cloneTree(node.children || [])
  }))
}
</script>
