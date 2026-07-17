<template>
  <view class="page-shell">
    <scroll-view scroll-x :show-scrollbar="false" class="tabs-scroll"><view class="tabs"><view v-for="tab in tabs" :key="String(tab.value)" class="tab" :class="{ active: activeStatus === tab.value }" @click="changeStatus(tab.value)">{{ tab.label }}</view></view></scroll-view>
    <view v-if="orderStore.loading" class="loading-wrap"><u-loading-icon size="28" color="#3A7BF7" /></view>
    <empty-state v-else-if="!orderStore.orders.length" title="暂无相关订单" description="预约护理服务后，订单会出现在这里" />
    <view v-else class="order-list">
      <view v-for="order in orderStore.orders" :key="order.orderId" class="order-card" @click="goDetail(order.orderId)">
        <view class="card-head"><text class="order-no">订单 {{ order.orderNo }}</text><text class="status" :class="`tone-${orderStore.getStatusMeta(order).tone}`">{{ orderStore.getStatusText(order) }}</text></view>
        <view class="service-row"><view class="service-icon"><u-icon name="heart-fill" size="25" color="#3A7BF7" /></view><view class="service-copy"><text class="service-name">{{ order.serviceItemName }}</text><text class="service-spec">{{ order.specName }} · {{ order.serviceDate }}</text></view><text class="amount">¥{{ order.totalAmount }}</text></view>
        <view v-if="order.currentAssignment" class="caregiver-row"><u-icon name="server-man" size="16" color="#3A7BF7" /><text>{{ order.currentAssignment.caregiverName }}</text><text class="assignment-state">{{ assignmentText(order.assignmentStatus) }}</text></view>
        <view class="meta-row"><u-icon name="clock" size="14" color="#98A5B3" /><text>{{ orderStore.getSlotText(order.serviceTimeSlot) }}</text><u-icon name="map" size="14" color="#98A5B3" /><text class="address">{{ order.addressDetail }}</text></view>
        <view class="card-actions" @click.stop>
          <button v-if="orderStore.canCustomerPay(order)" class="action-btn primary" @click="pay(order)">立即支付</button>
          <button v-if="orderStore.canCustomerCancel(order)" class="action-btn" @click="cancel(order.orderId)">取消订单</button>
          <button v-if="orderStore.canCustomerReview(order)" class="action-btn primary" @click="review(order.orderId)">评价服务</button>
          <button v-if="orderStore.canCustomerConfirm(order)" class="action-btn primary" @click="confirm(order.orderId)">确认完成</button>
          <button class="action-btn" @click="goDetail(order.orderId)">查看详情</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useOrderStore } from '@/store/order.js'
import EmptyState from '@/components/base/empty-state.vue'

const orderStore = useOrderStore()
const activeStatus = ref(null)
const tabs = [{ label: '全部', value: null }, { label: '待支付', value: 0 }, { label: '待服务', value: 1 }, { label: '已完成', value: 2 }, { label: '已取消', value: 3 }]
onLoad((options) => { if (options.status !== undefined) activeStatus.value = Number(options.status) })
onShow(() => loadOrders())
function loadOrders() { orderStore.fetchOrders(activeStatus.value === null ? {} : { status: activeStatus.value }) }
function changeStatus(status) { activeStatus.value = status; loadOrders() }
function goDetail(id) { uni.navigateTo({ url: `/pages/order/order-detail?id=${id}` }) }
async function pay(order) { try { const payment = await orderStore.executePayment(order.orderId); const status = payment.success ? 'success' : 'failed'; uni.navigateTo({ url: `/pages/payment-result/payment-result?status=${status}&orderId=${order.orderId}&amount=${order.totalAmount}` }) } catch { uni.navigateTo({ url: `/pages/payment-result/payment-result?status=failed&orderId=${order.orderId}&amount=${order.totalAmount}` }) } }
function review(id) { uni.navigateTo({ url: `/pages/review/review-submit?orderId=${id}` }) }
async function confirm(id) { await orderStore.confirmOrder(id); loadOrders() }
function cancel(id) { uni.showModal({ title: '取消订单', content: '确定取消本次护理服务吗？', success: async ({ confirm }) => { if (confirm) { await orderStore.cancelOrder(id, '用户主动取消'); loadOrders() } } }) }
function assignmentText(status) { return ({ UNASSIGNED: '待派单', WAITING_ACCEPT: '待接单', ACCEPTED: '已接单', REJECTED: '已拒绝', EXPIRED: '已过期', CANCELED: '已取消' }[status] || '') }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 20rpx $spacing-base 50rpx; background: $page-gradient; }.tabs-scroll { white-space: nowrap; }.tabs { display: flex; gap: 14rpx; padding: 4rpx 0 22rpx; }.tab { flex-shrink: 0; padding: 13rpx 26rpx; border-radius: $radius-round; background: rgba(255,255,255,0.74); color: $text-color-secondary; font-size: $font-size-sm; }.tab.active { color: #fff; background: $primary-gradient; box-shadow: $shadow-glow; }.loading-wrap { padding: 160rpx 0; display: flex; justify-content: center; }
.order-card { margin-bottom: 20rpx; padding: 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.card-head { display: flex; align-items: center; justify-content: space-between; padding-bottom: 18rpx; border-bottom: 1rpx solid $divider-color; }.order-no { color: $text-color-hint; font-size: $font-size-xs; }.status { padding: 6rpx 13rpx; border-radius: $radius-round; font-size: $font-size-xs; }.tone-warning { color: $warning-color; background: #fff2eb; }.tone-primary { color: $primary-color; background: $primary-bg; }.tone-success { color: $success-color; background: #e9fbf7; }.tone-neutral { color: $info-color; background: #f0f3f7; }
.service-row { display: flex; align-items: center; gap: 16rpx; padding: 22rpx 0; }.service-icon { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 23rpx; background: $primary-bg; }.service-copy { flex: 1; }.service-name { display: block; color: $text-color; font-size: 30rpx; font-weight: 700; }.service-spec { display: block; margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }.amount { color: $warning-color; font-size: 34rpx; font-weight: 700; }.meta-row { display: flex; align-items: center; gap: 7rpx; color: $text-color-hint; font-size: 20rpx; }.address { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.card-actions { display: flex; justify-content: flex-end; gap: 12rpx; margin-top: 22rpx; }.action-btn { height: 58rpx; margin: 0; padding: 0 22rpx; border: 1rpx solid $border-color; border-radius: $radius-round; background: #fff; color: $text-color-secondary; font-size: $font-size-xs; line-height: 56rpx; }.action-btn::after { border: none; }.action-btn.primary { border: none; color: #fff; background: $primary-gradient; }
.caregiver-row { display: flex; align-items: center; gap: 8rpx; margin-bottom: 16rpx; padding: 13rpx 16rpx; border-radius: 18rpx; background: $primary-bg; color: $text-color-secondary; font-size: $font-size-xs; }.assignment-state { margin-left: auto; color: $primary-color; }
</style>
