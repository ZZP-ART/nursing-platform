import { ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotencyKey } from '../api/request.js'

export const useComplaintStore = defineStore('adminComplaint', () => {
  const complaints = ref([])
  const currentComplaint = ref(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchComplaints(params = {}) {
    loading.value = true
    try {
      const res = await http.get('/api/v1/admin/feedback/complaints', { params })
      complaints.value = res.data.list || []
      total.value = res.data.total || 0
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchComplaintDetail(id) {
    const res = await http.get('/api/v1/admin/feedback/complaints', { params: { page: 1, size: 50 } })
    currentComplaint.value = (res.data.list || []).find((item) => String(item.id) === String(id)) || null
    return currentComplaint.value
  }

  async function arbitrateComplaint(id, payload) {
    const status = payload.status ?? (payload.decision === 'NO_REFUND' ? 3 : 2)
    const content = payload.content || payload.remark
    await http.post(`/api/v1/admin/feedback/complaints/${id}/handle`, { status, content }, {
      headers: { 'Idempotency-Key': createIdempotencyKey('complaint-handle') },
    })
    currentComplaint.value = { ...currentComplaint.value, status }
    return currentComplaint.value
  }

  return { complaints, currentComplaint, total, loading, fetchComplaints, fetchComplaintDetail, arbitrateComplaint }
})
