import { ref } from 'vue'
import { defineStore } from 'pinia'
import http from '../api/request.js'

const useMock = import.meta.env.MODE === 'test' || import.meta.env.VITE_USE_MOCK === 'true'

export const useExceptionStore = defineStore('adminException', () => {
  const exceptions = ref([])
  const currentException = ref(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchExceptions(params = {}) {
    if (!useMock) { exceptions.value = []; total.value = 0; return { list: [], total: 0 } }
    loading.value = true
    try {
      const res = await http.get('/api/v1/admin/exceptions', { params })
      exceptions.value = res.data.list
      total.value = res.data.total
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchExceptionDetail(id) {
    if (!useMock) return null
    const res = await http.get(`/api/v1/admin/exceptions/${id}`)
    currentException.value = res.data
    return currentException.value
  }

  async function processException(id, action, remark) {
    if (!useMock) throw new Error('后端当前契约暂未提供异常监管接口')
    const res = await http.post(`/api/v1/admin/exceptions/${id}/${action}`, { remark }, {
      headers: { 'Idempotency-Key': `admin-exception-${id}-${action}-${Date.now()}` },
    })
    currentException.value = res.data
    return currentException.value
  }

  return { exceptions, currentException, total, loading, fetchExceptions, fetchExceptionDetail, processException }
})
