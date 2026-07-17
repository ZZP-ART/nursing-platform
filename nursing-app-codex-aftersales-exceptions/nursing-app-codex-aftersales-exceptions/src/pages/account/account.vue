<template>
  <view class="account-page">
    <view class="profile-card">
      <view class="avatar"><u-icon name="account-fill" size="38" color="#FFFFFF" /></view>
      <view class="profile-info">
        <text class="profile-name">{{ userStore.isLoggedIn ? (userStore.userInfo?.nickname || '智慧护理用户') : '欢迎使用智慧护理' }}</text>
        <text class="profile-phone">{{ userStore.isLoggedIn ? (userStore.userInfo?.phone || '已登录') : '登录后管理订单与服务' }}</text>
      </view>
      <view v-if="!userStore.isLoggedIn" class="profile-action" @click="handleProfileAction"><text>登录</text><u-icon name="arrow-right" size="14" color="#FFFFFF" /></view>
      <view class="profile-orb orb-one" /><view class="profile-orb orb-two" />
    </view>

    <view class="order-panel">
      <view class="section-heading"><text class="section-title">我的订单</text><text class="section-link" @click="goOrders()">查看全部</text></view>
      <view class="order-grid">
        <view v-for="entry in orderEntries" :key="entry.label" class="order-entry" @click="goOrders(entry.status)">
          <view class="entry-icon" :class="entry.tone"><u-icon :name="entry.icon" size="24" :color="entry.color" /></view>
          <text>{{ entry.label }}</text>
        </view>
      </view>
    </view>

    <view class="menu-panel">
      <view v-for="item in visibleMenus" :key="item.label" class="menu-item" @click="navigate(item.url)">
        <view class="menu-icon"><u-icon :name="item.icon" size="22" color="#3A7BF7" /></view>
        <view class="menu-copy"><text class="menu-title">{{ item.label }}</text><text class="menu-desc">{{ item.roleOnly ? `当前身份：${getRoleLabel(roleStore.currentRole)}` : item.desc }}</text></view>
        <u-icon name="arrow-right" size="16" color="#C5CDD8" />
      </view>
    </view>

    <view class="support-card"><u-icon name="server-man" size="24" color="#00B8A9" /><view><text class="support-title">需要帮助？</text><text class="support-desc">在线客服每日 08:00 - 22:00 为您服务</text></view></view>
    <u-button v-if="userStore.isLoggedIn" shape="round" class="logout-btn" @click="logout">退出登录</u-button>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import { getRoleLabel } from '@/constants/roles.js'
import { useRoleStore } from '@/store/role.js'
import { useUserStore } from '@/store/user.js'

const userStore = useUserStore()
const roleStore = useRoleStore()
const orderEntries = [
  { label: '待支付', status: 0, icon: 'rmb-circle', color: '#FF8A5C', tone: 'orange' },
  { label: '待服务', status: 1, icon: 'clock', color: '#3A7BF7', tone: 'blue' },
  { label: '已完成', status: 2, icon: 'checkmark-circle', color: '#00B8A9', tone: 'green' },
  { label: '已取消', status: 3, icon: 'close-circle', color: '#8E9DAE', tone: 'grey' },
]
const menus = [
  { label: '切换身份', desc: '进入顾客、护理人员或商户工作台', icon: 'reload', url: '/pages/role-switch/role-switch', roleOnly: true },
  { label: '服务地址', desc: '管理上门服务地点', icon: 'map', url: '/pages/address/address-list' },
  { label: '护理人员认证', desc: '提交身份、证书和服务能力资料', icon: 'server-man', url: '/subpkg-caregiver/apply/index' },
  { label: '投诉记录', desc: '查看问题处理进度', icon: 'chat', url: '/pages/complaint/complaint-list' },
]
const visibleMenus = computed(() => menus.filter((item) => !item.roleOnly || roleStore.availableRoles.length > 1))

function requireLogin() {
  if (userStore.isLoggedIn) return true
  uni.navigateTo({ url: '/pages/login/login' })
  return false
}
function handleProfileAction() { uni.navigateTo({ url: '/pages/login/login' }) }
function goOrders(status) { if (!requireLogin()) return; uni.navigateTo({ url: `/pages/order/order-list${status === undefined ? '' : `?status=${status}`}` }) }
function navigate(url) { if (!requireLogin()) return; uni.navigateTo({ url }) }
function logout() { uni.showModal({ title: '退出登录', content: '确认退出当前账号吗？', success: ({ confirm }) => confirm && userStore.doLogout() }) }
</script>

