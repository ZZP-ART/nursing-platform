<template>
  <view class="page-shell">
    <view class="page-intro">
      <text class="eyebrow">上门服务地址</text>
      <text class="page-title">选择护理人员上门地点</text>
      <text class="page-desc">请确保联系人与详细地址准确无误</text>
    </view>

    <view v-if="addressStore.loading" class="loading-wrap"><u-loading-icon size="28" color="#3A7BF7" /></view>
    <empty-state v-else-if="!addressStore.addresses.length" title="还没有服务地址" description="添加地址后即可预约上门护理" />
    <view v-else class="address-list">
      <view
        v-for="address in addressStore.addresses"
        :key="address.addressId"
        class="address-card"
        :class="{ selected: selectedId === address.addressId }"
        @click="selectAddress(address)"
      >
        <view class="address-icon"><u-icon name="map-fill" size="24" color="#3A7BF7" /></view>
        <view class="address-main">
          <view class="person-row">
            <text class="person-name">{{ address.receiverName }}</text>
            <text class="person-phone">{{ address.receiverPhone }}</text>
            <text v-if="address.tag" class="tag">{{ address.tag }}</text>
            <text v-if="address.isDefault === 1" class="default-tag">默认</text>
          </view>
          <text class="address-text">{{ formatAddress(address) }}</text>
          <view class="card-actions" @click.stop>
            <text v-if="address.isDefault !== 1" class="action-link" @click="setDefault(address.addressId)">设为默认</text>
            <text class="action-link" @click="editAddress(address.addressId)">编辑</text>
            <text class="action-link danger" @click="removeAddress(address.addressId)">删除</text>
          </view>
        </view>
        <u-icon v-if="selectMode" :name="selectedId === address.addressId ? 'checkmark-circle-fill' : 'more-circle'" size="22" :color="selectedId === address.addressId ? '#3A7BF7' : '#C5CDD8'" />
      </view>
    </view>

    <view class="bottom-action">
      <u-button type="primary" shape="round" class="primary-btn" @click="addAddress">
        <u-icon name="plus" size="18" color="#FFFFFF" /> 添加新地址
      </u-button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAddressStore } from '@/store/address.js'
import EmptyState from '@/components/base/empty-state.vue'

const addressStore = useAddressStore()
const selectMode = ref(false)
const selectedId = ref(null)

onLoad((options) => {
  selectMode.value = options.mode === 'select'
  selectedId.value = options.selectedId ? String(options.selectedId) : null
})

onMounted(() => addressStore.fetchAddresses())
onShow(() => {
  if (addressStore.addresses.length) addressStore.fetchAddresses()
})

function formatAddress(address) {
  return `${address.province}${address.city}${address.district}${address.detailAddress}`
}

function selectAddress(address) {
  if (!selectMode.value) return
  uni.$emit('address:selected', address)
  uni.navigateBack()
}

function addAddress() { uni.navigateTo({ url: '/pages/address/address-edit' }) }
function editAddress(id) { uni.navigateTo({ url: `/pages/address/address-edit?id=${id}` }) }

async function setDefault(id) {
  await addressStore.setDefault(id)
  uni.showToast({ title: '默认地址已更新', icon: 'success' })
}

function removeAddress(id) {
  uni.showModal({
    title: '删除地址',
    content: '确认删除这个服务地址吗？',
    success: async ({ confirm }) => {
      if (!confirm) return
      await addressStore.removeAddress(id)
      uni.showToast({ title: '地址已删除', icon: 'success' })
    },
  })
}
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 30rpx $spacing-base 160rpx; background: $page-gradient; }
.page-intro { margin-bottom: 28rpx; }
.eyebrow { display: block; color: $primary-color; font-size: $font-size-sm; font-weight: 600; }
.page-title { display: block; margin-top: 8rpx; color: $text-color; font-size: 38rpx; font-weight: 700; }
.page-desc { display: block; margin-top: 10rpx; color: $text-color-hint; font-size: $font-size-sm; }
.loading-wrap { padding: 140rpx 0; display: flex; justify-content: center; }
.address-card { display: flex; align-items: flex-start; gap: 18rpx; margin-bottom: 18rpx; padding: 26rpx 22rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }
.address-card.selected { border-color: rgba(58,123,247,0.4); box-shadow: 0 8rpx 28rpx rgba(58,123,247,0.12); }
.address-icon { width: 68rpx; height: 68rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; border-radius: 22rpx; background: $primary-bg; }
.address-main { flex: 1; min-width: 0; }
.person-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10rpx; }
.person-name { color: $text-color; font-size: 30rpx; font-weight: 700; }
.person-phone { color: $text-color-secondary; font-size: $font-size-sm; }
.tag, .default-tag { padding: 4rpx 10rpx; border-radius: $radius-round; font-size: 19rpx; }
.tag { color: $primary-color; background: $primary-bg; }
.default-tag { color: $success-color; background: #e9fbf7; }
.address-text { display: block; margin-top: 12rpx; color: $text-color-secondary; font-size: $font-size-sm; line-height: 1.6; }
.card-actions { display: flex; gap: 28rpx; margin-top: 20rpx; }
.action-link { color: $primary-color; font-size: $font-size-xs; }
.action-link.danger { color: $warning-color; }
.bottom-action { position: fixed; left: 0; right: 0; bottom: 0; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,0.9); backdrop-filter: $glass-blur; }
.primary-btn { height: 88rpx !important; border: none !important; background: $primary-gradient !important; box-shadow: $shadow-glow; font-weight: 600 !important; }
</style>
