import { ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotentKey } from '@/utils/request.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useSettlementStore = defineStore('settlement', () => {
  const caregiverEarnings = ref(null)
  const merchantSettlements = ref(null)
  const loading = ref(false)

  async function fetchCaregiverEarnings() {
    if (!USE_MOCK_API) throw unavailableApi('护理人员收益')
    loading.value = true
    try { const res = await http.get('/api/v1/caregiver/earnings'); caregiverEarnings.value = res.data; return res.data } finally { loading.value = false }
  }

  async function applyCaregiverWithdrawal(payload) {
    if (!USE_MOCK_API) throw unavailableApi('护理人员提现')
    const res = await http.post('/api/v1/caregiver/withdrawals', payload, { idempotentKey: createIdempotentKey('caregiver-withdraw') })
    await fetchCaregiverEarnings()
    return res.data
  }

  async function fetchMerchantSettlements() {
    if (!USE_MOCK_API) throw unavailableApi('商户结算')
    loading.value = true
    try { const res = await http.get('/api/v1/merchant/settlements'); merchantSettlements.value = res.data; return res.data } finally { loading.value = false }
  }

  async function applyMerchantWithdrawal(payload) {
    if (!USE_MOCK_API) throw unavailableApi('商户结算')
    const res = await http.post('/api/v1/merchant/withdrawals', payload, { idempotentKey: createIdempotentKey('merchant-withdraw') })
    await fetchMerchantSettlements()
    return res.data
  }

  return { caregiverEarnings, merchantSettlements, loading, fetchCaregiverEarnings, applyCaregiverWithdrawal, fetchMerchantSettlements, applyMerchantWithdrawal }
})
