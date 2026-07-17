<template>
  <view v-if="service" class="page-shell">
    <view class="status-card" :class="`tone-${auditMeta.tone}`"><view><text class="status-title">{{ auditMeta.text }}</text><text class="status-desc">{{ statusDescription }}</text></view><u-icon :name="statusIcon" size="44" color="rgba(255,255,255,.92)" /></view>

    <view v-if="service.rejectReason" class="reject-card"><u-icon name="warning-fill" size="22" color="#FF8A5C" /><view><text class="reject-title">审核驳回原因</text><text class="reject-desc">{{ service.rejectReason }}</text></view></view>
    <view v-if="service.auditStatus === SERVICE_AUDIT_STATUS.PENDING_REVIEW" class="review-card"><u-loading-icon size="22" color="#3A7BF7" /><view><text class="review-title">平台审核中</text><text class="review-desc">Mock 环境约 8 秒后自动审核通过，点击“刷新状态”查看结果。</text></view><text class="refresh-link" @click="refresh">刷新状态</text></view>

    <view class="detail-card"><text class="card-title">服务信息</text><view class="service-head"><view class="cover"><image v-if="service.coverImage" :src="service.coverImage" mode="aspectFill" /><u-icon v-else name="grid" size="32" color="#98A5B3" /></view><view class="service-copy"><text class="service-name">{{ service.name }}</text><text class="secondary">{{ service.categoryName }}</text><text class="price">¥{{ service.minPrice }} 起</text></view></view><view class="description">{{ service.description }}</view></view>

    <view class="detail-card"><text class="card-title">服务规格</text><view v-for="spec in service.specs" :key="spec.specId" class="spec-row"><view><text class="spec-name">{{ spec.name }}</text><text class="spec-duration">服务时长 {{ spec.duration }} 分钟</text></view><view class="spec-price"><text>¥{{ spec.price }}</text><text v-if="spec.originalPrice > spec.price" class="original-price">¥{{ spec.originalPrice }}</text></view></view></view>

    <view class="detail-card"><text class="card-title">版本与发布</text><view class="info-row"><text class="label">当前版本</text><text class="value">v{{ service.version }}</text></view><view class="info-row"><text class="label">发布状态</text><text class="value" :class="{ published: service.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED }">{{ service.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED ? '已上架' : '未上架' }}</text></view><view class="info-row"><text class="label">更新时间</text><text class="value">{{ formatTime(service.updateTime) }}</text></view></view>

    <view v-if="service.auditRecords?.length" class="detail-card"><text class="card-title">审核记录</text><view v-for="record in service.auditRecords" :key="record.recordId" class="record-row"><view class="record-dot" /><view><text class="record-title">{{ actionText(record.action) }} · {{ record.operatorName }}</text><text class="record-desc">{{ record.remark }}</text><text class="record-time">{{ formatTime(record.createTime) }}</text></view></view></view>

    <view v-if="hasActions" class="bottom-actions">
      <button v-if="canEdit" class="action-btn" @click="editService">编辑服务</button>
      <button v-if="canSubmit" class="action-btn primary" @click="submitReview">提交审核</button>
      <button v-if="canPublish" class="action-btn primary" @click="publish">上架服务</button>
      <button v-if="canOffline" class="action-btn danger" @click="offline">下架服务</button>
    </view>
  </view>
  <view v-else class="loading-page"><u-loading-icon size="32" color="#3A7BF7" /></view>
</template>

