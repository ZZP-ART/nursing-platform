import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMerchantOnboardingStore } from '@/store/merchant-onboarding.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn(), put: vi.fn(), post: vi.fn() },
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
}))

import http from '@/utils/request.js'

describe('merchantOnboardingStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads and saves merchant onboarding data', async () => {
    http.get.mockResolvedValueOnce({ data: { auditStatus: 'REJECTED' } })
    http.put.mockResolvedValueOnce({ data: { auditStatus: 'DRAFT', merchantName: '安心护理' } })
    const store = useMerchantOnboardingStore()

    await store.fetchApplication()
    await store.saveApplication({ merchantName: '安心护理' })

    expect(http.get).toHaveBeenCalledWith('/api/v1/merchants/application')
    expect(http.put).toHaveBeenCalledWith('/api/v1/merchants/application', { merchantName: '安心护理' })
    expect(store.canEditApplication).toBe(true)
  })

  it('submits onboarding data with an idempotency key', async () => {
    http.post.mockResolvedValueOnce({ data: { auditStatus: 'PENDING_REVIEW' } })
    const store = useMerchantOnboardingStore()

    await store.submitApplication({ merchantName: '安心护理' })

    expect(http.post).toHaveBeenCalledWith('/api/v1/merchants/apply', { merchantName: '安心护理' }, {
      idempotentKey: 'merchant-apply-key',
    })
    expect(store.canEditApplication).toBe(false)
  })
})
