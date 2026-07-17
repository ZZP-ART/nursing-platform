import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useComplaintStore } from '@/store/complaint.js'

vi.mock('@/utils/request.js', () => ({ default: { get: vi.fn(), post: vi.fn() }, createIdempotentKey: vi.fn((prefix) => `${prefix}-key`) }))
import http from '@/utils/request.js'

describe('complaint store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })

  it('loads merchant complaints', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ complaintId: 1 }], total: 1 } })
    const store = useComplaintStore()
    await store.fetchMerchantComplaints({ status: 0 })
    expect(http.get).toHaveBeenCalledWith('/api/v1/merchant/complaints', { status: 0 })
  })

  it('escalates complaint with evidence idempotently', async () => {
    http.post.mockResolvedValueOnce({ data: { complaintId: 1, status: 4 } })
    const store = useComplaintStore()
    await store.processMerchantComplaint(1, 'escalate', { statement: '双方存在争议', evidence: [] })
    expect(http.post).toHaveBeenCalledWith('/api/v1/merchant/complaints/1/escalate', { statement: '双方存在争议', evidence: [] }, { idempotentKey: 'merchant-complaint-escalate-key' })
  })
})
