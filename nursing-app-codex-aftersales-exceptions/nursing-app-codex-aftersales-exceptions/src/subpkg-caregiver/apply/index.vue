<template>
  <view class="page-shell">
    <view v-if="!userStore.isLoggedIn" class="auth-card">
      <view class="auth-icon"><u-icon name="server-man" size="42" color="#3A7BF7" /></view>
      <text class="auth-title">申请成为护理人员</text>
      <text class="auth-desc">护理人员身份基于个人账号申请。请先注册或以顾客身份登录，再提交身份和资质资料。</text>
      <button class="primary-button" @click="goLogin">先登录个人账号</button>
      <text class="register-link" @click="goRegister">还没有账号？立即注册</text>
    </view>

    <template v-else>
      <view class="status-card" :class="statusMeta.tone">
        <view class="status-copy">
          <text class="status-eyebrow">护理人员认证</text>
          <text class="status-title">{{ statusMeta.label }}</text>
          <text class="status-desc">{{ statusMeta.description }}</text>
        </view>
        <view class="status-icon"><u-icon :name="statusIcon" size="32" color="#FFFFFF" /></view>
      </view>

      <view v-if="application?.rejectReason" class="notice-card danger">
        <u-icon name="info-circle" size="22" color="#E55B63" />
        <view><text class="notice-title">审核意见</text><text class="notice-desc">{{ application.rejectReason }}</text></view>
      </view>
      <view v-else-if="auditStatus === STATUS.PENDING_REVIEW" class="notice-card">
        <u-icon name="clock" size="22" color="#3A7BF7" />
        <view><text class="notice-title">资料核验中</text><text class="notice-desc">Mock 会在提交约 8 秒后，于刷新时模拟审核通过。</text></view>
      </view>

      <template v-if="canEdit">
        <view class="form-card">
          <text class="section-title">身份资料</text>
          <view class="field-grid">
            <view><text class="field-label">真实姓名 *</text><input v-model="form.realName" maxlength="20" class="text-input" placeholder="请输入身份证姓名" /></view>
            <view><text class="field-label">性别</text><picker :range="genderOptions" range-key="label" :value="genderIndex" @change="changeGender"><view class="picker-input">{{ selectedGenderLabel }}<u-icon name="arrow-right" size="14" color="#C5CDD8" /></view></picker></view>
          </view>
          <text class="field-label">联系电话 *</text><input v-model="form.phone" maxlength="20" class="text-input" placeholder="请输入申请联系电话" />
          <text class="field-label">身份证号 *</text><input v-model="form.idCard" maxlength="18" class="text-input" placeholder="用于实名认证，仅平台审核可见" />
          <text class="field-label">从业年限</text><input v-model="form.workYears" type="number" class="text-input" placeholder="请输入从业年限" />
          <text class="field-label">个人简介</text><textarea v-model="form.introduction" maxlength="500" class="textarea-input" placeholder="介绍护理经验、擅长领域和服务特点" /><text class="counter">{{ form.introduction.length }}/500</text>
        </view>

        <view class="form-card">
          <text class="section-title">服务能力</text>
          <text class="field-label">服务区域 *</text><input v-model="form.serviceArea" maxlength="100" class="text-input" placeholder="例如：北京市朝阳区、海淀区" />
          <text class="field-label">可服务项目 *</text>
          <view class="skill-grid"><view v-for="skill in skillOptions" :key="skill" class="skill-chip" :class="{ active: form.skills.includes(skill) }" @click="toggleSkill(skill)"><u-icon :name="form.skills.includes(skill) ? 'checkmark-circle-fill' : 'plus-circle'" size="17" :color="form.skills.includes(skill) ? '#3A7BF7' : '#8E9DAE'" /><text>{{ skill }}</text></view></view>
          <text class="field-label">意向合作商户</text><input v-model="form.merchantName" maxlength="40" class="text-input" placeholder="选填，后续由商户确认合作关系" />
        </view>

        <view class="form-card">
          <view class="section-head"><text class="section-title">资质证书</text><text class="add-link" @click="addCertificate">+ 添加证书</text></view>
          <view v-for="(certificate, index) in form.certificates" :key="certificate.localId" class="certificate-card">
            <view class="certificate-head"><text>证书 {{ index + 1 }}</text><text v-if="form.certificates.length > 1" class="delete-link" @click="removeCertificate(index)">删除</text></view>
            <text class="field-label compact">证书名称 *</text><input v-model="certificate.name" maxlength="40" class="text-input" placeholder="例如：养老护理员职业技能等级证书" />
            <text class="field-label compact">证书编号 *</text><input v-model="certificate.number" maxlength="40" class="text-input" placeholder="请输入证书编号" />
            <text class="field-label compact">签发机构 *</text><input v-model="certificate.issuer" maxlength="60" class="text-input" placeholder="请输入证书签发机构" />
            <text class="field-label compact">有效期至 *</text><picker mode="date" :value="certificate.validUntil" @change="changeCertificateDate(index, $event)"><view class="picker-input" :class="{ placeholder: !certificate.validUntil }">{{ certificate.validUntil || '请选择有效期' }}<u-icon name="calendar" size="16" color="#8E9DAE" /></view></picker>
            <text class="field-label compact">证书图片地址</text><input v-model="certificate.imageUrl" class="text-input" placeholder="前端演示可填写图片 URL" />
          </view>
        </view>

        <view class="bottom-bar"><button class="secondary-button" :disabled="submitting" @click="save(false)">保存草稿</button><button class="primary-button submit" :disabled="submitting" @click="save(true)">{{ submitting ? '处理中...' : '提交平台审核' }}</button></view>
      </template>

      <template v-else>
        <view class="detail-card">
          <view class="section-head"><text class="section-title">申请资料</text><text class="application-no">编号 {{ application?.applicationId || '--' }}</text></view>
          <view class="detail-row"><text>姓名</text><text>{{ application?.realName || '--' }}</text></view>
          <view class="detail-row"><text>从业年限</text><text>{{ application?.workYears || 0 }} 年</text></view>
          <view class="detail-row"><text>服务区域</text><text>{{ application?.serviceArea || '--' }}</text></view>
          <view class="detail-row"><text>合作商户</text><text>{{ application?.merchantName || '暂未绑定' }}</text></view>
          <view class="summary-block"><text class="summary-label">服务技能</text><view class="tag-row"><text v-for="skill in application?.skills || []" :key="skill" class="tag">{{ skill }}</text></view></view>
          <view class="summary-block"><text class="summary-label">资质证书</text><view v-for="certificate in application?.certificates || []" :key="certificate.certificateId" class="certificate-summary"><text class="certificate-name">{{ certificate.name }}</text><text>{{ certificate.number }} · 有效期至 {{ certificate.validUntil }}</text></view></view>
        </view>

        <view v-if="application?.reviewRecords?.length" class="detail-card">
          <text class="section-title">审核记录</text>
          <view v-for="record in application.reviewRecords" :key="record.recordId" class="record-row"><view class="record-dot" /><view class="record-copy"><text class="record-action">{{ getRecordLabel(record.action) }}</text><text class="record-remark">{{ record.remark }}</text><text class="record-time">{{ formatTime(record.createTime) }} · {{ record.operatorName }}</text></view></view>
        </view>

        <button v-if="auditStatus === STATUS.PENDING_REVIEW" class="refresh-button" :disabled="loading" @click="loadApplication">{{ loading ? '刷新中...' : '刷新审核状态' }}</button>
        <button v-if="auditStatus === STATUS.APPROVED" class="primary-button workspace-button" @click="showLoginTip">以护理人员身份登录</button>
      </template>
    </template>
  </view>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useCaregiverStore } from '@/store/caregiver.js'
