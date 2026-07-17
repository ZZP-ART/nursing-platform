<template>
  <view class="workspace-page">
    <view class="page-content">
      <view class="hero-card">
        <view>
          <text class="eyebrow">护理人员工作台</text>
          <text class="title">你好，{{ userStore.userInfo?.nickname || '护理人员' }}</text>
          <text class="subtitle">{{ todayText }} · 合理安排上门时间</text>
        </view>
        <view class="hero-icon"><u-icon name="server-man" size="35" color="#FFFFFF" /></view>
      </view>

      <view class="stats-grid">
        <view class="stat-card" @click="goTasks('pending')"><text class="stat-value orange">{{ pendingCount }}</text><text class="stat-label">待接单</text></view>
        <view class="stat-card" @click="goTasks('today')"><text class="stat-value blue">{{ todayCount }}</text><text class="stat-label">今日任务</text></view>
        <view class="stat-card" @click="goTasks('active')"><text class="stat-value green">{{ activeCount }}</text><text class="stat-label">进行中</text></view>
      </view>

      <view class="section-head"><text class="section-title">下一项任务</text><text class="section-link" @click="goTasks()">查看全部</text></view>
      <view v-if="nextTask" class="next-card" @click="goDetail(nextTask.orderId)">
        <view class="next-time"><text class="time-date">{{ nextTask.serviceDate }}</text><text class="time-slot">{{ getSlotText(nextTask.serviceTimeSlot) }}</text></view>
        <view class="next-copy"><text class="next-name">{{ nextTask.serviceItemName }}</text><text class="next-address">{{ nextTask.addressDetail }}</text><text class="next-status">{{ getTaskStatusText(nextTask) }}</text></view>
        <u-icon name="arrow-right" size="18" color="#C5CDD8" />
      </view>
      <view v-else class="empty-card"><u-icon name="calendar" size="34" color="#98A5B3" /><text>暂无待处理任务</text></view>

      <view class="section-head"><text class="section-title">快捷操作</text></view>
      <view class="feature-grid">
        <view class="feature-card" @click="goTasks()"><view class="feature-icon"><u-icon name="list-dot" size="26" color="#3A7BF7" /></view><text class="feature-title">我的任务</text><text class="feature-desc">接单并查看上门安排</text></view>
      </view>
    </view>

    <role-tab-bar :tabs="tabs" current="/subpkg-caregiver/home/index" />
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user.js'
import { useWorkOrderStore } from '@/store/work-order.js'
import { ASSIGNMENT_STATUS, ORDER_STATUS } from '@/constants/order-status.js'
import { ROLES } from '@/constants/roles.js'
import { CAREGIVER_TABS } from '@/constants/caregiver-navigation.js'
import { requireRole } from '@/utils/permission.js'
import RoleTabBar from '@/components/base/role-tab-bar.vue'

const userStore = useUserStore()
const workOrderStore = useWorkOrderStore()
const tabs = computed(() => CAREGIVER_TABS.map((tab) => tab.label === '任务' ? { ...tab, badge: pendingCount.value || '' } : tab))
const todayText = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'short' }).format(new Date())
const pendingCount = computed(() => workOrderStore.tasks.filter((task) => task.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT).length)
const activeCount = computed(() => workOrderStore.tasks.filter((task) => task.orderStatus === ORDER_STATUS.IN_SERVICE).length)
const todayCount = computed(() => workOrderStore.tasks.filter((task) => task.serviceDate === localDate()).length)
const nextTask = computed(() => workOrderStore.tasks.find((task) => [ASSIGNMENT_STATUS.WAITING_ACCEPT, ASSIGNMENT_STATUS.ACCEPTED].includes(task.assignmentStatus) && ![ORDER_STATUS.COMPLETED, ORDER_STATUS.WAITING_CONFIRM].includes(task.orderStatus)))

onShow(async () => {
  if (!requireRole(ROLES.CAREGIVER)) return
  await workOrderStore.fetchTasks()
})

