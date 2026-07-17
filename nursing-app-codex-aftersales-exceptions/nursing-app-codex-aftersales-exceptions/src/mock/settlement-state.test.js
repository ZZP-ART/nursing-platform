import { describe, expect, it } from 'vitest'
import { buildSettlementRecord, createWithdrawal } from '@/mock/settlement-state.js'
import { ORDER_STATUS, PAYMENT_STATUS } from '@/constants/order-status.js'

describe('settlement state', () => {
  it('splits completed order income using the snapshot rule', () => { const record = buildSettlementRecord({ orderId: 1, orderNo: 'O1', merchantId: 2, totalAmount: 100, orderStatus: ORDER_STATUS.COMPLETED, paymentStatus: PAYMENT_STATUS.PAID, currentAssignment: { caregiverId: 3, caregiverName: '护理员' }, operationLogs: [] }); expect(record.platformFee).toBe(10); expect(record.caregiverCommission).toBe(70); expect(record.merchantIncome).toBe(20) })
  it('freezes disputed order income', () => { const record = buildSettlementRecord({ orderId: 2, orderNo: 'O2', merchantId: 2, totalAmount: 100, orderStatus: ORDER_STATUS.DISPUTED, paymentStatus: PAYMENT_STATUS.PAID, currentAssignment: { caregiverId: 3, caregiverName: '护理员' }, operationLogs: [] }); expect(record.status).toBe('FROZEN') })
  it('marks completed paid order income as available', () => { const record = buildSettlementRecord({ orderId: 3, orderNo: 'O3', merchantId: 2, totalAmount: 100, orderStatus: ORDER_STATUS.COMPLETED, paymentStatus: PAYMENT_STATUS.PAID, currentAssignment: { caregiverId: 3, caregiverName: '护理员' }, operationLogs: [] }); expect(record.status).toBe('AVAILABLE') })
  it.each([0, -1, Number.NaN, Number.POSITIVE_INFINITY])('rejects invalid withdrawal amount %s', (amount) => {
    expect(() => createWithdrawal('CAREGIVER', 3, amount, { accountName: '护理员', accountNo: 'mock account' })).toThrow(RangeError)
  })
})
