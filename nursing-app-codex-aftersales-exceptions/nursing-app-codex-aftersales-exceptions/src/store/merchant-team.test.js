import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useMerchantTeamStore } from '@/store/merchant-team.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
}))
import http from '@/utils/request.js'

describe('merchantTeamStore', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })

  it('loads caregiver team and summary', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ relationId: 1 }], summary: { active: 1 } } })
    const store = useMerchantTeamStore()
    await store.fetchCaregivers({ status: 'ACTIVE' })
    expect(http.get).toHaveBeenCalledWith('/api/v1/merchant/caregivers', { status: 'ACTIVE' })
    expect(store.summary.active).toBe(1)
  })

  it('invites caregiver and updates relationship status with idempotency keys', async () => {
    http.post.mockResolvedValueOnce({ data: { relationId: 2, status: 'INVITED' } }).mockResolvedValueOnce({ data: { relationId: 2, status: 'SUSPENDED' } })
    const store = useMerchantTeamStore()
    await store.inviteCaregiver({ phone: '13800138001' })
    await store.updateCaregiverStatus(2, 'suspend', '暂时停止合作')
    expect(http.post).toHaveBeenNthCalledWith(1, '/api/v1/merchant/caregivers/invite', { phone: '13800138001' }, { idempotentKey: 'invite-caregiver-key' })
    expect(http.post).toHaveBeenNthCalledWith(2, '/api/v1/merchant/caregivers/2/suspend', { reason: '暂时停止合作' }, { idempotentKey: 'caregiver-suspend-key' })
  })

  it('invites an internal merchant member with a position', async () => {
    http.post.mockResolvedValueOnce({ data: { memberId: 5, position: 'DISPATCHER' } })
    const store = useMerchantTeamStore()
    await store.inviteMember({ phone: '13900139000', position: 'DISPATCHER' })
    expect(store.members[0].position).toBe('DISPATCHER')
  })
})
