import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNotificationStore } from '@/store/notification.js'

vi.mock('@/utils/request.js', () => ({ default: { get: vi.fn(), patch: vi.fn() } }))
import http from '@/utils/request.js'

describe('notification store', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.clearAllMocks() })
  it('loads role notifications and unread count', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ notificationId: 1, read: false }], total: 1, unreadCount: 1 } })
    const store = useNotificationStore(); await store.fetchNotifications({ unreadOnly: true })
    expect(http.get).toHaveBeenCalledWith('/api/v1/notifications', { unreadOnly: true })
    expect(store.unreadCount).toBe(1)
  })
  it('marks one notification as read', async () => {
    http.get.mockResolvedValueOnce({ data: { list: [{ notificationId: 1, read: false }], total: 1, unreadCount: 1 } })
    http.patch.mockResolvedValueOnce({ data: { notificationId: 1, read: true, readTime: 'now' } })
    const store = useNotificationStore(); await store.fetchNotifications(); await store.markAsRead(1)
    expect(http.patch).toHaveBeenCalledWith('/api/v1/notifications/1/read')
    expect(store.unreadCount).toBe(0)
  })
})
