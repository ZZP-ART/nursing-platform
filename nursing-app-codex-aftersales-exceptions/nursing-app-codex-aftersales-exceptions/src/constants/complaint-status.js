export const COMPLAINT_STATUS = Object.freeze({
  SUBMITTED: 0,
  MERCHANT_PROCESSING: 1,
  RESOLVED: 2,
  CLOSED: 3,
  ADMIN_REVIEW: 4,
})

export const COMPLAINT_DECISION = Object.freeze({
  FULL_REFUND: 'FULL_REFUND',
  PARTIAL_REFUND: 'PARTIAL_REFUND',
  NO_REFUND: 'NO_REFUND',
})

export const COMPLAINT_STATUS_META = Object.freeze({
  [COMPLAINT_STATUS.SUBMITTED]: { label: '待商户响应', tone: 'warning' },
  [COMPLAINT_STATUS.MERCHANT_PROCESSING]: { label: '商户处理中', tone: 'primary' },
  [COMPLAINT_STATUS.ADMIN_REVIEW]: { label: '平台仲裁中', tone: 'danger' },
  [COMPLAINT_STATUS.RESOLVED]: { label: '已裁决', tone: 'success' },
  [COMPLAINT_STATUS.CLOSED]: { label: '已关闭', tone: 'muted' },
})

export function getComplaintStatusMeta(status) {
  return COMPLAINT_STATUS_META[status] || { label: '未知', tone: 'muted' }
}
