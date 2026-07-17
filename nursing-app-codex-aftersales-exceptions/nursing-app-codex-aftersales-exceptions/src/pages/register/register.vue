<template>
  <view class="register-page">
    <!-- 背景弥散光 -->
    <view class="bg-glow bg-glow-top" />
    <view class="bg-glow bg-glow-bottom" />

    <!-- 标题 -->
    <view class="header">
      <text class="header-title">注册新账号</text>
      <text class="header-desc">注册后即可预约护理服务</text>
    </view>

    <!-- 注册表单 — 玻璃拟态卡片 -->
    <view class="form-section">
      <view class="form-card">
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

        <!-- 验证码 -->
        <view class="input-group">
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

        <!-- 昵称（选填） -->
        <view class="input-group">
          <text class="input-label">昵称（选填）</text>
          <u-input
            v-model="nickname"
            type="text"
            maxlength="16"
            placeholder="给自己取个名字吧"
            border="bottom"
          >
            <template #prefix>
              <u-icon name="account" size="20" color="#6B7B8D" />
            </template>
          </u-input>
        </view>

        <!-- 设置密码 -->
        <view class="input-group">
          <text class="input-label">设置密码</text>
          <u-input
            v-model="password"
            type="password"
            placeholder="8-32位，需包含字母和数字"
            border="bottom"
          >
            <template #prefix>
              <u-icon name="lock-fill" size="20" color="#6B7B8D" />
            </template>
          </u-input>
        </view>

        <!-- 确认密码 -->
        <view class="input-group">
          <text class="input-label">确认密码</text>
          <u-input
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            border="bottom"
          >
            <template #prefix>
              <u-icon name="lock-fill" size="20" color="#6B7B8D" />
            </template>
          </u-input>
        </view>

        <!-- 注册按钮 — 渐变 -->
        <view class="submit-wrap">
          <u-button
            type="primary"
            shape="round"
            :loading="submitting"
            :disabled="!canSubmit"
            class="submit-btn"
            @click="handleRegister"
          >
            注 册
          </u-button>
        </view>

        <!-- 返回登录 -->
        <view class="form-footer">
          <text class="footer-text">已有账号？</text>
          <text class="footer-link" @click="goLogin">立即登录</text>
        </view>
      </view>
    </view>

    <!-- 协议 -->
    <view class="agreement">
      <text class="agreement-text">
        注册即表示同意《用户服务协议》和《隐私政策》
      </text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user.js'
import { useRoleStore } from '@/store/role.js'

const userStore = useUserStore()
const roleStore = useRoleStore()
const phone = ref('')
const code = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const countdown = ref(0)
const submitting = ref(false)
let timer = null

// 密码校验：8-32位，包含字母和数字
const isPasswordValid = computed(() => {
  const pwd = password.value
  return pwd.length >= 8 && pwd.length <= 32 && /[a-zA-Z]/.test(pwd) && /\d/.test(pwd)
})

const canSubmit = computed(() => {
  return (
    phone.value.length === 11 &&
    code.value.length === 6 &&
    isPasswordValid.value &&
    password.value === confirmPassword.value &&
    !submitting.value
  )
})

function sendCode() {
  if (phone.value.length !== 11) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  userStore.sendSmsCode(phone.value, 'register')
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

async function handleRegister() {
  if (!canSubmit.value) return

  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    await userStore.register(
      phone.value,
      code.value,
      password.value,
      nickname.value || undefined
    )
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      roleStore.goToWorkspace()
    }, 1000)
  } catch {
    // 错误提示已在 request.js 拦截器中统一处理
  } finally {
    submitting.value = false
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: $page-gradient;
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

.header {
  padding: 72rpx $spacing-lg 40rpx;
  text-align: center;
  position: relative;
  z-index: 1;
}

.header-title {
  font-size: 44rpx;
  font-weight: 700;
  color: $text-color;
  letter-spacing: 2rpx;
}

.header-desc {
  display: block;
  font-size: $font-size-base;
  color: $text-color-hint;
  margin-top: $spacing-sm;
}

.form-section {
  padding: 0 $spacing-base;
  position: relative;
  z-index: 1;
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
  border-color: rgba(58,123,247,0.28) !important;
  color: $primary-color !important;
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
