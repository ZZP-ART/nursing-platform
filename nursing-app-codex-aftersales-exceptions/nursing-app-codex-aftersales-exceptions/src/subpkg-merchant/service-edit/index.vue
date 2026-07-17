<template>
  <view class="page-shell">
    <view class="form-card">
      <text class="section-title">基础信息</text>
      <text class="field-label">服务名称 <text class="required">*</text></text><input v-model="form.name" maxlength="30" class="text-input" placeholder="请输入服务名称" />
      <text class="field-label">服务分类 <text class="required">*</text></text><picker :range="serviceStore.categories" range-key="name" :value="categoryIndex" @change="changeCategory"><view class="picker-input" :class="{ placeholder: !form.categoryId }">{{ selectedCategoryName || '请选择服务分类' }}<u-icon name="arrow-right" size="15" color="#C5CDD8" /></view></picker>
      <text class="field-label">封面图片地址</text><input v-model="form.coverImage" class="text-input" placeholder="可填写图片 URL，暂不要求" />
      <text class="field-label">服务说明 <text class="required">*</text></text><textarea v-model="form.description" maxlength="2000" class="description-input" placeholder="请说明服务内容、适用人群、服务流程、禁忌事项和风险提示" /><text class="counter">{{ form.description.length }}/2000</text>
    </view>

    <view class="form-card"><view class="section-head"><text class="section-title">服务规格</text><text class="add-spec" @click="addSpec">+ 添加规格</text></view><view v-for="(spec, index) in form.specs" :key="spec.localId" class="spec-card"><view class="spec-head"><text>规格 {{ index + 1 }}</text><text v-if="form.specs.length > 1" class="remove-spec" @click="removeSpec(index)">删除</text></view><text class="field-label compact">规格名称 <text class="required">*</text></text><input v-model="spec.name" maxlength="20" class="text-input" placeholder="例如：单次服务" /><view class="spec-grid"><view><text class="field-label compact">价格（元）</text><input v-model="spec.price" type="digit" class="text-input" placeholder="0" /></view><view><text class="field-label compact">原价（元）</text><input v-model="spec.originalPrice" type="digit" class="text-input" placeholder="可选" /></view><view><text class="field-label compact">时长（分钟）</text><input v-model="spec.duration" type="number" class="text-input" placeholder="60" /></view></view></view></view>

    <view class="bottom-bar"><button class="save-btn" :disabled="submitting" @click="save(false)">保存草稿</button><button class="submit-btn" :disabled="submitting" @click="save(true)">{{ submitting ? '处理中...' : '保存并提交审核' }}</button></view>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ROLES } from '@/constants/roles.js'
import { useServiceManageStore } from '@/store/service-manage.js'
import { useServiceStore } from '@/store/service.js'
import { requireRole } from '@/utils/permission.js'

const serviceStore = useServiceStore()
const serviceManageStore = useServiceManageStore()
const itemId = ref(null)
const submitting = ref(false)
let localId = 1
const form = reactive({ name: '', categoryId: null, coverImage: '', description: '', specs: [createEmptySpec()] })
const categoryIndex = computed(() => Math.max(0, serviceStore.categories.findIndex((item) => item.categoryId === form.categoryId)))
const selectedCategoryName = computed(() => serviceStore.categories.find((item) => item.categoryId === form.categoryId)?.name || '')

