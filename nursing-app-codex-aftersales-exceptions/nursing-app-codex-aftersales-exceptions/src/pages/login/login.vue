<template>
  <view class="login-page">
    <!-- 背景弥散光 -->
    <view class="bg-glow bg-glow-top" />
    <view class="bg-glow bg-glow-bottom" />

    <!-- Logo 区域 -->
    <view class="logo-section">
      <view class="logo-icon">
        <u-icon name="heart-fill" size="56" color="#FFFFFF" />
      </view>
      <text class="app-name">智慧护理</text>
      <text class="app-slogan">专业的居家护理服务平台</text>
    </view>

    <!-- 登录表单 — 玻璃拟态卡片 -->
    <view class="form-section">
      <view class="role-section">
        <text class="role-title">选择登录身份</text>
        <view class="role-tabs">
          <view
            v-for="role in APP_LOGIN_ROLES"
            :key="role.value"
            class="role-item"
            :class="{ active: selectedRole === role.value }"
            @click="selectRole(role.value)"
          >
            <u-icon
              :name="role.icon"
              size="22"
              :color="selectedRole === role.value ? '#3A7BF7' : '#8E9DAE'"
            />
            <text class="role-label">{{ role.label }}</text>
            <text class="role-desc">{{ role.description }}</text>
          </view>
        </view>
      </view>

      <view class="form-card">
        <!-- 登录方式 Tab -->
        <view class="login-tabs">
          <view
            class="tab-item"
            :class="{ active: loginMode === 'password' }"
            @click="loginMode = 'password'"
          >
            <text class="tab-text">密码登录</text>
          </view>
          <view
            class="tab-item"
            :class="{ active: loginMode === 'sms' }"
            @click="loginMode = 'sms'"
          >
            <text class="tab-text">验证码登录</text>
          </view>
        </view>

        <!-- 手机号 -->
        <view class="input-group">
          <text class="input-label">手机号</text>
          <u-input
            v-model="phone"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            border="bottom"
            clearable
          >
            <template #prefix>
              <u-icon name="phone" size="20" color="#6B7B8D" />
            </template>
          </u-input>
        </view>

        <!-- 密码模式 -->
        <view v-if="loginMode === 'password'" class="input-group">
          <text class="input-label">密码</text>
          <u-input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            border="bottom"
          >
            <template #prefix>
              <u-icon name="lock" size="20" color="#6B7B8D" />
            </template>
          </u-input>
          <text class="forgot-pwd" @click="forgotPassword">忘记密码？</text>
        </view>

        <!-- 验证码模式 -->
        <view v-if="loginMode === 'sms'" class="input-group">
          <text class="input-label">验证码</text>
          <view class="code-row">
            <u-input
              v-model="code"
              type="number"
              maxlength="6"
              placeholder="请输入验证码"
              border="none"
              class="code-input"
            >
              <template #prefix>
                <u-icon name="lock" size="20" color="#6B7B8D" />
              </template>
            </u-input>
            <u-button
              type="primary"
              :plain="true"
              :disabled="countdown > 0 || phone.length !== 11"
              size="small"
              shape="round"
              class="code-btn"
              @click="sendCode"
            >
              <text v-if="countdown > 0">{{ countdown }}s</text>
              <text v-else>获取验证码</text>
            </u-button>
          </view>
        </view>

        <!-- 登录按钮 — 渐变 -->
        <view class="submit-wrap">
          <u-button
            type="primary"
            shape="round"
            :loading="submitting"
            :disabled="!canSubmit"
            class="submit-btn"
            @click="handleLogin"
          >
            登 录
          </u-button>
        </view>

        <!-- 底部提示 -->
        <view v-if="selectedRole !== ROLES.MERCHANT_MEMBER" class="form-footer">
          <text class="footer-text">还没有账号？</text>
          <text class="footer-link" @click="handleRoleApply">{{ roleActionText }}</text>
        </view>
      </view>
    </view>

    <!-- 协议 -->
    <view class="agreement">
      <text class="agreement-text">
        登录即表示同意《用户服务协议》和《隐私政策》
      </text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user.js'
import { useRoleStore } from '@/store/role.js'
import { APP_LOGIN_ROLES, ROLES } from '@/constants/roles.js'

const userStore = useUserStore()
const roleStore = useRoleStore()
const selectedRole = ref(roleStore.lastLoginRole)
const loginMode = ref('password')
const phone = ref('')
const code = ref('')
const password = ref('')
const countdown = ref(0)
const submitting = ref(false)
let timer = null

const roleActionText = computed(() => {
  if (selectedRole.value === ROLES.CAREGIVER) return '申请成为护理人员'
  return '立即注册'
})

const canSubmit = computed(() => {
  if (phone.value.length !== 11 || submitting.value) return false
  if (loginMode.value === 'password') {
    return password.value.length >= 6
  }
  return code.value.length === 6
})

function selectRole(role) {
  selectedRole.value = role
  roleStore.rememberLoginRole(role)
}

function sendCode() {
  if (phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  userStore.sendSmsCode(phone.value, 'login')
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function handleLogin() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const credential = loginMode.value === 'password' ? password.value : code.value
    await userStore.login(phone.value, credential, loginMode.value, selectedRole.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      roleStore.goToWorkspace()
    }, 1000)
  } catch {
    // 错误提示已在 request.js 拦截器中统一处理
  } finally {
    submitting.value = false
  }
}

function forgotPassword() {
  uni.showToast({ title: '请使用验证码登录后重置密码', icon: 'none' })
}

