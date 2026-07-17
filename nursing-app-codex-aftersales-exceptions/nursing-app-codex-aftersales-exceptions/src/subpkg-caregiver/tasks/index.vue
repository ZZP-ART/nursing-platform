<template>
  <view class="page-shell">
    <view class="page-content">
      <scroll-view scroll-x :show-scrollbar="false" class="tabs-scroll">
        <view class="tabs">
          <view v-for="tab in filters" :key="tab.value" class="tab" :class="{ active: activeFilter === tab.value }" @click="changeFilter(tab.value)">{{ tab.label }}<text v-if="tabCount(tab.value)" class="tab-count">{{ tabCount(tab.value) }}</text></view>
        </view>
      </scroll-view>

      <view v-if="workOrderStore.loading" class="loading-wrap"><u-loading-icon size="30" color="#3A7BF7" /></view>
      <empty-state v-else-if="!filteredTasks.length" title="暂无相关任务" description="新的上门任务会及时出现在这里" />
      <view v-else class="task-list">
        <view v-for="task in filteredTasks" :key="task.orderId" class="task-card" @click="goDetail(task.orderId)">
          <view class="card-head"><view><text class="service-name">{{ task.serviceItemName }}</text><text class="order-no">{{ task.orderNo }}</text></view><text class="status-tag" :class="`tone-${statusMeta(task).tone}`">{{ statusMeta(task).text }}</text></view>
          <view class="schedule-row"><view class="schedule-icon"><u-icon name="calendar" size="24" color="#3A7BF7" /></view><view><text class="schedule-date">{{ task.serviceDate }}</text><text class="schedule-slot">{{ slotText(task.serviceTimeSlot) }}</text></view></view>
          <view class="info-line"><u-icon name="account" size="15" color="#98A5B3" /><text>{{ task.receiverName }} {{ task.receiverPhone }}</text></view>
          <view class="info-line"><u-icon name="map" size="15" color="#98A5B3" /><text class="address">{{ task.addressDetail }}</text></view>
          <view class="card-foot"><text class="merchant">{{ task.merchantName }}</text><view class="detail-link"><text>查看详情</text><u-icon name="arrow-right" size="13" color="#3A7BF7" /></view></view>
        </view>
      </view>
    </view>

    <role-tab-bar :tabs="tabs" current="/subpkg-caregiver/tasks/index" />
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import EmptyState from '@/components/base/empty-state.vue'
import RoleTabBar from '@/components/base/role-tab-bar.vue'
import { CAREGIVER_TABS } from '@/constants/caregiver-navigation.js'
import { ASSIGNMENT_STATUS, ORDER_STATUS, getOrderStatusMeta } from '@/constants/order-status.js'
import { ROLES } from '@/constants/roles.js'
import { useWorkOrderStore } from '@/store/work-order.js'
import { useNotificationStore } from '@/store/notification.js'
import { requireRole } from '@/utils/permission.js'

const workOrderStore = useWorkOrderStore()
const notificationStore = useNotificationStore()
const activeFilter = ref('all')
const filters = [
  { label: '全部', value: 'all' },
  { label: '待接单', value: 'pending' },
  { label: '今日', value: 'today' },
  { label: '进行中', value: 'active' },
  { label: '已完成', value: 'completed' },
]
const pendingCount = computed(() => workOrderStore.tasks.filter((task) => task.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT).length)
const tabs = computed(() => CAREGIVER_TABS.map((tab) => tab.label === '任务' ? { ...tab, badge: pendingCount.value || '' } : tab.label === '我的' ? { ...tab, badge: notificationStore.unreadCount || '' } : tab))
const filteredTasks = computed(() => workOrderStore.tasks.filter((task) => matchesFilter(task, activeFilter.value)))

onLoad((options) => { if (filters.some((item) => item.value === options.filter)) activeFilter.value = options.filter })
onShow(async () => {
  if (!requireRole(ROLES.CAREGIVER)) return
  await Promise.all([workOrderStore.fetchTasks(), notificationStore.fetchUnreadCount()])
})

