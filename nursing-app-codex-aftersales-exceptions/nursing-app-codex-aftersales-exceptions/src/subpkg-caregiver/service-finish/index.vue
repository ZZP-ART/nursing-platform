<template>
  <view class="page-shell">
    <view class="notice-card"><u-icon name="info-circle-fill" size="23" color="#3A7BF7" /><view><text class="notice-title">提交后将等待顾客确认</text><text class="notice-desc">请如实填写本次服务内容和异常情况。</text></view></view>

    <view class="form-card">
      <text class="field-label">服务摘要 <text class="required">*</text></text>
      <textarea v-model="summary" maxlength="500" class="summary-input" placeholder="请描述已完成的护理项目、用户状态和注意事项" />
      <text class="counter">{{ summary.length }}/500</text>

      <text class="field-label section-label">服务结果</text>
      <view class="result-options"><view v-for="item in results" :key="item.value" class="result-item" :class="{ active: result === item.value }" @click="result = item.value"><u-icon :name="item.icon" size="23" :color="result === item.value ? '#3A7BF7' : '#98A5B3'" /><text>{{ item.label }}</text></view></view>

      <text class="field-label section-label">异常说明</text>
      <textarea v-model="exceptionNote" maxlength="300" class="exception-input" placeholder="无异常可不填写；如有异常请说明处理情况" />

      <view class="confirm-row" @click="confirmed = !confirmed"><view class="checkbox" :class="{ checked: confirmed }"><u-icon v-if="confirmed" name="checkmark" size="13" color="#FFFFFF" /></view><text>我确认以上服务记录真实、完整</text></view>
    </view>

    <view class="bottom-bar"><u-button type="primary" shape="round" :loading="submitting" :disabled="!canSubmit" class="submit-btn" @click="submit">提交并结束服务</u-button></view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ROLES } from '@/constants/roles.js'
import { useWorkOrderStore } from '@/store/work-order.js'
import { requireRole } from '@/utils/permission.js'

const workOrderStore = useWorkOrderStore()
const orderId = ref(null)
const summary = ref('')
const exceptionNote = ref('')
const result = ref('NORMAL')
const confirmed = ref(false)
const submitting = ref(false)
const results = [
  { value: 'NORMAL', label: '正常完成', icon: 'checkmark-circle' },
  { value: 'FOLLOW_UP', label: '需要复查', icon: 'calendar' },
  { value: 'EXCEPTION', label: '存在异常', icon: 'warning' },
]
const canSubmit = computed(() => summary.value.trim().length >= 10 && confirmed.value && !submitting.value)

onLoad((options) => {
  if (!requireRole(ROLES.CAREGIVER)) return
  orderId.value = String(options.id || '')
})

async function submit() {
  if (!canSubmit.value) return
  if (result.value === 'EXCEPTION' && !exceptionNote.value.trim()) {
    uni.showToast({ title: '请填写异常说明', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await workOrderStore.finishService(orderId.value, {
      summary: summary.value.trim(),
      result: result.value,
      exceptionNote: exceptionNote.value.trim(),
    })
    uni.showToast({ title: '服务记录已提交', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: `/subpkg-caregiver/task-detail/index?id=${orderId.value}` }), 700)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 150rpx; background: $page-gradient; }.notice-card { display: flex; align-items: flex-start; gap: 14rpx; padding: 22rpx; border-radius: 24rpx; background: $primary-bg; }.notice-title,.notice-desc { display: block; }.notice-title { color: $text-color; font-size: $font-size-sm; font-weight: 600; }.notice-desc { margin-top: 5rpx; color: $text-color-secondary; font-size: $font-size-xs; }
.form-card { margin-top: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.field-label { display: block; color: $text-color; font-size: $font-size-base; font-weight: 600; }.required { color: $error-color; }.section-label { margin-top: 30rpx; }.summary-input,.exception-input { width: 100%; margin-top: 15rpx; padding: 20rpx; border: 1rpx solid $border-color; border-radius: 22rpx; background: rgba(255,255,255,.72); color: $text-color; font-size: $font-size-sm; line-height: 1.6; }.summary-input { height: 250rpx; }.exception-input { height: 180rpx; }.counter { display: block; margin-top: 8rpx; color: $text-color-disabled; font-size: $font-size-xs; text-align: right; }
.result-options { display: grid; grid-template-columns: repeat(3,1fr); gap: 12rpx; margin-top: 15rpx; }.result-item { display: flex; flex-direction: column; align-items: center; gap: 8rpx; padding: 20rpx 6rpx; border: 2rpx solid transparent; border-radius: 20rpx; background: $bg-color-grey; color: $text-color-secondary; font-size: $font-size-xs; }.result-item.active { border-color: rgba(58,123,247,.32); background: $primary-bg; color: $primary-color; }
.confirm-row { display: flex; align-items: center; gap: 12rpx; margin-top: 28rpx; color: $text-color-secondary; font-size: $font-size-sm; }.checkbox { display: flex; align-items: center; justify-content: center; width: 34rpx; height: 34rpx; border: 2rpx solid $border-color; border-radius: 10rpx; background: #fff; }.checkbox.checked { border-color: $primary-color; background: $primary-color; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,.95); box-shadow: 0 -6rpx 24rpx rgba(42,91,170,.08); }.submit-btn { height: 84rpx !important; border: none !important; background: $primary-gradient !important; font-size: 30rpx !important; font-weight: 600 !important; }
</style>
