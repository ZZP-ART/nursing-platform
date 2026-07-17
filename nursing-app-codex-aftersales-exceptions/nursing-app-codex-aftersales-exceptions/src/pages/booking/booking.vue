<template>
  <view v-if="service" class="page-shell">
    <view class="step-card service-summary">
      <view class="step-title"><text class="step-index">1</text><text>确认服务</text></view>
      <view class="service-row">
        <view class="service-icon"><u-icon name="heart-fill" size="28" color="#3A7BF7" /></view>
        <view class="service-info"><text class="service-name">{{ service.name }}</text><text class="service-desc">{{ selectedSpec?.name }} · {{ selectedSpec?.duration }}分钟</text></view>
        <text class="service-price">¥{{ selectedSpec?.price }}</text>
      </view>
      <view class="spec-list">
        <view v-for="spec in service.specs" :key="spec.specId" class="spec-option" :class="{ active: selectedSpec?.specId === spec.specId }" @click="selectedSpec = spec">
          <text class="spec-name">{{ spec.name }}</text><text class="spec-price">¥{{ spec.price }}</text>
        </view>
      </view>
    </view>

    <view class="step-card">
      <view class="step-title"><text class="step-index">2</text><text>服务地址</text></view>
      <view v-if="selectedAddress" class="address-row" @click="chooseAddress">
        <view class="address-icon"><u-icon name="map-fill" size="22" color="#3A7BF7" /></view>
        <view class="address-info"><text class="address-person">{{ selectedAddress.receiverName }} · {{ selectedAddress.receiverPhone }}</text><text class="address-text">{{ formatAddress(selectedAddress) }}</text></view>
        <u-icon name="arrow-right" size="16" color="#98A5B3" />
      </view>
      <view v-else class="empty-address" @click="chooseAddress"><u-icon name="plus-circle" size="22" color="#3A7BF7" /><text>请选择或添加服务地址</text></view>
    </view>

    <view class="step-card">
      <view class="step-title"><text class="step-index">3</text><text>预约时间</text></view>
      <scroll-view scroll-x :show-scrollbar="false"><view class="date-list"><view v-for="date in dates" :key="date.value" class="date-option" :class="{ active: serviceDate === date.value }" @click="serviceDate = date.value"><text>{{ date.week }}</text><text class="date-day">{{ date.label }}</text></view></view></scroll-view>
      <view class="slot-list"><view v-for="slot in slots" :key="slot.value" class="slot-option" :class="{ active: serviceTimeSlot === slot.value }" @click="serviceTimeSlot = slot.value"><text class="slot-label">{{ slot.label }}</text><text class="slot-time">{{ slot.time }}</text></view></view>
    </view>

    <view class="step-card">
      <view class="step-title"><text class="step-index">4</text><text>补充说明</text></view>
      <textarea v-model="remark" class="remark-input" maxlength="200" placeholder="可填写老人身体状况、上门注意事项等（选填）" />
      <text class="remark-count">{{ remark.length }}/200</text>
    </view>

    <view class="notice-card"><u-icon name="info-circle" size="18" color="#00B8A9" /><text>护理人员接单后会提前与您电话确认，请保持通讯畅通。</text></view>

    <view class="bottom-bar">
      <view><text class="total-label">合计</text><text class="total-price">¥{{ selectedSpec?.price || 0 }}</text></view>
      <u-button type="primary" shape="round" :loading="submitting" class="submit-btn" @click="submitOrder">提交并支付</u-button>
    </view>
  </view>
  <view v-else class="loading-page"><u-loading-icon size="32" color="#3A7BF7" /></view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { useServiceStore } from '@/store/service.js'
import { useAddressStore } from '@/store/address.js'
import { useOrderStore } from '@/store/order.js'

const serviceStore = useServiceStore()
const addressStore = useAddressStore()
const orderStore = useOrderStore()
const service = ref(null)
const selectedSpec = ref(null)
const selectedAddress = ref(null)
const serviceDate = ref('')
const serviceTimeSlot = ref('MORNING')
const remark = ref('')
const submitting = ref(false)

const slots = [
  { value: 'MORNING', label: '上午', time: '08:00 - 12:00' },
  { value: 'AFTERNOON', label: '下午', time: '13:00 - 17:00' },
  { value: 'EVENING', label: '晚上', time: '18:00 - 21:00' },
]

const dates = Array.from({ length: 7 }, (_, index) => {
  const date = new Date()
  date.setDate(date.getDate() + index + 1)
  const value = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return { value, label: `${date.getMonth() + 1}/${date.getDate()}`, week: index === 0 ? '明天' : ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()] }
})
serviceDate.value = dates[0].value

onLoad(async (options) => {
  const itemId = String(options.itemId || '')
  const specId = String(options.specId || '')
  service.value = await serviceStore.fetchServiceDetail(itemId)
  selectedSpec.value = service.value.specs.find((spec) => spec.specId === specId) || service.value.specs[0]
  await addressStore.fetchAddresses()
  selectedAddress.value = addressStore.defaultAddress
  uni.$on('address:selected', onAddressSelected)
})

