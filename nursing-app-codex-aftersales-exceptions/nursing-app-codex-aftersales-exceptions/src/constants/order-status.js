export const ORDER_STATUS = Object.freeze({
  CREATED: 'CREATED',
  WAITING_DISPATCH: 'WAITING_DISPATCH',
  WAITING_ACCEPT: 'WAITING_ACCEPT',
  WAITING_SERVICE: 'WAITING_SERVICE',
  IN_SERVICE: 'IN_SERVICE',
  WAITING_CONFIRM: 'WAITING_CONFIRM',
  COMPLETED: 'COMPLETED',
  CANCELED: 'CANCELED',
  CLOSED: 'CLOSED',
  DISPUTED: 'DISPUTED',
})

export const PAYMENT_STATUS = Object.freeze({
  UNPAID: 'UNPAID',
  PAID: 'PAID',
  REFUNDING: 'REFUNDING',
  PARTIALLY_REFUNDED: 'PARTIALLY_REFUNDED',
  REFUNDED: 'REFUNDED',
})

export const ASSIGNMENT_STATUS = Object.freeze({
  UNASSIGNED: 'UNASSIGNED',
  WAITING_ACCEPT: 'WAITING_ACCEPT',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
  EXPIRED: 'EXPIRED',
  CANCELED: 'CANCELED',
})

export const SERVICE_ACTION = Object.freeze({
  DEPART: 'DEPART',
  CHECK_IN: 'CHECK_IN',
  START: 'START',
  FINISH: 'FINISH',
})

export const ORDER_STATUS_META = Object.freeze({
  [ORDER_STATUS.CREATED]: {
    text: '待支付',
    description: '请尽快完成支付，订单将为您保留',
    icon: 'rmb-circle-fill',
    tone: 'warning',
  },
  [ORDER_STATUS.WAITING_DISPATCH]: {
    text: '待派单',
    description: '商户正在为您安排合适的护理人员',
    icon: 'clock-fill',
    tone: 'primary',
  },
  [ORDER_STATUS.WAITING_ACCEPT]: {
    text: '待接单',
    description: '商户已派单，等待护理人员确认任务',
    icon: 'clock-fill',
    tone: 'primary',
  },
  [ORDER_STATUS.WAITING_SERVICE]: {
    text: '待上门',
    description: '护理人员已接单，将按预约时间上门',
    icon: 'calendar-fill',
    tone: 'primary',
  },
  [ORDER_STATUS.IN_SERVICE]: {
    text: '服务中',
    description: '护理人员正在提供本次护理服务',
    icon: 'server-man',
    tone: 'primary',
  },
  [ORDER_STATUS.WAITING_CONFIRM]: {
    text: '待确认',
    description: '护理人员已结束服务，请确认服务结果',
    icon: 'checkmark-circle',
    tone: 'primary',
  },
  [ORDER_STATUS.COMPLETED]: {
    text: '已完成',
    description: '本次护理服务已顺利完成',
    icon: 'checkmark-circle-fill',
    tone: 'success',
  },
  [ORDER_STATUS.CANCELED]: {
    text: '已取消',
    description: '订单已取消，如有疑问请联系客服',
    icon: 'close-circle-fill',
    tone: 'neutral',
  },
  [ORDER_STATUS.CLOSED]: {
    text: '已关闭',
    description: '订单已关闭',
    icon: 'close-circle',
    tone: 'neutral',
  },
  [ORDER_STATUS.DISPUTED]: {
    text: '争议处理中',
    description: '平台正在处理本次服务争议',
    icon: 'info-circle-fill',
    tone: 'neutral',
  },
})

export const LEGACY_STATUS_MAP = Object.freeze({
  0: '待支付',
  1: '待派单',
  2: '已完成',
  3: '已取消',
  4: '退款中',
  5: '已退款',
  6: '已派单',
  7: '已接单',
  8: '服务中',
  9: '待确认',
})

export const ORDER_TRANSITIONS = Object.freeze({
  [ORDER_STATUS.CREATED]: [ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.CANCELED],
  [ORDER_STATUS.WAITING_DISPATCH]: [ORDER_STATUS.WAITING_ACCEPT, ORDER_STATUS.CANCELED],
  [ORDER_STATUS.WAITING_ACCEPT]: [ORDER_STATUS.WAITING_SERVICE],
  [ORDER_STATUS.WAITING_SERVICE]: [ORDER_STATUS.IN_SERVICE],
  [ORDER_STATUS.IN_SERVICE]: [ORDER_STATUS.WAITING_CONFIRM, ORDER_STATUS.DISPUTED],
  [ORDER_STATUS.WAITING_CONFIRM]: [ORDER_STATUS.COMPLETED, ORDER_STATUS.DISPUTED],
  [ORDER_STATUS.DISPUTED]: [ORDER_STATUS.COMPLETED, ORDER_STATUS.CANCELED, ORDER_STATUS.CLOSED],
  [ORDER_STATUS.CANCELED]: [ORDER_STATUS.CLOSED],
  [ORDER_STATUS.COMPLETED]: [],
  [ORDER_STATUS.CLOSED]: [],
})

export function canTransitionOrderStatus(fromStatus, toStatus) {
  return ORDER_TRANSITIONS[fromStatus]?.includes(toStatus) || false
}