import { useUserStore } from '@/store/user.js'
import {
  CAREGIVER_AUDIT_STATUS as STATUS,
  CAREGIVER_SKILL_OPTIONS,
  getCaregiverAuditMeta,
} from '@/constants/caregiver-status.js'

const caregiverStore = useCaregiverStore()
const userStore = useUserStore()
const skillOptions = CAREGIVER_SKILL_OPTIONS
const submitting = ref(false)
const loading = computed(() => caregiverStore.loading)
const application = computed(() => caregiverStore.application)
const auditStatus = computed(() => caregiverStore.auditStatus)
const statusMeta = computed(() => getCaregiverAuditMeta(auditStatus.value))
const canEdit = computed(() => caregiverStore.canEditApplication)
const statusIcon = computed(() => ({ PENDING_REVIEW: 'clock', APPROVED: 'checkmark-circle', REJECTED: 'close-circle' }[auditStatus.value] || 'edit-pen'))
const genderOptions = [{ label: '未设置', value: 0 }, { label: '男', value: 1 }, { label: '女', value: 2 }]
const genderIndex = computed(() => Math.max(0, genderOptions.findIndex((item) => item.value === form.gender)))
const selectedGenderLabel = computed(() => genderOptions[genderIndex.value].label)
let localId = 1
const form = reactive({ realName: '', phone: '', gender: 0, idCard: '', workYears: '', introduction: '', serviceArea: '', skills: [], merchantName: '', certificates: [createCertificate()] })

