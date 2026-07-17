import { ref } from 'vue'
import { defineStore } from 'pinia'
import http, { createIdempotentKey } from '@/utils/request.js'
import { USE_MOCK_API, unavailableApi } from '@/constants/api-capabilities.js'

export const useMerchantTeamStore = defineStore('merchantTeam', () => {
  const caregivers = ref([])
  const currentCaregiver = ref(null)
  const members = ref([])
  const summary = ref(null)
  const loading = ref(false)

  async function fetchCaregivers(params = {}) {
    if (!USE_MOCK_API) { caregivers.value = []; return { list: [], summary: null } }
    loading.value = true
    try {
      const res = await http.get('/api/v1/merchant/caregivers', params)
      caregivers.value = res.data.list || []
      summary.value = res.data.summary || null
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function fetchCaregiverDetail(relationId) {
    if (!USE_MOCK_API) throw unavailableApi('商户团队')
    const res = await http.get(`/api/v1/merchant/caregivers/${relationId}`)
    currentCaregiver.value = res.data
    return currentCaregiver.value
  }

  async function inviteCaregiver(payload) {
    if (!USE_MOCK_API) throw unavailableApi('商户团队')
    const res = await http.post('/api/v1/merchant/caregivers/invite', payload, {
      idempotentKey: createIdempotentKey('invite-caregiver'),
    })
    return res.data
  }

  async function updateCaregiverStatus(relationId, action, reason = '') {
    if (!USE_MOCK_API) throw unavailableApi('商户团队')
    const res = await http.post(`/api/v1/merchant/caregivers/${relationId}/${action}`, { reason }, {
      idempotentKey: createIdempotentKey(`caregiver-${action}`),
    })
    currentCaregiver.value = res.data
    return res.data
  }

  async function fetchMembers() {
    if (!USE_MOCK_API) { members.value = []; return [] }
    const res = await http.get('/api/v1/merchant/members')
    members.value = res.data.list || []
    return members.value
  }

  async function inviteMember(payload) {
    if (!USE_MOCK_API) throw unavailableApi('商户成员')
    const res = await http.post('/api/v1/merchant/members/invite', payload, {
      idempotentKey: createIdempotentKey('invite-member'),
    })
    members.value.unshift(res.data)
    return res.data
  }

  async function updateMemberStatus(memberId, action) {
    if (!USE_MOCK_API) throw unavailableApi('商户成员')
    const res = await http.post(`/api/v1/merchant/members/${memberId}/${action}`, null, {
      idempotentKey: createIdempotentKey(`member-${action}`),
    })
    const index = members.value.findIndex((item) => item.memberId === memberId)
    if (index >= 0) members.value[index] = res.data
    return res.data
  }

  return { caregivers, currentCaregiver, members, summary, loading, fetchCaregivers, fetchCaregiverDetail, inviteCaregiver, updateCaregiverStatus, fetchMembers, inviteMember, updateMemberStatus }
})
