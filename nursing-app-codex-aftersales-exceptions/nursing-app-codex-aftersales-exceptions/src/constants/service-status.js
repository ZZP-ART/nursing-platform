export const SERVICE_AUDIT_STATUS = Object.freeze({
  DRAFT: 'DRAFT',
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
})

export const SERVICE_PUBLISH_STATUS = Object.freeze({
  OFFLINE: 'OFFLINE',
  PUBLISHED: 'PUBLISHED',
})

export const SERVICE_AUDIT_META = Object.freeze({
  [SERVICE_AUDIT_STATUS.DRAFT]: { text: '草稿', tone: 'neutral' },
  [SERVICE_AUDIT_STATUS.PENDING_REVIEW]: { text: '审核中', tone: 'primary' },
  [SERVICE_AUDIT_STATUS.APPROVED]: { text: '审核通过', tone: 'success' },
  [SERVICE_AUDIT_STATUS.REJECTED]: { text: '已驳回', tone: 'warning' },
})

export function getServiceAuditMeta(status) {
  return SERVICE_AUDIT_META[status] || { text: '未知', tone: 'neutral' }
}

export function canEditService(service) {
  return [SERVICE_AUDIT_STATUS.DRAFT, SERVICE_AUDIT_STATUS.REJECTED].includes(service?.auditStatus)
}

export function canSubmitService(service) {
  return canEditService(service) && service?.specs?.length > 0
}

export function canPublishService(service) {
  return service?.auditStatus === SERVICE_AUDIT_STATUS.APPROVED &&
    service?.publishStatus !== SERVICE_PUBLISH_STATUS.PUBLISHED
}

export function canOfflineService(service) {
  return service?.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED
}
