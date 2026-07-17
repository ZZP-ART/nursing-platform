import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useComplaintStore } from './complaint.js'

vi.mock('../api/request.js', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  createIdempotencyKey: vi.fn((prefix) => `${prefix}-key`),
}))
import http from '../api/request.js'

describe('admin complaint store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })

  it('loads the feedback complaint queue', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ id: '1' }], total: 1 } })
    const store = useComplaintStore()
    await store.fetchComplaints({ status: 0 })
    expect(http.get).toHaveBeenCalledWith('/api/v1/admin/feedback/complaints', { params: { status: 0 } })
  })

  it('handles a complaint with the contract body and idempotency header', async () => {
    http.post.mockResolvedValueOnce({ data: null })
    const store = useComplaintStore()
    await store.arbitrateComplaint('1', { status: 2, content: '核验完成并解决投诉' })
    expect(http.post).toHaveBeenCalledWith(
      '/api/v1/admin/feedback/complaints/1/handle',
      { status: 2, content: '核验完成并解决投诉' },
      { headers: { 'Idempotency-Key': 'complaint-handle-key' } },
    )
  })
})