function createEmptySpec(spec = {}) { return { localId: localId++, specId: spec.specId || null, name: spec.name || '', price: String(spec.price ?? ''), originalPrice: String(spec.originalPrice ?? ''), duration: String(spec.duration ?? 60) } }
onLoad(async (options) => { if (!requireRole(ROLES.MERCHANT_MEMBER)) return; await serviceStore.fetchCategories(); if (options.id) { itemId.value = String(options.id); const service = await serviceManageStore.fetchServiceDetail(itemId.value); Object.assign(form, { name: service.name, categoryId: service.categoryId, coverImage: service.coverImage || '', description: service.description, specs: service.specs.map(createEmptySpec) }) } })
function changeCategory(event) { form.categoryId = serviceStore.categories[Number(event.detail.value)]?.categoryId || null }
function addSpec() { if (form.specs.length >= 6) return uni.showToast({ title: '最多添加6个规格', icon: 'none' }); form.specs.push(createEmptySpec()) }
function removeSpec(index) { form.specs.splice(index, 1) }
function validate() { if (!form.name.trim()) return '请输入服务名称'; if (!form.categoryId) return '请选择服务分类'; if (form.description.trim().length < 10) return '服务说明至少10个字符'; if (!form.specs.length) return '至少添加一个规格'; if (form.specs.some((spec) => !spec.name.trim() || Number(spec.price) <= 0 || Number(spec.duration) <= 0)) return '请完整填写规格名称、价格和时长'; return '' }
function buildPayload() { return { name: form.name.trim(), categoryId: form.categoryId, coverImage: form.coverImage.trim(), description: form.description.trim(), specs: form.specs.map((spec) => ({ specId: spec.specId, name: spec.name.trim(), price: Number(spec.price), originalPrice: Number(spec.originalPrice || spec.price), duration: Number(spec.duration) })) } }
async function save(shouldSubmit) { const message = validate(); if (message) return uni.showToast({ title: message, icon: 'none' }); submitting.value = true; try { const service = itemId.value ? await serviceManageStore.updateService(itemId.value, buildPayload()) : await serviceManageStore.createService(buildPayload()); itemId.value = service.itemId; if (shouldSubmit) { await serviceManageStore.submitService(service.itemId); uni.showToast({ title: '已提交审核', icon: 'success' }) } else { uni.showToast({ title: '草稿已保存', icon: 'success' }) } setTimeout(() => uni.redirectTo({ url: `/subpkg-merchant/service-detail/index?id=${service.itemId}` }), 700) } finally { submitting.value = false } }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 160rpx; background: $page-gradient; }.form-card { margin-bottom: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }.add-spec { color: $primary-color; font-size: $font-size-sm; }.field-label { display: block; margin-top: 24rpx; color: $text-color-secondary; font-size: $font-size-sm; font-weight: 600; }.field-label.compact { margin-top: 15rpx; }.required { color: $error-color; }.text-input,.picker-input,.description-input { width: 100%; margin-top: 10rpx; border: 1rpx solid $border-color; border-radius: 20rpx; background: rgba(255,255,255,.78); color: $text-color; font-size: $font-size-sm; }.text-input { height: 76rpx; padding: 0 18rpx; }.picker-input { display: flex; align-items: center; justify-content: space-between; height: 76rpx; padding: 0 18rpx; }.picker-input.placeholder { color: $text-color-disabled; }.description-input { height: 260rpx; padding: 18rpx; line-height: 1.6; }.counter { display: block; margin-top: 7rpx; color: $text-color-disabled; font-size: $font-size-xs; text-align: right; }
.spec-card { margin-top: 16rpx; padding: 20rpx; border: 1rpx solid $border-color-light; border-radius: 22rpx; background: $bg-color-grey; }.spec-head { display: flex; align-items: center; justify-content: space-between; color: $text-color; font-size: $font-size-sm; font-weight: 600; }.remove-spec { color: $error-color; font-size: $font-size-xs; font-weight: 400; }.spec-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 10rpx; }
.bottom-bar { position: fixed; left: 0; right: 0; bottom: 0; display: grid; grid-template-columns: .8fr 1.2fr; gap: 14rpx; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,.95); box-shadow: 0 -6rpx 24rpx rgba(42,91,170,.08); }.save-btn,.submit-btn { height: 78rpx; margin: 0; border-radius: $radius-round; font-size: $font-size-sm; line-height: 78rpx; }.save-btn { border: 1rpx solid rgba(58,123,247,.25); background: $primary-bg; color: $primary-color; }.submit-btn { border: none; background: $primary-gradient; color: #fff; }.save-btn::after,.submit-btn::after { border: none; }
</style>
