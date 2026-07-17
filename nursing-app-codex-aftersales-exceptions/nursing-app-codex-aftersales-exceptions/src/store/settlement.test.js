import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSettlementStore } from '@/store/settlement.js'

vi.mock('@/utils/request.js', () => ({ default: { get: vi.fn(), post: vi.fn() }, createIdempotentKey: vi.fn((prefix) => `${prefix}-key`) }))
import http from '@/utils/request.js'

describe('settlement store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })
  it('loads caregiver earnings', async () => { http.get.mockResolvedValueOnce({ data: { summary: { availableAmount: 56 } } }); const store = useSettlementStore(); await store.fetchCaregiverEarnings(); expect(http.get).toHaveBeenCalledWith('/api/v1/caregiver/earnings') })
  it('submits merchant withdrawal idempotently', async () => { http.post.mockResolvedValueOnce({ data: { withdrawalId: 1 } }); http.get.mockResolvedValueOnce({ data: { summary: {} } }); const store = useSettlementStore(); await store.applyMerchantWithdrawal({ amount: 10 }); expect(http.post).toHaveBeenCalledWith('/api/v1/merchant/withdrawals', { amount: 10 }, { idempotentKey: 'merchant-withdraw-key' }) })
})