function createCertificate(data = {}) { return { localId: localId++, certificateId: data.certificateId || null, name: data.name || '', number: data.number || '', issuer: data.issuer || '', validUntil: data.validUntil || '', imageUrl: data.imageUrl || '' } }
function fillForm(data) { if (!data) return; Object.assign(form, { realName: data.realName || '', phone: data.phone || userStore.phone || '', gender: data.gender || 0, idCard: data.idCard || '', workYears: String(data.workYears ?? ''), introduction: data.introduction || '', serviceArea: data.serviceArea || data.serviceDistrict || '', skills: [...(data.skills || [])], merchantName: data.merchantName || '', certificates: data.certificates?.length ? data.certificates.map(createCertificate) : [createCertificate()] }) }
async function loadApplication() { if (!userStore.isLoggedIn) return; const data = await caregiverStore.fetchApplication(); fillForm(data) }
onShow(loadApplication)
function goLogin() { uni.navigateTo({ url: '/pages/login/login' }) }
function goRegister() { uni.navigateTo({ url: '/pages/register/register' }) }
function changeGender(event) { form.gender = genderOptions[Number(event.detail.value)]?.value || 0 }
function toggleSkill(skill) { const index = form.skills.indexOf(skill); if (index >= 0) form.skills.splice(index, 1); else form.skills.push(skill) }
function addCertificate() { if (form.certificates.length >= 5) return uni.showToast({ title: '最多添加5项证书', icon: 'none' }); form.certificates.push(createCertificate()) }
function removeCertificate(index) { form.certificates.splice(index, 1) }
function changeCertificateDate(index, event) { form.certificates[index].validUntil = event.detail.value }
function validate() { if (!form.realName.trim()) return '请填写真实姓名'; if (!form.phone.trim()) return '请填写联系电话'; if (!form.serviceArea.trim()) return '请填写服务区域'; if (!form.skills.length) return '请至少选择一项服务技能'; return '' }
function buildPayload() { return { realName: form.realName.trim(), phone: form.phone.trim(), gender: form.gender, idCard: form.idCard.trim(), workYears: Number(form.workYears || 0), introduction: form.introduction.trim(), serviceArea: form.serviceArea.trim(), serviceDistrict: form.serviceArea.trim(), skills: [...form.skills], merchantName: form.merchantName.trim(), certificates: form.certificates.map(({ localId: _localId, ...item }) => ({ ...item, name: item.name.trim(), number: item.number.trim(), issuer: item.issuer.trim(), imageUrl: item.imageUrl.trim() })) } }
async function save(shouldSubmit) { if (shouldSubmit) { const message = validate(); if (message) return uni.showToast({ title: message, icon: 'none' }) } submitting.value = true; try { if (shouldSubmit) { await caregiverStore.submitApplication(buildPayload()); uni.showToast({ title: '已提交审核', icon: 'success' }) } else { await caregiverStore.saveApplication(buildPayload()); uni.showToast({ title: '草稿已保存', icon: 'success' }) } } catch (error) { if (error?.code !== 'API_NOT_AVAILABLE') throw error } finally { submitting.value = false } }
function getRecordLabel(action) { return { SUBMIT: '提交认证', APPROVE: '审核通过', REJECT: '审核驳回' }[action] || action }
function formatTime(value) { return value ? value.replace('T', ' ').replace(/\+08:00$/, '').slice(0, 16) : '--' }
function showLoginTip() { uni.showModal({ title: '身份已开通', content: '现在可以直接切换到护理人员工作台，无需退出重新登录。', confirmText: '切换身份', success: ({ confirm }) => { if (confirm) uni.navigateTo({ url: '/pages/role-switch/role-switch' }) } }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 24rpx $spacing-base 160rpx; background: $page-gradient; }.auth-card { margin-top: 100rpx; padding: 52rpx 34rpx; border: $glass-border-soft; border-radius: 34rpx; background: $surface-gradient; box-shadow: $shadow-float; text-align: center; }.auth-icon { display: flex; align-items: center; justify-content: center; width: 104rpx; height: 104rpx; margin: 0 auto; border-radius: 32rpx; background: $primary-bg; }.auth-title,.auth-desc { display: block; }.auth-title { margin-top: 24rpx; color: $text-color; font-size: 36rpx; font-weight: 700; }.auth-desc { margin: 16rpx 0 34rpx; color: $text-color-secondary; font-size: $font-size-sm; line-height: 1.7; }.register-link { display: block; margin-top: 24rpx; color: $primary-color; font-size: $font-size-sm; }
.status-card { display: flex; align-items: center; min-height: 190rpx; padding: 28rpx; border-radius: 32rpx; color: #fff; box-shadow: $shadow-float; background: linear-gradient(135deg,#245ddc,#3a7bf7 55%,#00b8d8); }.status-card.warning { background: linear-gradient(135deg,#e7833b,#f4a259); }.status-card.success { background: linear-gradient(135deg,#078d83,#00b8a9); }.status-card.danger { background: linear-gradient(135deg,#cc4954,#ed7780); }.status-card.muted { background: linear-gradient(135deg,#66788c,#8e9dae); }.status-copy { flex: 1; }.status-eyebrow,.status-title,.status-desc { display: block; }.status-eyebrow { font-size: $font-size-xs; opacity: .76; }.status-title { margin-top: 5rpx; font-size: 42rpx; font-weight: 700; }.status-desc { margin-top: 8rpx; font-size: $font-size-sm; opacity: .84; }.status-icon { display: flex; align-items: center; justify-content: center; width: 82rpx; height: 82rpx; border-radius: 28rpx; background: rgba(255,255,255,.16); }
.notice-card { display: flex; gap: 16rpx; margin-top: 20rpx; padding: 22rpx; border: 1rpx solid rgba(58,123,247,.16); border-radius: 24rpx; background: $primary-bg; }.notice-card.danger { border-color: rgba(229,91,99,.18); background: #fff0f1; }.notice-title,.notice-desc { display: block; }.notice-title { color: $text-color; font-size: $font-size-sm; font-weight: 700; }.notice-desc { margin-top: 6rpx; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.6; }
.form-card,.detail-card { margin-top: 20rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.section-head { display: flex; align-items: center; justify-content: space-between; }.add-link,.application-no { color: $primary-color; font-size: $font-size-xs; }.field-grid { display: grid; grid-template-columns: 1fr .65fr; gap: 14rpx; }.field-label { display: block; margin-top: 22rpx; color: $text-color-secondary; font-size: $font-size-sm; font-weight: 600; }.field-label.compact { margin-top: 15rpx; }.text-input,.picker-input,.textarea-input { width: 100%; margin-top: 10rpx; border: 1rpx solid $border-color; border-radius: 20rpx; background: rgba(255,255,255,.78); color: $text-color; font-size: $font-size-sm; }.text-input { height: 76rpx; padding: 0 18rpx; }.picker-input { display: flex; align-items: center; justify-content: space-between; height: 76rpx; padding: 0 18rpx; }.picker-input.placeholder { color: $text-color-disabled; }.textarea-input { height: 190rpx; padding: 18rpx; line-height: 1.6; }.counter { display: block; margin-top: 6rpx; color: $text-color-disabled; font-size: $font-size-xs; text-align: right; }.skill-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 12rpx; margin-top: 12rpx; }.skill-chip { display: flex; align-items: center; justify-content: center; gap: 6rpx; min-height: 68rpx; border: 1rpx solid $border-color; border-radius: 20rpx; color: $text-color-secondary; font-size: $font-size-xs; background: rgba(255,255,255,.68); }.skill-chip.active { border-color: rgba(58,123,247,.3); color: $primary-color; background: $primary-bg; }.certificate-card { margin-top: 18rpx; padding: 20rpx; border: 1rpx solid $border-color-light; border-radius: 22rpx; background: $bg-color-grey; }.certificate-head { display: flex; align-items: center; justify-content: space-between; color: $text-color; font-size: $font-size-sm; font-weight: 600; }.delete-link { color: $error-color; font-size: $font-size-xs; font-weight: 400; }
.bottom-bar { position: fixed; right: 0; bottom: 0; left: 0; display: grid; grid-template-columns: .8fr 1.2fr; gap: 14rpx; padding: 18rpx $spacing-base calc(18rpx + env(safe-area-inset-bottom)); background: rgba(249,251,255,.96); box-shadow: 0 -6rpx 24rpx rgba(42,91,170,.08); z-index: 3; }.primary-button,.secondary-button,.refresh-button { height: 80rpx; margin: 0; border-radius: $radius-round; font-size: $font-size-sm; line-height: 80rpx; }.primary-button { border: none; background: $primary-gradient; color: #fff; }.secondary-button,.refresh-button { border: 1rpx solid rgba(58,123,247,.25); background: $primary-bg; color: $primary-color; }.primary-button::after,.secondary-button::after,.refresh-button::after { border: none; }.auth-card .primary-button { width: 100%; }.workspace-button,.refresh-button { width: 100%; margin-top: 22rpx; }
.detail-row { display: flex; justify-content: space-between; gap: 20rpx; padding: 20rpx 0; border-bottom: 1rpx solid $divider-color; color: $text-color-secondary; font-size: $font-size-sm; }.detail-row text:last-child { color: $text-color; text-align: right; }.summary-block { padding-top: 22rpx; }.summary-label { color: $text-color-secondary; font-size: $font-size-sm; }.tag-row { display: flex; flex-wrap: wrap; gap: 10rpx; margin-top: 12rpx; }.tag { padding: 8rpx 14rpx; border-radius: $radius-round; color: $primary-color; font-size: $font-size-xs; background: $primary-bg; }.certificate-summary { margin-top: 12rpx; padding: 16rpx; border-radius: 18rpx; color: $text-color-hint; font-size: $font-size-xs; background: $bg-color-grey; }.certificate-name { display: block; margin-bottom: 6rpx; color: $text-color; font-size: $font-size-sm; font-weight: 600; }.record-row { display: flex; gap: 16rpx; padding-top: 22rpx; }.record-dot { width: 14rpx; height: 14rpx; margin-top: 8rpx; border: 4rpx solid #dbe8ff; border-radius: 50%; background: $primary-color; }.record-copy { flex: 1; }.record-action,.record-remark,.record-time { display: block; }.record-action { color: $text-color; font-size: $font-size-sm; font-weight: 700; }.record-remark { margin-top: 6rpx; color: $text-color-secondary; font-size: $font-size-xs; line-height: 1.6; }.record-time { margin-top: 7rpx; color: $text-color-disabled; font-size: 22rpx; }
</style>
