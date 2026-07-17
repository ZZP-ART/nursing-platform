<template>
  <view class="page-shell">
    <view v-if="!userStore.isLoggedIn" class="auth-card">
      <view class="auth-icon"><u-icon name="home" size="42" color="#00A89D" /></view>
      <text class="auth-title">申请商户入驻</text>
      <text class="auth-desc">商户负责人需要先创建个人账号，再提交企业主体、经营资质和服务区域信息。</text>
      <button class="primary-button" @click="goLogin">先登录个人账号</button>
      <text class="register-link" @click="goRegister">还没有账号？立即注册</text>
    </view>

    <template v-else>
      <view class="status-card" :class="statusMeta.tone">
        <view class="status-copy"><text class="status-eyebrow">商户入驻</text><text class="status-title">{{ statusMeta.label }}</text><text class="status-desc">{{ statusMeta.description }}</text></view>
        <view class="status-icon"><u-icon :name="statusIcon" size="32" color="#FFFFFF" /></view>
      </view>

      <view v-if="application?.rejectReason" class="notice-card danger"><u-icon name="info-circle" size="22" color="#E55B63" /><view><text class="notice-title">审核意见</text><text class="notice-desc">{{ application.rejectReason }}</text></view></view>
      <view v-else-if="auditStatus === STATUS.PENDING_REVIEW" class="notice-card"><u-icon name="clock" size="22" color="#00A89D" /><view><text class="notice-title">资料核验中</text><text class="notice-desc">Mock 会在提交约 8 秒后，于刷新时模拟创建商户组织和负责人关系。</text></view></view>

      <template v-if="canEdit">
        <view class="form-card">
          <text class="section-title">企业主体</text>
          <text class="field-label">商户名称 *</text><input v-model="form.merchantName" maxlength="60" class="text-input" placeholder="请输入营业执照上的主体名称" />
          <text class="field-label">商户类型 *</text><picker :range="typeOptions" range-key="label" :value="typeIndex" @change="changeType"><view class="picker-input" :class="{ placeholder: !form.merchantType }">{{ selectedTypeLabel || '请选择商户类型' }}<u-icon name="arrow-right" size="14" color="#C5CDD8" /></view></picker>
          <text class="field-label">统一社会信用代码 *</text><input v-model="form.creditCode" maxlength="18" class="text-input" placeholder="请输入18位统一社会信用代码" />
          <text class="field-label">法定代表人 *</text><input v-model="form.legalRepresentative" maxlength="20" class="text-input" placeholder="请输入法定代表人姓名" />
          <text class="field-label">营业执照图片地址 *</text><input v-model="form.licenseImageUrl" class="text-input" placeholder="前端演示可填写图片 URL" />
        </view>

        <view class="form-card">
          <text class="section-title">负责人信息</text>
          <view class="field-grid"><view><text class="field-label">负责人姓名 *</text><input v-model="form.contactName" maxlength="20" class="text-input" placeholder="请输入姓名" /></view><view><text class="field-label">负责人手机号 *</text><input v-model="form.contactPhone" type="number" maxlength="11" class="text-input" placeholder="请输入手机号" /></view></view>
          <text class="field-label">负责人身份证号 *</text><input v-model="form.contactIdCard" maxlength="18" class="text-input" placeholder="用于负责人实名认证" />
          <text class="field-tip">审核通过后，当前账号将成为该商户的负责人并取得商户成员身份。</text>
        </view>

        <view class="form-card">
          <text class="section-title">经营信息</text>
          <text class="field-label">经营地址 *</text><input v-model="form.businessAddress" maxlength="120" class="text-input" placeholder="请输入营业执照或实际经营地址" />
          <text class="field-label">服务区域 *</text><input v-model="form.serviceAreas" maxlength="120" class="text-input" placeholder="例如：北京市朝阳区、海淀区" />
          <text class="field-label">经营范围 *</text><textarea v-model="form.businessScope" maxlength="500" class="textarea-input" placeholder="请说明经营范围、护理服务能力和主要服务对象" /><text class="counter">{{ form.businessScope.length }}/500</text>
        </view>

        <view class="form-card">
          <text class="section-title">经营许可</text>
          <text class="field-label">许可或备案名称 *</text><input v-model="form.permitName" maxlength="60" class="text-input" placeholder="例如：养老服务机构备案回执" />
          <text class="field-label">许可或备案编号 *</text><input v-model="form.permitNumber" maxlength="50" class="text-input" placeholder="请输入证照编号" />
          <text class="field-label">许可附件图片地址 *</text><input v-model="form.permitImageUrl" class="text-input" placeholder="前端演示可填写图片 URL" />
        </view>

        <view class="form-card">
          <text class="section-title">结算信息 <text class="optional">（选填）</text></text>
          <text class="section-desc">正式项目需通过独立加密接口提交，当前仅用于前端字段和流程演示。</text>
          <text class="field-label">开户银行</text><input v-model="form.settlementBank" maxlength="60" class="text-input" placeholder="请输入开户银行" />
          <text class="field-label">账户名称</text><input v-model="form.settlementAccountName" maxlength="60" class="text-input" placeholder="请输入企业账户名称" />
          <text class="field-label">银行账号</text><input v-model="form.settlementAccountNo" type="number" maxlength="30" class="text-input" placeholder="请输入企业银行账号" />
        </view>

        <view class="bottom-bar"><button class="secondary-button" :disabled="submitting" @click="save(false)">保存草稿</button><button class="primary-button submit" :disabled="submitting" @click="save(true)">{{ submitting ? '处理中...' : '提交平台审核' }}</button></view>
      </template>

      <template v-else>
        <view class="detail-card">
          <view class="section-head"><text class="section-title">入驻资料</text><text class="application-no">编号 {{ application?.applicationId || '--' }}</text></view>
          <view class="merchant-header"><view class="merchant-logo"><u-icon name="home-fill" size="28" color="#FFFFFF" /></view><view><text class="merchant-name">{{ application?.merchantName }}</text><text class="merchant-type">{{ getTypeLabel(application?.merchantType) }}</text></view></view>
          <view class="detail-row"><text>信用代码</text><text>{{ application?.creditCode || '--' }}</text></view>
          <view class="detail-row"><text>法定代表人</text><text>{{ application?.legalRepresentative || '--' }}</text></view>
          <view class="detail-row"><text>负责人</text><text>{{ application?.contactName }} · {{ maskPhone(application?.contactPhone) }}</text></view>
          <view class="detail-row"><text>经营地址</text><text>{{ application?.businessAddress || '--' }}</text></view>
          <view class="detail-row"><text>服务区域</text><text>{{ application?.serviceAreas || '--' }}</text></view>
          <view class="summary-block"><text class="summary-label">经营资质</text><view class="permit-summary"><text class="permit-name">营业执照</text><text>已提交营业执照附件</text></view><view class="permit-summary"><text class="permit-name">{{ application?.permitName }}</text><text>{{ application?.permitNumber || '--' }}</text></view></view>
          <view v-if="application?.merchantId" class="organization-card"><u-icon name="checkmark-circle-fill" size="24" color="#00A89D" /><view><text class="organization-title">商户组织已创建</text><text class="organization-desc">商户编号 {{ application.merchantId }} · 当前账号为负责人</text></view></view>
        </view>

        <view v-if="application?.reviewRecords?.length" class="detail-card"><text class="section-title">审核记录</text><view v-for="record in application.reviewRecords" :key="record.recordId" class="record-row"><view class="record-dot" /><view class="record-copy"><text class="record-action">{{ getRecordLabel(record.action) }}</text><text class="record-remark">{{ record.remark }}</text><text class="record-time">{{ formatTime(record.createTime) }} · {{ record.operatorName }}</text></view></view></view>
        <button v-if="auditStatus === STATUS.PENDING_REVIEW" class="refresh-button" :disabled="loading" @click="loadApplication">{{ loading ? '刷新中...' : '刷新审核状态' }}</button>
        <button v-if="auditStatus === STATUS.APPROVED" class="primary-button workspace-button" @click="showLoginTip">以商户身份登录</button>
      </template>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useMerchantOnboardingStore } from '@/store/merchant-onboarding.js'
