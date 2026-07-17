import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useOrderStore } from '@/store/order.js'
import { ORDER_STATUS, PAYMENT_STATUS } from '@/constants/order-status.js'

vi.mock('@/utils/request.js', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
  setIdempotentKey: vi.fn(),
  clearIdempotentKey: vi.fn(),
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
}))

import http from '@/utils/request.js'

describe('orderStore multi-state support', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('订单列表补齐新状态并保留旧 status', async () => {
    http.get.mockResolvedValueOnce({
      data: {
        list: [{ orderId: 1, status: 1, serviceItemName: '测试服务' }],
        total: 1,
      },
    })
    const store = useOrderStore()

    const result = await store.fetchOrders()

    expect(result.list[0].orderStatus).toBe(ORDER_STATUS.WAITING_DISPATCH)
    expect(result.list[0].paymentStatus).toBe(PAYMENT_STATUS.PAID)
    expect(result.list[0].status).toBe(1)
  })

  it('顾客确认使用 confirm 专用接口和幂等键', async () => {
    http.post.mockResolvedValueOnce({ data: { orderId: 1, orderStatus: ORDER_STATUS.COMPLETED } })
    const store = useOrderStore()

    await store.confirmOrder(1)

    expect(http.post).toHaveBeenCalledWith('/api/v1/orders/1/confirm', null, {
      idempotentKey: 'confirm-key',
    })
  })

  it('取消订单使用专用幂等键', async () => {
    http.post.mockResolvedValueOnce({ data: { refundStatus: 'PROCESSING' } })
    const store = useOrderStore()
    await store.cancelOrder(1, '计划有变')
    expect(http.post).toHaveBeenCalledWith('/api/v1/orders/1/cancel', { cancelReason: '计划有变' }, { idempotentKey: 'cancel-key' })
  })
})
