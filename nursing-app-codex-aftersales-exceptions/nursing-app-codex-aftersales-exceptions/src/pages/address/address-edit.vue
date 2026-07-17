<template>
  <view class="page-shell">
    <view class="page-intro">
      <text class="eyebrow">地址信息</text>
      <text class="page-title">{{ addressId ? '修改服务地址' : '添加服务地址' }}</text>
    </view>

    <view class="form-card">
      <view class="field"><text class="label">联系人</text><input v-model="form.receiverName" class="input" placeholder="请输入联系人姓名" /></view>
      <view class="field"><text class="label">手机号</text><input v-model="form.receiverPhone" class="input" type="number" maxlength="11" placeholder="请输入联系电话" /></view>
      <view class="field" @click="regionVisible = true">
        <text class="label">所在地区</text>
        <view class="picker-value"><text :class="{ placeholder: !regionText }">{{ regionText || '请选择省 / 市 / 区' }}</text><u-icon name="arrow-right" size="16" color="#98A5B3" /></view>
      </view>
      <view class="field"><text class="label">详细地址</text><textarea v-model="form.detailAddress" class="textarea" maxlength="100" placeholder="街道、门牌号、小区楼栋等" /></view>
      <view class="field"><text class="label">地址标签</text><view class="tag-list"><text v-for="tag in tags" :key="tag" class="tag-option" :class="{ active: form.tag === tag }" @click="form.tag = tag">{{ tag }}</text></view></view>
      <view class="switch-row"><view><text class="switch-title">设为默认地址</text><text class="switch-desc">下次预约时优先使用</text></view><switch :checked="form.isDefault === 1" color="#3A7BF7" @change="form.isDefault = $event.detail.value ? 1 : 0" /></view>
    </view>

    <u-button type="primary" shape="round" :loading="submitting" class="primary-btn" @click="submit">保存地址</u-button>

    <u-picker :show="regionVisible" :columns="regionColumns" key-name="label" @confirm="confirmRegion" @cancel="regionVisible = false" />
  </view>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAddressStore } from '@/store/address.js'

const addressStore = useAddressStore()
const addressId = ref(null)
const submitting = ref(false)
const regionVisible = ref(false)
const tags = ['家', '公司', '学校', '其他']
const regionColumns = [[
  { label: '北京市 / 北京市 / 朝阳区', value: ['北京市', '北京市', '朝阳区'] },
  { label: '北京市 / 北京市 / 海淀区', value: ['北京市', '北京市', '海淀区'] },
  { label: '上海市 / 上海市 / 浦东新区', value: ['上海市', '上海市', '浦东新区'] },
  { label: '广东省 / 广州市 / 天河区', value: ['广东省', '广州市', '天河区'] },
  { label: '浙江省 / 杭州市 / 西湖区', value: ['浙江省', '杭州市', '西湖区'] },
]]

const form = reactive({ receiverName: '', receiverPhone: '', tag: '家', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
const regionText = computed(() => [form.province, form.city, form.district].filter(Boolean).join(' / '))

onLoad(async (options) => {
  if (!options.id) return
  addressId.value = String(options.id)
  if (!addressStore.addresses.length) await addressStore.fetchAddresses()
  const address = addressStore.addresses.find((item) => item.addressId === addressId.value)
  if (address) Object.assign(form, address)
})

function confirmRegion({ value }) {
  const selected = value[0]?.value || []
  ;[form.province, form.city, form.district] = selected
  regionVisible.value = false
}

async function submit() {
  if (form.receiverName.trim().length < 2 || form.receiverPhone.length !== 11 || !form.province || form.detailAddress.trim().length < 5) {
    uni.showToast({ title: '请完整填写地址信息', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const payload = {
      receiverName: form.receiverName.trim(),
      receiverPhone: form.receiverPhone,
      tag: form.tag,
      province: form.province,
      city: form.city,
      district: form.district,
      detailAddress: form.detailAddress.trim(),
      isDefault: form.isDefault,
    }
    if (addressId.value) await addressStore.updateAddress(addressId.value, payload)
    else await addressStore.addAddress(payload)
    uni.showToast({ title: '地址已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 30rpx $spacing-base 60rpx; background: $page-gradient; }
.page-intro { margin-bottom: 28rpx; }
.eyebrow { display: block; color: $primary-color; font-size: $font-size-sm; font-weight: 600; }
.page-title { display: block; margin-top: 8rpx; color: $text-color; font-size: 38rpx; font-weight: 700; }
.form-card { padding: 10rpx 24rpx; border: $glass-border-soft; border-radius: 30rpx; background: $surface-gradient; box-shadow: $shadow-float; }
.field { padding: 24rpx 0; border-bottom: 1rpx solid $divider-color; }
.label { display: block; margin-bottom: 14rpx; color: $text-color-secondary; font-size: $font-size-sm; font-weight: 600; }
.input, .textarea { width: 100%; color: $text-color; font-size: $font-size-base; }
.textarea { height: 130rpx; line-height: 1.6; }
.picker-value { display: flex; align-items: center; justify-content: space-between; color: $text-color; font-size: $font-size-base; }
.placeholder { color: $text-color-hint; }
.tag-list { display: flex; flex-wrap: wrap; gap: 14rpx; }
.tag-option { padding: 12rpx 24rpx; border-radius: $radius-round; background: $bg-color-grey; color: $text-color-secondary; font-size: $font-size-sm; }
.tag-option.active { color: #fff; background: $primary-gradient; }
.switch-row { display: flex; align-items: center; justify-content: space-between; padding: 26rpx 0; }
.switch-title { display: block; color: $text-color; font-size: $font-size-base; font-weight: 600; }
.switch-desc { display: block; margin-top: 6rpx; color: $text-color-hint; font-size: $font-size-xs; }
.primary-btn { height: 88rpx !important; margin-top: 34rpx; border: none !important; background: $primary-gradient !important; box-shadow: $shadow-glow; font-weight: 600 !important; }
</style>
