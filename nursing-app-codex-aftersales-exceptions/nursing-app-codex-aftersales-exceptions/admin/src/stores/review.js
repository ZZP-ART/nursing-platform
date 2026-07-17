import { ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotencyKey } from '../api/request.js'

function normalizeStatus(status) {
  return ({ 0: 'PENDING_REVIEW', 1: 'APPROVED', 2: 'REJECTED' })[status] || status
}

function normalizeApplication(item = {}) {
  return {
    ...item,
    id: item.userId ?? item.id,
    applicationId: item.id,
    subjectName: item.subjectName ?? item.realName ?? '--',
    referenceNo: item.referenceNo ?? item.id ?? item.userId,
    applicantName: item.applicantName ?? item.phone ?? '--',
    summary: item.summary ?? `${item.serviceDistrict || '--'} · ${item.skills || '--'}`,
    submitTime: item.submitTime ?? item.createTime,
    status: normalizeStatus(item.status),
    fields: item.fields || [
      { label: '真实姓名', value: item.realName },
      { label: '手机号', value: item.phone },
      { label: '服务区域', value: item.serviceDistrict },
      { label: '服务技能', value: item.skills, wide: true },
    ],
    documents: item.documents || [],
    records: item.records || [],
  }
}

export const useReviewStore = defineStore('adminReview', () => {
  const dashboard = ref(null)
  const reviews = ref([])
  const currentReview = ref(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchDashboard() {
    const res = await http.get('/api/v1/admin/caregiver-applications')
    const list = (res.data || []).map(normalizeApplication)
    const pending = list.filter((item) => item.status === 'PENDING_REVIEW')
    dashboard.value = {
      pendingTotal: pending.length,
      pending: { merchant: 0, caregiver: pending.length, service: 0 },
      processedToday: 0,
      priorityQueue: pending.map((item) => ({ ...item, type: 'caregiver' })),
    }
    return dashboard.value
  }

  async function fetchReviews(type, params = {}) {
    loading.value = true
    try {
      if (type !== 'caregiver') {
        reviews.value = []
        total.value = 0
        return { list: [], total: 0 }
      }
      const res = await http.get('/api/v1/admin/caregiver-applications')
      const normalized = (res.data || []).map(normalizeApplication)
      reviews.value = params.status ? normalized.filter((item) => item.status === params.status) : normalized
      total.value = reviews.value.length
      return { list: reviews.value, total: total.value }
    } finally {
      loading.value = false
    }
  }

  async function fetchReviewDetail(type, id) {
    if (type !== 'caregiver') return null
    const res = await http.get('/api/v1/admin/caregiver-applications')
    currentReview.value = (res.data || []).map(normalizeApplication)
      .find((item) => String(item.userId) === String(id) || String(item.id) === String(id)) || null
    return currentReview.value
  }

  async function auditReview(type, id, action, reason) {
    if (type !== 'caregiver') throw new Error('后端当前仅提供护理人员申请审核接口')
    const apiAction = action === 'approve' ? 'approve' : 'reject'
    await http.post(`/api/v1/admin/caregiver-applications/${id}/${apiAction}`, null, {
      params: reason ? { remark: reason } : undefined,
      headers: { 'Idempotency-Key': createIdempotencyKey(`caregiver-${apiAction}`) },
    })
    const nextStatus = action === 'approve' ? 'APPROVED' : 'REJECTED'
    currentReview.value = { ...currentReview.value, status: nextStatus, auditReason: reason }
    const index = reviews.value.findIndex((item) => String(item.id) === String(id))
    if (index >= 0) reviews.value[index] = { ...reviews.value[index], status: nextStatus }
    await fetchDashboard()
    return currentReview.value
  }

  return { dashboard, reviews, currentReview, total, loading, fetchDashboard, fetchReviews, fetchReviewDetail, auditReview }
})
