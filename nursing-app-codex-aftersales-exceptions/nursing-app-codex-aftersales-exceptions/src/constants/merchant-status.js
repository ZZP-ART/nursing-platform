export const MERCHANT_AUDIT_STATUS = Object.freeze({
  NOT_APPLIED: 'NOT_APPLIED',
  DRAFT: 'DRAFT',
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
  DISABLED: 'DISABLED',
})

export const MERCHANT_AUDIT_META = Object.freeze({
  [MERCHANT_AUDIT_STATUS.NOT_APPLIED]: { label: '未入驻', tone: 'muted', description: '提交企业主体和经营资质后即可申请入驻' },
  [MERCHANT_AUDIT_STATUS.DRAFT]: { label: '草稿', tone: 'warning', description: '入驻资料尚未提交平台审核' },
  [MERCHANT_AUDIT_STATUS.PENDING_REVIEW]: { label: '审核中', tone: 'primary', description: '平台正在核验企业主体和经营资质' },
  [MERCHANT_AUDIT_STATUS.APPROVED]: { label: '已认证', tone: 'success', description: '商户组织和负责人关系已开通' },
  [MERCHANT_AUDIT_STATUS.REJECTED]: { label: '已驳回', tone: 'danger', description: '请按审核意见修改资料后重新提交' },
  [MERCHANT_AUDIT_STATUS.DISABLED]: { label: '已停用', tone: 'muted', description: '商户当前不可开展平台业务' },
})

export const MERCHANT_TYPE_OPTIONS = Object.freeze([
  { label: '护理服务机构', value: 'NURSING_ORG' },
  { label: '医疗机构', value: 'MEDICAL_ORG' },
  { label: '养老服务机构', value: 'ELDERLY_CARE_ORG' },
  { label: '个体经营者', value: 'SOLE_PROPRIETOR' },
])

export function getMerchantAuditMeta(status) {
  return MERCHANT_AUDIT_META[status] || MERCHANT_AUDIT_META[MERCHANT_AUDIT_STATUS.NOT_APPLIED]
}
