<template>
  <view v-if="task" class="page-shell">
    <view class="status-card" :class="`tone-${taskMeta.tone}`"><view><text class="status-title">{{ taskMeta.text }}</text><text class="status-desc">{{ taskDescription }}</text></view><u-icon :name="taskMeta.icon || 'clock-fill'" size="44" color="rgba(255,255,255,.92)" /></view>

    <view class="detail-card"><text class="card-title">服务安排</text><view class="service-head"><view class="service-icon"><u-icon name="heart-fill" size="27" color="#3A7BF7" /></view><view class="service-copy"><text class="service-name">{{ task.serviceItemName }}</text><text class="secondary">{{ task.specName }}</text></view></view><view class="info-row"><text class="label">预约日期</text><text class="value">{{ task.serviceDate }}</text></view><view class="info-row"><text class="label">预约时段</text><text class="value">{{ slotText(task.serviceTimeSlot) }}</text></view><view class="info-row"><text class="label">服务商户</text><text class="value">{{ task.merchantName }}</text></view></view>

    <view class="detail-card"><text class="card-title">上门信息</text><view class="info-row"><text class="label">联系人</text><text class="value">{{ task.receiverName }} {{ task.receiverPhone }}</text></view><view class="info-row"><text class="label">服务地址</text><text class="value multiline">{{ task.addressDetail }}</text></view><view v-if="task.remark" class="notice-box"><u-icon name="info-circle" size="18" color="#FF8A5C" /><text>{{ task.remark }}</text></view><button v-if="isAccepted" class="map-btn" @click="openLocation"><u-icon name="map" size="17" color="#3A7BF7" /><text>打开地图导航</text></button></view>

    <view v-if="task.serviceRecords?.length" class="detail-card"><text class="card-title">履约记录</text><view v-for="record in task.serviceRecords" :key="record.recordId" class="record-row"><view class="record-dot" /><view><text class="record-title">{{ record.content }}</text><text v-if="record.summary" class="record-summary">{{ record.summary }}</text><text class="record-time">{{ formatTime(record.createTime) }}</text></view></view></view>

    <view class="detail-card"><text class="card-title">订单信息</text><view class="info-row"><text class="label">订单编号</text><text class="value">{{ task.orderNo }}</text></view><view class="info-row"><text class="label">创建时间</text><text class="value">{{ formatTime(task.createTime) }}</text></view></view>

    <view v-if="hasActions" class="bottom-actions">
      <button v-if="isWaitingAccept" class="action-btn danger" @click="rejectTask">拒绝</button>
      <button v-if="isWaitingAccept" class="action-btn primary" @click="acceptTask">接受任务</button>
      <button v-if="canDepart" class="action-btn" @click="depart">确认出发</button>
      <button v-if="canCheckIn" class="action-btn primary" @click="checkIn">到达签到</button>
      <button v-if="canStart" class="action-btn primary" @click="startService">开始服务</button>
      <button v-if="canFinish" class="action-btn primary" @click="goFinish">填写记录并结束</button>
    </view>
  </view>
  <view v-else class="loading-page"><u-loading-icon size="32" color="#3A7BF7" /></view>
</template>

<script setup>
import { computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ASSIGNMENT_STATUS, ORDER_STATUS, getOrderStatusMeta } from '@/constants/order-status.js'
import { ROLES } from '@/constants/roles.js'
import { useWorkOrderStore } from '@/store/work-order.js'
import { requireRole } from '@/utils/permission.js'

