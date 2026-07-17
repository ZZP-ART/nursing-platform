import { describe, expect, it } from 'vitest'
import { calculateCancellation } from '@/mock/aftersales-state.js'
import { ORDER_STATUS, PAYMENT_STATUS } from '@/constants/order-status.js'

describe('aftersales mock state', () => {
  it('does not allow an unpaid order to bypass service-stage cancellation rules', () => {
    const preview = calculateCancellation({
      orderStatus: ORDER_STATUS.IN_SERVICE,
      paymentStatus: PAYMENT_STATUS.UNPAID,
      totalAmount: 120,
    })

    expect(preview.cancellable).toBe(false)
  })

  it('deducts ten percent after a caregiver has accepted the order', () => {
    const preview = calculateCancellation({
      orderStatus: ORDER_STATUS.WAITING_SERVICE,
      paymentStatus: PAYMENT_STATUS.PAID,
      totalAmount: 120,
    })

    expect(preview.deductionAmount).toBe(12)
    expect(preview.refundAmount).toBe(108)
  })
})
