import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAftersalesStore } from '@/store/aftersales.js'

vi.mock('@/utils/request.js', () => ({ default: { get: vi.fn(), post: vi.fn() }, createIdempotentKey: vi.fn((prefix) => `${prefix}-key`) }))
import http from '@/utils/request.js'

describe('aftersalesStore', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })
  it('previews cancellation and submits cancellation idempotently', async () => {
    http.get.mockResolvedValueOnce({ data: { refundAmount: 120 } })
    http.post.mockResolvedValueOnce({ data: { refundStatus: 'PROCESSING' } })
    const store = useAftersalesStore()
    await store.previewCancellation(1)
    await store.cancelOrder(1, '计划有变')
    expect(http.get).toHaveBeenCalledWith('/api/v1/orders/1/cancel-preview')
    expect(http.post).toHaveBeenCalledWith('/api/v1/orders/1/cancel', { cancelReason: '计划有变' }, { idempotentKey: 'cancel-key' })
  })
  it('processes a merchant exception with a dedicated action endpoint', async () => {
    http.post.mockResolvedValueOnce({ data: { exceptionId: 1, status: 'RESOLVED' } })
    const store = useAftersalesStore()
    await store.processMerchantException(1, 'approve-check-in', { remark: '核验通过' })
    expect(http.post).toHaveBeenCalledWith('/api/v1/merchant/exceptions/1/approve-check-in', { remark: '核验通过' }, { idempotentKey: 'exception-approve-check-in-key' })
  })
})
