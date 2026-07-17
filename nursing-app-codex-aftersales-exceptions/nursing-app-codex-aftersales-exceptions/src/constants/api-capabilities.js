export const USE_MOCK_API = import.meta.env.MODE === 'test' || import.meta.env.VITE_USE_MOCK === 'true'

export const API_CAPABILITY = Object.freeze({
  CAREGIVER_DRAFT: 'caregiver-draft',
  CAREGIVER_PROFILE: 'caregiver-profile',
  CAREGIVER_SCHEDULE: 'caregiver-schedule',
  CAREGIVER_TASK_DETAIL: 'caregiver-task-detail',
  MERCHANT_ONBOARDING: 'merchant-onboarding',
  MERCHANT_DASHBOARD: 'merchant-dashboard',
  MERCHANT_ORDER_DETAIL: 'merchant-order-detail',
  MERCHANT_DISPATCH_CANDIDATES: 'merchant-dispatch-candidates',
  MERCHANT_SERVICE_MANAGEMENT: 'merchant-service-management',
  MERCHANT_TEAM: 'merchant-team',
  MERCHANT_FEEDBACK: 'merchant-feedback',
  NOTIFICATIONS: 'notifications',
  AFTERSALES_DETAIL: 'aftersales-detail',
  SETTLEMENTS: 'settlements',
  REPORTS: 'reports',
})

export function isApiCapabilityAvailable() {
  return USE_MOCK_API
}

export function unavailableApi(feature = '该功能') {
  const error = new Error(`${feature}：后端当前契约暂未提供此接口`)
  error.code = 'API_NOT_AVAILABLE'
  if (typeof uni !== 'undefined') {
    uni.showToast({ title: '后端暂未提供此接口', icon: 'none' })
  }
  return error
}
