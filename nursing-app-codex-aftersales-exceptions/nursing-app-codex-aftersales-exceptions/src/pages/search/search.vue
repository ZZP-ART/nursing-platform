<template>
  <view class="search-page">
    <view class="bg-glow" />
    <view class="search-header">
      <view class="search-input-wrap">
        <u-icon name="search" size="20" color="#3A7BF7" />
        <input v-model="keyword" class="search-input" placeholder="搜索护理服务..." focus @confirm="onSearch" />
        <u-icon v-if="keyword" name="close-circle-fill" size="18" color="#98A5B3" @click="clearSearch" />
      </view>
      <text class="search-cancel" @click="goBack">取消</text>
    </view>

    <view v-if="!keyword && history.length" class="history-section glass-panel">
      <view class="history-header">
        <view><text class="section-title">最近搜索</text><text class="section-subtitle">继续查找您关心的服务</text></view>
        <view class="clear-history" @click="clearHistory"><u-icon name="trash" size="17" color="#98A5B3" /></view>
      </view>
      <view class="history-list">
        <text v-for="(item, index) in history" :key="index" class="history-tag" @click="onHistoryClick(item)">{{ item }}</text>
      </view>
    </view>

    <view v-if="keyword" class="result-section">
      <view v-if="searching" class="searching"><u-loading-icon size="26" color="#3A7BF7" /><text>正在为您查找...</text></view>
      <empty-state v-else-if="results.length === 0" title="没有找到相关服务" description="换个关键词试试，我们会继续为您寻找" />
      <view v-else>
        <view class="result-heading"><text class="section-title">搜索结果</text><text class="result-count">已找到 {{ total }} 项</text></view>
        <service-card v-for="item in results" :key="item.itemId" :data="item" @click="goDetail(item.itemId)" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useServiceStore } from '@/store/service.js'
import ServiceCard from '@/components/base/service-card.vue'
import EmptyState from '@/components/base/empty-state.vue'

const serviceStore = useServiceStore()
const keyword = ref('')
const results = ref([])
const total = ref(0)
const searching = ref(false)
const history = ref(JSON.parse(uni.getStorageSync('searchHistory') || '[]'))

function onSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  if (!history.value.includes(kw)) {
    history.value.unshift(kw)
    if (history.value.length > 10) history.value.pop()
    uni.setStorageSync('searchHistory', JSON.stringify(history.value))
  }
  doSearch(kw)
}

async function doSearch(kw) {
  searching.value = true
  try {
    const { list, total: count } = await serviceStore.searchServices(kw)
    results.value = list
    total.value = count
  } catch {
    results.value = []
    total.value = 0
  } finally {
    searching.value = false
  }
}

function onHistoryClick(item) { keyword.value = item; onSearch() }
function clearSearch() { keyword.value = ''; results.value = []; total.value = 0 }
function clearHistory() { history.value = []; uni.removeStorageSync('searchHistory') }
function goBack() { uni.navigateBack() }
function goDetail(id) { uni.navigateTo({ url: `/pages/service-detail/service-detail?id=${id}` }) }
</script>

<style lang="scss" scoped>
.search-page { min-height: 100vh; padding: 20rpx $spacing-base 48rpx; background: $page-gradient; position: relative; overflow: hidden; }
.bg-glow { position: absolute; width: 420rpx; height: 420rpx; top: -150rpx; right: -160rpx; border-radius: 50%; background: rgba(0,194,255,0.14); filter: blur(80rpx); pointer-events: none; }
.search-header, .glass-panel, .result-section { position: relative; z-index: 1; }
.search-header { display: flex; align-items: center; gap: 18rpx; }
.search-input-wrap { flex: 1; height: $touch-min; display: flex; align-items: center; padding: 0 22rpx; border: $glass-border-soft; border-radius: $radius-round; background: $glass-bg-strong; box-shadow: $shadow-sm; backdrop-filter: $glass-blur; }
.search-input { flex: 1; min-width: 0; margin: 0 14rpx; color: $text-color; font-size: $font-size-base; }
.search-cancel { flex-shrink: 0; color: $text-color-secondary; font-size: $font-size-sm; }
.glass-panel { margin-top: 28rpx; padding: 28rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $glass-bg-strong; box-shadow: $shadow-sm; backdrop-filter: $glass-blur; }
.history-header, .result-heading { display: flex; align-items: center; justify-content: space-between; }
.section-title { display: block; color: $text-color; font-size: 32rpx; font-weight: 700; }
.section-subtitle { display: block; margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }
.clear-history { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; border-radius: 20rpx; background: $bg-color-grey; }
.history-list { display: flex; flex-wrap: wrap; gap: 14rpx; margin-top: 24rpx; }
.history-tag { padding: 12rpx 22rpx; border-radius: $radius-round; background: $bg-color-grey; color: $text-color-secondary; font-size: $font-size-sm; }
.history-tag:active { color: $primary-color; background: $primary-bg; }
.result-section { margin-top: 30rpx; }
.result-heading { margin-bottom: 20rpx; }
.result-count { color: $text-color-hint; font-size: $font-size-sm; }
.searching { display: flex; flex-direction: column; align-items: center; gap: 18rpx; padding: 160rpx 0; color: $text-color-hint; font-size: $font-size-sm; }
</style>
