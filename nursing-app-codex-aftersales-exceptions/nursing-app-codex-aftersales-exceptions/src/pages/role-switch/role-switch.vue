<template>
  <view class="page-shell">
    <view class="hero-card">
      <view><text class="eyebrow">账号身份</text><text class="title">选择要进入的工作台</text><text class="subtitle">切换后系统会重新签发当前身份的登录凭证和操作权限</text></view>
      <view class="hero-icon"><u-icon name="reload" size="34" color="#FFFFFF" /></view>
    </view>

    <view class="security-note"><u-icon name="lock-fill" size="18" color="#3A7BF7" /><text>页面只展示已通过审核的身份。角色资格和权限最终由后端校验。</text></view>

    <view class="role-list">
      <view v-for="role in roleCards" :key="role.value" class="role-card" :class="[{ current: role.value === roleStore.currentRole }, role.tone]" @click="selectRole(role.value)">
        <view class="role-icon"><u-icon :name="role.icon" size="30" :color="role.color" /></view>
        <view class="role-copy"><view class="role-heading"><text class="role-name">{{ role.label }}</text><text v-if="role.value === roleStore.currentRole" class="current-badge">当前身份</text></view><text class="role-desc">{{ role.description }}</text><text class="role-permission">{{ role.permissionSummary }}</text></view>
        <u-icon :name="role.value === roleStore.currentRole ? 'checkmark-circle-fill' : 'arrow-right'" size="22" :color="role.value === roleStore.currentRole ? '#00B8A9' : '#C5CDD8'" />
      </view>
    </view>

    <view v-if="missingRoleCards.length" class="apply-card"><text class="section-title">申请更多身份</text><text class="section-desc">未获批身份不会直接进入工作台，可先提交申请并等待平台审核。</text><view class="apply-grid"><view v-for="role in missingRoleCards" :key="role.value" class="apply-item" @click="goApply(role)"><u-icon :name="role.icon" size="23" :color="role.color" /><view><text>{{ role.applyLabel }}</text><text>{{ role.applyDescription }}</text></view><u-icon name="arrow-right" size="15" color="#C5CDD8" /></view></view></view>

    <text class="refresh-link" @click="refreshRoles">{{ refreshing ? '正在刷新身份...' : '刚通过审核？点击刷新身份列表' }}</text>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { ROLES, navigateToRoleHome } from '@/constants/roles.js'
import { useRoleStore } from '@/store/role.js'
import { useUserStore } from '@/store/user.js'

const roleStore = useRoleStore()
const userStore = useUserStore()
const switching = ref(false)
const refreshing = ref(false)
const roleDefinitions = [
  { value: ROLES.CUSTOMER, label: '顾客', description: '为自己或家人预约居家护理服务', permissionSummary: '浏览服务、预约下单、订单与售后', icon: 'account-fill', color: '#3A7BF7', tone: 'customer', applyLabel: '', applyDescription: '', applyUrl: '' },
  { value: ROLES.CAREGIVER, label: '护理人员', description: '接收任务并完成上门服务履约', permissionSummary: '任务、签到、服务记录与排班', icon: 'server-man', color: '#00A89D', tone: 'caregiver', applyLabel: '申请成为护理人员', applyDescription: '提交身份、证书和服务能力', applyUrl: '/subpkg-caregiver/apply/index' },
  { value: ROLES.MERCHANT_MEMBER, label: '商户', description: '管理订单并调度护理人员', permissionSummary: '订单查看与派单调度', icon: 'home-fill', color: '#E7833B', tone: 'merchant', applyLabel: '', applyDescription: '', applyUrl: '' },
]
const roleCards = computed(() => roleDefinitions.filter((role) => roleStore.availableRoles.includes(role.value)))
const missingRoleCards = computed(() => roleDefinitions.filter((role) => role.applyUrl && !roleStore.availableRoles.includes(role.value)))

