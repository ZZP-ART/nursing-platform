import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNotificationStore } from './notification.js'

vi.mock('../api/request.js', () => ({ default: { get: vi.fn(), patch: vi.fn() } }))
import http from '../api/request.js'

describe('admin notification store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })
  it('loads platform notifications', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ notificationId: 1 }], unreadCount: 1 } })
    const store = useNotificationStore(); await store.fetchNotifications({ unreadOnly: true })
    expect(http.get).toHaveBeenCalledWith('/api/v1/admin/notifications', { params: { unreadOnly: true } })
  })
  it('marks all platform notifications read', async () => {
    http.patch.mockResolvedValueOnce({ data: { unreadCount: 0 } })
    const store = useNotificationStore(); await store.markAllAsRead()
    expect(http.patch).toHaveBeenCalledWith('/api/v1/admin/notifications/read-all')
  })
})