const workOrderStore = useWorkOrderStore()
const task = computed(() => workOrderStore.currentTask)
const isWaitingAccept = computed(() => task.value?.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT)
const isAccepted = computed(() => task.value?.assignmentStatus === ASSIGNMENT_STATUS.ACCEPTED)
const hasCheckIn = computed(() => task.value?.serviceRecords?.some((record) => record.action === 'CHECK_IN'))
const hasDeparted = computed(() => task.value?.serviceRecords?.some((record) => record.action === 'DEPART'))
const canDepart = computed(() => isAccepted.value && task.value?.orderStatus === ORDER_STATUS.WAITING_SERVICE && !hasDeparted.value)
const canCheckIn = computed(() => isAccepted.value && task.value?.orderStatus === ORDER_STATUS.WAITING_SERVICE && !hasCheckIn.value)
const canStart = computed(() => isAccepted.value && task.value?.orderStatus === ORDER_STATUS.WAITING_SERVICE && hasCheckIn.value)
const canFinish = computed(() => isAccepted.value && task.value?.orderStatus === ORDER_STATUS.IN_SERVICE)
const hasActions = computed(() => isWaitingAccept.value || canDepart.value || canCheckIn.value || canStart.value || canFinish.value)
const taskMeta = computed(() => {
  if (isWaitingAccept.value) return { text: '待接单', description: '请确认时间和地点后处理任务', icon: 'bell-fill', tone: 'warning' }
  if (task.value?.assignmentStatus === ASSIGNMENT_STATUS.REJECTED) return { text: '已拒绝', description: '该任务已退回商户重新派单', icon: 'close-circle-fill', tone: 'neutral' }
  return getOrderStatusMeta(task.value || {})
})
const taskDescription = computed(() => taskMeta.value.description || ({ WAITING_SERVICE: '请按预约时间准时上门', IN_SERVICE: '服务进行中，请按规范完成记录', WAITING_CONFIRM: '服务已结束，等待顾客确认', COMPLETED: '本次服务已经完成' }[task.value?.orderStatus] || '任务状态已更新'))

onLoad(async (options) => {
  if (!requireRole(ROLES.CAREGIVER)) return
  await workOrderStore.fetchTaskDetail(String(options.id || ''))
})

