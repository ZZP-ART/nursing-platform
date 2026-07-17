<template>
  <view class="role-tab-bar">
    <view
      v-for="tab in tabs"
      :key="tab.path"
      class="tab-item"
      :class="{ active: current === tab.path }"
      @click="switchTab(tab)"
    >
      <view class="icon-wrap">
        <u-icon
          :name="tab.icon"
          size="23"
          :color="current === tab.path ? activeColor : inactiveColor"
        />
        <text v-if="tab.badge" class="tab-badge">{{ formatBadge(tab.badge) }}</text>
      </view>
      <text class="tab-label">{{ tab.label }}</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  tabs: { type: Array, required: true },
  current: { type: String, required: true },
  activeColor: { type: String, default: '#3A7BF7' },
  inactiveColor: { type: String, default: '#98A5B3' },
})

function formatBadge(value) {
  const count = Number(value)
  return count > 99 ? '99+' : String(value)
}

function switchTab(tab) {
  if (tab.path === props.current) return
  uni.redirectTo({
    url: tab.path,
    fail: () => uni.showToast({ title: '页面暂时无法打开', icon: 'none' }),
  })
}
</script>

<style lang="scss" scoped>
.role-tab-bar { position: fixed; left: 0; right: 0; bottom: 0; z-index: 999; display: flex; height: calc(104rpx + env(safe-area-inset-bottom)); padding-bottom: env(safe-area-inset-bottom); border-top: 1rpx solid rgba(228,233,240,.86); background: rgba(249,251,255,.94); backdrop-filter: $glass-blur; box-shadow: 0 -8rpx 28rpx rgba(42,91,170,.08); }
.tab-item { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 5rpx; color: $text-color-hint; transition: color $transition-fast; }.tab-item.active { color: $primary-color; }
.icon-wrap { position: relative; display: flex; align-items: center; justify-content: center; min-width: 56rpx; height: 44rpx; }.tab-label { font-size: $font-size-xs; }
.tab-badge { position: absolute; top: -5rpx; left: 34rpx; min-width: 30rpx; height: 30rpx; padding: 0 8rpx; border: 3rpx solid #fff; border-radius: $radius-round; background: $error-color; color: #fff; font-size: 17rpx; line-height: 24rpx; text-align: center; }
</style>
