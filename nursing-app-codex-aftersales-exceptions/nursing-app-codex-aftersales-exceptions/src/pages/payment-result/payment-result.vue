<template>
  <view class="result-page">
    <view class="result-card">
      <view class="result-icon" :class="status"><u-icon :name="status === 'success' ? 'checkmark-circle-fill' : 'close-circle-fill'" size="72" :color="status === 'success' ? '#00B8A9' : '#FF8A5C'" /></view>
      <text class="result-title">{{ status === 'success' ? '支付成功' : '支付未完成' }}</text>
      <text class="result-desc">{{ status === 'success' ? '护理服务已预约成功，我们会尽快安排人员' : '订单已保留，可前往订单详情重新支付' }}</text>
      <view v-if="amount" class="amount-row"><text>支付金额</text><text class="amount">¥{{ amount }}</text></view>
      <view class="result-actions">
        <u-button type="primary" shape="round" class="primary-btn" @click="viewOrder">查看订单</u-button>
        <u-button shape="round" class="secondary-btn" @click="goHome">返回首页</u-button>
      </view>
    </view>
    <view class="care-note"><u-icon name="heart-fill" size="18" color="#3A7BF7" /><text>感谢您的信任，智慧护理将全程守护本次服务。</text></view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const status = ref('success')
const orderId = ref(null)
const amount = ref('')

onLoad((options) => {
  status.value = options.status || 'success'
  orderId.value = options.orderId ? String(options.orderId) : null
  amount.value = options.amount || ''
})

function viewOrder() { uni.redirectTo({ url: `/pages/order/order-detail?id=${orderId.value}` }) }
function goHome() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style lang="scss" scoped>
.result-page { min-height: 100vh; padding: 100rpx $spacing-base 40rpx; background: $page-gradient; }
.result-card { padding: 54rpx 34rpx 36rpx; border: $glass-border-soft; border-radius: 36rpx; background: $surface-gradient; box-shadow: $shadow-float; text-align: center; }
.result-icon { width: 130rpx; height: 130rpx; display: flex; align-items: center; justify-content: center; margin: 0 auto; border-radius: 50%; background: #e9fbf7; }
.result-icon.failed { background: #fff2eb; }
.result-title { display: block; margin-top: 28rpx; color: $text-color; font-size: 42rpx; font-weight: 700; }
.result-desc { display: block; max-width: 540rpx; margin: 14rpx auto 0; color: $text-color-secondary; font-size: $font-size-sm; line-height: 1.7; }
.amount-row { display: flex; align-items: center; justify-content: space-between; margin-top: 34rpx; padding: 22rpx; border-radius: 20rpx; background: $bg-color-grey; color: $text-color-secondary; font-size: $font-size-sm; }
.amount { color: $warning-color; font-size: 36rpx; font-weight: 700; }
.result-actions { margin-top: 36rpx; }
.primary-btn, .secondary-btn { height: 88rpx !important; margin-top: 18rpx; }
.primary-btn { border: none !important; background: $primary-gradient !important; box-shadow: $shadow-glow; font-weight: 600 !important; }
.secondary-btn { color: $primary-color !important; border-color: rgba(58,123,247,0.25) !important; background: $primary-bg !important; }
.care-note { display: flex; align-items: center; justify-content: center; gap: 8rpx; margin-top: 30rpx; color: $text-color-hint; font-size: $font-size-xs; }
</style>
