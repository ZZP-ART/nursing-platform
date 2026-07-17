<template>
  <view class="service-card" :class="{ 'has-shadow': shadow }" @click="handleClick">
    <view class="card-body">
      <view class="cover-wrap">
        <image
          v-if="data.coverImage && !imageError"
          :src="data.coverImage"
          class="card-cover"
          mode="aspectFill"
          @error="imageError = true"
        />
        <view v-else class="cover-placeholder"><u-icon name="heart-fill" size="28" color="#3A7BF7" /></view>
        <view class="cover-badge"><u-icon name="checkmark-circle-fill" size="12" color="#00B8A9" /> 安心服务</view>
      </view>
      <view class="card-info">
        <view class="card-title-row">
          <text class="card-name">{{ data.name }}</text>
          <u-icon name="arrow-right" size="15" color="#98A5B3" />
        </view>
        <text class="card-desc">{{ data.description }}</text>
        <view v-if="data.specs && data.specs.length" class="card-meta">
          <text v-for="spec in data.specs.slice(0, 2)" :key="spec.specId || spec.id" class="card-spec">{{ spec.name }}</text>
        </view>
        <view class="card-footer">
          <view class="price-label">¥<text class="price-num">{{ data.minPrice || (data.specs && data.specs[0]?.price) || '--' }}</text> 起</view>
          <view class="duration"><u-icon name="clock" size="13" color="#98A5B3" /> <text>{{ data.specs?.[0]?.duration || '--' }}分钟</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  data: {
    type: Object,
    required: true,
  },
  shadow: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['click'])
const imageError = ref(false)

function handleClick() {
  emit('click')
}
</script>

<style lang="scss" scoped>
.service-card {
  background: $surface-gradient;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  border-radius: 28rpx;
  margin-bottom: 18rpx;
  padding: 20rpx;
  border: $glass-border-soft;
  transition: transform $transition-fast, box-shadow $transition-fast;

  &.has-shadow {
    box-shadow: 0 4rpx 20rpx rgba(58, 123, 247, 0.06);
  }

  &:active {
    transform: scale(0.98);
    box-shadow: 0 2rpx 12rpx rgba(58, 123, 247, 0.1);
  }
}

.card-body {
  display: flex;
}

.cover-wrap { position: relative; width: 170rpx; height: 170rpx; margin-right: 20rpx; flex-shrink: 0; }

.card-cover {
  width: 170rpx;
  height: 170rpx;
  border-radius: 22rpx;
  background-color: $bg-color-grey;
}

.cover-placeholder { width: 170rpx; height: 170rpx; display: flex; align-items: center; justify-content: center; border-radius: 22rpx; background: linear-gradient(145deg, #eaf2ff, #dff9fa); }
.cover-badge { position: absolute; left: 10rpx; bottom: 10rpx; display: flex; align-items: center; gap: 4rpx; padding: 5rpx 10rpx; border-radius: $radius-round; background: rgba(255,255,255,0.86); color: $success-color; font-size: 18rpx; }

.card-info {
  flex: 1;
  margin-right: 16rpx;
  min-width: 0;
}

.card-name {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  font-weight: 600;
  color: $text-color;
  line-height: 1.4;
}

.card-title-row { display: flex; align-items: center; gap: 8rpx; }

.card-desc {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  font-size: 24rpx;
  color: $text-color-secondary;
  margin-top: 8rpx;
  line-height: 1.5;
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 10rpx;
  gap: 8rpx;
}

.card-spec {
  font-size: 20rpx;
  color: $primary-color;
  background-color: $primary-bg;
  padding: 4rpx 12rpx;
  border-radius: 6rpx;
  font-weight: 500;
}

.card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 16rpx; }
.card-price { display: flex; flex-direction: column; align-items: flex-end; justify-content: center; min-width: 100rpx; flex-shrink: 0; }

.price-num {
  font-size: 38rpx;
  font-weight: 700;
  color: $warning-color;
  line-height: 1.1;
}

.price-label {
  color: $warning-color;
  font-size: 22rpx;
  font-weight: 600;
}

.duration { display: flex; align-items: center; gap: 4rpx; color: $text-color-hint; font-size: 20rpx; }
</style>
