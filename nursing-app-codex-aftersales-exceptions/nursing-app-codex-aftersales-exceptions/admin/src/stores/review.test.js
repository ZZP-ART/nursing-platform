import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useReviewStore } from './review.js'

vi.mock('../api/request.js', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  createIdempotencyKey: vi.fn((prefix) => `${prefix}-key`),
}))

import http from '../api/request.js'

describe('admin caregiver review store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads the caregiver application dashboard and list', async () => {
    const applications = [{ id: '11', userId: '101', realName: '张三', status: 0 }]
    http.get.mockResolvedValue({ data: applications })
    const store = useReviewStore()

    await store.fetchDashboard()
    await store.fetchReviews('caregiver', { status: 'PENDING_REVIEW' })

    expect(http.get).toHaveBeenNthCalledWith(1, '/api/v1/admin/caregiver-applications')
    expect(http.get).toHaveBeenNthCalledWith(2, '/api/v1/admin/caregiver-applications')
    expect(store.total).toBe(1)
    expect(store.reviews[0].id).toBe('101')
  })

  it('approves a caregiver application by userId', async () => {
    http.post.mockResolvedValueOnce({ data: null })
    http.get.mockResolvedValueOnce({ data: [] })
    const store = useReviewStore()

    await store.auditReview('caregiver', '101', 'approve', '资料核验无误')

    expect(http.post).toHaveBeenCalledWith(
      '/api/v1/admin/caregiver-applications/101/approve',
      null,
      {
        params: { remark: '资料核验无误' },
        headers: { 'Idempotency-Key': 'caregiver-approve-key' },
      },
    )
  })
})
