<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand"><div class="brand-mark">护</div><div><strong>智慧护理</strong><span>平台管理后台</span></div></div>
      <nav class="side-nav">
        <router-link to="/dashboard"><el-icon><DataAnalysis /></el-icon><span>审核工作台</span></router-link>
        <router-link to="/reviews/caregiver"><el-icon><UserFilled /></el-icon><span>护理人员审核</span><b v-if="counts.caregiver">{{ counts.caregiver }}</b></router-link>
        <router-link v-if="useMock" to="/reviews/merchant"><el-icon><OfficeBuilding /></el-icon><span>商户审核</span><b v-if="counts.merchant">{{ counts.merchant }}</b></router-link>
        <router-link v-if="useMock" to="/reviews/service"><el-icon><Management /></el-icon><span>服务审核</span><b v-if="counts.service">{{ counts.service }}</b></router-link>
        <router-link v-if="useMock" to="/exceptions"><el-icon><WarningFilled /></el-icon><span>异常监管</span><b v-if="exceptionCount">{{ exceptionCount }}</b></router-link>
        <router-link to="/complaints"><el-icon><ChatDotRound /></el-icon><span>投诉仲裁</span><b v-if="complaintCount">{{ complaintCount }}</b></router-link>
        <router-link v-if="useMock" to="/notifications"><el-icon><BellFilled /></el-icon><span>平台消息</span><b v-if="notificationStore.unreadCount">{{ notificationStore.unreadCount }}</b></router-link>
      </nav>
      <div class="side-footer"><span>{{ useMock ? '当前为 Mock 演示环境' : '当前为真实后端环境' }}</span><small>真实权限由后端校验</small></div>
    </aside>
    <main class="main-area">
      <header class="topbar"><div><span class="breadcrumb">智慧护理 / {{ route.meta.title }}</span><h1>{{ route.meta.title }}</h1></div><div class="admin-profile"><div class="avatar">{{ session.user?.name?.slice(0, 1) || '管' }}</div><div><strong>{{ session.user?.name || '平台管理员' }}</strong><span>{{ session.user?.roleName || '超级管理员' }}</span></div><el-dropdown @command="handleCommand"><button class="icon-button">•••</button><template #dropdown><el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></header>
      <section class="content-area"><router-view /></section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BellFilled, ChatDotRound, DataAnalysis, Management, OfficeBuilding, UserFilled, WarningFilled } from '@element-plus/icons-vue'
import { useNotificationStore } from '../stores/notification.js'
import { useReviewStore } from '../stores/review.js'
import { useSessionStore } from '../stores/session.js'

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()
const session = useSessionStore()
const notificationStore = useNotificationStore()
const useMock = import.meta.env.MODE === 'test' || import.meta.env.VITE_USE_MOCK === 'true'
const counts = computed(() => reviewStore.dashboard?.pending || {})
const exceptionCount = computed(() => reviewStore.dashboard?.exceptionCount || 0)
const complaintCount = computed(() => reviewStore.dashboard?.complaintCount || 0)
onMounted(() => Promise.all([reviewStore.fetchDashboard(), notificationStore.fetchUnreadCount()]))
function handleCommand(command) { if (command === 'logout') { session.logout(); router.replace('/login') } }
</script>
