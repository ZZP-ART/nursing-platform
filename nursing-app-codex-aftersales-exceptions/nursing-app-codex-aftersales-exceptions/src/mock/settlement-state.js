import Mock from 'mockjs'
import { ORDER_STATUS, PAYMENT_STATUS } from '@/constants/order-status.js'
import { SETTLEMENT_STATUS, WITHDRAW_STATUS } from '@/constants/settlement.js'

export const SPLIT_RULE = Object.freeze({ platformRate: 0.1, caregiverRate: 0.7, merchantRate: 0.2 })

const records = new Map()
export const withdrawals = [
  { withdrawalId: 88001, ownerType: 'CAREGIVER', ownerId: 50001, amount: 56, status: WITHDRAW_STATUS.SUCCESS, accountName: '王护理员', accountNo: '支付宝 138****8000', applyTime: '2026-07-05T10:00:00+08:00', finishTime: '2026-07-05T10:05:00+08:00' },
  { withdrawalId: 88002, ownerType: 'MERCHANT', ownerId: 20001, amount: 16, status: WITHDRAW_STATUS.SUCCESS, accountName: '北京康宁护理服务有限公司', accountNo: '建设银行 ****1234', applyTime: '2026-07-06T14:00:00+08:00', finishTime: '2026-07-06T14:08:00+08:00' },
]

export function roundMoney(value) { return Math.round(Number(value || 0) * 100) / 100 }
export function cloneSettlement(value) { return value == null ? value : JSON.parse(JSON.stringify(value)) }

export function buildSettlementRecord(order) {
  if (!order?.currentAssignment?.caregiverId || order.paymentStatus === PAYMENT_STATUS.UNPAID) return null
  const existing = records.get(order.orderId)
  const grossAmount = roundMoney(order.totalAmount)
  const refundedAmount = roundMoney(order.refund?.status === 'SUCCESS' ? order.refund.refundAmount : 0)
  const netAmount = Math.max(0, roundMoney(grossAmount - refundedAmount))
  const frozen = order.orderStatus === ORDER_STATUS.DISPUTED || order.paymentStatus === PAYMENT_STATUS.REFUNDING
  const completed = order.orderStatus === ORDER_STATUS.COMPLETED
  const status = existing?.status === SETTLEMENT_STATUS.SETTLED ? SETTLEMENT_STATUS.SETTLED : frozen || !completed ? SETTLEMENT_STATUS.FROZEN : SETTLEMENT_STATUS.AVAILABLE
  const record = {
    settlementId: existing?.settlementId || Mock.Random.integer(86000, 86999), orderId: order.orderId, orderNo: order.orderNo,
    merchantId: order.merchantId, merchantName: order.merchantName, caregiverId: order.currentAssignment.caregiverId,
    caregiverName: order.currentAssignment.caregiverName, serviceItemName: order.serviceItemName, serviceDate: order.serviceDate,
    grossAmount, refundedAmount, netAmount, platformFee: roundMoney(netAmount * SPLIT_RULE.platformRate),
    caregiverCommission: roundMoney(netAmount * SPLIT_RULE.caregiverRate), merchantIncome: roundMoney(netAmount * SPLIT_RULE.merchantRate),
    status, freezeReason: frozen ? '订单处于争议或退款处理中' : !completed ? '订单尚未完成' : '',
    availableTime: completed && !frozen ? (existing?.availableTime || order.operationLogs?.find((item) => item.action === 'confirm')?.createTime || order.createTime) : null,
    settledTime: existing?.settledTime || null,
  }
  records.set(order.orderId, record)
  return record
}

export function listSettlementRecords(orders) {
  return orders.map(buildSettlementRecord).filter(Boolean)
}

export function createWithdrawal(ownerType, ownerId, amount, account) {
  const withdrawalAmount = Number(amount)
  if (!Number.isFinite(withdrawalAmount) || withdrawalAmount <= 0) throw new RangeError('Withdrawal amount must be greater than zero')
  const withdrawal = { withdrawalId: Mock.Random.integer(88100, 88999), ownerType, ownerId, amount: roundMoney(withdrawalAmount), status: WITHDRAW_STATUS.PROCESSING, accountName: account.accountName, accountNo: account.accountNo, applyTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'), finishTime: null, mockCompleteAt: Date.now() + 8000 }
  withdrawals.unshift(withdrawal)
  return withdrawal
}

export function refreshWithdrawal(item) {
  if (item.status === WITHDRAW_STATUS.PROCESSING && Date.now() >= item.mockCompleteAt) { item.status = WITHDRAW_STATUS.SUCCESS; item.finishTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'); delete item.mockCompleteAt }
  return item
}
