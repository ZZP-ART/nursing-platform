<template>
  <view v-if="service" class="detail-page">
    <!-- 背景弥散光 -->
    <view class="bg-glow bg-glow-top" />

    <view class="cover-stage">
      <image v-if="service.coverImage" :src="service.coverImage" class="detail-cover" mode="aspectFill" />
      <view v-else class="cover-placeholder"><u-icon name="heart-fill" size="58" color="#3A7BF7" /></view>
      <view class="cover-label"><u-icon name="checkmark-circle-fill" size="14" color="#00B8A9" /> 专业护理服务</view>
    </view>

    <!-- 服务名称 & 价格 -->
    <view class="detail-header">
      <view class="title-row">
        <text class="detail-name">{{ service.name }}</text>
        <view class="favorite-icon"><u-icon name="heart" size="20" color="#3A7BF7" /></view>
      </view>
      <text v-if="service.categoryName" class="detail-category">{{ service.categoryName }}</text>
      <view class="detail-price">
        <text class="price-num">¥{{ service.minPrice }}</text>
        <text class="price-unit">起</text>
      </view>
    </view>

    <!-- 规格选择 -->
    <view v-if="service.specs?.length" class="detail-section">
      <text class="section-title">服务规格</text>
      <view class="specs-list">
        <view
          v-for="spec in service.specs"
          :key="spec.specId"
          class="spec-item"
          :class="{ active: selectedSpec?.specId === spec.specId }"
          @click="selectSpec(spec)"
        >
          <view class="spec-info">
            <text class="spec-name">{{ spec.name }}</text>
            <text class="spec-duration">⏱ {{ spec.duration }}分钟</text>
          </view>
          <view class="spec-price">
            <text class="spec-current">¥{{ spec.price }}</text>
            <text v-if="spec.originalPrice && spec.originalPrice > spec.price" class="spec-original">¥{{ spec.originalPrice }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 服务介绍 -->
    <view v-if="service.description" class="detail-section">
      <text class="section-title">服务介绍</text>
      <view class="description-content">
        <text class="desc-text">{{ service.description }}</text>
      </view>
    </view>

    <!-- 图片展示 -->
    <view v-if="service.images?.length" class="detail-section">
      <text class="section-title">服务展示</text>
      <scroll-view scroll-x class="images-scroll" :show-scrollbar="false">
        <image
          v-for="(img, i) in service.images"
          :key="i"
          :src="img"
          class="detail-image"
          mode="aspectFill"
          @click="previewImage(i)"
        />
      </scroll-view>
    </view>

    <!-- 用户评价 -->
    <view v-if="reviews.length" class="detail-section">
      <text class="section-title">用户评价 ({{ reviewTotal }})</text>
      <view v-for="review in reviews" :key="review.reviewId" class="review-item">
        <view class="review-header">
          <text class="review-user">{{ review.userNickname }}</text>
          <view class="review-stars">
            <text v-for="s in 5" :key="s" class="star" :class="{ filled: s <= review.rating }">★</text>
          </view>
        </view>
        <text class="review-content">{{ review.content }}</text>
        <text class="review-time">{{ review.createTime }}</text>
      </view>
    </view>

    <!-- 底部预约栏 — 玻璃拟态 -->
    <view class="bottom-bar">
      <view v-if="selectedSpec" class="bottom-info">
        <text class="bottom-spec">{{ selectedSpec.name }}</text>
        <text class="bottom-price">¥{{ selectedSpec.price }}</text>
      </view>
      <view v-else class="bottom-info">
        <text class="bottom-spec">请选择规格</text>
      </view>
      <u-button type="primary" shape="round" size="large" class="book-btn" @click="handleBook">
        立即预约
      </u-button>
    </view>
  </view>

  <!-- 加载中 -->
  <view v-else class="loading-page">
    <u-loading-icon size="40" />
    <text class="loading-text">加载中...</text>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useServiceStore } from '@/store/service.js'
