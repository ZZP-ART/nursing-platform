<template>
  <div class="login-page">
    <div class="login-visual">
      <div class="visual-glow glow-one" /><div class="visual-glow glow-two" />
      <div class="visual-content"><span class="visual-eyebrow">智慧护理平台</span><h1>让每一次审核<br />都有依据、有记录</h1><p>统一处理商户入驻、护理人员认证和服务发布，让平台运营更清晰、更可靠。</p><div class="visual-metrics"><div><strong>3</strong><span>审核领域</span></div><div><strong>全程</strong><span>操作留痕</span></div><div><strong>独立</strong><span>管理权限</span></div></div></div>
    </div>
    <div class="login-panel">
      <div class="login-form-wrap"><div class="mobile-brand"><span>护</span><strong>智慧护理管理后台</strong></div><span class="form-eyebrow">ADMIN PORTAL</span><h2>欢迎回来</h2><p class="form-subtitle">使用平台管理员账号登录</p>
        <el-form label-position="top" @submit.prevent="handleLogin">
          <el-form-item label="管理员账号"><el-input v-model="form.account" size="large" placeholder="请输入手机号或账号" clearable /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" size="large" type="password" placeholder="请输入密码" show-password @keyup.enter="handleLogin" /></el-form-item>
          <div class="login-hint"><span>本地演示账号</span><code>admin / Admin123456</code></div>
          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">登录管理后台</el-button>
        </el-form>
        <p class="security-note">管理员账号仅由平台内部授予，不开放自主注册</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '../../stores/session.js'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const loading = ref(false)
const form = reactive({ account: 'admin', password: 'Admin123456' })

async function handleLogin() {
  if (!form.account.trim() || !form.password) return ElMessage.warning('请输入管理员账号和密码')
  loading.value = true
  try {
    await session.login(form.account.trim(), form.password)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>
