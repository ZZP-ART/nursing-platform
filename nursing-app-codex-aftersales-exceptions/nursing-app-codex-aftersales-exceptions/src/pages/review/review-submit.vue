<template>
  <view class="page-shell">
    <view class="intro-card"><u-icon name="heart-fill" size="32" color="#3A7BF7" /><text class="intro-title">这次服务体验如何？</text><text class="intro-desc">您的真实评价会帮助我们持续提升护理品质</text></view>
    <view class="form-card">
      <text class="section-title">服务评分</text>
      <view class="stars"><text v-for="star in 5" :key="star" class="star" :class="{ active: star <= rating }" @click="rating = star">★</text></view>
      <text class="rating-text">{{ ratingLabels[rating] }}</text>
      <text class="section-title content-title">评价内容</text>
      <textarea v-model="content" class="content-input" maxlength="500" placeholder="说说护理人员的专业度、服务态度和整体体验吧" />
      <text class="count">{{ content.length }}/500</text>
      <text class="section-title content-title">添加图片</text>
      <view class="image-list"><view v-for="(image, index) in images" :key="image" class="image-item"><image :src="image" mode="aspectFill" /><view class="remove-image" @click="images.splice(index, 1)">×</view></view><view v-if="images.length < 6" class="add-image" @click="chooseImages"><u-icon name="camera-fill" size="25" color="#98A5B3" /><text>上传图片</text></view></view>
    </view>
    <u-button type="primary" shape="round" :loading="submitting" class="submit-btn" @click="submit">提交评价</u-button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useReviewStore } from '@/store/review.js'
import { useUserStore } from '@/store/user.js'

const reviewStore = useReviewStore(); const userStore = useUserStore(); const orderId = ref(null); const rating = ref(5); const content = ref(''); const images = ref([]); const submitting = ref(false)
const ratingLabels = ['', '非常不满意', '不满意', '一般', '满意', '非常满意']
onLoad((options) => { orderId.value = String(options.orderId || '') })
function chooseImages() { uni.chooseImage({ count: 6 - images.value.length, success: ({ tempFilePaths }) => { images.value.push(...tempFilePaths) } }) }
async function submit() {
  if (!rating.value) return uni.showToast({ title: '请选择服务评分', icon: 'none' })
  if (!content.value.trim()) return uni.showToast({ title: '请填写评价内容', icon: 'none' })
  submitting.value = true
  try {
    const uploaded = []
    for (const path of images.value) { try { const file = await userStore.uploadFile(path, 'review'); uploaded.push(file.fileUrl) } catch { uploaded.push(path) } }
    await reviewStore.submitReview({ orderId: orderId.value, rating: rating.value, content: content.value.trim(), images: uploaded })
    uni.showToast({ title: '感谢您的评价', icon: 'success' }); setTimeout(() => uni.navigateBack(), 700)
  } finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 30rpx $spacing-base 60rpx; background: $page-gradient; }.intro-card { display: flex; flex-direction: column; align-items: center; padding: 36rpx; border-radius: 30rpx; background: linear-gradient(145deg,#edf4fe,#e9fbf7); }.intro-title { margin-top: 14rpx; color: $text-color; font-size: 34rpx; font-weight: 700; }.intro-desc { margin-top: 8rpx; color: $text-color-hint; font-size: $font-size-xs; }.form-card { margin-top: 22rpx; padding: 28rpx 24rpx; border: $glass-border-soft; border-radius: 30rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.section-title { display: block; color: $text-color; font-size: $font-size-md; font-weight: 700; }.stars { display: flex; justify-content: center; gap: 18rpx; margin-top: 28rpx; }.star { color: #dfe5ed; font-size: 58rpx; }.star.active { color: #ffb55f; }.rating-text { display: block; margin-top: 10rpx; color: $warning-color; font-size: $font-size-sm; text-align: center; }.content-title { margin-top: 34rpx; }.content-input { width: 100%; height: 190rpx; margin-top: 18rpx; padding: 20rpx; border-radius: 20rpx; background: $bg-color-grey; color: $text-color; font-size: $font-size-sm; line-height: 1.6; }.count { display: block; margin-top: 7rpx; color: $text-color-disabled; font-size: $font-size-xs; text-align: right; }.image-list { display: flex; flex-wrap: wrap; gap: 14rpx; margin-top: 18rpx; }.image-item,.add-image { position: relative; width: 150rpx; height: 150rpx; border-radius: 20rpx; overflow: hidden; }.image-item image { width: 100%; height: 100%; }.remove-image { position: absolute; top: 6rpx; right: 6rpx; width: 38rpx; height: 38rpx; border-radius: 50%; background: rgba(26,37,48,0.65); color: #fff; font-size: 30rpx; line-height: 34rpx; text-align: center; }.add-image { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8rpx; border: 2rpx dashed $border-color; color: $text-color-hint; font-size: $font-size-xs; }.submit-btn { height: 88rpx !important; margin-top: 30rpx; border: none !important; background: $primary-gradient !important; box-shadow: $shadow-glow; font-weight: 600 !important; }
</style>
