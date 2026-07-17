<template>
  <view class="page-shell">
    <view class="page-content">
      <scroll-view scroll-x :show-scrollbar="false" class="tabs-scroll"><view class="tabs"><view v-for="tab in filters" :key="tab.value" class="tab" :class="{ active: activeFilter === tab.value }" @click="activeFilter = tab.value">{{ tab.label }}<text v-if="countFor(tab.value)" class="tab-count">{{ countFor(tab.value) }}</text></view></view></scroll-view>
      <view v-if="merchantStore.loading" class="loading-wrap"><u-loading-icon size="30" color="#3A7BF7" /></view>
      <empty-state v-else-if="!filteredOrders.length" title="暂无相关订单" description="符合条件的商户订单会显示在这里" />
      <view v-else><view v-for="order in filteredOrders" :key="order.orderId" class="order-card" @click="goDetail(order.orderId)"><view class="card-head"><view><text class="service-name">{{ order.serviceItemName }}</text><text class="order-no">{{ order.orderNo }}</text></view><text class="status-tag" :class="`tone-${statusMeta(order).tone}`">{{ statusMeta(order).text }}</text></view><view class="schedule-line"><u-icon name="calendar" size="16" color="#3A7BF7" /><text>{{ order.serviceDate }} {{ slotText(order.serviceTimeSlot) }}</text></view><view class="info-line"><u-icon name="account" size="15" color="#98A5B3" /><text>{{ order.receiverName }}</text><u-icon name="map" size="15" color="#98A5B3" /><text class="address">{{ order.addressDetail }}</text></view><view v-if="order.currentAssignment" class="assignment-row"><u-icon name="server-man" size="17" color="#3A7BF7" /><text>{{ order.currentAssignment.caregiverName }}</text><text class="assignment-status">{{ assignmentText(order.assignmentStatus) }}</text></view><view class="card-foot"><text class="amount">¥{{ order.totalAmount }}</text><button v-if="needsDispatch(order)" class="dispatch-btn" @click.stop="goDispatch(order)">{{ order.assignments?.length ? '重新派单' : '立即派单' }}</button><view v-else class="detail-link"><text>查看详情</text><u-icon name="arrow-right" size="13" color="#3A7BF7" /></view></view></view></view>
    </view>
    <role-tab-bar :tabs="tabs" current="/subpkg-merchant/orders/index" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/base/empty-state.vue'
import RoleTabBar from '@/components/base/role-tab-bar.vue'
import { MERCHANT_TABS } from '@/constants/merchant-navigation.js'
import { ASSIGNMENT_STATUS, ORDER_STATUS, getOrderStatusMeta } from '@/constants/order-status.js'
import { ROLES } from '@/constants/roles.js'
import { useMerchantStore } from '@/store/merchant.js'
import { requireRole } from '@/utils/permission.js'