function localDate() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
function matchesFilter(task, filter) {
  if (filter === 'pending') return task.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT
  if (filter === 'today') return task.serviceDate === localDate()
  if (filter === 'active') return [ORDER_STATUS.WAITING_SERVICE, ORDER_STATUS.IN_SERVICE].includes(task.orderStatus)
  if (filter === 'completed') return [ORDER_STATUS.WAITING_CONFIRM, ORDER_STATUS.COMPLETED].includes(task.orderStatus)
  return true
}
function tabCount(filter) { return filter === 'all' ? 0 : workOrderStore.tasks.filter((task) => matchesFilter(task, filter)).length }
function changeFilter(filter) { activeFilter.value = filter }
function statusMeta(task) {
  if (task.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT) return { text: '待接单', tone: 'warning' }
  if (task.assignmentStatus === ASSIGNMENT_STATUS.REJECTED) return { text: '已拒绝', tone: 'neutral' }
  return getOrderStatusMeta(task)
}
function slotText(slot) { return ({ MORNING: '上午 08:00-12:00', AFTERNOON: '下午 13:00-17:00', EVENING: '晚上 18:00-21:00' }[slot] || slot) }
function goDetail(id) { uni.navigateTo({ url: `/subpkg-caregiver/task-detail/index?id=${id}` }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; background: $page-gradient; }.page-content { padding: 20rpx $spacing-base calc(140rpx + env(safe-area-inset-bottom)); }.tabs-scroll { white-space: nowrap; }.tabs { display: flex; gap: 12rpx; padding: 4rpx 0 22rpx; }.tab { flex-shrink: 0; padding: 13rpx 24rpx; border-radius: $radius-round; background: rgba(255,255,255,.76); color: $text-color-secondary; font-size: $font-size-sm; }.tab.active { background: $primary-gradient; color: #fff; box-shadow: $shadow-glow; }.tab-count { margin-left: 7rpx; font-size: 19rpx; }.loading-wrap { display: flex; justify-content: center; padding: 180rpx 0; }
.task-card { margin-bottom: 20rpx; padding: 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; padding-bottom: 18rpx; border-bottom: 1rpx solid $divider-color; }.service-name,.order-no { display: block; }.service-name { color: $text-color; font-size: 30rpx; font-weight: 700; }.order-no { margin-top: 6rpx; color: $text-color-disabled; font-size: $font-size-xs; }.status-tag { flex-shrink: 0; padding: 7rpx 14rpx; border-radius: $radius-round; font-size: $font-size-xs; }.tone-warning { color: $warning-color; background: #fff2eb; }.tone-primary { color: $primary-color; background: $primary-bg; }.tone-success { color: $success-color; background: #e9fbf7; }.tone-neutral { color: $info-color; background: #f0f3f7; }
.schedule-row { display: flex; align-items: center; gap: 15rpx; padding: 20rpx 0 16rpx; }.schedule-icon { display: flex; align-items: center; justify-content: center; width: 66rpx; height: 66rpx; border-radius: 20rpx; background: $primary-bg; }.schedule-date,.schedule-slot { display: block; }.schedule-date { color: $text-color; font-size: $font-size-base; font-weight: 600; }.schedule-slot { margin-top: 4rpx; color: $text-color-hint; font-size: $font-size-xs; }.info-line { display: flex; align-items: center; gap: 9rpx; margin-top: 10rpx; color: $text-color-secondary; font-size: $font-size-xs; }.address { overflow: hidden; flex: 1; text-overflow: ellipsis; white-space: nowrap; }.card-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 18rpx; padding-top: 17rpx; border-top: 1rpx solid $divider-color; }.merchant { color: $text-color-hint; font-size: $font-size-xs; }.detail-link { display: flex; align-items: center; gap: 3rpx; color: $primary-color; font-size: $font-size-xs; }
</style>
