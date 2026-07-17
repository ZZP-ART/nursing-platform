import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMerchantReportStore } from '@/store/merchant-report.js'

vi.mock('@/utils/request.js', () => ({ default: { get: vi.fn() } }))
import http from '@/utils/request.js'

describe('merchant report store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })

  it('loads the selected operating report range', async () => {
    http.get.mockResolvedValueOnce({ data: { range: '7d', overview: { orderCount: 2 } } })
    const store = useMerchantReportStore()
    await store.fetchReport('7d')
    expect(http.get).toHaveBeenCalledWith('/api/v1/merchant/reports/operations', { range: '7d' })
    expect(store.range).toBe('7d')
    expect(store.report.overview.orderCount).toBe(2)
  })
})
