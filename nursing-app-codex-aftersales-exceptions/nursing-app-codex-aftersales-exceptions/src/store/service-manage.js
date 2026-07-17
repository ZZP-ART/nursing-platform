import { defineStore } from 'pinia'
import { ref } from 'vue'
import http, { createIdempotentKey, resolveAssetUrl } from '@/utils/request.js'

function normalizeManagedService(item = {}) {
  const specs = (item.specs || []).map((spec) => ({
    ...spec,
    specId: spec.specId ?? spec.id,
    price: Number(spec.price || 0),
    originalPrice: Number(spec.originalPrice || 0),
    duration: Number(spec.duration || 0),
  }))
  return {
    ...item,
    itemId: item.itemId ?? item.id,
    coverImage: resolveAssetUrl(item.coverImage),
    images: (item.images || []).map(resolveAssetUrl),
    specs,
    minPrice: item.minPrice ?? (specs.length ? Math.min(...specs.map((spec) => spec.price)) : null),
    auditRecords: item.auditRecords || [],
  }
}

export const useServiceManageStore = defineStore('serviceManage', () => {
  const services = ref([])
  const currentService = ref(null)
  const summary = ref({ total: 0, draft: 0, pending: 0, approved: 0, rejected: 0, published: 0 })
  const loading = ref(false)

  async function fetchServices(params = {}) {
    loading.value = true
    try {
      const res = await http.get('/api/v1/merchants/services', params)
      services.value = (res.data?.list || []).map(normalizeManagedService)
      summary.value = res.data?.summary || summary.value
      return services.value
    } finally {
      loading.value = false
    }
  }

  async function fetchServiceDetail(itemId) {
    loading.value = true
    try {
      const res = await http.get(`/api/v1/merchants/services/${itemId}`)
      currentService.value = normalizeManagedService(res.data)
      return currentService.value
    } finally {
      loading.value = false
    }
  }

  async function createService(data) {
    const res = await http.post('/api/v1/merchants/services', data, {
      idempotentKey: createIdempotentKey('service-create'),
    })
    return normalizeManagedService(res.data)
  }

  async function updateService(itemId, data) {
    const res = await http.put(`/api/v1/merchants/services/${itemId}`, data)
    currentService.value = normalizeManagedService(res.data)
    return currentService.value
  }

  async function submitService(itemId) {
    const res = await http.post(`/api/v1/merchants/services/${itemId}/submit`, null, {
      idempotentKey: createIdempotentKey('service-submit'),
    })
    currentService.value = normalizeManagedService(res.data)
    return currentService.value
  }

  async function publishService(itemId) {
    const res = await http.post(`/api/v1/merchants/services/${itemId}/publish`, null, {
      idempotentKey: createIdempotentKey('service-publish'),
    })
    return normalizeManagedService(res.data)
  }

  async function offlineService(itemId) {
    const res = await http.post(`/api/v1/merchants/services/${itemId}/offline`, null, {
      idempotentKey: createIdempotentKey('service-offline'),
    })
    return normalizeManagedService(res.data)
  }

  return {
    services,
    currentService,
    summary,
    loading,
    fetchServices,
    fetchServiceDetail,
    createService,
    updateService,
    submitService,
    publishService,
    offlineService,
  }
})
