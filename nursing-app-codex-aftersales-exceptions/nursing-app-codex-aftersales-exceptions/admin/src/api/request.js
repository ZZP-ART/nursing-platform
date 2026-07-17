import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? '/backend' : '/api'),
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function createIdempotencyKey(prefix = 'admin') {
  const uuid = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `${prefix}-${uuid}`.slice(0, 128)
}

http.interceptors.response.use((response) => {
  const payload = response.data
  if (payload?.code === 0) return payload
  ElMessage.error(payload?.message || '请求失败')
  return Promise.reject(payload)
}, (error) => {
  if (error.response?.status === 401) {
    localStorage.removeItem('adminToken')
    window.location.href = '/login'
  }
  ElMessage.error(error.response?.data?.message || error.message || '网络连接失败')
  return Promise.reject(error)
})

export default http