import { useUserStore } from '@/store/user.js'
import { MERCHANT_AUDIT_STATUS as STATUS, MERCHANT_TYPE_OPTIONS, getMerchantAuditMeta } from '@/constants/merchant-status.js'

const onboardingStore = useMerchantOnboardingStore()
const userStore = useUserStore()
const typeOptions = MERCHANT_TYPE_OPTIONS
const submitting = ref(false)
const loading = computed(() => onboardingStore.loading)
const application = computed(() => onboardingStore.application)
const auditStatus = computed(() => onboardingStore.auditStatus)
const canEdit = computed(() => onboardingStore.canEditApplication)
const statusMeta = computed(() => getMerchantAuditMeta(auditStatus.value))
const statusIcon = computed(() => ({ PENDING_REVIEW: 'clock', APPROVED: 'checkmark-circle', REJECTED: 'close-circle', DISABLED: 'lock' }[auditStatus.value] || 'edit-pen'))
const typeIndex = computed(() => Math.max(0, typeOptions.findIndex((item) => item.value === form.merchantType)))
const selectedTypeLabel = computed(() => typeOptions.find((item) => item.value === form.merchantType)?.label || '')
const form = reactive({ merchantName: '', merchantType: '', creditCode: '', legalRepresentative: '', contactName: '', contactPhone: '', contactIdCard: '', businessAddress: '', serviceAreas: '', businessScope: '', licenseImageUrl: '', permitName: '', permitNumber: '', permitImageUrl: '', settlementBank: '', settlementAccountName: '', settlementAccountNo: '' })

