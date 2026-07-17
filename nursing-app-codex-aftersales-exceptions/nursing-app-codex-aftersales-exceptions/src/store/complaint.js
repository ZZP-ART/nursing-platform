/**
 * Pinia — 投诉管理
 *
 * 对齐 API v1.0（feedback-service / complaints）：
 * - 提交投诉（4种类型）
 * - 投诉列表（分页）
 * - 投诉处理记录（时间线）
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { createIdempotentKey } from '@/utils/request.js'
import { COMPLAINT_STATUS, getComplaintStatusMeta } from '@/constants/complaint-status.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useComplaintStore = defineStore('complaint', () => {
  // ===== 状态 =====
  const complaints = ref([])
  const total = ref(0)
  const currentTracks = ref(null)
  const merchantComplaints = ref([])
  const currentMerchantComplaint = ref(null)
  const loading = ref(false)

  // ===== 类型映射 =====
  const TYPE_OPTIONS = [
    { value: 1, label: '服务质量' },
    { value: 2, label: '服务态度' },
    { value: 3, label: '乱收费' },
    { value: 4, label: '其他' },
  ]

  const STATUS_MAP = {
    0: '待商户响应',
    1: '商户处理中',
    2: '已裁决',
    3: '已关闭',
    4: '平台仲裁中',
  }

  // ===== 方法 =====

  /**
   * 提交投诉
   * @param {Object} params - { orderId, type, content, images }
   */
  async function submitComplaint(params) {
    const res = await http.post('/api/v1/complaints', params, {
      idempotentKey: createIdempotentKey('complaint'),
    })
    return res.data // { complaintId }
  }

  /**
   * 投诉列表
   * @param {Object} params - { page, size }
   */
  async function fetchComplaints(params = {}) {
    loading.value = true
    try {
      const query = { page: params.page || 1, size: params.size || 20 }
      const res = await http.get('/api/v1/complaints', query)
      const data = res.data
      complaints.value = (data.list || []).map((item) => ({
        ...item,
        statusText: getStatusText(item.status),
      }))
      total.value = data.total || 0
      return { list: complaints.value, total: total.value }
    } finally {
      loading.value = false
    }
  }

  /**
   * 投诉处理记录
   * @param {number} complaintId
   */
  async function fetchComplaintTracks(complaintId) {
    loading.value = true
    try {
      const res = await http.get(`/api/v1/complaints/${complaintId}/tracks`)
      currentTracks.value = {
        ...res.data,
        statusText: getStatusText(res.data.status),
      }
      return currentTracks.value
    } finally {
      loading.value = false
    }
  }

  /** 获取状态文案 */
  function getStatusText(status) {
    return getComplaintStatusMeta(status).label
  }

  async function fetchMerchantComplaints(params = {}) {
    if (!USE_MOCK_API) { merchantComplaints.value = []; return { list: [] } }
    loading.value = true
    try {
      const res = await http.get('/api/v1/merchant/complaints', params)
      merchantComplaints.value = res.data?.list || []
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchMerchantComplaintDetail(complaintId) {
    if (!USE_MOCK_API) throw unavailableApi('商户投诉协同')
    const res = await http.get(`/api/v1/merchant/complaints/${complaintId}`)
    currentMerchantComplaint.value = res.data
    return currentMerchantComplaint.value
  }

  async function processMerchantComplaint(complaintId, action, payload) {
    if (!USE_MOCK_API) throw unavailableApi('商户投诉协同')
    const res = await http.post(`/api/v1/merchant/complaints/${complaintId}/${action}`, payload, {
      idempotentKey: createIdempotentKey(`merchant-complaint-${action}`),
    })
    currentMerchantComplaint.value = res.data
    return currentMerchantComplaint.value
  }

  return {
    complaints,
    total,
    currentTracks,
    merchantComplaints,
    currentMerchantComplaint,
    loading,
    TYPE_OPTIONS,
    STATUS_MAP,
    COMPLAINT_STATUS,
    submitComplaint,
    fetchComplaints,
    fetchComplaintTracks,
    fetchMerchantComplaints,
    fetchMerchantComplaintDetail,
    processMerchantComplaint,
    getStatusText,
  }
})
