import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useDispatchStore } from '@/store/dispatch.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
}))

import http from '@/utils/request.js'

describe('dispatchStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('查询候选人员并提交派单', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ caregiverId: 50001 }] } })
    http.post.mockResolvedValueOnce({ data: { assignmentId: 60001 } })
    const store = useDispatchStore()

    await store.fetchCandidates(19998)
    await store.dispatchOrder(19998, 50001, '优先指派')

    expect(store.candidates).toEqual([{ caregiverId: 50001 }])
    expect(http.post).toHaveBeenCalledWith('/api/v1/merchants/orders/19998/dispatch', {
      caregiverId: 50001,
      remark: '优先指派',
    }, {
      idempotentKey: 'dispatch-key',
    })
  })

  it('重新派单使用独立接口和幂等键', async () => {
    http.post.mockResolvedValueOnce({ data: { assignmentId: 60006 } })
    const store = useDispatchStore()

    await store.redispatchOrder(19998, 50002, '原护理人员拒绝')

    expect(http.post).toHaveBeenCalledWith('/api/v1/merchants/orders/19998/redispatch', {
      caregiverId: 50002,
      remark: '原护理人员拒绝',
    }, {
      idempotentKey: 'redispatch-key',
    })
  })
})