function fillForm(data) { if (!data) return; Object.keys(form).forEach((key) => { form[key] = data[key] || '' }) }
async function loadApplication() { if (!userStore.isLoggedIn) return; const data = await onboardingStore.fetchApplication(); fillForm(data) }
onShow(loadApplication)
function goLogin() { uni.navigateTo({ url: '/pages/login/login' }) }
function goRegister() { uni.navigateTo({ url: '/pages/register/register' }) }
function changeType(event) { form.merchantType = typeOptions[Number(event.detail.value)]?.value || '' }
function validate() { if (!form.merchantName.trim()) return '请填写商户名称'; if (!form.merchantType) return '请选择商户类型'; if (!/^[0-9A-Z]{18}$/.test(form.creditCode.trim().toUpperCase())) return '请填写18位统一社会信用代码'; if (!form.legalRepresentative.trim()) return '请填写法定代表人'; if (!form.contactName.trim() || !/^1\d{10}$/.test(form.contactPhone)) return '请填写正确的负责人信息'; if (!/^\d{17}[\dXx]$/.test(form.contactIdCard)) return '请填写正确的负责人身份证号'; if (!form.businessAddress.trim() || !form.serviceAreas.trim()) return '请填写经营地址和服务区域'; if (!form.businessScope.trim()) return '请填写经营范围'; if (!form.licenseImageUrl.trim()) return '请填写营业执照图片地址'; if (!form.permitName.trim() || !form.permitNumber.trim() || !form.permitImageUrl.trim()) return '请完整填写经营许可信息'; return '' }
function buildPayload() { return Object.fromEntries(Object.entries(form).map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])) }
async function save(shouldSubmit) { if (shouldSubmit) { const message = validate(); if (message) return uni.showToast({ title: message, icon: 'none' }) } submitting.value = true; try { if (shouldSubmit) { await onboardingStore.submitApplication(buildPayload()); uni.showToast({ title: '已提交审核', icon: 'success' }) } else { await onboardingStore.saveApplication(buildPayload()); uni.showToast({ title: '草稿已保存', icon: 'success' }) } } finally { submitting.value = false } }
function getTypeLabel(value) { return typeOptions.find((item) => item.value === value)?.label || '其他机构' }
function getRecordLabel(action) { return { SUBMIT: '提交入驻', APPROVE: '审核通过', REJECT: '审核驳回' }[action] || action }
function formatTime(value) { return value ? value.replace('T', ' ').replace(/\+08:00$/, '').slice(0, 16) : '--' }
function maskPhone(value = '') { return value.length === 11 ? `${value.slice(0, 3)}****${value.slice(-4)}` : value || '--' }
function showLoginTip() { uni.showModal({ title: '商户身份已开通', content: '现在可以直接切换到商户工作台，无需退出重新登录。', confirmText: '切换身份', success: ({ confirm }) => { if (confirm) uni.navigateTo({ url: '/pages/role-switch/role-switch' }) } }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 160rpx; background: $page-gradient; }.auth-card { margin-top: 100rpx; padding: 52rpx 34rpx; border: $glass-border-soft; border-radius: 34rpx; background: $surface-gradient; box-shadow: $shadow-float; text-align: center; }.auth-icon { display: flex; align-items: center; justify-content: center; width: 104rpx; height: 104rpx; margin: 0 auto; border-radius: 32rpx; background: #e9fbf7; }.auth-title,.auth-desc { display: block; }.auth-title { margin-top: 24rpx; color: $text-color; font-size: 36rpx; font-weight: 700; }.auth-desc { margin: 16rpx 0 34rpx; color: $text-color-secondary; font-size: $font-size-sm; line-height: 1.7; }.register-link { display: block; margin-top: 24rpx; color: #00a89d; font-size: $font-size-sm; }
.status-card { display: flex; align-items: center; min-height: 190rpx; padding: 28rpx; border-radius: 32rpx; color: #fff; box-shadow: $shadow-float; background: linear-gradient(135deg,#116b68,#00a89d 55%,#38c6b5); }.status-card.warning { background: linear-gradient(135deg,#e7833b,#f4a259); }.status-card.danger { background: linear-gradient(135deg,#cc4954,#ed7780); }.status-card.muted { background: linear-gradient(135deg,#66788c,#8e9dae); }.status-copy { flex: 1; }.status-eyebrow,.status-title,.status-desc { display: block; }.status-eyebrow { font-size: $font-size-xs; opacity: .76; }.status-title { margin-top: 5rpx; font-size: 42rpx; font-weight: 700; }.status-desc { margin-top: 8rpx; font-size: $font-size-sm; opacity: .84; }.status-icon { display: flex; align-items: center; justify-content: center; width: 82rpx; height: 82rpx; border-radius: 28rpx; background: rgba(255,255,255,.16); }
.notice-card { display: flex; gap: 16rpx; margin-top: 20rpx; padding: 22rpx; border: 1rpx solid rgba(0,168,157,.18); border-radius: 24rpx; background: #e9fbf7; }.notice-card.danger { border-color: rgba(229,91,99,.18); background: #fff0f1; }.notice-title,.notice-desc { display: block; }.notice-title { color: $text-color; font-size: $font-size-sm; font-weight: 700; }.notice-desc { margin-top: 6rpx; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.6; }
.form-card,.detail-card { margin-top: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.section-desc { display: block; margin-top: 8rpx; color: $text-color-hint; font-size: $font-size-xs; line-height: 1.5; }.optional { color: $text-color-hint; font-size: $font-size-xs; font-weight: 400; }.section-head { display: flex; align-items: center; justify-content: space-between; }.application-no { color: #00a89d; font-size: $font-size-xs; }.field-grid { display: grid; grid-template-columns: .8fr 1.2fr; gap: 14rpx; }.field-label { display: block; margin-top: 22rpx; color: $text-color-secondary; font-size: $font-size-sm; font-weight: 600; }.field-tip { display: block; margin-top: 12rpx; color: $text-color-hint; font-size: $font-size-xs; line-height: 1.5; }.text-input,.picker-input,.textarea-input { width: 100%; margin-top: 10rpx; border: 1rpx solid $border-color; border-radius: 20rpx; background: rgba(255,255,255,.78); color: $text-color; font-size: $font-size-sm; }.text-input { height: 76rpx; padding: 0 18rpx; }.picker-input { display: flex; align-items: center; justify-content: space-between; height: 76rpx; padding: 0 18rpx; }.picker-input.placeholder { color: $text-color-disabled; }.textarea-input { height: 200rpx; padding: 18rpx; line-height: 1.6; }.counter { display: block; margin-top: 6rpx; color: $text-color-disabled; font-size: $font-size-xs; text-align: right; }
.bottom-bar { position: fixed; right: 0; bottom: 0; left: 0; display: grid; grid-template-columns: .8fr 1.2fr; gap: 14rpx; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,.96); box-shadow: 0 -6rpx 24rpx rgba(42,91,170,.08); z-index: 3; }.primary-button,.secondary-button,.refresh-button { height: 80rpx; margin: 0; border-radius: $radius-round; font-size: $font-size-sm; line-height: 80rpx; }.primary-button { border: none; background: linear-gradient(135deg,#116b68,#00a89d 60%,#38c6b5); color: #fff; }.secondary-button,.refresh-button { border: 1rpx solid rgba(0,168,157,.28); background: #e9fbf7; color: #008d84; }.primary-button::after,.secondary-button::after,.refresh-button::after { border: none; }.auth-card .primary-button { width: 100%; }.workspace-button,.refresh-button { width: 100%; margin-top: 22rpx; }
.merchant-header { display: flex; align-items: center; gap: 16rpx; margin-top: 22rpx; padding: 20rpx; border-radius: 22rpx; background: #e9fbf7; }.merchant-logo { display: flex; align-items: center; justify-content: center; width: 70rpx; height: 70rpx; border-radius: 22rpx; background: #00a89d; }.merchant-name,.merchant-type { display: block; }.merchant-name { color: $text-color; font-size: $font-size-base; font-weight: 700; }.merchant-type { margin-top: 4rpx; color: $text-color-hint; font-size: $font-size-xs; }.detail-row { display: flex; justify-content: space-between; gap: 20rpx; padding: 20rpx 0; border-bottom: 1rpx solid $divider-color; color: $text-color-secondary; font-size: $font-size-sm; }.detail-row text:last-child { max-width: 65%; color: $text-color; text-align: right; }.summary-block { padding-top: 22rpx; }.summary-label { color: $text-color-secondary; font-size: $font-size-sm; }.permit-summary { margin-top: 12rpx; padding: 16rpx; border-radius: 18rpx; color: $text-color-hint; font-size: $font-size-xs; background: $bg-color-grey; }.permit-name { display: block; margin-bottom: 5rpx; color: $text-color; font-size: $font-size-sm; font-weight: 600; }.organization-card { display: flex; gap: 14rpx; margin-top: 22rpx; padding: 18rpx; border-radius: 20rpx; background: #e9fbf7; }.organization-title,.organization-desc { display: block; }.organization-title { color: $text-color; font-size: $font-size-sm; font-weight: 700; }.organization-desc { margin-top: 5rpx; color: $text-color-secondary; font-size: $font-size-xs; }
.record-row { display: flex; gap: 16rpx; padding-top: 22rpx; }.record-dot { width: 14rpx; height: 14rpx; margin-top: 8rpx; border: 4rpx solid #ccefe9; border-radius: 50%; background: #00a89d; }.record-copy { flex: 1; }.record-action,.record-remark,.record-time { display: block; }.record-action { color: $text-color; font-size: $font-size-sm; font-weight: 700; }.record-remark { margin-top: 6rpx; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.6; }.record-time { margin-top: 7rpx; color: $text-color-disabled; font-size: 22rpx; }
</style>
