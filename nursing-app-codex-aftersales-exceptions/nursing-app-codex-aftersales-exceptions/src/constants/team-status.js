export const COOPERATION_STATUS = Object.freeze({
  INVITED: 'INVITED',
  ACTIVE: 'ACTIVE',
  SUSPENDED: 'SUSPENDED',
  LEFT: 'LEFT',
})

export const COOPERATION_STATUS_META = Object.freeze({
  [COOPERATION_STATUS.INVITED]: { label: '待确认', tone: 'warning' },
  [COOPERATION_STATUS.ACTIVE]: { label: '合作中', tone: 'success' },
  [COOPERATION_STATUS.SUSPENDED]: { label: '已停用', tone: 'danger' },
  [COOPERATION_STATUS.LEFT]: { label: '已离开', tone: 'muted' },
})

export const MEMBER_STATUS = Object.freeze({ ACTIVE: 'ACTIVE', DISABLED: 'DISABLED' })

export const MERCHANT_POSITION_OPTIONS = Object.freeze([
  { value: 'OWNER', label: '负责人', description: '全部商户权限、成员管理和结算配置' },
  { value: 'OPERATOR', label: '运营', description: '服务管理、订单查看和运营配置' },
  { value: 'DISPATCHER', label: '调度员', description: '候选人员查询、派单和改派' },
  { value: 'FINANCE', label: '财务', description: '账单、退款和结算数据' },
])

export function getCooperationStatusMeta(status) {
  return COOPERATION_STATUS_META[status] || { label: status || '未知', tone: 'muted' }
}

export function getPositionLabel(position) {
  return MERCHANT_POSITION_OPTIONS.find((item) => item.value === position)?.label || position || '未设置'
}
