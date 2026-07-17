<template>
  <div v-loading="loading" class="dashboard-page">
    <section class="welcome-banner"><div><span class="banner-eyebrow">今日审核概览</span><h2>上午好，{{ session.user?.name || '平台管理员' }}</h2><p>当前共有 <strong>{{ dashboard?.pendingTotal || 0 }}</strong> 条资料等待审核，请优先处理提交时间较早的任务。</p></div><div class="banner-orbit"><span>审核</span><i /><i /><i /></div></section>

    <section class="stat-grid">
      <stat-card label="待审核商户" :value="dashboard?.pending?.merchant || 0" note="企业主体与经营资质" icon="商" color="#3156d3" />
      <stat-card label="待认证护理人员" :value="dashboard?.pending?.caregiver || 0" note="身份、证书与服务能力" icon="护" color="#00a89d" />
      <stat-card label="待审核服务" :value="dashboard?.pending?.service || 0" note="内容、规格与风险提示" icon="服" color="#e7833b" />
      <stat-card label="今日已处理" :value="dashboard?.processedToday || 0" note="通过与驳回均计入" icon="✓" color="#7658d6" />
    </section>

    <section class="dashboard-grid">
      <article class="panel queue-panel"><div class="panel-head"><div><span>优先队列</span><h3>等待处理的护理人员申请</h3></div><el-button text type="primary" @click="router.push('/reviews/caregiver')">查看全部</el-button></div><el-table :data="dashboard?.priorityQueue || []" row-class-name="clickable-row" @row-click="goDetail"><el-table-column label="类型" width="130"><template #default="{ row }"><span class="type-pill" :style="{ '--type-color': getReviewTypeMeta(row.type).color }">{{ getReviewTypeMeta(row.type).label }}</span></template></el-table-column><el-table-column prop="subjectName" label="审核主体" min-width="180" /><el-table-column prop="applicantName" label="申请人/商户" min-width="140" /><el-table-column prop="submitTime" label="提交时间" width="170" /><el-table-column label="状态" width="100"><template #default="{ row }"><status-badge :status="row.status" /></template></el-table-column></el-table></article>
      <article class="panel guide-panel"><div class="panel-head"><div><span>审核规范</span><h3>处理前请重点核验</h3></div></div><div class="guide-list"><div><b>01</b><div><strong>主体真实性</strong><span>证件名称、编号与申请主体保持一致</span></div></div><div><b>02</b><div><strong>资质有效性</strong><span>证书清晰可辨，且处于有效期内</span></div></div><div><b>03</b><div><strong>服务合规性</strong><span>内容、定价、风险提示和适用范围完整</span></div></div><div><b>04</b><div><strong>操作可追溯</strong><span>驳回必须填写明确、可执行的修改意见</span></div></div></div></article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import StatCard from '../../components/stat-card.vue'
import StatusBadge from '../../components/status-badge.vue'
import { getReviewTypeMeta } from '../../constants/review.js'
import { useReviewStore } from '../../stores/review.js'
import { useSessionStore } from '../../stores/session.js'

const router = useRouter()
const reviewStore = useReviewStore()
const session = useSessionStore()
const loading = ref(true)
const dashboard = computed(() => reviewStore.dashboard)
onMounted(async () => { try { await reviewStore.fetchDashboard() } finally { loading.value = false } })
function goDetail(row) { router.push(`/reviews/${row.type}/${row.id}`) }
</script>
