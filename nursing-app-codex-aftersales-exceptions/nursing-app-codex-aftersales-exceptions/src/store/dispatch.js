import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { createIdempotentKey } from '@/utils/request.js'
import { USE_MOCK_API } from '@/constants/api-capabilities.js'

export const useDispatchStore = defineStore('dispatch', () => {
  const candidates = ref([])
  const assignments = ref([])
  const loading = ref(false)

  async function fetchCandidates(orderId) {
    loading.value = true
    try {
      const res = await http.get(`/api/v1/merchants/orders/${orderId}/candidates`)
      candidates.value = Array.isArray(res.data) ? res.data : res.data?.list || []
      return candidates.value
    } finally {
      loading.value = false
    }
  }

  async function fetchAssignments(orderId) {
    const path = USE_MOCK_API
      ? `/api/v1/merchants/orders/${orderId}/assignments`
      : '/api/v1/merchants/assignments'
    const res = await http.get(path)
    const list = Array.isArray(res.data) ? res.data : res.data?.list || []
    assignments.value = list.filter((item) => String(item.orderId) === String(orderId))
    return assignments.value
  }

  async function dispatchOrder(orderId, caregiverUserId, remark = '') {
    const res = await http.post(`/api/v1/merchants/orders/${orderId}/dispatch`, {
      [USE_MOCK_API ? 'caregiverId' : 'caregiverUserId']: caregiverUserId,
      remark,
    }, {
      idempotentKey: createIdempotentKey('dispatch'),
    })
    return res.data
  }

  async function redispatchOrder(orderId, caregiverUserId, remark = '') {
    const res = await http.post(`/api/v1/merchants/orders/${orderId}/redispatch`, {
      [USE_MOCK_API ? 'caregiverId' : 'caregiverUserId']: caregiverUserId,
      remark,
    }, {
      idempotentKey: createIdempotentKey('redispatch'),
    })
    return res.data
  }

  return {
    candidates,
    assignments,
    loading,
    fetchCandidates,
    fetchAssignments,
    dispatchOrder,
    redispatchOrder,
  }
})
