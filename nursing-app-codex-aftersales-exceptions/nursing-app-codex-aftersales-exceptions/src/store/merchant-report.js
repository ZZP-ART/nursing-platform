import { ref } from 'vue'
import { defineStore } from 'pinia'
import http from '@/utils/request.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useMerchantReportStore = defineStore('merchant-report', () => {
  const report = ref(null)
  const range = ref('30d')
  const loading = ref(false)

  async function fetchReport(nextRange = range.value) {
    if (!USE_MOCK_API) throw unavailableApi('经营报表')
    loading.value = true
    range.value = nextRange
    try {
      const res = await http.get('/api/v1/merchant/reports/operations', { range: nextRange })
      report.value = res.data
      return report.value
    } finally {
      loading.value = false
    }
  }

  return { report, range, loading, fetchReport }
})