export function deriveLegacyStatus(orderStatus, paymentStatus = PAYMENT_STATUS.UNPAID) {
  if (orderStatus === ORDER_STATUS.CREATED) return 0
  if (orderStatus === ORDER_STATUS.WAITING_DISPATCH) return 1
  if (orderStatus === ORDER_STATUS.WAITING_ACCEPT) return 6
  if (orderStatus === ORDER_STATUS.WAITING_SERVICE) return 7
  if (orderStatus === ORDER_STATUS.IN_SERVICE) return 8
  if (orderStatus === ORDER_STATUS.WAITING_CONFIRM) return 9
  if (orderStatus === ORDER_STATUS.COMPLETED) return 2
  if (orderStatus === ORDER_STATUS.CANCELED || orderStatus === ORDER_STATUS.CLOSED) {
    if (paymentStatus === PAYMENT_STATUS.REFUNDING) return 4
    if (
      paymentStatus === PAYMENT_STATUS.REFUNDED ||
      paymentStatus === PAYMENT_STATUS.PARTIALLY_REFUNDED
    ) return 5
    return 3
  }
  return 1
}

export function inferOrderStatus(legacyStatus) {
  const status = Number(legacyStatus)
  if (status === 0) return ORDER_STATUS.CREATED
  if (status === 1) return ORDER_STATUS.WAITING_DISPATCH
  if (status === 2) return ORDER_STATUS.COMPLETED
  if (status === 3 || status === 4) return ORDER_STATUS.CANCELED
  if (status === 5) return ORDER_STATUS.CLOSED
  if (status === 6) return ORDER_STATUS.WAITING_ACCEPT
  if (status === 7) return ORDER_STATUS.WAITING_SERVICE
  if (status === 8) return ORDER_STATUS.IN_SERVICE
  if (status === 9) return ORDER_STATUS.WAITING_CONFIRM
  return ORDER_STATUS.WAITING_DISPATCH
}

export function inferPaymentStatus(legacyStatus) {
  const status = Number(legacyStatus)
  if (status === 0 || status === 3) return PAYMENT_STATUS.UNPAID
  if (status === 4) return PAYMENT_STATUS.REFUNDING
  if (status === 5) return PAYMENT_STATUS.REFUNDED
  return PAYMENT_STATUS.PAID
}

export function normalizeOrderState(order = {}) {
  const orderStatus = typeof order.orderStatus === 'number'
    ? inferOrderStatus(order.orderStatus)
    : order.orderStatus || inferOrderStatus(order.status)
  const paymentStatus = order.paymentStatus || inferPaymentStatus(order.status)
  const numericAssignmentStatus = order.currentAssignment?.status ?? order.assignmentStatus
  const assignmentStatus = typeof numericAssignmentStatus === 'number'
    ? ({ 0: ASSIGNMENT_STATUS.WAITING_ACCEPT, 1: ASSIGNMENT_STATUS.ACCEPTED, 2: ASSIGNMENT_STATUS.REJECTED }[numericAssignmentStatus] || ASSIGNMENT_STATUS.UNASSIGNED)
    : numericAssignmentStatus || (
      orderStatus === ORDER_STATUS.WAITING_DISPATCH
        ? ASSIGNMENT_STATUS.UNASSIGNED
        : orderStatus === ORDER_STATUS.WAITING_ACCEPT
          ? ASSIGNMENT_STATUS.WAITING_ACCEPT
          : ASSIGNMENT_STATUS.ACCEPTED
    )

  return {
    ...order,
    orderId: order.orderId == null ? order.orderId : String(order.orderId),
    serviceItemId: order.serviceItemId == null ? order.serviceItemId : String(order.serviceItemId),
    orderStatus,
    paymentStatus,
    assignmentStatus,
    status: order.status ?? deriveLegacyStatus(orderStatus, paymentStatus),
    assignments: order.assignments || [],
    serviceRecords: order.serviceRecords || [],
    operationLogs: (order.operationLogs || []).map((log) => ({
      ...log,
      fromOrderStatus: log.fromOrderStatus ?? log.fromStatus,
      toOrderStatus: log.toOrderStatus ?? log.toStatus,
    })),
  }
}

export function getOrderStatusMeta(orderOrStatus) {
  if (typeof orderOrStatus === 'number') return ORDER_STATUS_META[inferOrderStatus(orderOrStatus)]

  const orderStatus = typeof orderOrStatus === 'string'
    ? orderOrStatus
    : normalizeOrderState(orderOrStatus).orderStatus
  return ORDER_STATUS_META[orderStatus] || {
    text: '未知',
    description: '订单状态已更新',
    icon: 'info-circle',
    tone: 'neutral',
  }
}

export function canCustomerPay(order) {
  const normalized = normalizeOrderState(order)
  return normalized.orderStatus === ORDER_STATUS.CREATED && normalized.paymentStatus === PAYMENT_STATUS.UNPAID
}

export function canCustomerCancel(order) {
  return [
    ORDER_STATUS.CREATED,
    ORDER_STATUS.WAITING_DISPATCH,
  ].includes(normalizeOrderState(order).orderStatus)
}

export function canCustomerConfirm(order) {
  return normalizeOrderState(order).orderStatus === ORDER_STATUS.WAITING_CONFIRM
}

export function canCustomerReview(order) {
  return normalizeOrderState(order).orderStatus === ORDER_STATUS.COMPLETED
}

export function canCustomerComplain(order) {
  const normalized = normalizeOrderState(order)
  if (normalized.complaintId || normalized.orderStatus === ORDER_STATUS.DISPUTED) return false
  return [
    ORDER_STATUS.WAITING_SERVICE,
    ORDER_STATUS.IN_SERVICE,
    ORDER_STATUS.WAITING_CONFIRM,
    ORDER_STATUS.COMPLETED,
  ].includes(normalized.orderStatus)
}