const merchantStore = useMerchantStore()
const activeFilter = ref('all')
const filters = [{ label: '全部', value: 'all' }, { label: '待派单', value: 'dispatch' }, { label: '待接单', value: 'accept' }, { label: '今日', value: 'today' }, { label: '待服务', value: 'service' }, { label: '服务中', value: 'active' }, { label: '已完成', value: 'completed' }]
const waitingDispatchCount = computed(() => merchantStore.orders.filter(needsDispatch).length)
const tabs = computed(() => MERCHANT_TABS.map((tab) => tab.label === '订单' ? { ...tab, badge: waitingDispatchCount.value || '' } : tab))
const filteredOrders = computed(() => merchantStore.orders.filter((order) => matchesFilter(order, activeFilter.value)))
onLoad((options) => { if (filters.some((item) => item.value === options.filter)) activeFilter.value = options.filter })
onShow(async () => { if (!requireRole(ROLES.MERCHANT_MEMBER)) return; await merchantStore.fetchOrders() })
function needsDispatch(order) { return order.orderStatus === ORDER_STATUS.WAITING_DISPATCH && [ASSIGNMENT_STATUS.UNASSIGNED, ASSIGNMENT_STATUS.REJECTED, ASSIGNMENT_STATUS.EXPIRED].includes(order.assignmentStatus) }
function matchesFilter(order, filter) { if (filter === 'dispatch') return needsDispatch(order); if (filter === 'accept') return order.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT; if (filter === 'today') return order.serviceDate === localDate(); if (filter === 'service') return order.orderStatus === ORDER_STATUS.WAITING_SERVICE; if (filter === 'active') return order.orderStatus === ORDER_STATUS.IN_SERVICE; if (filter === 'completed') return [ORDER_STATUS.WAITING_CONFIRM, ORDER_STATUS.COMPLETED].includes(order.orderStatus); return true }
function localDate() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}` }
function countFor(filter) { return filter === 'all' ? 0 : merchantStore.orders.filter((order) => matchesFilter(order, filter)).length }
function statusMeta(order) { if (needsDispatch(order)) return { text: '待派单', tone: 'warning' }; if (order.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT) return { text: '待接单', tone: 'warning' }; return getOrderStatusMeta(order) }
function assignmentText(status) { return ({ WAITING_ACCEPT: '等待接单', ACCEPTED: '已接单', REJECTED: '已拒绝', EXPIRED: '已过期', CANCELED: '已取消' }[status] || '待派单') }
function slotText(slot) { return ({ MORNING: '上午', AFTERNOON: '下午', EVENING: '晚上' }[slot] || slot) }
function goDetail(id) { uni.navigateTo({ url: `/subpkg-merchant/order-detail/index?id=${id}` }) }
function goDispatch(order) { uni.navigateTo({ url: `/subpkg-merchant/dispatch/index?id=${order.orderId}&mode=${order.assignments?.length ? 'redispatch' : 'dispatch'}` }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; background: $page-gradient; }.page-content { padding: 20rpx $spacing-base calc(140rpx + env(safe-area-inset-bottom)); }.tabs-scroll { white-space: nowrap; }.tabs { display: flex; gap: 12rpx; padding: 4rpx 0 22rpx; }.tab { flex-shrink: 0; padding: 13rpx 23rpx; border-radius: $radius-round; background: rgba(255,255,255,.76); color: $text-color-secondary; font-size: $font-size-sm; }.tab.active { background: $primary-gradient; color: #fff; box-shadow: $shadow-glow; }.tab-count { margin-left: 7rpx; font-size: 19rpx; }.loading-wrap { display: flex; justify-content: center; padding: 180rpx 0; }
.order-card { margin-bottom: 20rpx; padding: 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18rpx; padding-bottom: 17rpx; border-bottom: 1rpx solid $divider-color; }.service-name,.order-no { display: block; }.service-name { color: $text-color; font-size: 30rpx; font-weight: 700; }.order-no { margin-top: 5rpx; color: $text-color-disabled; font-size: $font-size-xs; }.status-tag { flex-shrink: 0; padding: 7rpx 14rpx; border-radius: $radius-round; font-size: $font-size-xs; }.tone-warning { color: $warning-color; background: #fff2eb; }.tone-primary { color: $primary-color; background: $primary-bg; }.tone-success { color: $success-color; background: #e9fbf7; }.tone-neutral { color: $info-color; background: #f0f3f7; }
.schedule-line,.info-line { display: flex; align-items: center; gap: 8rpx; margin-top: 16rpx; color: $text-color-secondary; font-size: $font-size-xs; }.info-line { margin-top: 11rpx; }.address { overflow: hidden; flex: 1; text-overflow: ellipsis; white-space: nowrap; }.assignment-row { display: flex; align-items: center; gap: 8rpx; margin-top: 15rpx; padding: 13rpx 16rpx; border-radius: 18rpx; background: $primary-bg; color: $text-color-secondary; font-size: $font-size-xs; }.assignment-status { margin-left: auto; color: $primary-color; }.card-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 18rpx; padding-top: 17rpx; border-top: 1rpx solid $divider-color; }.amount { color: $warning-color; font-size: 30rpx; font-weight: 700; }.dispatch-btn { height: 58rpx; margin: 0; padding: 0 24rpx; border: none; border-radius: $radius-round; background: $primary-gradient; color: #fff; font-size: $font-size-xs; line-height: 58rpx; }.dispatch-btn::after { border: none; }.detail-link { display: flex; align-items: center; gap: 3rpx; color: $primary-color; font-size: $font-size-xs; }
</style>
