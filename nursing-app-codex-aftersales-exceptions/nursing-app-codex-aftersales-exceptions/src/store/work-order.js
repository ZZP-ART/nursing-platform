import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { createIdempotentKey } from '@/utils/request.js'
import { normalizeOrderState } from '@/constants/order-status.js'
import { ASSIGNMENT_STATUS, ORDER_STATUS } from '@/constants/order-status.js'
import { USE_MOCK_API } from '@/constants/api-capabilities.js'

function normalizeAssignmentTask(assignment = {}) {
  const assignmentStatus = {
    0: ASSIGNMENT_STATUS.WAITING_ACCEPT,
    1: ASSIGNMENT_STATUS.ACCEPTED,
    2: ASSIGNMENT_STATUS.REJECTED,
  }[assignment.status] || assignment.assignmentStatus
  return normalizeOrderState({
    ...assignment,
    orderStatus: assignment.orderStatus || (
      assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT
        ? ORDER_STATUS.WAITING_ACCEPT
        : ORDER_STATUS.WAITING_SERVICE
    ),
    assignmentStatus,
    currentAssignment: assignment.currentAssignment || assignment,
  })
}

export const useWorkOrderStore = defineStore('workOrder', () => {
  const tasks = ref([])
  const currentTask = ref(null)
  const loading = ref(false)

  async function fetchTasks(params = {}) {
    loading.value = true
    try {
      const res = await http.get('/api/v1/caregivers/tasks', USE_MOCK_API ? params : undefined)
      const list = Array.isArray(res.data) ? res.data : res.data?.list || []
      tasks.value = list.map(USE_MOCK_API ? normalizeOrderState : normalizeAssignmentTask)
      return tasks.value
    } finally {
      loading.value = false
    }
  }

  async function fetchTaskDetail(orderId) {
    if (!USE_MOCK_API) {
      currentTask.value = tasks.value.find((item) => String(item.orderId) === String(orderId)) || null
      if (!currentTask.value) {
        await fetchTasks()
        currentTask.value = tasks.value.find((item) => String(item.orderId) === String(orderId)) || null
      }
      return currentTask.value
    }
    const res = await http.get(`/api/v1/caregivers/tasks/${orderId}`)
    currentTask.value = normalizeOrderState(res.data)
    return currentTask.value
  }

  async function acceptAssignment(assignmentId) {
    const res = await http.post(`/api/v1/caregivers/assignments/${assignmentId}/accept`, null, {
      idempotentKey: createIdempotentKey('accept'),
    })
    if (currentTask.value?.currentAssignment?.assignmentId === assignmentId) {
      await fetchTaskDetail(currentTask.value.orderId)
    }
    return res.data
  }

  async function rejectAssignment(assignmentId, reason) {
    const res = await http.post(`/api/v1/caregivers/assignments/${assignmentId}/reject`, {
      [USE_MOCK_API ? 'reason' : 'remark']: reason,
    }, {
      idempotentKey: createIdempotentKey('reject'),
    })
    if (currentTask.value?.currentAssignment?.assignmentId === assignmentId) {
      await fetchTaskDetail(currentTask.value.orderId)
    }
    return res.data
  }

  async function updateServiceStatus(orderId, action, payload = {}) {
    const data = USE_MOCK_API ? payload : { remark: payload?.remark || payload?.summary || '' }
    const res = await http.post(`/api/v1/caregivers/orders/${orderId}/${action}`, data, {
      idempotentKey: createIdempotentKey(action),
    })
    if (currentTask.value?.orderId === orderId) {
      await fetchTaskDetail(orderId)
    }
    return res.data
  }

  const depart = (orderId, payload) => updateServiceStatus(orderId, 'depart', payload)
  const checkIn = (orderId, payload) => updateServiceStatus(orderId, 'check-in', payload)
  const startService = (orderId, payload) => updateServiceStatus(orderId, 'start', payload)
  const finishService = (orderId, payload) => updateServiceStatus(orderId, 'finish', payload)

  return {
    tasks,
    currentTask,
    loading,
    fetchTasks,
    fetchTaskDetail,
    acceptAssignment,
    rejectAssignment,
    depart,
    checkIn,
    startService,
    finishService,
  }
})
