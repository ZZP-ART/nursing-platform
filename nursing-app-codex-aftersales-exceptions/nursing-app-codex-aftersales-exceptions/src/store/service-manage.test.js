import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useServiceManageStore } from '@/store/service-manage.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
  resolveAssetUrl: vi.fn((url) => url || ''),
}))

import http from '@/utils/request.js'

const service = {
  itemId: 598,
  name: '居家用药指导',
  auditStatus: 'DRAFT',
  specs: [{ specId: 1, name: '单次服务', price: '100', originalPrice: '120', duration: '45' }],
}

describe('serviceManageStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('获取商户服务列表并规范化价格', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [service], summary: { total: 1, draft: 1 } } })
    const store = useServiceManageStore()

    await store.fetchServices()

    expect(http.get).toHaveBeenCalledWith('/api/v1/merchants/services', {})
    expect(store.services[0].specs[0].price).toBe(100)
    expect(store.summary.total).toBe(1)
  })

  it('创建服务使用幂等键', async () => {
    http.post.mockResolvedValueOnce({ data: service })
    const store = useServiceManageStore()
    const payload = { name: service.name, specs: service.specs }

    await store.createService(payload)

    expect(http.post).toHaveBeenCalledWith('/api/v1/merchants/services', payload, {
      idempotentKey: 'service-create-key',
    })
  })

  it('更新并提交审核使用对应接口', async () => {
    http.put.mockResolvedValueOnce({ data: service })
    http.post.mockResolvedValueOnce({ data: { ...service, auditStatus: 'PENDING_REVIEW' } })
    const store = useServiceManageStore()

    await store.updateService(598, service)
    await store.submitService(598)

    expect(http.put).toHaveBeenCalledWith('/api/v1/merchants/services/598', service)
    expect(http.post).toHaveBeenCalledWith('/api/v1/merchants/services/598/submit', null, {
      idempotentKey: 'service-submit-key',
    })
    expect(store.currentService.auditStatus).toBe('PENDING_REVIEW')
  })

  it('上架与下架使用独立动作接口', async () => {
    http.post
      .mockResolvedValueOnce({ data: { ...service, auditStatus: 'APPROVED', publishStatus: 'PUBLISHED' } })
      .mockResolvedValueOnce({ data: { ...service, auditStatus: 'APPROVED', publishStatus: 'OFFLINE' } })
    const store = useServiceManageStore()

    await store.publishService(598)
    await store.offlineService(598)

    expect(http.post).toHaveBeenNthCalledWith(1, '/api/v1/merchants/services/598/publish', null, {
      idempotentKey: 'service-publish-key',
    })
    expect(http.post).toHaveBeenNthCalledWith(2, '/api/v1/merchants/services/598/offline', null, {
      idempotentKey: 'service-offline-key',
    })
  })
})