onLoad(async () => { if (!userStore.isLoggedIn) return uni.reLaunch({ url: '/pages/login/login' }); await refreshRoles(false) })
async function refreshRoles(showToast = true) { refreshing.value = true; try { await userStore.fetchRoles(); if (showToast) uni.showToast({ title: '身份列表已刷新', icon: 'success' }) } finally { refreshing.value = false } }
async function selectRole(role) { if (switching.value) return; if (role === roleStore.currentRole) return navigateToRoleHome(role); switching.value = true; try { await userStore.switchRole(role); uni.showToast({ title: `已切换为${roleDefinitions.find((item) => item.value === role)?.label}`, icon: 'success' }); setTimeout(() => roleStore.goToWorkspace(), 500) } finally { switching.value = false } }
function goApply(role) { uni.navigateTo({ url: role.applyUrl }) }
</script>

<style lang="scss" scoped>
.page-shell { min-height: 100vh; padding: 28rpx $spacing-base 70rpx; background: $page-gradient; }.hero-card { display: flex; align-items: center; justify-content: space-between; min-height: 220rpx; padding: 32rpx; border-radius: 34rpx; color: #fff; background: linear-gradient(135deg,#245ddc,#3a7bf7 54%,#00b8d8); box-shadow: $shadow-float; }.eyebrow,.title,.subtitle { display: block; }.eyebrow { font-size: $font-size-xs; opacity: .76; }.title { margin-top: 9rpx; font-size: 38rpx; font-weight: 700; }.subtitle { max-width: 500rpx; margin-top: 11rpx; font-size: $font-size-xs; line-height: 1.55; opacity: .82; }.hero-icon { display: flex; align-items: center; justify-content: center; width: 84rpx; height: 84rpx; border-radius: 28rpx; background: rgba(255,255,255,.18); }.security-note { display: flex; align-items: flex-start; gap: 12rpx; margin-top: 20rpx; padding: 18rpx 20rpx; border-radius: 22rpx; color: $text-color-secondary; background: $primary-bg; font-size: $font-size-xs; line-height: 1.55; }
.role-list { display: grid; gap: 16rpx; margin-top: 22rpx; }.role-card { display: flex; align-items: center; gap: 18rpx; min-height: 152rpx; padding: 24rpx; border: 2rpx solid transparent; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.role-card.current { border-color: rgba(0,184,169,.28); box-shadow: 0 12rpx 30rpx rgba(0,184,169,.09); }.role-icon { display: flex; align-items: center; justify-content: center; width: 78rpx; height: 78rpx; border-radius: 24rpx; background: $primary-bg; }.role-card.caregiver .role-icon { background: #e9fbf7; }.role-card.merchant .role-icon { background: #fff4ea; }.role-copy { flex: 1; }.role-heading { display: flex; align-items: center; gap: 11rpx; }.role-name { color: $text-color; font-size: $font-size-md; font-weight: 700; }.current-badge { padding: 5rpx 10rpx; border-radius: $radius-round; color: #008d84; background: #e9fbf7; font-size: 20rpx; }.role-desc,.role-permission { display: block; }.role-desc { margin-top: 7rpx; color: $text-color-secondary; font-size: $font-size-sm; }.role-permission { margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }
.apply-card { margin-top: 26rpx; padding: 25rpx 22rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }.section-title,.section-desc { display: block; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.section-desc { margin-top: 7rpx; color: $text-color-hint; font-size: $font-size-xs; line-height: 1.5; }.apply-grid { display: grid; gap: 12rpx; margin-top: 18rpx; }.apply-item { display: flex; align-items: center; gap: 14rpx; min-height: 90rpx; padding: 14rpx 16rpx; border-radius: 20rpx; background: $bg-color-grey; }.apply-item>view { flex: 1; }.apply-item text { display: block; }.apply-item text:first-child { color: $text-color; font-size: $font-size-sm; font-weight: 600; }.apply-item text:last-child { margin-top: 4rpx; color: $text-color-hint; font-size: $font-size-xs; }.refresh-link { display: block; margin-top: 26rpx; color: $primary-color; font-size: $font-size-sm; text-align: center; }
</style>
