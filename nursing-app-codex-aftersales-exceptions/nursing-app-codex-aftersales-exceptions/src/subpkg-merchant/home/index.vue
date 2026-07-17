<template>
  <view class="page-shell">
    <view class="page-content">
      <view class="hero-card"><view><text class="eyebrow">商户工作台</text><text class="title">{{ dashboard?.merchantName || userStore.userInfo?.nickname || '商户' }}</text><text class="subtitle">订单调度与履约监控</text></view><view class="hero-icon"><u-icon name="share" size="35" color="#FFFFFF" /></view></view>

      <view class="alert-card" @click="goOrders('dispatch')"><view class="alert-icon"><u-icon name="share" size="24" color="#FF8A5C" /></view><view class="alert-copy"><text class="alert-title">{{ dashboard?.waitingDispatch || 0 }} 个待派单事项</text><text class="alert-desc">为待服务订单选择合适的护理人员</text></view><u-icon name="arrow-right" size="16" color="#C5CDD8" /></view>

      <view class="stats-grid">
        <view class="stat-card" @click="goOrders('accept')"><text class="stat-value orange">{{ dashboard?.waitingAccept || 0 }}</text><text class="stat-label">待接单</text></view>
        <view class="stat-card" @click="goOrders('today')"><text class="stat-value blue">{{ dashboard?.todayServices || 0 }}</text><text class="stat-label">今日服务</text></view>
        <view class="stat-card" @click="goOrders('service')"><text class="stat-value green">{{ dashboard?.inService || 0 }}</text><text class="stat-label">服务中</text></view>
      </view>

      <view class="section-head"><text class="section-title">经营概览</text></view>
      <view class="overview-card"><view><text class="overview-label">累计订单</text><text class="overview-value">{{ dashboard?.totalOrders || 0 }}</text></view><view class="divider" /><view><text class="overview-label">已完成</text><text class="overview-value">{{ dashboard?.completed || 0 }}</text></view></view>

      <view class="section-head"><text class="section-title">快捷操作</text></view>
      <view class="feature-grid"><view class="feature-card" @click="goOrders('dispatch')"><view class="feature-icon"><u-icon name="share" size="26" color="#3A7BF7" /></view><text class="feature-title">派单调度</text><text class="feature-desc">处理待派单和改派订单</text></view></view>
    </view>
    <role-tab-bar :tabs="tabs" current="/subpkg-merchant/home/index" />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import RoleTabBar from '@/components/base/role-tab-bar.vue'
import { MERCHANT_TABS } from '@/constants/merchant-navigation.js'
import { ROLES } from '@/constants/roles.js'
import { useMerchantStore } from '@/store/merchant.js'
import { useUserStore } from '@/store/user.js'
import { requireRole } from '@/utils/permission.js'

const merchantStore = useMerchantStore()
const userStore = useUserStore()
const dashboard = computed(() => merchantStore.dashboard)
const tabs = computed(() => MERCHANT_TABS.map((tab) => tab.label === '订单' ? { ...tab, badge: dashboard.value?.waitingDispatch || '' } : tab))
onShow(async () => { if (!requireRole(ROLES.MERCHANT_MEMBER)) return; await merchantStore.fetchDashboard() })
function goOrders(filter) { uni.redirectTo({ url: `/subpkg-merchant/orders/index${filter ? `?filter=${filter}` : ''}` }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; background: $page-gradient; }.page-content { padding: 30rpx $spacing-base calc(140rpx + env(safe-area-inset-bottom)); }.hero-card { display: flex; align-items: center; justify-content: space-between; min-height: 230rpx; padding: 34rpx; border-radius: 34rpx; background: linear-gradient(135deg,#116b68,#00a89d 55%,#38c6b5); color: #fff; box-shadow: $shadow-float; }.eyebrow,.title,.subtitle { display: block; }.eyebrow { font-size: $font-size-sm; opacity: .78; }.title { margin-top: 12rpx; font-size: 38rpx; font-weight: 700; }.subtitle { margin-top: 12rpx; font-size: $font-size-xs; opacity: .82; }.hero-icon { position:relative;display:flex;align-items:center;justify-content:center;width:96rpx;height:96rpx;border-radius:30rpx;background:rgba(255,255,255,.18); }.hero-badge { position:absolute;right:-7rpx;top:-7rpx;min-width:31rpx;height:31rpx;padding:0 7rpx;border:3rpx solid #fff;border-radius:$radius-round;color:#fff;background:$error-color;font-size:17rpx;line-height:25rpx;text-align:center; }
.alert-card { display: flex; align-items: center; gap: 16rpx; margin-top: 22rpx; padding: 22rpx; border-radius: 25rpx; background: #fff6ef; box-shadow: $shadow-sm; }.alert-icon { display: flex; align-items: center; justify-content: center; width: 62rpx; height: 62rpx; border-radius: 20rpx; background: #ffe9dc; }.alert-copy { flex: 1; }.alert-title,.alert-desc { display: block; }.alert-title { color: $text-color; font-size: $font-size-sm; font-weight: 600; }.alert-desc { margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }
.stats-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16rpx; margin-top: 20rpx; }.stat-card { padding: 24rpx 10rpx; border: $glass-border-soft; border-radius: 24rpx; background: $surface-gradient; box-shadow: $shadow-sm; text-align: center; }.stat-value,.stat-label { display: block; }.stat-value { font-size: 38rpx; font-weight: 700; }.orange { color: $warning-color; }.blue { color: $primary-color; }.green { color: $success-color; }.stat-label { margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }.section-head { margin: 31rpx 4rpx 17rpx; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }
.overview-card { display: grid; grid-template-columns: 1fr 1rpx 1fr; align-items: center; padding: 27rpx 18rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; text-align: center; }.divider { width: 1rpx; height: 58rpx; background: $divider-color; }.overview-label,.overview-value { display: block; }.overview-label { color: $text-color-hint; font-size: $font-size-xs; }.overview-value { margin-top: 8rpx; color: $text-color; font-size: 31rpx; font-weight: 700; }
.feature-grid { display: grid; grid-template-columns: repeat(2,1fr); gap: 18rpx; }.feature-card { min-height: 180rpx; padding: 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.feature-icon { display: flex; align-items: center; justify-content: center; width: 64rpx; height: 64rpx; border-radius: 20rpx; background: $primary-bg; }.feature-title,.feature-desc { display: block; }.feature-title { margin-top: 16rpx; color: $text-color; font-size: $font-size-base; font-weight: 600; }.feature-desc { margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }
.feature-icon.orange-bg { background:#fff2eb; }
.feature-icon.green-bg { background:#e9fbf7; }
.feature-icon.purple-bg { background:#f1edff; }
</style>
