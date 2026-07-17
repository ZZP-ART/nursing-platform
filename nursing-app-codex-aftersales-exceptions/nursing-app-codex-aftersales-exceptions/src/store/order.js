/**
 * Pinia — 订单管理
 *
 * 对齐 API v1.0（order-service）：
 * - 下单：prepay-token → create order → pay
 * - 列表、详情、取消
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, {
  setIdempotentKey,
  clearIdempotentKey,
  createIdempotentKey,
} from '@/utils/request.js'
import {
  LEGACY_STATUS_MAP,
  canCustomerCancel,
  canCustomerComplain,
  canCustomerConfirm,
  canCustomerPay,
  canCustomerReview,
  getOrderStatusMeta,
  normalizeOrderState,
} from '@/constants/order-status.js'

export const useOrderStore = defineStore('order', () => {
  // ===== 状态 =====
  const orders = ref([])
  const total = ref(0)
  const currentOrder = ref(null)
  const prepayToken = ref('')
  const loading = ref(false)
  const paymentKeys = new Map()

  // ===== 状态映射 =====
  const STATUS_MAP = LEGACY_STATUS_MAP

  const SLOT_MAP = {
    MORNING: '上午 (08:00-12:00)',
    AFTERNOON: '下午 (13:00-17:00)',
    EVENING: '晚上 (18:00-21:00)',
  }

  // ===== 方法 =====

  /** 第一步：获取下单幂等令牌 */
  async function getPrepayToken() {
    const res = await http.post('/api/v1/orders/prepay-token')
    const token = res.data?.prepayToken
    prepayToken.value = token
    setIdempotentKey(token)
    return token
  }

  /**
   * 第二步：创建订单
   * @param {Object} params - { serviceItemId, serviceSpecId, addressId, serviceDate, serviceTimeSlot, remark }
   */
  async function createOrder(params) {
    if (!prepayToken.value) {
      throw new Error('请先获取下单令牌')
    }
    const res = await http.post('/api/v1/orders', params, {
      idempotentKey: prepayToken.value,
    })
    clearIdempotentKey()
    prepayToken.value = ''
    return res.data // { orderId, orderNo }
  }

  /**
   * 第三步：发起支付
   */
  async function payOrder(orderId) {
    if (!paymentKeys.has(orderId)) {
      paymentKeys.set(orderId, createIdempotentKey('pay'))
    }
    const res = await http.post(`/api/v1/orders/${orderId}/pay`, {
      payChannel: 'alipay',
    }, {
      idempotentKey: paymentKeys.get(orderId),
    })
    return res.data
  }

  /** 发起支付并在非 Mock 环境调起支付宝。 */
  async function executePayment(orderId) {
    const payment = await payOrder(orderId)
    if (payment.mock || payment.payStatus === 'SUCCESS') {
      return { ...payment, success: true }
    }
    if (payment.payStatus !== 'READY' || !payment.payParams) {
      return { ...payment, success: false }
    }

    await new Promise((resolve, reject) => {
      uni.requestPayment({
        provider: 'alipay',
        orderInfo: payment.payParams,
        success: resolve,
        fail: reject,
      })
    })
    return { ...payment, success: true }
  }

  /** 用户确认护理人员已完成服务 */
  async function confirmOrder(orderId) {
    const res = await http.post(`/api/v1/orders/${orderId}/confirm`, null, {
      idempotentKey: createIdempotentKey('confirm'),
    })
    if (currentOrder.value?.orderId === orderId) {
      await fetchOrderDetail(orderId)
    }
    return res.data
  }

  const completeOrder = confirmOrder

  /** 订单列表（支持状态筛选 + 分页） */
  async function fetchOrders(params = {}) {
    loading.value = true
    try {
      const query = { page: params.page || 1, size: params.size || 20 }
      if (params.status !== undefined && params.status !== null) {
        query.status = params.status
      }
      const res = await http.get('/api/v1/orders', query)
      const data = res.data
      orders.value = (data.list || []).map(normalizeOrderState)
      total.value = data.total || 0
      return { list: orders.value, total: total.value }
    } finally {
      loading.value = false
    }
  }

  /** 订单详情 */
  async function fetchOrderDetail(orderId) {
    loading.value = true
    try {
      const res = await http.get(`/api/v1/orders/${orderId}`)
      currentOrder.value = normalizeOrderState(res.data)
      return currentOrder.value
    } finally {
      loading.value = false
    }
  }

  /** 取消订单 */
  async function cancelOrder(orderId, cancelReason) {
    const res = await http.post(`/api/v1/orders/${orderId}/cancel`, {
      cancelReason: cancelReason || '',
    }, {
      idempotentKey: createIdempotentKey('cancel'),
    })
    // 刷新详情
    if (currentOrder.value?.orderId === orderId) {
      await fetchOrderDetail(orderId)
    }
    return res.data
  }

  async function fetchAftersales() {
    return null
  }

  /** 获取状态文案 */
  function getStatusText(status) {
    if (typeof status === 'object') return getOrderStatusMeta(status).text
    return getOrderStatusMeta(status).text
  }

  function getStatusMeta(order) {
    return getOrderStatusMeta(order)
  }

  /** 获取时段文案 */
  function getSlotText(slot) {
    return SLOT_MAP[slot] || slot
  }

  return {
    orders,
    total,
    currentOrder,
    prepayToken,
    loading,
    STATUS_MAP,
    SLOT_MAP,
    getPrepayToken,
    createOrder,
    payOrder,
    executePayment,
    confirmOrder,
    completeOrder,
    fetchOrders,
    fetchOrderDetail,
    cancelOrder,
    fetchAftersales,
    getStatusText,
    getStatusMeta,
    getSlotText,
    canCustomerPay,
    canCustomerCancel,
    canCustomerConfirm,
    canCustomerReview,
    canCustomerComplain,
  }
})
