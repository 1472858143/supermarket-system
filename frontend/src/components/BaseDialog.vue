<template>
  <Teleport to="body">
    <div v-if="modelValue" class="dialog-mask">
      <section class="dialog">
        <header class="dialog-header">
          <div class="dialog-title">{{ title }}</div>
          <button class="btn btn-ghost" type="button" @click="$emit('update:modelValue', false)">×</button>
        </header>
        <div class="dialog-body">
          <slot />
        </div>
        <footer class="dialog-footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    default: '操作'
  }
})

defineEmits(['update:modelValue'])
</script>