<script setup>
import { computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { ROLES } from '@/constants/roles.js'
import {
  SERVICE_AUDIT_STATUS,
  SERVICE_PUBLISH_STATUS,
  canEditService,
  canOfflineService,
  canPublishService,
  canSubmitService,
  getServiceAuditMeta,
} from '@/constants/service-status.js'
import { useServiceManageStore } from '@/store/service-manage.js'
import { requireRole } from '@/utils/permission.js'

const serviceManageStore = useServiceManageStore()
let itemId = null
const service = computed(() => serviceManageStore.currentService)
const auditMeta = computed(() => getServiceAuditMeta(service.value?.auditStatus))
const canEdit = computed(() => canEditService(service.value))
const canSubmit = computed(() => canSubmitService(service.value))
const canPublish = computed(() => canPublishService(service.value))
const canOffline = computed(() => canOfflineService(service.value))
const hasActions = computed(() => canEdit.value || canSubmit.value || canPublish.value || canOffline.value)
const statusDescription = computed(() => ({ DRAFT: '服务仍为草稿，可继续编辑后提交审核', PENDING_REVIEW: '平台正在审核服务资料，审核期间不可编辑', APPROVED: service.value?.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED ? '服务已审核通过并在顾客端展示' : '服务已审核通过，可以上架', REJECTED: '请根据审核意见修改后重新提交' }[service.value?.auditStatus] || '服务状态已更新'))
const statusIcon = computed(() => ({ DRAFT: 'edit-pen', PENDING_REVIEW: 'clock-fill', APPROVED: 'checkmark-circle-fill', REJECTED: 'close-circle-fill' }[service.value?.auditStatus] || 'info-circle'))

onLoad(async (options) => { if (!requireRole(ROLES.MERCHANT_MEMBER)) return; itemId = String(options.id || ''); await refresh() })
onShow(async () => { if (itemId) await refresh() })
async function refresh() { await serviceManageStore.fetchServiceDetail(itemId) }
function formatTime(value) { return value ? value.replace('T', ' ').replace('+08:00', '').slice(0, 16) : '--' }
function actionText(action) { return ({ SUBMIT: '提交审核', APPROVE: '审核通过', REJECT: '审核驳回' }[action] || action) }
function editService() { uni.navigateTo({ url: `/subpkg-merchant/service-edit/index?id=${service.value.itemId}` }) }
async function submitReview() { await serviceManageStore.submitService(service.value.itemId); uni.showToast({ title: '已提交审核', icon: 'success' }) }
async function publish() { await serviceManageStore.publishService(service.value.itemId); await refresh(); uni.showToast({ title: '服务已上架', icon: 'success' }) }
function offline() { uni.showModal({ title: '下架服务', content: '下架后顾客端将不再展示，但不会影响历史订单。', success: async ({ confirm }) => { if (!confirm) return; await serviceManageStore.offlineService(service.value.itemId); await refresh(); uni.showToast({ title: '服务已下架', icon: 'success' }) } }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 170rpx; background: $page-gradient; }.loading-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: $page-gradient; }.status-card { display: flex; align-items: center; justify-content: space-between; min-height: 170rpx; padding: 30rpx; border-radius: 30rpx; color: #fff; box-shadow: $shadow-float; }.tone-primary { background: $primary-gradient; }.tone-success { background: linear-gradient(135deg,#00a99d,#2dd4bf); }.tone-warning { background: linear-gradient(135deg,#ff9a6c,#ff7b55); }.tone-neutral { background: linear-gradient(135deg,#728096,#9aa7b7); }.status-title,.status-desc { display: block; }.status-title { font-size: 40rpx; font-weight: 700; }.status-desc { margin-top: 9rpx; font-size: $font-size-sm; opacity: .84; }
.reject-card,.review-card { display: flex; align-items: flex-start; gap: 13rpx; margin-top: 20rpx; padding: 21rpx; border-radius: 23rpx; }.reject-card { background: #fff2eb; }.review-card { align-items: center; background: $primary-bg; }.reject-title,.reject-desc,.review-title,.review-desc { display: block; }.reject-title,.review-title { color: $text-color; font-size: $font-size-sm; font-weight: 600; }.reject-desc,.review-desc { margin-top: 5rpx; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.5; }.refresh-link { margin-left: auto; flex-shrink: 0; color: $primary-color; font-size: $font-size-xs; }
.detail-card { margin-top: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.card-title { display: block; margin-bottom: 21rpx; color: $text-color; font-size: $font-size-md; font-weight: 700; }.service-head { display: flex; align-items: center; gap: 17rpx; }.cover { display: flex; align-items: center; justify-content: center; width: 120rpx; height: 120rpx; flex-shrink: 0; overflow: hidden; border-radius: 24rpx; background: $bg-color-grey; }.cover image { width: 100%; height: 100%; }.service-copy { flex: 1; }.service-name,.secondary,.price { display: block; }.service-name { color: $text-color; font-size: 31rpx; font-weight: 700; }.secondary { margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }.price { margin-top: 10rpx; color: $warning-color; font-size: $font-size-sm; font-weight: 600; }.description { margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid $divider-color; color: $text-color-secondary; font-size: $font-size-sm; line-height: 1.7; white-space: pre-wrap; }
.spec-row { display: flex; align-items: center; justify-content: space-between; padding: 17rpx 0; border-bottom: 1rpx solid $divider-color; }.spec-row:last-child { border-bottom: none; }.spec-name,.spec-duration { display: block; }.spec-name { color: $text-color; font-size: $font-size-sm; font-weight: 600; }.spec-duration { margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }.spec-price { text-align: right; color: $warning-color; font-size: $font-size-base; font-weight: 700; }.original-price { display: block; margin-top: 3rpx; color: $text-color-disabled; font-size: $font-size-xs; font-weight: 400; text-decoration: line-through; }.info-row { display: flex; align-items: center; justify-content: space-between; padding: 14rpx 0; }.label { color: $text-color-hint; font-size: $font-size-sm; }.value { color: $text-color-secondary; font-size: $font-size-sm; }.value.published { color: $success-color; }
.record-row { display: flex; gap: 14rpx; padding-bottom: 24rpx; }.record-row:last-child { padding-bottom: 0; }.record-dot { width: 17rpx; height: 17rpx; margin-top: 5rpx; flex-shrink: 0; border: 4rpx solid rgba(58,123,247,.2); border-radius: 50%; background: $primary-color; }.record-title,.record-desc,.record-time { display: block; }.record-title { color: $text-color-secondary; font-size: $font-size-sm; }.record-desc { margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; line-height: 1.5; }.record-time { margin-top: 6rpx; color: $text-color-disabled; font-size: $font-size-xs; }
.bottom-actions { position: fixed; left: 0; right: 0; bottom: 0; display: flex; justify-content: flex-end; gap: 13rpx; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,.95); box-shadow: 0 -6rpx 24rpx rgba(42,91,170,.08); }.action-btn { height: 68rpx; margin: 0; padding: 0 25rpx; border: 1rpx solid $border-color; border-radius: $radius-round; background: #fff; color: $text-color-secondary; font-size: $font-size-sm; line-height: 66rpx; }.action-btn::after { border: none; }.action-btn.primary { border: none; background: $primary-gradient; color: #fff; }.action-btn.danger { border-color: rgba(255,82,82,.22); color: $error-color; }
</style>
