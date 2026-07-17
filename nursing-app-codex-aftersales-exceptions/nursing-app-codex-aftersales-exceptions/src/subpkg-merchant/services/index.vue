<template>
  <view class="page-shell">
    <view class="page-content">
      <view class="summary-card"><view><text class="summary-title">服务管理</text><text class="summary-desc">创建服务、编辑规格并跟踪审核状态</text></view><view class="summary-count">{{ serviceManageStore.summary.total }}</view></view>

      <view class="status-grid">
        <view v-for="item in statusSummary" :key="item.key" class="status-card" :class="{ active: activeFilter === item.key }" @click="activeFilter = item.key"><text class="status-value" :class="item.tone">{{ item.value }}</text><text class="status-label">{{ item.label }}</text></view>
      </view>

      <view class="toolbar"><text class="section-title">服务项目</text><button class="create-btn" @click="createService"><u-icon name="plus" size="16" color="#FFFFFF" /><text>新建服务</text></button></view>

      <view v-if="serviceManageStore.loading" class="loading-wrap"><u-loading-icon size="30" color="#3A7BF7" /></view>
      <empty-state v-else-if="!filteredServices.length" title="暂无相关服务" description="新建服务后可设置规格并提交平台审核" />
      <view v-else class="service-list">
        <view v-for="service in filteredServices" :key="service.itemId" class="service-card" @click="goDetail(service.itemId)">
          <view class="service-cover"><image v-if="service.coverImage" :src="service.coverImage" mode="aspectFill" /><u-icon v-else name="grid" size="30" color="#98A5B3" /></view>
          <view class="service-copy"><view class="name-row"><text class="service-name">{{ service.name }}</text><text class="status-tag" :class="`tone-${auditMeta(service.auditStatus).tone}`">{{ auditMeta(service.auditStatus).text }}</text></view><text class="service-category">{{ service.categoryName }} · {{ service.specs.length }} 个规格</text><view class="price-row"><text class="price">¥{{ service.minPrice ?? '--' }} 起</text><text class="publish-state" :class="{ published: service.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED }">{{ service.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED ? '已上架' : '未上架' }}</text></view><text v-if="service.rejectReason" class="reject-reason">{{ service.rejectReason }}</text></view>
          <u-icon name="arrow-right" size="16" color="#C5CDD8" />
        </view>
      </view>
    </view>
    <role-tab-bar :tabs="tabs" current="/subpkg-merchant/services/index" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/base/empty-state.vue'
import RoleTabBar from '@/components/base/role-tab-bar.vue'
import { MERCHANT_TABS } from '@/constants/merchant-navigation.js'
import { ROLES } from '@/constants/roles.js'
import { SERVICE_AUDIT_STATUS, SERVICE_PUBLISH_STATUS, getServiceAuditMeta } from '@/constants/service-status.js'
import { useServiceManageStore } from '@/store/service-manage.js'
import { useNotificationStore } from '@/store/notification.js'
import { requireRole } from '@/utils/permission.js'

const serviceManageStore = useServiceManageStore()
const notificationStore = useNotificationStore()
const tabs = computed(() => MERCHANT_TABS.map((tab) => tab.label === '我的' ? { ...tab, badge: notificationStore.unreadCount || '' } : tab))
const activeFilter = ref('ALL')
const statusSummary = computed(() => [
  { key: 'ALL', label: '全部', value: serviceManageStore.summary.total, tone: 'grey' },
  { key: SERVICE_AUDIT_STATUS.DRAFT, label: '草稿', value: serviceManageStore.summary.draft, tone: 'grey' },
  { key: SERVICE_AUDIT_STATUS.PENDING_REVIEW, label: '审核中', value: serviceManageStore.summary.pending, tone: 'blue' },
  { key: SERVICE_AUDIT_STATUS.REJECTED, label: '已驳回', value: serviceManageStore.summary.rejected, tone: 'orange' },
])
const filteredServices = computed(() => activeFilter.value === 'ALL' ? serviceManageStore.services : serviceManageStore.services.filter((service) => service.auditStatus === activeFilter.value))