onUnload(() => uni.$off('address:selected', onAddressSelected))

function onAddressSelected(address) { selectedAddress.value = address }
function formatAddress(address) { return `${address.province}${address.city}${address.district}${address.detailAddress}` }
function chooseAddress() { uni.navigateTo({ url: `/pages/address/address-list?mode=select&selectedId=${selectedAddress.value?.addressId || ''}` }) }

async function submitOrder() {
  if (!selectedSpec.value || !selectedAddress.value || !serviceDate.value) {
    uni.showToast({ title: '请完善预约信息', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    await orderStore.getPrepayToken()
    const order = await orderStore.createOrder({
      serviceItemId: service.value.itemId,
      serviceSpecId: selectedSpec.value.specId,
      addressId: selectedAddress.value.addressId,
      serviceDate: serviceDate.value,
      serviceTimeSlot: serviceTimeSlot.value,
      remark: remark.value.trim(),
    })
    try {
      const payment = await orderStore.executePayment(order.orderId)
      const status = payment.success ? 'success' : 'failed'
      uni.redirectTo({ url: `/pages/payment-result/payment-result?status=${status}&orderId=${order.orderId}&amount=${selectedSpec.value.price}` })
    } catch {
      uni.redirectTo({ url: `/pages/payment-result/payment-result?status=failed&orderId=${order.orderId}&amount=${selectedSpec.value.price}` })
    }
  } catch {
    uni.showToast({ title: '订单提交失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 190rpx; background: $page-gradient; }
.step-card { margin-bottom: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }
.step-title { display: flex; align-items: center; gap: 12rpx; margin-bottom: 22rpx; color: $text-color; font-size: $font-size-md; font-weight: 700; }
.step-index { width: 42rpx; height: 42rpx; display: flex; align-items: center; justify-content: center; border-radius: 15rpx; background: $primary-gradient; color: #fff; font-size: $font-size-xs; }
.service-row, .address-row { display: flex; align-items: center; gap: 18rpx; }
.service-icon, .address-icon { width: 70rpx; height: 70rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; border-radius: 22rpx; background: $primary-bg; }
.service-info, .address-info { flex: 1; min-width: 0; }
.service-name, .address-person { display: block; color: $text-color; font-size: $font-size-base; font-weight: 700; }
.service-desc, .address-text { display: block; margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; line-height: 1.5; }
.service-price { color: $warning-color; font-size: 34rpx; font-weight: 700; }
.spec-list { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 22rpx; }
.spec-option { display: flex; gap: 12rpx; padding: 12rpx 18rpx; border: 1rpx solid $border-color; border-radius: $radius-round; color: $text-color-secondary; font-size: $font-size-xs; }
.spec-option.active { border-color: $primary-color; color: $primary-color; background: $primary-bg; }
.spec-price { font-weight: 700; }
.empty-address { display: flex; align-items: center; justify-content: center; gap: 10rpx; min-height: 100rpx; border: 2rpx dashed rgba(58,123,247,0.25); border-radius: 20rpx; color: $primary-color; font-size: $font-size-sm; }
.date-list { display: flex; gap: 14rpx; }
.date-option { width: 104rpx; flex-shrink: 0; padding: 16rpx 8rpx; border-radius: 20rpx; background: $bg-color-grey; color: $text-color-secondary; text-align: center; font-size: $font-size-xs; }
.date-option.active { background: $primary-gradient; color: #fff; box-shadow: $shadow-glow; }
.date-day { display: block; margin-top: 8rpx; font-size: $font-size-base; font-weight: 700; }
.slot-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; margin-top: 20rpx; }
.slot-option { padding: 18rpx 8rpx; border: 1rpx solid $border-color; border-radius: 20rpx; text-align: center; }
.slot-option.active { border-color: $primary-color; background: $primary-bg; }
.slot-label { display: block; color: $text-color; font-size: $font-size-sm; font-weight: 700; }
.slot-time { display: block; margin-top: 6rpx; color: $text-color-hint; font-size: 17rpx; }
.remark-input { width: 100%; height: 150rpx; padding: 18rpx; border-radius: 18rpx; background: $bg-color-grey; color: $text-color; font-size: $font-size-sm; line-height: 1.6; }
.remark-count { display: block; margin-top: 8rpx; color: $text-color-disabled; font-size: 19rpx; text-align: right; }
.notice-card { display: flex; align-items: flex-start; gap: 10rpx; padding: 20rpx; border-radius: 20rpx; background: #e9fbf7; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.6; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; display: flex; align-items: center; justify-content: space-between; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,0.92); backdrop-filter: $glass-blur; box-shadow: 0 -6rpx 26rpx rgba(42,91,170,0.08); }
.total-label { color: $text-color-hint; font-size: $font-size-xs; }
.total-price { margin-left: 8rpx; color: $warning-color; font-size: 42rpx; font-weight: 700; }
.submit-btn { width: 280rpx !important; height: 88rpx !important; margin: 0 !important; border: none !important; background: $primary-gradient !important; box-shadow: $shadow-glow; font-weight: 600 !important; }
.loading-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: $page-gradient; }
</style>