function formatTime(value) { return value ? value.replace('T', ' ').replace('+08:00', '').slice(0, 16) : '--' }
function slotText(slot) { return ({ MORNING: '上午 08:00-12:00', AFTERNOON: '下午 13:00-17:00', EVENING: '晚上 18:00-21:00' }[slot] || slot) }
async function acceptTask() { await workOrderStore.acceptAssignment(task.value.currentAssignment.assignmentId); uni.showToast({ title: '接单成功', icon: 'success' }) }
function rejectTask() {
  uni.showModal({ title: '拒绝任务', editable: true, placeholderText: '请填写拒绝原因', success: async ({ confirm, content }) => { if (!confirm) return; if (!content?.trim()) return uni.showToast({ title: '请填写拒绝原因', icon: 'none' }); await workOrderStore.rejectAssignment(task.value.currentAssignment.assignmentId, content.trim()); uni.showToast({ title: '已拒绝任务', icon: 'none' }); setTimeout(() => uni.navigateBack(), 600) } })
}
async function depart() { await workOrderStore.depart(task.value.orderId); uni.showToast({ title: '已记录出发', icon: 'success' }) }
async function resolveLocation() {
  return new Promise((resolve, reject) => uni.getLocation({ type: 'gcj02', success: resolve, fail: reject }))
}
async function checkIn() {
  try {
    await resolveLocation()
    await workOrderStore.checkIn(task.value.orderId, { remark: '护理人员已到达服务地点' })
    uni.showToast({ title: '签到成功', icon: 'success' })
  } catch {
    uni.showModal({ title: '无法获取定位', content: '是否仍提交到达签到？', success: async ({ confirm }) => { if (!confirm) return; await workOrderStore.checkIn(task.value.orderId, { remark: '设备定位不可用，护理人员手动确认到达' }); uni.showToast({ title: '签到已提交', icon: 'success' }) } })
  }
}
function startService() { uni.showModal({ title: '开始服务', content: '请确认已完成身份核验，并准备开始护理服务。', success: async ({ confirm }) => { if (!confirm) return; await workOrderStore.startService(task.value.orderId); uni.showToast({ title: '服务已开始', icon: 'success' }) } }) }
function goFinish() { uni.navigateTo({ url: `/subpkg-caregiver/service-finish/index?id=${task.value.orderId}` }) }
function openLocation() { uni.showToast({ title: '地图导航将在接入真实地址坐标后开放', icon: 'none' }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 170rpx; background: $page-gradient; }.loading-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: $page-gradient; }.status-card { display: flex; align-items: center; justify-content: space-between; min-height: 170rpx; padding: 30rpx; border-radius: 30rpx; color: #fff; box-shadow: $shadow-float; }.tone-warning { background: linear-gradient(135deg,#ff9a6c,#ff7b55); }.tone-primary { background: $primary-gradient; }.tone-success { background: linear-gradient(135deg,#00a99d,#2dd4bf); }.tone-neutral { background: linear-gradient(135deg,#728096,#9aa7b7); }.status-title,.status-desc { display: block; }.status-title { font-size: 40rpx; font-weight: 700; }.status-desc { margin-top: 9rpx; font-size: $font-size-sm; opacity: .84; }
.detail-card { margin-top: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.card-title { display: block; margin-bottom: 21rpx; color: $text-color; font-size: $font-size-md; font-weight: 700; }.service-head { display: flex; align-items: center; gap: 16rpx; margin-bottom: 14rpx; padding-bottom: 20rpx; border-bottom: 1rpx solid $divider-color; }.service-icon { display: flex; align-items: center; justify-content: center; width: 74rpx; height: 74rpx; border-radius: 23rpx; background: $primary-bg; }.service-copy { flex: 1; }.service-name,.secondary { display: block; }.service-name { color: $text-color; font-size: 30rpx; font-weight: 700; }.secondary { margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }.info-row { display: flex; justify-content: space-between; gap: 28rpx; padding: 14rpx 0; }.label { flex-shrink: 0; color: $text-color-hint; font-size: $font-size-sm; }.value { color: $text-color-secondary; font-size: $font-size-sm; text-align: right; }.value.multiline { line-height: 1.6; }.notice-box { display: flex; align-items: flex-start; gap: 10rpx; margin-top: 15rpx; padding: 17rpx; border-radius: 18rpx; background: #fff2eb; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.5; }.map-btn { display: flex; align-items: center; justify-content: center; gap: 8rpx; height: 68rpx; margin-top: 20rpx; border: 1rpx solid rgba(58,123,247,.22); border-radius: $radius-round; background: $primary-bg; color: $primary-color; font-size: $font-size-sm; line-height: 66rpx; }.map-btn::after { border: none; }
.record-row { position: relative; display: flex; gap: 15rpx; padding: 0 0 25rpx; }.record-row:last-child { padding-bottom: 0; }.record-dot { width: 17rpx; height: 17rpx; margin-top: 5rpx; flex-shrink: 0; border: 4rpx solid rgba(58,123,247,.2); border-radius: 50%; background: $primary-color; }.record-title,.record-summary,.record-time { display: block; }.record-title { color: $text-color-secondary; font-size: $font-size-sm; }.record-summary { margin-top: 7rpx; color: $text-color-hint; font-size: $font-size-xs; line-height: 1.5; }.record-time { margin-top: 7rpx; color: $text-color-disabled; font-size: $font-size-xs; }
.bottom-actions { position: fixed; left: 0; right: 0; bottom: 0; z-index: 20; display: flex; justify-content: flex-end; gap: 12rpx; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,.95); backdrop-filter: $glass-blur; box-shadow: 0 -6rpx 24rpx rgba(42,91,170,.08); }.action-btn { min-width: 170rpx; height: 68rpx; margin: 0; padding: 0 24rpx; border: 1rpx solid $border-color; border-radius: $radius-round; background: #fff; color: $text-color-secondary; font-size: $font-size-sm; line-height: 66rpx; }.action-btn::after { border: none; }.action-btn.primary { border: none; background: $primary-gradient; color: #fff; }.action-btn.danger { border-color: rgba(255,82,82,.2); color: $error-color; }
</style>
