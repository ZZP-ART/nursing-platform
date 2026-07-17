import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMerchantStore } from '@/store/merchant.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn() },
}))

import http from '@/utils/request.js'

describe('merchantStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('获取商户工作台统计', async () => {
    http.get.mockResolvedValueOnce({ data: { merchantId: 20001, waitingDispatch: 2 } })
    const store = useMerchantStore()

    const result = await store.fetchDashboard()

    expect(http.get).toHaveBeenCalledWith('/api/v1/merchants/dashboard')
    expect(result.waitingDispatch).toBe(2)
  })

  it('订单列表使用商户接口并规范化状态', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ orderId: 1, status: 1 }], total: 1 } })
    const store = useMerchantStore()

    await store.fetchOrders({ orderStatus: 'WAITING_DISPATCH' })

    expect(http.get).toHaveBeenCalledWith('/api/v1/merchants/orders', {
      page: 1,
      size: 20,
      orderStatus: 'WAITING_DISPATCH',
    })
    expect(store.orders[0].orderStatus).toBe('WAITING_DISPATCH')
    expect(store.total).toBe(1)
  })

  it('获取商户订单详情', async () => {
    http.get.mockResolvedValueOnce({ data: { orderId: 19998, orderStatus: 'WAITING_DISPATCH', paymentStatus: 'PAID' } })
    const store = useMerchantStore()

    await store.fetchOrderDetail(19998)

    expect(http.get).toHaveBeenCalledWith('/api/v1/merchants/orders/19998')
    expect(store.currentOrder.assignmentStatus).toBe('UNASSIGNED')
  })
})
