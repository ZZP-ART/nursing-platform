<template>
  <view class="home-page">
    <view class="bg-glow bg-glow-top" />
    <view class="bg-glow bg-glow-mid" />

    <view class="home-header">
      <view>
        <text class="eyebrow">智慧护理 · 温暖到家</text>
        <text class="page-title">今天想为家人准备什么服务？</text>
      </view>
    </view>

    <view class="search-bar" @click="goSearch">
      <view class="search-icon"><u-icon name="search" size="20" color="#3A7BF7" /></view>
      <text class="search-placeholder">搜索护理服务、健康照护...</text>
      <view class="voice-icon"><u-icon name="volume" size="18" color="#6B7B8D" /></view>
    </view>

    <view class="hero-card">
      <view class="hero-copy">
        <text class="hero-kicker">专业团队 · 安心守护</text>
        <text class="hero-title">把细致护理，送到家门口</text>
        <text class="hero-desc">线上选服务，预约更省心</text>
        <view class="hero-action" @click="goService">
          <text>查看全部服务</text>
          <u-icon name="arrow-right" size="14" color="#FFFFFF" />
        </view>
      </view>
      <view class="hero-ring hero-ring-one" />
      <view class="hero-ring hero-ring-two" />
      <view class="hero-heart"><u-icon name="heart-fill" size="42" color="rgba(255,255,255,0.88)" /></view>
    </view>

    <view class="quick-panel">
      <view class="section-heading">
        <view>
          <text class="section-title">服务分类</text>
          <text class="section-subtitle">按需求快速找到合适的照护</text>
        </view>
        <text class="section-link" @click="goService">全部</text>
      </view>
      <view class="category-grid">
        <view
          v-for="(cat, index) in serviceStore.categories.slice(0, 4)"
          :key="cat.categoryId"
          class="category-item"
          @click="onCategoryClick(cat)"
        >
          <view class="category-icon-wrap" :class="`tone-${index}`">
            <image
              v-if="cat.icon && !failedCategoryIds.has(cat.categoryId)"
              :src="cat.icon"
              class="category-icon-img"
              mode="aspectFit"
              @error="handleCategoryImageError(cat.categoryId)"
            />
            <u-icon v-else :name="['heart', 'home', 'account', 'clock'][index]" size="24" color="#3A7BF7" />
          </view>
          <text class="category-name">{{ cat.name }}</text>
        </view>
        <template v-if="!serviceStore.categories.length">
          <view v-for="i in 4" :key="`placeholder-${i}`" class="category-item">
            <view class="category-icon-wrap tone-0"><u-icon name="heart" size="24" color="#3A7BF7" /></view>
            <text class="category-name">护理服务</text>
          </view>
        </template>
      </view>
    </view>

    <view class="service-section">
      <view class="section-heading">
        <view>
          <text class="section-title">热门服务</text>
          <text class="section-subtitle">精选护理项目，随时为您准备</text>
        </view>
        <text class="section-link" @click="goService">更多</text>
      </view>

      <view v-if="serviceStore.loading" class="skeleton-list">
        <view v-for="i in 3" :key="i" class="skeleton-card">
          <view class="skeleton-line skeleton-title" />
          <view class="skeleton-line skeleton-desc" />
          <view class="skeleton-line skeleton-short" />
        </view>
      </view>
      <empty-state
        v-else-if="serviceStore.services.length === 0"
        title="暂时没有服务"
        description="稍后再来看看吧"
      />
      <view v-else class="service-list">
        <service-card
          v-for="item in serviceStore.services.slice(0, 3)"
          :key="item.itemId"
          :data="item"
          @click="goDetail(item.itemId)"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useServiceStore } from '@/store/service.js'
import ServiceCard from '@/components/base/service-card.vue'
import EmptyState from '@/components/base/empty-state.vue'

const serviceStore = useServiceStore()
const failedCategoryIds = ref(new Set())

onMounted(async () => {
  await serviceStore.fetchCategories()
  await serviceStore.fetchServices()
})

function onCategoryClick(cat) {
  serviceStore.setActiveCategory(cat.categoryId)
  uni.switchTab({ url: '/pages/service/service' })
}

function handleCategoryImageError(categoryId) {
  failedCategoryIds.value = new Set([...failedCategoryIds.value, categoryId])
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}

function goService() {
  uni.switchTab({ url: '/pages/service/service' })
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/service-detail/service-detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  padding: 28rpx $spacing-base 48rpx;
  background: $page-gradient;
  position: relative;
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80rpx);
  pointer-events: none;
}

.bg-glow-top { width: 420rpx; height: 420rpx; top: -150rpx; right: -100rpx; background: rgba(0, 194, 255, 0.16); }
.bg-glow-mid { width: 320rpx; height: 320rpx; top: 780rpx; left: -160rpx; background: rgba(58, 123, 247, 0.12); }

