import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import http, { createIdempotentKey } from '@/utils/request.js'
import { CAREGIVER_AUDIT_STATUS } from '@/constants/caregiver-status.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

const STATUS_MAP = {
  0: CAREGIVER_AUDIT_STATUS.PENDING_REVIEW,
  1: CAREGIVER_AUDIT_STATUS.APPROVED,
  2: CAREGIVER_AUDIT_STATUS.REJECTED,
}

function normalizeApplication(data) {
  if (!data) return null
  return {
    ...data,
    applicationId: data.applicationId ?? data.id,
    auditStatus: data.auditStatus || STATUS_MAP[data.status] || CAREGIVER_AUDIT_STATUS.NOT_APPLIED,
    rejectReason: data.rejectReason ?? data.reviewRemark,
    serviceArea: data.serviceArea ?? data.serviceDistrict,
    skills: Array.isArray(data.skills)
      ? data.skills
      : String(data.skills || '').split(/[、,，]/).filter(Boolean),
  }
}

export const useCaregiverStore = defineStore('caregiver', () => {
  const application = ref(null)
  const profile = ref(null)
  const schedule = ref(null)
  const loading = ref(false)

  const auditStatus = computed(() => application.value?.auditStatus || CAREGIVER_AUDIT_STATUS.NOT_APPLIED)
  const canEditApplication = computed(() => [
    CAREGIVER_AUDIT_STATUS.NOT_APPLIED,
    CAREGIVER_AUDIT_STATUS.DRAFT,
    CAREGIVER_AUDIT_STATUS.REJECTED,
  ].includes(auditStatus.value))

  async function fetchApplication() {
    loading.value = true
    try {
      const path = USE_MOCK_API
        ? '/api/v1/caregivers/application'
        : '/api/v1/caregiver/applications/me'
      const res = await http.get(path)
      application.value = normalizeApplication(res.data)
      return application.value
    } finally {
      loading.value = false
    }
  }

  async function saveApplication(payload) {
    if (!USE_MOCK_API) throw unavailableApi('护理人员申请草稿')
    const res = await http.put('/api/v1/caregivers/application', payload)
    application.value = res.data
    return application.value
  }

  async function submitApplication(payload) {
    const path = USE_MOCK_API ? '/api/v1/caregivers/apply' : '/api/v1/caregiver/applications'
    const data = USE_MOCK_API ? payload : {
      realName: payload.realName,
      phone: payload.phone,
      serviceDistrict: payload.serviceDistrict || payload.serviceArea,
      skills: Array.isArray(payload.skills) ? payload.skills.join('、') : payload.skills,
    }
    const res = await http.post(path, data, {
      idempotentKey: createIdempotentKey('caregiver-apply'),
    })
    application.value = normalizeApplication(res.data)
    return application.value
  }

  async function fetchProfile() {
    if (!USE_MOCK_API) {
      profile.value = application.value || await fetchApplication()
      return profile.value
    }
    const res = await http.get('/api/v1/caregiver/profile')
    profile.value = res.data
    return profile.value
  }

  async function fetchSchedule() {
    if (!USE_MOCK_API) {
      schedule.value = { enabled: false, maxDailyOrders: 0, serviceAreas: [], days: [] }
      return schedule.value
    }
    loading.value = true
    try {
      const res = await http.get('/api/v1/caregiver/schedule')
      schedule.value = res.data
      return schedule.value
    } finally {
      loading.value = false
    }
  }

  async function saveSchedule(payload) {
    if (!USE_MOCK_API) throw unavailableApi('护理人员排班')
    const res = await http.put('/api/v1/caregiver/schedule', payload)
    schedule.value = res.data
    return schedule.value
  }

  return {
    application,
    profile,
    schedule,
    loading,
    auditStatus,
    canEditApplication,
    fetchApplication,
    saveApplication,
    submitApplication,
    fetchProfile,
    fetchSchedule,
    saveSchedule,
  }
})
