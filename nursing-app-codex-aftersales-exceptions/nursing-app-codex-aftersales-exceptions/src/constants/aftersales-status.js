export const REFUND_STATUS = Object.freeze({
  NONE: 'NONE',
  NOT_REQUIRED: 'NOT_REQUIRED',
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  SUCCESS: 'SUCCESS',
  REJECTED: 'REJECTED',
})

export const EXCEPTION_TYPE = Object.freeze({
  ASSIGNMENT_TIMEOUT: 'ASSIGNMENT_TIMEOUT',
  NO_CAREGIVER: 'NO_CAREGIVER',
  ABNORMAL_CHECK_IN: 'ABNORMAL_CHECK_IN',
})

export const EXCEPTION_STATUS = Object.freeze({
  OPEN: 'OPEN',
  MERCHANT_PROCESSING: 'MERCHANT_PROCESSING',
  ADMIN_REVIEW: 'ADMIN_REVIEW',
  RESOLVED: 'RESOLVED',
  CLOSED: 'CLOSED',
})

export const REFUND_STATUS_META = Object.freeze({
  [REFUND_STATUS.NONE]: { label: '无退款', tone: 'muted' },
  [REFUND_STATUS.NOT_REQUIRED]: { label: '无需退款', tone: 'muted' },
  [REFUND_STATUS.PENDING]: { label: '待退款', tone: 'warning' },
  [REFUND_STATUS.PROCESSING]: { label: '退款处理中', tone: 'primary' },
  [REFUND_STATUS.SUCCESS]: { label: '退款成功', tone: 'success' },
  [REFUND_STATUS.REJECTED]: { label: '退款驳回', tone: 'danger' },
})

export const EXCEPTION_TYPE_META = Object.freeze({
  [EXCEPTION_TYPE.ASSIGNMENT_TIMEOUT]: { label: '接单超时', description: '护理人员未在规定时间内响应派单' },
  [EXCEPTION_TYPE.NO_CAREGIVER]: { label: '无人可派', description: '当前没有满足条件且可接单的护理人员' },
  [EXCEPTION_TYPE.ABNORMAL_CHECK_IN]: { label: '异常签到', description: '定位或到达信息需要人工核验' },
})

export function getRefundStatusMeta(status) {
  return REFUND_STATUS_META[status] || { label: status || '未知', tone: 'muted' }
}

export function getExceptionTypeMeta(type) {
  return EXCEPTION_TYPE_META[type] || { label: type || '其他异常', description: '需要人工处理' }
}
