export const REVIEW_TYPES = Object.freeze({
  MERCHANT: 'merchant',
  CAREGIVER: 'caregiver',
  SERVICE: 'service',
})

export const REVIEW_TYPE_META = Object.freeze({
  [REVIEW_TYPES.MERCHANT]: { label: '商户入驻', subjectLabel: '商户名称', color: '#3156d3' },
  [REVIEW_TYPES.CAREGIVER]: { label: '护理人员认证', subjectLabel: '申请人', color: '#00a89d' },
  [REVIEW_TYPES.SERVICE]: { label: '服务审核', subjectLabel: '服务名称', color: '#e7833b' },
})

export const REVIEW_STATUS = Object.freeze({
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
})

export const REVIEW_STATUS_META = Object.freeze({
  [REVIEW_STATUS.PENDING_REVIEW]: { label: '待审核', type: 'warning' },
  [REVIEW_STATUS.APPROVED]: { label: '已通过', type: 'success' },
  [REVIEW_STATUS.REJECTED]: { label: '已驳回', type: 'danger' },
})

export function getReviewTypeMeta(type) {
  return REVIEW_TYPE_META[type] || { label: '审核任务', subjectLabel: '主体', color: '#3156d3' }
}

export function getReviewStatusMeta(status) {
  return REVIEW_STATUS_META[status] || { label: status || '未知', type: 'info' }
}
