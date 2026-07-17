<template>
  <view class="empty-state">
    <u-icon :name="icon" :size="size" :color="iconColor" />
    <text class="empty-title">{{ title }}</text>
    <text v-if="description" class="empty-desc">{{ description }}</text>
    <slot />
  </view>
</template>

<script setup>
defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  icon: { type: String, default: 'frown' },
  size: { type: [Number, String], default: 80 },
  iconColor: { type: String, default: '#C5CDD8' },
})
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 16rpx 0;
  padding: 92rpx 24rpx;
  border: $glass-border-soft;
  border-radius: 28rpx;
  background: $glass-bg-strong;
  box-shadow: $shadow-sm;
  backdrop-filter: $glass-blur;
}

.empty-title {
  font-size: $font-size-base;
  color: $text-color-hint;
  margin-top: 24rpx;
}

.empty-desc {
  font-size: $font-size-sm;
  color: $text-color-disabled;
  margin-top: 8rpx;
}
</style>