import { useUserStore } from '@/store/user.js'
import http from '@/utils/request.js'

const serviceStore = useServiceStore()
const userStore = useUserStore()
const service = ref(null)
const selectedSpec = ref(null)
const reviews = ref([])
const reviewTotal = ref(0)

onLoad(async (options) => {
  const { id } = options
  if (id) {
    try {
      service.value = await serviceStore.fetchServiceDetail(String(id))
      if (service.value?.specs?.length) {
        selectedSpec.value = service.value.specs[0]
      }
      if (userStore.isLoggedIn) loadReviews(id)
    } catch {
      uni.showToast({ title: '服务不存在', icon: 'none' })
    }
  }
})

async function loadReviews(itemId) {
  try {
    const res = await http.get('/api/v1/reviews', { itemId: String(itemId), page: 1, size: 5 })
    reviews.value = res.data?.list || []
    reviewTotal.value = res.data?.total || 0
  } catch {
    // 评价加载失败不影响详情展示
  }
}

function selectSpec(spec) {
  selectedSpec.value = spec
}

function previewImage(index) {
  uni.previewImage({
    urls: service.value.images,
    current: index,
  })
}

function handleBook() {
  if (!userStore.isLoggedIn) {
    uni.navigateTo({ url: '/pages/login/login' })
    return
  }
  if (!selectedSpec.value) {
    uni.showToast({ title: '请选择服务规格', icon: 'none' })
    return
  }
  uni.navigateTo({
    url: `/pages/booking/booking?itemId=${service.value.itemId}&specId=${selectedSpec.value.specId}`,
  })
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background-color: $bg-color-grey;
  padding-bottom: 180rpx;
  position: relative;
  overflow: hidden;
}

/* 背景弥散光 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80rpx);
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
}
.bg-glow-top {
  width: 380rpx;
  height: 380rpx;
  background: radial-gradient(circle, rgba(58, 123, 247, 0.16), transparent 70%);
  top: 300rpx;
  right: -80rpx;
}

/* 封面 */
.cover-stage { position: relative; }
.detail-cover {
  width: 100%;
  height: 420rpx;
  background-color: $bg-color-grey;
}
.cover-placeholder { width: 100%; height: 420rpx; display: flex; align-items: center; justify-content: center; background: linear-gradient(145deg, #eaf2ff, #dff9fa); }
.cover-label { position: absolute; left: 28rpx; bottom: 24rpx; display: flex; align-items: center; gap: 6rpx; padding: 9rpx 16rpx; border-radius: $radius-round; background: rgba(255,255,255,0.88); color: $success-color; font-size: $font-size-xs; }

/* 头部信息 */
.detail-header {
  margin: -24rpx $spacing-base 0;
  position: relative;
  z-index: 2;
  padding: 28rpx 24rpx;
  border: $glass-border-soft;
  border-radius: 28rpx;
  background: $glass-bg-strong;
  box-shadow: $shadow-float;
  backdrop-filter: $glass-blur;
}
.title-row { display: flex; align-items: flex-start; gap: 18rpx; }
.favorite-icon { width: 66rpx; height: 66rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; border-radius: 22rpx; background: $primary-bg; }

.detail-name {
  flex: 1;
  font-size: $font-size-xl;
  font-weight: 700;
  color: $text-color;
  line-height: 1.4;
}

.detail-category {
  font-size: $font-size-sm;
  color: $primary-color;
  background-color: $primary-bg;
  padding: 6rpx 18rpx;
  border-radius: 6rpx;
  margin-top: 16rpx;
  display: inline-block;
  font-weight: 500;
}

.detail-price {
  margin-top: 20rpx;
}

.price-num {
  font-size: 52rpx;
  font-weight: 700;
  color: $warning-color;
}

.price-unit {
  font-size: $font-size-base;
  color: $text-color-hint;
  margin-left: 6rpx;
}

/* 版块卡片 */
.detail-section {
  background: $surface-gradient;
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  margin: 18rpx $spacing-base 0;
  border: $glass-border-soft;
  border-radius: 28rpx;
  padding: $spacing-lg $spacing-base;
  box-shadow: 0 2rpx 16rpx rgba(58, 123, 247, 0.04);
}

.section-title {
  font-size: $font-size-md;
  font-weight: 600;
  color: $text-color;
  margin-bottom: 20rpx;
  display: block;
  position: relative;
  padding-left: 16rpx;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 6rpx;
    height: 24rpx;
    background: $primary-gradient;
    border-radius: 3rpx;
  }
}

/* 规格 */
.specs-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.spec-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 20rpx;
  border: 2rpx solid $border-color;
  border-radius: $radius-base;
  transition: all $transition-base;
  min-height: $touch-min;

  &:active {
    transform: scale(0.98);
  }

  &.active {
    border-color: $primary-color;
    background-color: $primary-bg;
    box-shadow: 0 4rpx 16rpx rgba(58, 123, 247, 0.1);

    .spec-current {
      color: $primary-color;
    }
  }
}

