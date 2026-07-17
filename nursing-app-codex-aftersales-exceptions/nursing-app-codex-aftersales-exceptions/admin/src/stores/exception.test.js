import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useExceptionStore } from './exception.js'

vi.mock('../api/request.js', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
import http from '../api/request.js'

describe('admin exception store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })
  it('loads exception cases', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ exceptionId: 1 }], total: 1 } })
    const store = useExceptionStore()
    await store.fetchExceptions({ status: 'ADMIN_REVIEW' })
    expect(http.get).toHaveBeenCalledWith('/api/v1/admin/exceptions', { params: { status: 'ADMIN_REVIEW' } })
  })
  it('processes exception with audit endpoint', async () => {
    http.post.mockResolvedValueOnce({ data: { exceptionId: 1, status: 'RESOLVED' } })
    const store = useExceptionStore()
    await store.processException(1, 'approve-refund', '同意退款')
    expect(http.post).toHaveBeenCalledWith('/api/v1/admin/exceptions/1/approve-refund', { remark: '同意退款' }, expect.objectContaining({ headers: expect.any(Object) }))
  })
})
