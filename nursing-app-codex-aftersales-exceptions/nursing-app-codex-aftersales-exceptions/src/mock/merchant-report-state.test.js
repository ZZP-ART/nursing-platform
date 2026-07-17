import { describe, expect, it } from 'vitest'
import { buildMerchantReport } from '@/mock/merchant-report-state.js'
import { ORDER_STATUS, PAYMENT_STATUS } from '@/constants/order-status.js'

describe('merchant report state', () => {
  const orders = [
    { orderId: 1, merchantId: 2, serviceItemId: 10, serviceItemName: '服务A', totalAmount: 100, orderStatus: ORDER_STATUS.COMPLETED, paymentStatus: PAYMENT_STATUS.PAID, createTime: '2026-07-15T10:00:00+08:00' },
    { orderId: 2, merchantId: 2, serviceItemId: 10, serviceItemName: '服务A', totalAmount: 200, orderStatus: ORDER_STATUS.IN_SERVICE, paymentStatus: PAYMENT_STATUS.PAID, createTime: '2026-07-14T10:00:00+08:00' },
    { orderId: 3, merchantId: 3, serviceItemId: 11, serviceItemName: '其他商户', totalAmount: 500, orderStatus: ORDER_STATUS.COMPLETED, paymentStatus: PAYMENT_STATUS.PAID, createTime: '2026-07-15T10:00:00+08:00' },
  ]

  it('calculates merchant-scoped operating and quality metrics', () => {
    const report = buildMerchantReport({ merchantId: 2, range: '7d', orders, reviews: [{ orderId: 1, rating: 5, status: 2 }], complaints: [{ complaintId: 5, merchantId: 2, orderId: 2, status: 0, createTime: '2026-07-15T11:00:00+08:00' }], exceptions: [], now: new Date('2026-07-16T12:00:00+08:00') })
    expect(report.overview).toMatchObject({ orderCount: 2, revenue: 300, averageOrderAmount: 150 })
    expect(report.quality).toMatchObject({ completionRate: 50, averageRating: 5, positiveRate: 100, complaintRate: 50 })
    expect(report.alerts.total).toBe(1)
  })

  it('falls back to the 30-day range', () => {
    expect(buildMerchantReport({ merchantId: 2, range: 'invalid', orders, now: new Date('2026-07-16T12:00:00+08:00') }).range).toBe('30d')
  })
})
