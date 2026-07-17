import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotentKey } from '@/utils/request.js'
import { MERCHANT_AUDIT_STATUS } from '@/constants/merchant-status.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useMerchantOnboardingStore = defineStore('merchantOnboarding', () => {
  const application = ref(null)
  const profile = ref(null)
  const loading = ref(false)

  const auditStatus = computed(() => application.value?.auditStatus || MERCHANT_AUDIT_STATUS.NOT_APPLIED)
  const canEditApplication = computed(() => [
    MERCHANT_AUDIT_STATUS.NOT_APPLIED,
    MERCHANT_AUDIT_STATUS.DRAFT,
    MERCHANT_AUDIT_STATUS.REJECTED,
  ].includes(auditStatus.value))

  async function fetchApplication() {
    if (!USE_MOCK_API) return null
    loading.value = true
    try {
      const res = await http.get('/api/v1/merchants/application')
      application.value = res.data || null
      return application.value
    } finally {
      loading.value = false
    }
  }

  async function saveApplication(payload) {
    if (!USE_MOCK_API) throw unavailableApi('商户入驻')
    const res = await http.put('/api/v1/merchants/application', payload)
    application.value = res.data
    return application.value
  }

  async function submitApplication(payload) {
    if (!USE_MOCK_API) throw unavailableApi('商户入驻')
    const res = await http.post('/api/v1/merchants/apply', payload, {
      idempotentKey: createIdempotentKey('merchant-apply'),
    })
    application.value = res.data
    return application.value
  }

  async function fetchProfile() {
    if (!USE_MOCK_API) return null
    const res = await http.get('/api/v1/merchant/profile')
    profile.value = res.data
    return profile.value
  }

  return {
    application,
    profile,
    loading,
    auditStatus,
    canEditApplication,
    fetchApplication,
    saveApplication,
    submitApplication,
    fetchProfile,
  }
})
