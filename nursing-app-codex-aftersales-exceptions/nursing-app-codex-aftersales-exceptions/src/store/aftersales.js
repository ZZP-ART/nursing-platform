import { ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotentKey } from '@/utils/request.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useAftersalesStore = defineStore('aftersales', () => {
  const cancelPreview = ref(null)
  const exceptions = ref([])
  const currentException = ref(null)
  const loading = ref(false)

  async function previewCancellation(orderId) {
    if (!USE_MOCK_API) {
      cancelPreview.value = {
        cancellable: true,
        ruleTitle: '取消订单',
        ruleDescription: '实际退款状态以后端取消订单响应为准',
        totalAmount: '--',
        deductionAmount: '--',
        refundAmount: '--',
      }
      return cancelPreview.value
    }
    const res = await http.get(`/api/v1/orders/${orderId}/cancel-preview`)
    cancelPreview.value = res.data
    return cancelPreview.value
  }

  async function cancelOrder(orderId, reason) {
    const res = await http.post(`/api/v1/orders/${orderId}/cancel`, { cancelReason: reason }, {
      idempotentKey: createIdempotentKey('cancel'),
    })
    return res.data
  }

  async function fetchMerchantExceptions(params = {}) {
    if (!USE_MOCK_API) throw unavailableApi('异常工单')
    loading.value = true
    try {
      const res = await http.get('/api/v1/merchant/exceptions', params)
      exceptions.value = res.data.list || []
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchMerchantExceptionDetail(exceptionId) {
    if (!USE_MOCK_API) throw unavailableApi('异常工单')
    const res = await http.get(`/api/v1/merchant/exceptions/${exceptionId}`)
    currentException.value = res.data
    return currentException.value
  }

  async function processMerchantException(exceptionId, action, payload = {}) {
    if (!USE_MOCK_API) throw unavailableApi('异常工单')
    const res = await http.post(`/api/v1/merchant/exceptions/${exceptionId}/${action}`, payload, {
      idempotentKey: createIdempotentKey(`exception-${action}`),
    })
    currentException.value = res.data
    return currentException.value
  }

  return { cancelPreview, exceptions, currentException, loading, previewCancellation, cancelOrder, fetchMerchantExceptions, fetchMerchantExceptionDetail, processMerchantException }
})
