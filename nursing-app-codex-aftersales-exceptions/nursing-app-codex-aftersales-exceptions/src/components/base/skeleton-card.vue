<template>
  <view class="skeleton-list">
    <view v-for="i in count" :key="i" class="skeleton-card">
      <view class="skeleton-line skeleton-title" />
      <view class="skeleton-line skeleton-desc" />
      <view class="skeleton-line skeleton-short" />
    </view>
  </view>
</template>

<script setup>
defineProps({
  count: { type: Number, default: 3 },
})
</script>

<style lang="scss" scoped>
.skeleton-list {
  padding: 0;
}

.skeleton-card {
  background: $glass-bg-strong;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 18rpx;
  border: $glass-border-soft;
}

.skeleton-line {
  height: 24rpx;
  background: linear-gradient(90deg, #ECF0F6 25%, #DEE4ED 50%, #ECF0F6 75%);
  background-size: 200% 100%;
  border-radius: 6rpx;
  margin-bottom: 16rpx;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-title { width: 55%; height: 34rpx; }
.skeleton-desc { width: 88%; }
.skeleton-short { width: 32%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
