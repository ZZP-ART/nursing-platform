import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useWorkOrderStore } from '@/store/work-order.js'

vi.mock('@/utils/request.js', () => ({
  default: { get: vi.fn(), post: vi.fn() },
  createIdempotentKey: vi.fn((prefix) => `${prefix}-key`),
}))

import http from '@/utils/request.js'

describe('workOrderStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('护理人员接单和结束服务使用专用动作接口', async () => {
    http.post.mockResolvedValue({ data: { success: true } })
    const store = useWorkOrderStore()

    await store.acceptAssignment(60001)
    await store.finishService(19999, { summary: '服务过程正常' })

    expect(http.post).toHaveBeenNthCalledWith(1, '/api/v1/caregivers/assignments/60001/accept', null, {
      idempotentKey: 'accept-key',
    })
    expect(http.post).toHaveBeenNthCalledWith(2, '/api/v1/caregivers/orders/19999/finish', {
      summary: '服务过程正常',
    }, {
      idempotentKey: 'finish-key',
    })
  })

  it('任务列表和详情统一补齐三维状态字段', async () => {
    http.get
      .mockResolvedValueOnce({ data: { list: [{ orderId: 1, status: 1 }] } })
      .mockResolvedValueOnce({ data: { orderId: 1, status: 2 } })
    const store = useWorkOrderStore()

    await store.fetchTasks()
    await store.fetchTaskDetail(1)

    expect(store.tasks[0].orderStatus).toBe('WAITING_DISPATCH')
    expect(store.currentTask.orderStatus).toBe('COMPLETED')
    expect(store.currentTask.serviceRecords).toEqual([])
  })

  it('详情页动作成功后重新拉取当前任务', async () => {
    http.post.mockResolvedValueOnce({ data: { success: true } })
    http.get.mockResolvedValueOnce({ data: { orderId: 19995, status: 1, serviceRecords: [] } })
    const store = useWorkOrderStore()
    store.currentTask = { orderId: 19995, currentAssignment: { assignmentId: 60005 } }

    await store.startService(19995)

    expect(http.get).toHaveBeenCalledWith('/api/v1/caregivers/tasks/19995')
  })
})
