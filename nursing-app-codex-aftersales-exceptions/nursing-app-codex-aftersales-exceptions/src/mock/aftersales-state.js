import Mock from 'mockjs'
import { EXCEPTION_STATUS, EXCEPTION_TYPE, REFUND_STATUS } from '@/constants/aftersales-status.js'
import { ASSIGNMENT_STATUS, ORDER_STATUS, PAYMENT_STATUS, deriveLegacyStatus } from '@/constants/order-status.js'

export const refunds = new Map()
export const exceptions = [
  { exceptionId: 95001, orderId: 19997, merchantId: 20001, type: EXCEPTION_TYPE.ASSIGNMENT_TIMEOUT, status: EXCEPTION_STATUS.OPEN, title: '王护理员接单超时', description: '派单已超过响应时限，订单已恢复为待派单。', createTime: '2026-07-15T10:30:00+08:00', updateTime: '2026-07-15T10:30:00+08:00', evidence: [], records: [{ action: 'CREATE', title: '系统生成接单超时异常', remark: '护理人员未在规定时间内响应', operator: '系统', time: '2026-07-15T10:30:00+08:00' }] },
  { exceptionId: 95002, orderId: 19995, merchantId: 20001, type: EXCEPTION_TYPE.ABNORMAL_CHECK_IN, status: EXCEPTION_STATUS.MERCHANT_PROCESSING, title: '压疮护理异常签到', description: '护理人员定位不可用，提交了人工核验签到。', createTime: '2026-07-15T08:50:00+08:00', updateTime: '2026-07-15T08:50:00+08:00', evidence: [{ name: '设备定位异常说明', value: '设备定位不可用' }], records: [{ action: 'CREATE', title: '护理人员提交异常签到', remark: '设备定位不可用', operator: '王护理员', time: '2026-07-15T08:50:00+08:00' }] },
]

