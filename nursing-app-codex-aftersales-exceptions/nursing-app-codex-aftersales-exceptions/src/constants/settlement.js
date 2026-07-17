export const SETTLEMENT_STATUS = Object.freeze({
  FROZEN: 'FROZEN',
  AVAILABLE: 'AVAILABLE',
  SETTLING: 'SETTLING',
  SETTLED: 'SETTLED',
})

export const WITHDRAW_STATUS = Object.freeze({
  PROCESSING: 'PROCESSING',
  SUCCESS: 'SUCCESS',
  REJECTED: 'REJECTED',
})

export const SETTLEMENT_STATUS_META = Object.freeze({
  [SETTLEMENT_STATUS.FROZEN]: { label: '冻结中', tone: 'warning' },
  [SETTLEMENT_STATUS.AVAILABLE]: { label: '可结算', tone: 'primary' },
  [SETTLEMENT_STATUS.SETTLING]: { label: '结算中', tone: 'warning' },
  [SETTLEMENT_STATUS.SETTLED]: { label: '已结算', tone: 'success' },
})

export function getSettlementStatusMeta(status) {
  return SETTLEMENT_STATUS_META[status] || { label: status || '未知', tone: 'muted' }
}