function getSlotText(slot) { return ({ MORNING: '上午', AFTERNOON: '下午', EVENING: '晚上' }[slot] || slot) }
function localDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
function getTaskStatusText(task) {
  if (task.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT) return '等待接单'
  return ({ WAITING_SERVICE: '待上门', IN_SERVICE: '服务中', WAITING_CONFIRM: '待顾客确认', COMPLETED: '已完成' }[task.orderStatus] || '待处理')
}
function goTasks(filter) { uni.redirectTo({ url: `/subpkg-caregiver/tasks/index${filter ? `?filter=${filter}` : ''}` }) }
function goDetail(id) { uni.navigateTo({ url: `/subpkg-caregiver/task-detail/index?id=${id}` }) }
</script>

<style lang="scss" scoped>
.workspace-page { min-height: 100vh; background: $page-gradient; }.page-content { padding: 30rpx $spacing-base calc(140rpx + env(safe-area-inset-bottom)); }
.hero-card { display: flex; align-items: center; justify-content: space-between; min-height: 230rpx; padding: 34rpx; border-radius: 34rpx; background: linear-gradient(135deg,#245ddc,#3a7bf7 55%,#00b8d8); color: #fff; box-shadow: $shadow-float; }.eyebrow,.title,.subtitle { display: block; }.eyebrow { font-size: $font-size-sm; opacity: .78; }.title { margin-top: 12rpx; font-size: 38rpx; font-weight: 700; }.subtitle { margin-top: 12rpx; font-size: $font-size-xs; opacity: .82; }.hero-icon { position:relative;display:flex;align-items:center;justify-content:center;width:96rpx;height:96rpx;flex-shrink:0;border-radius:30rpx;background:rgba(255,255,255,.18); }.hero-badge { position:absolute;right:-7rpx;top:-7rpx;min-width:31rpx;height:31rpx;padding:0 7rpx;border:3rpx solid #fff;border-radius:$radius-round;color:#fff;background:$error-color;font-size:17rpx;line-height:25rpx;text-align:center; }
.stats-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 16rpx; margin-top: 22rpx; }.stat-card { padding: 24rpx 10rpx; text-align: center; border: $glass-border-soft; border-radius: 24rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.stat-value,.stat-label { display: block; }.stat-value { font-size: 38rpx; font-weight: 700; }.stat-value.orange { color: $warning-color; }.stat-value.blue { color: $primary-color; }.stat-value.green { color: $success-color; }.stat-label { margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin: 32rpx 4rpx 18rpx; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.section-link { color: $primary-color; font-size: $font-size-xs; }
.next-card { display: flex; align-items: center; gap: 20rpx; padding: 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.next-time { width: 112rpx; padding: 17rpx 8rpx; flex-shrink: 0; border-radius: 22rpx; background: $primary-bg; text-align: center; }.time-date,.time-slot { display: block; }.time-date { color: $primary-color; font-size: 22rpx; font-weight: 600; }.time-slot { margin-top: 7rpx; color: $text-color-secondary; font-size: $font-size-xs; }.next-copy { min-width: 0; flex: 1; }.next-name,.next-address,.next-status { display: block; }.next-name { color: $text-color; font-size: $font-size-base; font-weight: 700; }.next-address { margin-top: 7rpx; overflow: hidden; color: $text-color-hint; font-size: $font-size-xs; text-overflow: ellipsis; white-space: nowrap; }.next-status { margin-top: 10rpx; color: $primary-color; font-size: $font-size-xs; }.empty-card { display: flex; flex-direction: column; align-items: center; gap: 12rpx; padding: 48rpx; border-radius: 28rpx; background: $surface-gradient; color: $text-color-hint; font-size: $font-size-sm; }
.feature-grid { display: grid; grid-template-columns: repeat(2,1fr); gap: 18rpx; }.feature-card { min-height: 180rpx; padding: 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.feature-icon { display: flex; align-items: center; justify-content: center; width: 64rpx; height: 64rpx; border-radius: 20rpx; background: $primary-bg; }.feature-title,.feature-desc { display: block; }.feature-title { margin-top: 16rpx; color: $text-color; font-size: $font-size-base; font-weight: 600; }.feature-desc { margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }
</style>
