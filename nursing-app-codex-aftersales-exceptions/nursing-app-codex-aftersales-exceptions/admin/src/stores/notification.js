import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http from '../api/request.js'

const useMock = import.meta.env.MODE === 'test' || import.meta.env.VITE_USE_MOCK === 'true'

export const useNotificationStore = defineStore('adminNotification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchNotifications(params = {}) {
    if (!useMock) { notifications.value = []; unreadCount.value = 0; return { list: [], unreadCount: 0 } }
    loading.value = true
    try {
      const res = await http.get('/api/v1/admin/notifications', { params })
      notifications.value = res.data.list || []
      unreadCount.value = res.data.unreadCount || 0
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    if (!useMock) { unreadCount.value = 0; return 0 }
    const res = await http.get('/api/v1/admin/notifications/unread-count')
    unreadCount.value = res.data.unreadCount || 0
    return unreadCount.value
  }

  async function markAsRead(id) {
    if (!useMock) throw new Error('后端当前契约暂未提供平台消息接口')
    const res = await http.patch(`/api/v1/admin/notifications/${id}/read`)
    const item = notifications.value.find((entry) => String(entry.notificationId) === String(id))
    if (item && !item.read) { item.read = true; unreadCount.value = Math.max(0, unreadCount.value - 1) }
    return res.data
  }

  async function markAllAsRead() {
    if (!useMock) throw new Error('后端当前契约暂未提供平台消息接口')
    await http.patch('/api/v1/admin/notifications/read-all')
    notifications.value.forEach((item) => { item.read = true })
    unreadCount.value = 0
  }

  return { notifications, unreadCount, loading, hasUnread, fetchNotifications, fetchUnreadCount, markAsRead, markAllAsRead }
})
