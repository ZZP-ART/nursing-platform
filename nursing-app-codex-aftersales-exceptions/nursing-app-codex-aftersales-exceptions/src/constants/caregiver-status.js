export const CAREGIVER_AUDIT_STATUS = Object.freeze({
  NOT_APPLIED: 'NOT_APPLIED',
  DRAFT: 'DRAFT',
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
})

export const CAREGIVER_AUDIT_META = Object.freeze({
  [CAREGIVER_AUDIT_STATUS.NOT_APPLIED]: { label: '未申请', tone: 'muted', description: '完善身份和资质信息后即可提交认证' },
  [CAREGIVER_AUDIT_STATUS.DRAFT]: { label: '草稿', tone: 'warning', description: '资料尚未提交平台审核' },
  [CAREGIVER_AUDIT_STATUS.PENDING_REVIEW]: { label: '审核中', tone: 'primary', description: '平台正在核验身份与资质信息' },
  [CAREGIVER_AUDIT_STATUS.APPROVED]: { label: '已认证', tone: 'success', description: '护理人员身份已开通' },
  [CAREGIVER_AUDIT_STATUS.REJECTED]: { label: '已驳回', tone: 'danger', description: '请按审核意见修改后重新提交' },
})

export const CAREGIVER_SKILL_OPTIONS = Object.freeze([
  '生活照护',
  '陪诊陪护',
  '康复护理',
  '上门换药',
  '用药指导',
  '术后护理',
])

export function getCaregiverAuditMeta(status) {
  return CAREGIVER_AUDIT_META[status] || CAREGIVER_AUDIT_META[CAREGIVER_AUDIT_STATUS.NOT_APPLIED]
}

