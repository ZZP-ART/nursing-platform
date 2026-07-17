import { describe, expect, it } from 'vitest'
import {
  ASSIGNMENT_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  canCustomerCancel,
  canCustomerConfirm,
  canCustomerComplain,
  canCustomerPay,
  canCustomerReview,
  canTransitionOrderStatus,
  deriveLegacyStatus,
  normalizeOrderState,
} from '@/constants/order-status.js'

describe('order status model', () => {
  it('只允许合法的订单状态迁移', () => {
    expect(canTransitionOrderStatus(ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH)).toBe(true)
    expect(canTransitionOrderStatus(ORDER_STATUS.WAITING_SERVICE, ORDER_STATUS.IN_SERVICE)).toBe(true)
    expect(canTransitionOrderStatus(ORDER_STATUS.IN_SERVICE, ORDER_STATUS.COMPLETED)).toBe(false)
    expect(canTransitionOrderStatus(ORDER_STATUS.COMPLETED, ORDER_STATUS.CANCELED)).toBe(false)
  })

  it('将新状态映射到旧数字状态以兼容现有页面', () => {
    expect(deriveLegacyStatus(ORDER_STATUS.CREATED, PAYMENT_STATUS.UNPAID)).toBe(0)
    expect(deriveLegacyStatus(ORDER_STATUS.WAITING_DISPATCH, PAYMENT_STATUS.PAID)).toBe(1)
    expect(deriveLegacyStatus(ORDER_STATUS.WAITING_CONFIRM, PAYMENT_STATUS.PAID)).toBe(9)
    expect(deriveLegacyStatus(ORDER_STATUS.COMPLETED, PAYMENT_STATUS.PAID)).toBe(2)
    expect(deriveLegacyStatus(ORDER_STATUS.CANCELED, PAYMENT_STATUS.REFUNDING)).toBe(4)
  })

  it('为旧订单补齐新状态字段和记录数组', () => {
    const order = normalizeOrderState({ orderId: 1, status: 1 })
    expect(order.orderStatus).toBe(ORDER_STATUS.WAITING_DISPATCH)
    expect(order.paymentStatus).toBe(PAYMENT_STATUS.PAID)
    expect(order.assignmentStatus).toBe(ASSIGNMENT_STATUS.UNASSIGNED)
    expect(order.assignments).toEqual([])
    expect(order.serviceRecords).toEqual([])
  })

  it('顾客只能在对应阶段执行支付、取消、确认和评价', () => {
    expect(canCustomerPay({ orderStatus: ORDER_STATUS.CREATED, paymentStatus: PAYMENT_STATUS.UNPAID })).toBe(true)
    expect(canCustomerCancel({ orderStatus: ORDER_STATUS.WAITING_DISPATCH, paymentStatus: PAYMENT_STATUS.PAID })).toBe(true)
    expect(canCustomerConfirm({ orderStatus: ORDER_STATUS.IN_SERVICE })).toBe(false)
    expect(canCustomerConfirm({ orderStatus: ORDER_STATUS.WAITING_CONFIRM })).toBe(true)
    expect(canCustomerReview({ orderStatus: ORDER_STATUS.WAITING_CONFIRM })).toBe(false)
    expect(canCustomerReview({ orderStatus: ORDER_STATUS.COMPLETED })).toBe(true)
    expect(canCustomerComplain({ orderStatus: ORDER_STATUS.COMPLETED })).toBe(true)
    expect(canCustomerComplain({ orderStatus: ORDER_STATUS.DISPUTED, complaintId: 40001 })).toBe(false)
  })
})