onShow(async () => { if (!requireRole(ROLES.MERCHANT_MEMBER)) return; await Promise.all([serviceManageStore.fetchServices(), notificationStore.fetchUnreadCount()]) })
function auditMeta(status) { return getServiceAuditMeta(status) }
function createService() { uni.navigateTo({ url: '/subpkg-merchant/service-edit/index' }) }
function goDetail(itemId) { uni.navigateTo({ url: `/subpkg-merchant/service-detail/index?id=${itemId}` }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; background: $page-gradient; }.page-content { padding: 28rpx $spacing-base calc(140rpx + env(safe-area-inset-bottom)); }.summary-card { display: flex; align-items: center; justify-content: space-between; min-height: 180rpx; padding: 30rpx; border-radius: 32rpx; background: linear-gradient(135deg,#116b68,#00a89d 55%,#38c6b5); color: #fff; box-shadow: $shadow-float; }.summary-title,.summary-desc { display: block; }.summary-title { font-size: 35rpx; font-weight: 700; }.summary-desc { margin-top: 8rpx; font-size: $font-size-xs; opacity: .8; }.summary-count { display: flex; align-items: center; justify-content: center; width: 88rpx; height: 88rpx; border-radius: 28rpx; background: rgba(255,255,255,.18); font-size: 36rpx; font-weight: 700; }
.status-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 12rpx; margin-top: 20rpx; }.status-card { padding: 20rpx 4rpx; border: 2rpx solid transparent; border-radius: 22rpx; background: $surface-gradient; text-align: center; }.status-card.active { border-color: rgba(58,123,247,.28); background: $primary-bg; }.status-value,.status-label { display: block; }.status-value { font-size: 32rpx; font-weight: 700; }.green { color: $success-color; }.blue { color: $primary-color; }.orange { color: $warning-color; }.grey { color: $info-color; }.status-label { margin-top: 5rpx; color: $text-color-hint; font-size: 19rpx; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin: 30rpx 2rpx 17rpx; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.create-btn { display: flex; align-items: center; gap: 6rpx; height: 62rpx; margin: 0; padding: 0 20rpx; border: none; border-radius: $radius-round; background: $primary-gradient; color: #fff; font-size: $font-size-xs; line-height: 62rpx; }.create-btn::after { border: none; }.loading-wrap { display: flex; justify-content: center; padding: 150rpx 0; }
.service-card { display: flex; align-items: center; gap: 16rpx; margin-bottom: 17rpx; padding: 20rpx; border: $glass-border-soft; border-radius: 27rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.service-cover { display: flex; align-items: center; justify-content: center; width: 116rpx; height: 116rpx; flex-shrink: 0; overflow: hidden; border-radius: 23rpx; background: $bg-color-grey; }.service-cover image { width: 100%; height: 100%; }.service-copy { min-width: 0; flex: 1; }.name-row { display: flex; align-items: center; gap: 10rpx; }.service-name { overflow: hidden; flex: 1; color: $text-color; font-size: 29rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.status-tag { flex-shrink: 0; padding: 5rpx 10rpx; border-radius: $radius-round; font-size: 18rpx; }.tone-primary { color: $primary-color; background: $primary-bg; }.tone-success { color: $success-color; background: #e9fbf7; }.tone-warning { color: $warning-color; background: #fff2eb; }.tone-neutral { color: $info-color; background: #f0f3f7; }.service-category { display: block; margin-top: 7rpx; color: $text-color-hint; font-size: $font-size-xs; }.price-row { display: flex; align-items: center; gap: 12rpx; margin-top: 10rpx; }.price { color: $warning-color; font-size: $font-size-sm; font-weight: 600; }.publish-state { color: $text-color-disabled; font-size: $font-size-xs; }.publish-state.published { color: $success-color; }.reject-reason { display: block; margin-top: 8rpx; overflow: hidden; color: $warning-color; font-size: $font-size-xs; text-overflow: ellipsis; white-space: nowrap; }
</style>
