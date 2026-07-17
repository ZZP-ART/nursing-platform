<template>
  <article class="stat-card" :style="{ '--accent': color }">
    <div class="stat-icon"><slot name="icon">{{ icon }}</slot></div>
    <div><span class="stat-label">{{ label }}</span><strong class="stat-value">{{ value }}</strong><span class="stat-note">{{ note }}</span></div>
  </article>
</template>

<script setup>
defineProps({ label: String, value: [String, Number], note: String, icon: String, color: { type: String, default: '#3156d3' } })
</script>
