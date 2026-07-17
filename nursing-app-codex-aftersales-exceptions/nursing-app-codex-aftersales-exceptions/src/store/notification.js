import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '@/utils/request.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const total = ref(0)
  const loading = ref(false)
  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchNotifications(params = {}) {
    if (!USE_MOCK_API) {
      notifications.value = []
      total.value = 0
      unreadCount.value = 0
      return { list: [], total: 0, unreadCount: 0 }
    }
    loading.value = true
    try {
      const res = await http.get('/api/v1/notifications', {
        ...(params.category ? { category: params.category } : {}),
        ...(params.unreadOnly ? { unreadOnly: true } : {}),
      })
      notifications.value = res.data?.list || []
      total.value = res.data?.total || 0
      unreadCount.value = res.data?.unreadCount || 0
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    if (!USE_MOCK_API) {
      unreadCount.value = 0
      return 0
    }
    const res = await http.get('/api/v1/notifications/unread-count')
    unreadCount.value = res.data?.unreadCount || 0
    return unreadCount.value
  }

  async function markAsRead(notificationId) {
    if (!USE_MOCK_API) throw unavailableApi('消息中心')
    const res = await http.patch(`/api/v1/notifications/${notificationId}/read`)
    const item = notifications.value.find((entry) => String(entry.notificationId) === String(notificationId))
    if (item && !item.read) { item.read = true; item.readTime = res.data?.readTime; unreadCount.value = Math.max(0, unreadCount.value - 1) }
    return res.data
  }

  async function markAllAsRead() {
    if (!USE_MOCK_API) throw unavailableApi('消息中心')
    await http.patch('/api/v1/notifications/read-all')
    notifications.value.forEach((item) => { item.read = true })
    unreadCount.value = 0
  }

  return { notifications, unreadCount, total, loading, hasUnread, fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead }
})