function handleRoleApply() {
  if (selectedRole.value === ROLES.CUSTOMER) {
    uni.navigateTo({ url: '/pages/register/register' })
    return
  }
  if (selectedRole.value === ROLES.CAREGIVER) {
    uni.navigateTo({ url: '/subpkg-caregiver/apply/index' })
    return
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: $page-gradient;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 背景弥散光 */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80rpx);
  opacity: 0.55;
  pointer-events: none;
  z-index: 0;
}
.bg-glow-top {
  width: 500rpx;
  height: 500rpx;
  background: radial-gradient(circle, rgba(58, 123, 247, 0.18), transparent 70%);
  top: -160rpx;
  right: -120rpx;
}
.bg-glow-bottom {
  width: 400rpx;
  height: 400rpx;
  background: radial-gradient(circle, rgba(0, 194, 255, 0.14), transparent 70%);
  bottom: 200rpx;
  left: -100rpx;
}

/* Logo */
.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 72rpx 0 38rpx;
  position: relative;
  z-index: 1;
}

.logo-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 36rpx;
  background: $primary-gradient;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-md;
  box-shadow: 0 8rpx 32rpx rgba(58, 123, 247, 0.25);
}

.app-name {
  font-size: 48rpx;
  font-weight: 700;
  color: $text-color;
  letter-spacing: 2rpx;
}

.app-slogan {
  font-size: $font-size-base;
  color: $text-color-hint;
  margin-top: $spacing-xs;
}

/* 表单 */
.form-section {
  flex: 1;
  padding: 0 $spacing-base;
  position: relative;
  z-index: 1;
}

.role-section {
  margin-bottom: 22rpx;
  padding: 24rpx;
  border: $glass-border-soft;
  border-radius: 28rpx;
  background: $surface-gradient;
  box-shadow: $shadow-sm;
}

.role-title {
  display: block;
  margin-bottom: 18rpx;
  color: $text-color-secondary;
  font-size: $font-size-sm;
  font-weight: 600;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
}

.role-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  padding: 18rpx 8rpx 16rpx;
  border: 2rpx solid transparent;
  border-radius: 22rpx;
  background: rgba(244, 247, 251, 0.78);
  transition: all $transition-base;

  &.active {
    border-color: rgba(58, 123, 247, 0.34);
    background: $primary-bg;
    box-shadow: 0 8rpx 20rpx rgba(58, 123, 247, 0.12);
  }
}

.role-label {
  margin-top: 8rpx;
  color: $text-color;
  font-size: 26rpx;
  font-weight: 600;
}

.role-desc {
  margin-top: 4rpx;
  overflow: hidden;
  color: $text-color-hint;
  font-size: 19rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-card {
  background: linear-gradient(180deg, rgba(255,255,255,0.65) 0%, rgba(248,251,255,0.58) 100%);
  backdrop-filter: $glass-blur;
  -webkit-backdrop-filter: $glass-blur;
  border-radius: 32rpx;
  padding: 38rpx $spacing-lg;
  box-shadow: $shadow-float;
  border: $glass-border-soft;
}

/* 登录方式 Tab */
.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 44rpx;
  gap: 20rpx;
  padding: 8rpx;
  border-radius: $radius-round;
  background: $bg-color-grey;
}

.tab-item {
  flex: 1;
  padding: 16rpx 12rpx;
  border-radius: $radius-round;
  text-align: center;
  transition: all $transition-base;

  &.active {
    background: #fff;
    box-shadow: $shadow-sm;

    .tab-text {
      color: $primary-color;
      font-weight: 600;
    }
  }
}

.tab-text {
  font-size: 30rpx;
  color: $text-color-hint;
  transition: color $transition-base;
}

/* 输入组 */
.input-group {
  margin-bottom: 28rpx;
}

.input-label {
  font-size: $font-size-sm;
  color: $text-color-secondary;
  margin-bottom: 8rpx;
  display: block;
  font-weight: 500;
}

/* 验证码行 */
.code-row {
  position: relative;
  display: flex;
  align-items: center;
  border-bottom: 2rpx solid $border-color;
  padding-bottom: 8rpx;
}

.code-input {
  flex: 1;
  padding-right: 190rpx;
}

.code-btn {
  position: absolute;
  right: 0;
  bottom: 6rpx;
  width: 170rpx !important;
  height: 60rpx !important;
  font-size: 24rpx !important;
  padding: 0 !important;
  border-color: $primary-color !important;
  color: $primary-color !important;
}

.forgot-pwd {
  display: block;
  text-align: right;
  font-size: $font-size-sm;
  color: $primary-color;
  margin-top: 12rpx;
}

/* 提交按钮 */
.submit-wrap {
  margin-top: $spacing-lg;
}

.submit-btn {
  width: 100%;
  border-radius: $radius-round !important;
  height: 88rpx !important;
  font-size: 34rpx !important;
  font-weight: 600 !important;
  letter-spacing: 8rpx;
  background: $primary-gradient !important;
  border: none !important;
  box-shadow: 0 8rpx 28rpx rgba(58, 123, 247, 0.3);
  transition: transform $transition-fast, box-shadow $transition-fast;

  &:active {
    transform: scale(0.97);
    box-shadow: 0 4rpx 16rpx rgba(58, 123, 247, 0.2);
  }
}

/* 底部 */
.form-footer {
  display: flex;
  justify-content: center;
  margin-top: $spacing-lg;
}

.footer-text {
  font-size: $font-size-base;
  color: $text-color-hint;
}

.footer-link {
  font-size: $font-size-base;
  color: $primary-color;
  margin-left: 8rpx;
  font-weight: 500;
}

/* 协议 */
.agreement {
  padding: 32rpx $spacing-base 48rpx;
  text-align: center;
  position: relative;
  z-index: 1;
}

.agreement-text {
  font-size: $font-size-xs;
  color: $text-color-disabled;
}
</style>