<style lang="scss" scoped>
.account-page { min-height: 100vh; padding: 30rpx $spacing-base 60rpx; background: $page-gradient; }
.profile-card { position: relative; display: flex; align-items: center; gap: 20rpx; min-height: 210rpx; padding: 34rpx 28rpx; overflow: hidden; border-radius: 34rpx; background: linear-gradient(130deg, #245ddc, #3a7bf7 52%, #00b8d8); box-shadow: $shadow-float; color: #fff; }
.avatar { width: 94rpx; height: 94rpx; display: flex; align-items: center; justify-content: center; z-index: 1; flex-shrink: 0; border: 3rpx solid rgba(255,255,255,0.4); border-radius: 32rpx; background: rgba(255,255,255,0.18); }
.profile-info { flex: 1; z-index: 1; }
.profile-name { display: block; font-size: 34rpx; font-weight: 700; }
.profile-phone { display: block; margin-top: 8rpx; font-size: $font-size-sm; opacity: 0.78; }
.profile-action { display: flex; align-items: center; gap: 4rpx; z-index: 1; padding: 10rpx 16rpx; border-radius: $radius-round; background: rgba(255,255,255,0.16); font-size: $font-size-xs; }
.profile-orb { position: absolute; border: 2rpx solid rgba(255,255,255,0.14); border-radius: 50%; }.orb-one { width: 280rpx; height: 280rpx; right: -100rpx; top: -110rpx; }.orb-two { width: 160rpx; height: 160rpx; right: -35rpx; top: -45rpx; }
.order-panel, .menu-panel { margin-top: 24rpx; padding: 26rpx 24rpx; border: $glass-border-soft; border-radius: 28rpx; background: $surface-gradient; box-shadow: $shadow-sm; }
.section-heading { display: flex; align-items: center; justify-content: space-between; }.section-title { color: $text-color; font-size: $font-size-md; font-weight: 700; }.section-link { color: $primary-color; font-size: $font-size-xs; }
.order-grid { display: grid; grid-template-columns: repeat(4, 1fr); margin-top: 24rpx; }.order-entry { display: flex; flex-direction: column; align-items: center; gap: 12rpx; color: $text-color-secondary; font-size: $font-size-xs; }.entry-icon { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 24rpx; }.entry-icon.orange { background: #fff2eb; }.entry-icon.blue { background: $primary-bg; }.entry-icon.green { background: #e9fbf7; }.entry-icon.grey { background: #f0f3f7; }
.menu-panel { padding-top: 4rpx; padding-bottom: 4rpx; }.menu-item { display: flex; align-items: center; gap: 18rpx; padding: 24rpx 0; border-bottom: 1rpx solid $divider-color; }.menu-item:last-child { border-bottom: none; }.menu-icon { width: 68rpx; height: 68rpx; display: flex; align-items: center; justify-content: center; border-radius: 22rpx; background: $primary-bg; }.menu-copy { flex: 1; }.menu-title { display: block; color: $text-color; font-size: $font-size-base; font-weight: 600; }.menu-desc { display: block; margin-top: 5rpx; color: $text-color-hint; font-size: $font-size-xs; }
.menu-badge { min-width:34rpx;height:34rpx;padding:0 9rpx;border-radius:$radius-round;color:#fff;background:$error-color;font-size:19rpx;line-height:34rpx;text-align:center; }
.support-card { display: flex; align-items: center; gap: 16rpx; margin-top: 24rpx; padding: 22rpx; border-radius: 24rpx; background: #e9fbf7; }.support-title { display: block; color: $text-color; font-size: $font-size-sm; font-weight: 600; }.support-desc { display: block; margin-top: 4rpx; color: $text-color-secondary; font-size: $font-size-xs; }.logout-btn { height: 82rpx !important; margin-top: 30rpx; color: $warning-color !important; border-color: rgba(255,138,92,0.24) !important; background: rgba(255,255,255,0.72) !important; }
</style>
