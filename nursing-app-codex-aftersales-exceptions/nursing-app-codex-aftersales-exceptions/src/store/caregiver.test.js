import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCaregiverStore } from '@/store/caregiver.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn(), put: vi.fn(), post: vi.fn() },
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
}))

import http from '@/utils/request.js'

describe('caregiverStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('loads and saves the current account application', async () => {
    http.get.mockResolvedValueOnce({ data: { auditStatus: 'DRAFT' } })
    http.put.mockResolvedValueOnce({ data: { auditStatus: 'DRAFT', realName: '张三' } })
    const store = useCaregiverStore()

    await store.fetchApplication()
    await store.saveApplication({ realName: '张三' })

    expect(http.get).toHaveBeenCalledWith('/api/v1/caregivers/application')
    expect(http.put).toHaveBeenCalledWith('/api/v1/caregivers/application', { realName: '张三' })
    expect(store.canEditApplication).toBe(true)
  })

  it('submits certification with an idempotency key', async () => {
    http.post.mockResolvedValueOnce({ data: { auditStatus: 'PENDING_REVIEW' } })
    const store = useCaregiverStore()

    await store.submitApplication({ realName: '张三' })

    expect(http.post).toHaveBeenCalledWith('/api/v1/caregivers/apply', { realName: '张三' }, {
      idempotentKey: 'caregiver-apply-key',
    })
    expect(store.canEditApplication).toBe(false)
  })

  it('loads and saves caregiver schedule', async () => {
    http.get.mockResolvedValueOnce({ data: { caregiverId: 50001, days: [] } })
    http.put.mockResolvedValueOnce({ data: { caregiverId: 50001, enabled: true, days: [] } })
    const store = useCaregiverStore()
    await store.fetchSchedule()
    await store.saveSchedule({ enabled: true, serviceAreas: ['朝阳区'], days: [] })
    expect(http.get).toHaveBeenCalledWith('/api/v1/caregiver/schedule')
    expect(http.put).toHaveBeenCalledWith('/api/v1/caregiver/schedule', { enabled: true, serviceAreas: ['朝阳区'], days: [] })
  })
})
