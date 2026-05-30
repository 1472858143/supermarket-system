<template>
  <button v-if="visible" class="btn" :class="buttonClass" type="button" :disabled="disabled" @click="$emit('click')">
    <span v-if="icon">{{ icon }}</span>
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { hasAnyRole } from '../utils/permission'

const props = defineProps({
  roles: {
    type: Array,
    default: () => []
  },
  buttonClass: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  icon: {
    type: String,
    default: ''
  }
})

defineEmits(['click'])

const authStore = useAuthStore()
const visible = computed(() => hasAnyRole(authStore.roles, props.roles))
</script>
