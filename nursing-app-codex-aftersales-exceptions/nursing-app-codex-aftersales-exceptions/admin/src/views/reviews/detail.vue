<template>
  <div v-loading="loading" class="review-detail-page">
    <div class="detail-nav"><el-button text @click="router.back()"><el-icon><ArrowLeft /></el-icon>返回列表</el-button><div v-if="review"><status-badge :status="review.status" /></div></div>
    <template v-if="review">
      <section class="detail-hero"><div class="hero-avatar" :style="{ background: typeMeta.color }">{{ review.subjectName.slice(0, 1) }}</div><div class="hero-copy"><span>{{ typeMeta.label }} · {{ review.referenceNo }}</span><h2>{{ review.subjectName }}</h2><p>{{ review.summary }}</p></div><div class="hero-meta"><span>提交时间</span><strong>{{ review.submitTime }}</strong><span>申请人/所属商户</span><strong>{{ review.applicantName }}</strong></div></section>
      <section class="detail-layout">
        <div class="detail-main"><article class="panel detail-panel"><div class="panel-head"><div><span>申请资料</span><h3>主体与业务信息</h3></div></div><div class="field-table"><div v-for="field in review.fields" :key="field.label" :class="{ wide: field.wide }"><span>{{ field.label }}</span><strong>{{ field.value || '--' }}</strong></div></div></article><article class="panel detail-panel"><div class="panel-head"><div><span>资质附件</span><h3>证件和证明材料</h3></div></div><div class="document-grid"><div v-for="document in review.documents" :key="document.name" class="document-card"><div class="document-preview"><span>{{ document.type }}</span><b>已提交</b></div><strong>{{ document.name }}</strong><span>{{ document.number || '未填写编号' }}</span><small>{{ document.validUntil ? `有效期至 ${document.validUntil}` : '长期有效或未设置有效期' }}</small></div></div></article></div>
        <aside class="detail-side"><article class="panel timeline-panel"><div class="panel-head"><div><span>处理记录</span><h3>审核时间线</h3></div></div><el-timeline><el-timeline-item v-for="record in review.records" :key="record.id" :timestamp="record.time" :type="record.action === 'APPROVE' ? 'success' : record.action === 'REJECT' ? 'danger' : 'primary'" placement="top"><strong>{{ record.title }}</strong><p>{{ record.remark }}</p><small>{{ record.operator }}</small></el-timeline-item></el-timeline></article><article v-if="review.status === REVIEW_STATUS.PENDING_REVIEW" class="action-panel"><strong>审核操作</strong><p>请完成资料核验后选择通过或驳回。所有操作将写入审计记录。</p><el-button type="success" size="large" @click="dialog.open('approve')">审核通过</el-button><el-button type="danger" plain size="large" @click="dialog.open('reject')">驳回修改</el-button></article><article v-else class="result-panel" :class="review.status === REVIEW_STATUS.APPROVED ? 'success' : 'danger'"><strong>{{ review.status === REVIEW_STATUS.APPROVED ? '审核已通过' : '申请已驳回' }}</strong><p>{{ review.auditReason || '该审核任务已完成处理。' }}</p><span>{{ review.auditTime }} · {{ review.auditorName }}</span></article></aside>
      </section>
      <audit-dialog ref="dialog" @confirm="handleAudit" />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AuditDialog from '../../components/audit-dialog.vue'
import StatusBadge from '../../components/status-badge.vue'
import { REVIEW_STATUS, getReviewTypeMeta } from '../../constants/review.js'
import { useReviewStore } from '../../stores/review.js'

const route = useRoute()
const router = useRouter()
const reviewStore = useReviewStore()
const dialog = ref(null)
const loading = ref(true)
const review = computed(() => reviewStore.currentReview)
const typeMeta = computed(() => getReviewTypeMeta(route.params.type))
async function loadDetail() { loading.value = true; try { await reviewStore.fetchReviewDetail(route.params.type, route.params.id) } finally { loading.value = false } }
async function handleAudit({ action, reason, resolve, reject }) { try { await reviewStore.auditReview(route.params.type, route.params.id, action, reason); ElMessage.success(action === 'approve' ? '审核已通过' : '已驳回并记录审核意见'); resolve() } catch (error) { reject(error) } }
onMounted(loadDetail)
watch(() => [route.params.type, route.params.id], loadDetail)
</script>