.spec-info {
  display: flex;
  flex-direction: column;
}

.spec-name {
  font-size: $font-size-base;
  font-weight: 500;
  color: $text-color;
}

.spec-duration {
  font-size: $font-size-xs;
  color: $text-color-hint;
  margin-top: 6rpx;
}

.spec-price {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
}

.spec-current {
  font-size: 34rpx;
  font-weight: 700;
  color: $warning-color;
  transition: color $transition-base;
}

.spec-original {
  font-size: 22rpx;
  color: $text-color-disabled;
  text-decoration: line-through;
}

/* 服务介绍 */
.desc-text {
  font-size: $font-size-base;
  color: $text-color-secondary;
  line-height: 1.9;
  white-space: pre-line;
}

/* 图片展示 */
.images-scroll {
  white-space: nowrap;
}

.detail-image {
  width: 300rpx;
  height: 200rpx;
  border-radius: 12rpx;
  margin-right: 16rpx;
  display: inline-block;
  background-color: $bg-color-grey;
}

/* 评价 */
.review-item {
  padding: 24rpx 0;
  border-bottom: 1rpx solid $divider-color;

  &:last-child {
    border-bottom: none;
  }
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-user {
  font-size: $font-size-base;
  font-weight: 500;
  color: $text-color;
}

.review-stars {
  display: flex;
}

.star {
  font-size: 26rpx;
  color: #E4E9F0;

  &.filled {
    color: $warning-color;
  }
}

.review-content {
  display: block;
  font-size: $font-size-base;
  color: $text-color-secondary;
  margin-top: 10rpx;
  line-height: 1.7;
}

.review-time {
  display: block;
  font-size: $font-size-xs;
  color: $text-color-disabled;
  margin-top: 10rpx;
}

/* 底部栏 — 玻璃拟态 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 18rpx $spacing-base;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(249,251,255,0.88);
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.06);
  border-top: $glass-border-soft;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bottom-info {
  display: flex;
  flex-direction: column;
}

.bottom-spec {
  font-size: $font-size-sm;
  color: $text-color-secondary;
}

.bottom-price {
  font-size: 40rpx;
  font-weight: 700;
  color: $warning-color;
  margin-top: 4rpx;
}

.book-btn {
  background: $primary-gradient !important;
  border: none !important;
  box-shadow: 0 6rpx 24rpx rgba(58, 123, 247, 0.3);
  font-weight: 600 !important;
  min-width: 220rpx;
  transition: transform $transition-fast;

  &:active {
    transform: scale(0.96);
  }
}

/* 加载页 */
.loading-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background-color: $bg-color-grey;
}

.loading-text {
  margin-top: 24rpx;
  font-size: $font-size-base;
  color: $text-color-hint;
}
</style>
