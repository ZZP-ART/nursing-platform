import { defineStore } from 'pinia'
import { ref } from 'vue'
import http from '@/utils/request.js'
import { normalizeOrderState } from '@/constants/order-status.js'
import { USE_MOCK_API } from '@/constants/api-capabilities.js'

export const useMerchantStore = defineStore('merchant', () => {
  const dashboard = ref(null)
  const orders = ref([])
  const currentOrder = ref(null)
  const total = ref(0)
  const loading = ref(false)
  const exceptions = ref([])

  async function fetchDashboard() {
    const res = await http.get('/api/v1/merchants/dashboard')
    dashboard.value = res.data
    return dashboard.value
  }

  async function fetchOrders(params = {}) {
    loading.value = true
    try {
      const res = await http.get('/api/v1/merchants/orders', {
        page: params.page || 1,
        size: params.size || 20,
        ...(params.orderStatus ? { orderStatus: params.orderStatus } : {}),
        ...(params.assignmentStatus ? { assignmentStatus: params.assignmentStatus } : {}),
        ...(params.keyword ? { keyword: params.keyword } : {}),
      })
      const list = Array.isArray(res.data) ? res.data : res.data?.list || []
      orders.value = list.map(normalizeOrderState)
      total.value = res.data?.total || orders.value.length
      return { list: orders.value, total: total.value }
    } finally {
      loading.value = false
    }
  }

  async function fetchOrderDetail(orderId) {
    loading.value = true
    try {
      const res = await http.get(`/api/v1/merchants/orders/${orderId}`)
      currentOrder.value = normalizeOrderState(res.data)
      return currentOrder.value
    } finally {
      loading.value = false
    }
  }

  async function fetchExceptions(params = {}) {
    if (!USE_MOCK_API) {
      exceptions.value = []
      return { list: [] }
    }
    const res = await http.get('/api/v1/merchant/exceptions', params)
    exceptions.value = res.data?.list || []
    return res.data
  }

  return {
    dashboard,
    orders,
    currentOrder,
    total,
    loading,
    exceptions,
    fetchDashboard,
    fetchOrders,
    fetchOrderDetail,
    fetchExceptions,
  }
})
