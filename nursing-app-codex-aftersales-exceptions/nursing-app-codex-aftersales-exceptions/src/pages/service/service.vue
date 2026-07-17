<template>
  <view class="service-page">
    <view class="bg-glow bg-glow-top" />

    <view class="page-header">
      <view>
        <text class="eyebrow">护理服务库</text>
        <text class="page-title">找到适合家人的照护</text>
      </view>
      <view class="header-icon" @click="goSearch"><u-icon name="search" size="22" color="#3A7BF7" /></view>
    </view>

    <view class="search-bar" @click="goSearch">
      <u-icon name="search" size="20" color="#3A7BF7" />
      <text class="search-placeholder">搜索护理服务、健康照护...</text>
      <u-icon name="arrow-right" size="16" color="#98A5B3" />
    </view>

    <view class="filter-panel">
      <view class="filter-heading">
        <text class="filter-title">服务分类</text>
        <text class="filter-count">已加载 {{ serviceStore.total }} 项</text>
      </view>
      <scroll-view scroll-x :show-scrollbar="false">
        <view class="tab-list">
          <view
            class="tab-item all-tab"
            :class="{ active: !serviceStore.activeCategoryId }"
            @click="onCategoryChange(null)"
          >全部</view>
          <view
            v-for="cat in serviceStore.categories"
            :key="cat.categoryId"
            class="tab-item"
            :class="{ active: serviceStore.activeCategoryId === cat.categoryId }"
            @click="onCategoryChange(cat.categoryId)"
          >{{ cat.name }}</view>
        </view>
      </scroll-view>
    </view>

    <view class="service-list">
      <view v-if="serviceStore.loading && serviceStore.services.length === 0" class="skeleton-list">
        <view v-for="i in 3" :key="i" class="skeleton-card">
          <view class="skeleton-line skeleton-title" />
          <view class="skeleton-line skeleton-desc" />
          <view class="skeleton-line skeleton-short" />
        </view>
      </view>
      <empty-state
        v-else-if="!serviceStore.loading && serviceStore.services.length === 0"
        title="暂无相关服务"
        description="换一个分类或稍后再来看看"
      />
      <view v-else>
        <service-card
          v-for="item in serviceStore.services"
          :key="item.itemId"
          :data="item"
          @click="goDetail(item.itemId)"
        />
        <view class="load-more" @click="serviceStore.hasMore && serviceStore.loadMore()">
          <u-loading-icon v-if="serviceStore.loading" size="18" color="#3A7BF7" />
          <text v-else class="load-text">{{ serviceStore.hasMore ? '加载更多服务' : '已显示全部服务' }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { onMounted } from 'vue'
import { useServiceStore } from '@/store/service.js'
import ServiceCard from '@/components/base/service-card.vue'
import EmptyState from '@/components/base/empty-state.vue'

const serviceStore = useServiceStore()

onMounted(async () => {
  if (!serviceStore.categories.length) await serviceStore.fetchCategories()
  if (!serviceStore.services.length) await serviceStore.fetchServices()
})

function onCategoryChange(categoryId) {
  serviceStore.setActiveCategory(categoryId)
  serviceStore.fetchServices({ ...(categoryId ? { categoryId } : {}), page: 1 })
}

function goSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/service-detail/service-detail?id=${id}` })
}
</script>

<style lang="scss" scoped>
.service-page { min-height: 100vh; padding: 28rpx $spacing-base 48rpx; background: $page-gradient; position: relative; overflow: hidden; }
.bg-glow { position: absolute; width: 420rpx; height: 420rpx; top: -120rpx; right: -160rpx; border-radius: 50%; background: rgba(0,194,255,0.14); filter: blur(80rpx); pointer-events: none; }
.page-header, .search-bar, .filter-panel, .service-list { position: relative; z-index: 1; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 26rpx; }
.eyebrow { display: block; color: $primary-color; font-size: $font-size-sm; font-weight: 600; }
.page-title { display: block; margin-top: 10rpx; color: $text-color; font-size: 38rpx; font-weight: 700; }
.header-icon { width: 76rpx; height: 76rpx; display: flex; align-items: center; justify-content: center; border: $glass-border-soft; border-radius: 28rpx; background: $glass-bg-strong; box-shadow: $shadow-sm; }
.search-bar { height: $touch-min; display: flex; align-items: center; padding: 0 24rpx; border: $glass-border-soft; border-radius: $radius-round; background: $glass-bg-strong; box-shadow: $shadow-sm; backdrop-filter: $glass-blur; }
.search-placeholder { flex: 1; margin: 0 16rpx; color: $text-color-hint; font-size: $font-size-sm; }
.filter-panel { margin-top: 28rpx; padding: 26rpx 24rpx 22rpx; border: $glass-border-soft; border-radius: 28rpx; background: $glass-bg-strong; box-shadow: $shadow-sm; backdrop-filter: $glass-blur; }
.filter-heading { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 20rpx; }
.filter-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }
.filter-count { color: $text-color-hint; font-size: $font-size-xs; }
.tab-list { display: flex; }
.tab-item { flex-shrink: 0; margin-right: 14rpx; padding: 14rpx 24rpx; border: 1rpx solid transparent; border-radius: $radius-round; background: $bg-color-grey; color: $text-color-secondary; font-size: $font-size-sm; transition: all $transition-base; }
.tab-item:active { transform: scale(0.96); }
.tab-item.active { border-color: transparent; background: $primary-gradient; color: #fff; box-shadow: 0 6rpx 18rpx rgba(58,123,247,0.22); font-weight: 600; }
.service-list { margin-top: 28rpx; }
.skeleton-card { margin-bottom: 18rpx; padding: 28rpx; border-radius: $radius-lg; background: $glass-bg-strong; }
.skeleton-line { height: 24rpx; margin-bottom: 16rpx; border-radius: 8rpx; background: linear-gradient(90deg, #edf2f8 25%, #dfe8f3 50%, #edf2f8 75%); background-size: 200% 100%; animation: shimmer 1.5s ease-in-out infinite; }
.skeleton-title { width: 55%; height: 34rpx; }
.skeleton-desc { width: 88%; }
.skeleton-short { width: 32%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.load-more { display: flex; align-items: center; justify-content: center; min-height: 88rpx; color: $primary-color; font-size: $font-size-sm; }
.load-text { color: $text-color-hint; }
</style>