.home-header,
.search-bar,
.hero-card,
.quick-panel,
.service-section { position: relative; z-index: 1; }

.home-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 28rpx; }
.eyebrow { display: block; color: $primary-color; font-size: $font-size-sm; font-weight: 600; letter-spacing: 1rpx; }
.page-title { display: block; max-width: 580rpx; margin-top: 10rpx; color: $text-color; font-size: 38rpx; font-weight: 700; line-height: 1.35; }

.search-bar { height: $touch-min; display: flex; align-items: center; padding: 0 18rpx; border: $glass-border-soft; border-radius: $radius-round; background: $glass-bg-strong; box-shadow: $shadow-sm; backdrop-filter: $glass-blur; }
.search-icon { width: 54rpx; height: 54rpx; display: flex; align-items: center; justify-content: center; border-radius: 20rpx; background: $primary-bg; }
.search-placeholder { flex: 1; margin-left: 14rpx; color: $text-color-hint; font-size: $font-size-sm; }
.voice-icon { width: 54rpx; height: 54rpx; display: flex; align-items: center; justify-content: center; border-left: 1rpx solid $divider-color; }

.hero-card { min-height: 250rpx; margin-top: 28rpx; padding: 34rpx; overflow: hidden; border-radius: 32rpx; background: linear-gradient(130deg, #245ddc, #3a7bf7 48%, #00b8d8); box-shadow: $shadow-float; color: #fff; }
.hero-copy { position: relative; z-index: 2; }
.hero-kicker { display: block; font-size: $font-size-xs; opacity: 0.8; letter-spacing: 2rpx; }
.hero-title { display: block; max-width: 440rpx; margin-top: 16rpx; font-size: 38rpx; font-weight: 700; line-height: 1.35; }
.hero-desc { display: block; margin-top: 10rpx; font-size: $font-size-sm; opacity: 0.78; }
.hero-action { display: flex; align-items: center; gap: 8rpx; width: fit-content; margin-top: 28rpx; padding: 14rpx 22rpx; border: 1rpx solid rgba(255,255,255,0.34); border-radius: $radius-round; background: rgba(255,255,255,0.14); font-size: $font-size-sm; }
.hero-ring { position: absolute; border: 2rpx solid rgba(255,255,255,0.18); border-radius: 50%; }
.hero-ring-one { width: 380rpx; height: 380rpx; top: -150rpx; right: -90rpx; }
.hero-ring-two { width: 230rpx; height: 230rpx; top: -74rpx; right: -15rpx; }
.hero-heart { position: absolute; right: 72rpx; bottom: 44rpx; display: flex; align-items: center; justify-content: center; width: 88rpx; height: 88rpx; border-radius: 50%; background: rgba(255,255,255,0.14); }

.quick-panel, .service-section { margin-top: 30rpx; }
.quick-panel { padding: 28rpx 24rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $glass-bg-strong; box-shadow: $shadow-sm; backdrop-filter: $glass-blur; }
.section-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.section-title { display: block; color: $text-color; font-size: 32rpx; font-weight: 700; }
.section-subtitle { display: block; margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }
.section-link { color: $primary-color; font-size: $font-size-sm; font-weight: 600; }
.category-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18rpx 8rpx; }
.category-item { display: flex; flex-direction: column; align-items: center; min-height: 126rpx; transition: transform $transition-fast; }
.category-item:active { transform: scale(0.95); }
.category-icon-wrap { width: 78rpx; height: 78rpx; display: flex; align-items: center; justify-content: center; border-radius: 26rpx; background: $primary-bg; }
.category-icon-wrap.tone-1 { background: #e9fbf7; }
.category-icon-wrap.tone-2 { background: #fff2eb; }
.category-icon-wrap.tone-3 { background: #f0edff; }
.category-icon-img { width: 46rpx; height: 46rpx; }
.category-name { margin-top: 12rpx; color: $text-color-secondary; font-size: $font-size-xs; }
.service-list { display: flex; flex-direction: column; }

.skeleton-card { margin-bottom: 18rpx; padding: 28rpx; border-radius: $radius-lg; background: $glass-bg-strong; }
.skeleton-line { height: 24rpx; margin-bottom: 16rpx; border-radius: 8rpx; background: linear-gradient(90deg, #edf2f8 25%, #dfe8f3 50%, #edf2f8 75%); background-size: 200% 100%; animation: shimmer 1.5s ease-in-out infinite; }
.skeleton-title { width: 55%; height: 34rpx; }
.skeleton-desc { width: 88%; }
.skeleton-short { width: 32%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>