export function now() { return new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00') }
export function clone(value) { return value == null ? value : JSON.parse(JSON.stringify(value)) }

export function calculateCancellation(order) {
  const cancellableStatuses = [ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_SERVICE]
  if (!cancellableStatuses.includes(order.orderStatus)) return { cancellable: false, refundAmount: 0, deductionAmount: 0, refundStatus: REFUND_STATUS.NONE, ruleTitle: '当前阶段不可直接取消', ruleDescription: '服务已经开始或完成，如有争议请提交投诉由平台处理。' }
  if (order.paymentStatus !== PAYMENT_STATUS.PAID) return { cancellable: true, refundAmount: 0, deductionAmount: 0, refundStatus: REFUND_STATUS.NOT_REQUIRED, ruleTitle: '未支付订单可直接取消', ruleDescription: '订单尚未支付，取消后立即关闭且无需退款。' }
  if (order.orderStatus === ORDER_STATUS.WAITING_DISPATCH) return { cancellable: true, refundAmount: Number(order.totalAmount), deductionAmount: 0, refundStatus: REFUND_STATUS.PENDING, ruleTitle: '未派单可全额退款', ruleDescription: '订单尚未进入实际履约，取消后退还全部已支付金额。' }
  if (order.orderStatus === ORDER_STATUS.WAITING_SERVICE) { const deductionAmount = Math.round(Number(order.totalAmount) * 10) / 100; return { cancellable: true, refundAmount: Number(order.totalAmount) - deductionAmount, deductionAmount, refundStatus: REFUND_STATUS.PENDING, ruleTitle: '已接单取消扣除10%调度成本', ruleDescription: '护理人员已经确认服务，取消后扣除调度成本，其余金额原路退回。' } }
  return { cancellable: false, refundAmount: 0, deductionAmount: 0, refundStatus: REFUND_STATUS.NONE, ruleTitle: '当前阶段不可直接取消', ruleDescription: '服务已经开始或完成，如有争议请提交投诉由平台处理。' }
}

export function createOrderException(order, type, description, evidence = []) {
  const existing = exceptions.find((item) => item.orderId === order.orderId && item.type === type && ![EXCEPTION_STATUS.RESOLVED, EXCEPTION_STATUS.CLOSED].includes(item.status))
  if (existing) return existing
  const exception = { exceptionId: Mock.Random.integer(95100, 95999), orderId: order.orderId, merchantId: order.merchantId, type, status: type === EXCEPTION_TYPE.ABNORMAL_CHECK_IN ? EXCEPTION_STATUS.MERCHANT_PROCESSING : EXCEPTION_STATUS.OPEN, title: type === EXCEPTION_TYPE.ABNORMAL_CHECK_IN ? `${order.serviceItemName}异常签到` : type === EXCEPTION_TYPE.NO_CAREGIVER ? `${order.serviceItemName}暂无可派人员` : `${order.serviceItemName}接单超时`, description, createTime: now(), updateTime: now(), evidence, records: [{ action: 'CREATE', title: '系统创建异常工单', remark: description, operator: '系统', time: now() }] }
  exceptions.unshift(exception); order.exceptionId = exception.exceptionId; order.exceptionType = type; order.exceptionStatus = exception.status; return exception
}

export function registerAbnormalCheckIn(order, payload) {
  const existing = exceptions.find((item) => item.orderId === order.orderId && item.type === EXCEPTION_TYPE.ABNORMAL_CHECK_IN && item.status !== EXCEPTION_STATUS.RESOLVED)
  if (existing) return existing
  const exception = { exceptionId: Mock.Random.integer(95100, 95999), orderId: order.orderId, merchantId: order.merchantId, type: EXCEPTION_TYPE.ABNORMAL_CHECK_IN, status: EXCEPTION_STATUS.MERCHANT_PROCESSING, title: `${order.serviceItemName}异常签到`, description: payload.abnormalReason || '签到信息需要人工核验', createTime: now(), updateTime: now(), evidence: [{ name: '异常原因', value: payload.abnormalReason || '未说明' }], records: [{ action: 'CREATE', title: '护理人员提交异常签到', remark: payload.abnormalReason || '', operator: order.currentAssignment?.caregiverName || '护理人员', time: now() }] }
  exceptions.unshift(exception); order.exceptionId = exception.exceptionId; order.exceptionType = exception.type; order.exceptionStatus = exception.status; return exception
}

export function expireAssignmentIfNeeded(order) {
  const assignment = order.currentAssignment
  if (!assignment || order.assignmentStatus !== ASSIGNMENT_STATUS.WAITING_ACCEPT || new Date(assignment.expireTime).getTime() > Date.now()) return
  assignment.status = ASSIGNMENT_STATUS.EXPIRED; order.assignmentStatus = ASSIGNMENT_STATUS.EXPIRED
  order.operationLogs.push({ action: 'assignment-timeout', remark: '护理人员接单超时，订单恢复待派单', createTime: now(), fromOrderStatus: order.orderStatus, toOrderStatus: order.orderStatus })
  createOrderException(order, EXCEPTION_TYPE.ASSIGNMENT_TIMEOUT, '护理人员未在规定时间内响应派单，订单需要重新派单。')
}

export function createRefundForCancellation(order, preview, reason) {
  const refund = { refundId: `RF${order.orderId}${Date.now()}`, orderId: order.orderId, status: preview.refundStatus === REFUND_STATUS.NOT_REQUIRED ? REFUND_STATUS.NOT_REQUIRED : REFUND_STATUS.PROCESSING, totalAmount: Number(order.totalAmount), refundAmount: preview.refundAmount, deductionAmount: preview.deductionAmount, reason, applyTime: now(), successTime: null, finalPaymentStatus: preview.refundAmount < Number(order.totalAmount) ? PAYMENT_STATUS.PARTIALLY_REFUNDED : PAYMENT_STATUS.REFUNDED, records: [{ action: 'APPLY', title: preview.refundStatus === REFUND_STATUS.NOT_REQUIRED ? '订单取消，无需退款' : '退款申请已提交', remark: preview.ruleDescription, time: now() }] }
  if (preview.refundStatus !== REFUND_STATUS.NOT_REQUIRED) refund.mockCompleteAt = Date.now() + 8000
  refunds.set(order.orderId, refund); order.refund = refund; return refund
}

export function refreshRefund(order) {
  const refund = refunds.get(order.orderId)
  if (!refund || refund.status !== REFUND_STATUS.PROCESSING || Date.now() < refund.mockCompleteAt) return
  refund.status = REFUND_STATUS.SUCCESS; refund.successTime = now(); refund.records.unshift({ action: 'SUCCESS', title: '退款成功', remark: `¥${refund.refundAmount} 已原路退回`, time: refund.successTime }); delete refund.mockCompleteAt; order.paymentStatus = refund.finalPaymentStatus || PAYMENT_STATUS.REFUNDED; order.status = deriveLegacyStatus(order.orderStatus, order.paymentStatus)
}
