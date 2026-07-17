/**
 * Mock — 订单模块（order-service）
 *
 * 对齐 API v1.0：
 * - POST /api/v1/orders/prepay-token     → 获取下单幂等令牌
 * - POST /api/v1/orders                  → 创建订单
 * - GET  /api/v1/orders                  → 订单列表
 * - GET  /api/v1/orders/{id}             → 订单详情
 * - POST /api/v1/orders/{id}/cancel      → 取消订单
 * - POST /api/v1/orders/{id}/pay         → 发起支付
 * - POST /api/v1/orders/pay/callback     → 支付宝回调（前端不直接调用）
 */
import Mock from 'mockjs'
import { getMockAddress } from './address.js'
import { getMockServiceItem, getMockServiceSpec } from './service.js'
import { getActiveMerchantCaregivers } from './merchant-team.js'
import { getMockMerchantIdByUserId } from './user.js'
import { evaluateCaregiverForOrder } from './caregiver-schedule-state.js'
import {
  calculateCancellation,
  createOrderException,
  createRefundForCancellation,
  expireAssignmentIfNeeded,
  refreshRefund,
  registerAbnormalCheckIn,
} from './aftersales-state.js'
import { EXCEPTION_TYPE } from '@/constants/aftersales-status.js'
import {
  ASSIGNMENT_STATUS,
  ORDER_STATUS,
  PAYMENT_STATUS,
  deriveLegacyStatus,
} from '@/constants/order-status.js'

const Random = Mock.Random

// ========== 模拟数据 ==========
const orders = []
const prepayTokens = new Map() // prepayToken → { expireTime }
let nextOrderId = 20001
let nextAssignmentId = 60006

// 生成订单号
function generateOrderNo() {
  const now = new Date()
  const date = now.toISOString().slice(0, 10).replace(/-/g, '')
  return date + String(nextOrderId)
}

function createOperationLog(action, remark, createTime, fromOrderStatus, toOrderStatus) {
  return {
    action,
    remark,
    createTime,
    fromOrderStatus: fromOrderStatus || null,
    toOrderStatus: toOrderStatus || null,
  }
}

function syncLegacyStatus(order) {
  order.status = deriveLegacyStatus(order.orderStatus, order.paymentStatus)
  return order
}

function updateOrderStatus(order, nextStatus, action, remark) {
  const previousStatus = order.orderStatus
  order.orderStatus = nextStatus
  syncLegacyStatus(order)
  order.operationLogs.push(createOperationLog(
    action,
    remark,
    new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    previousStatus,
    nextStatus
  ))
}

function appendServiceRecord(order, action, content, payload = {}) {
  const record = {
    recordId: `${order.orderId}-${order.serviceRecords.length + 1}`,
    action,
    content,
    operatorId: order.currentAssignment?.caregiverId || null,
    operatorName: order.currentAssignment?.caregiverName || '护理人员',
    createTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    ...payload,
  }
  order.serviceRecords.push(record)
  return record
}

function getCaregiverIdFromRequest(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const userId = Number(auth.match(/mock_jwt_(\d+)_CAREGIVER_/)?.[1])
  return ({ 10001: 50001, 10002: 50002 })[userId] || null
}

function getMerchantIdFromRequest(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const userId = Number(auth.match(/mock_jwt_(\d+)_MERCHANT_MEMBER_/)?.[1])
  return getMockMerchantIdByUserId(userId)
}

function getMerchantOrder(options, orderId) {
  const merchantId = getMerchantIdFromRequest(options)
  if (!merchantId) return { error: { code: 1004, message: '暂无商户权限', data: null } }
  const order = getMockOrder(orderId)
  if (!order) return { error: { code: 3007, message: '订单不存在', data: null } }
  if (order.merchantId !== merchantId) {
    return { error: { code: 3008, message: '无权查看该订单', data: null } }
  }
  return { merchantId, order }
}

// 预设订单覆盖完整派单和履约状态
const presetOrders = [
  { orderId: 19994, orderNo: '2026062819994', serviceItemId: 103, serviceSpecId: 10301,
    serviceItemName: '日常起居照料', specName: '单次服务',
    specPrice: 200, totalAmount: 200, status: 2,
    merchantId: 20001, merchantName: '康宁护理中心',
    orderStatus: ORDER_STATUS.COMPLETED, paymentStatus: PAYMENT_STATUS.PAID,
    assignmentStatus: ASSIGNMENT_STATUS.ACCEPTED,
    receiverName: '周奶奶', receiverPhone: '135****6182', addressDetail: '北京市朝阳区安华里二区8号楼502',
    serviceDate: '2026-06-29', serviceTimeSlot: 'AFTERNOON',
    createTime: '2026-06-28T09:00:00+08:00',
    currentAssignment: { assignmentId: 59994, caregiverId: 50001, caregiverName: '王护理员', caregiverPhone: '138****8000', status: ASSIGNMENT_STATUS.ACCEPTED, acceptedTime: '2026-06-28T09:20:00+08:00' },
    assignments: [
      { assignmentId: 59994, caregiverId: 50001, caregiverName: '王护理员', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-06-28T09:10:00+08:00', acceptedTime: '2026-06-28T09:20:00+08:00' },
    ],
    serviceRecords: [
      { recordId: '19994-1', action: 'CHECK_IN', content: '护理人员已到达并签到', distanceMeters: 28, createTime: '2026-06-29T13:55:00+08:00' },
      { recordId: '19994-2', action: 'START', content: '护理服务已开始', createTime: '2026-06-29T14:00:00+08:00' },
      { recordId: '19994-3', action: 'FINISH', content: '护理服务已结束，服务记录已提交', summary: '协助完成日常起居照料，服务过程正常。', createTime: '2026-06-29T16:00:00+08:00' },
    ],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-06-28T09:00:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付成功，等待商户派单', '2026-06-28T09:05:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('dispatch', '商户已指派王护理员', '2026-06-28T09:10:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('accept', '护理人员已接单', '2026-06-28T09:20:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_SERVICE),
      createOperationLog('start', '护理服务已开始', '2026-06-29T14:00:00+08:00', ORDER_STATUS.WAITING_SERVICE, ORDER_STATUS.IN_SERVICE),
      createOperationLog('finish', '护理人员已结束服务', '2026-06-29T16:00:00+08:00', ORDER_STATUS.IN_SERVICE, ORDER_STATUS.WAITING_CONFIRM),
      createOperationLog('confirm', '用户确认服务完成', '2026-06-29T16:20:00+08:00', ORDER_STATUS.WAITING_CONFIRM, ORDER_STATUS.COMPLETED),
    ],
  },
  { orderId: 20000, orderNo: '2026070120000', serviceItemId: 401, serviceSpecId: 40101,
    serviceItemName: '静脉采血', specName: '单次服务',
    specPrice: 80, totalAmount: 80, status: 2,
    merchantId: 20001, merchantName: '康宁护理中心',
    orderStatus: ORDER_STATUS.COMPLETED, paymentStatus: PAYMENT_STATUS.PAID,
    assignmentStatus: ASSIGNMENT_STATUS.ACCEPTED,
    receiverName: '张三', receiverPhone: '13800138000', addressDetail: '北京市朝阳区建国路88号6栋301',
    serviceDate: '2026-07-02', serviceTimeSlot: 'MORNING',
    createTime: '2026-07-01T10:00:00+08:00',
    currentAssignment: { assignmentId: 60001, caregiverId: 50001, caregiverName: '王护理员', caregiverPhone: '138****8001', status: ASSIGNMENT_STATUS.ACCEPTED, acceptedTime: '2026-07-01T10:20:00+08:00' },
    assignments: [
      { assignmentId: 60001, caregiverId: 50001, caregiverName: '王护理员', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-07-01T10:10:00+08:00', acceptedTime: '2026-07-01T10:20:00+08:00', remark: '系统推荐后人工派单' },
    ],
    serviceRecords: [
      { recordId: '20000-1', action: 'DEPART', content: '护理人员已出发', createTime: '2026-07-02T08:20:00+08:00' },
      { recordId: '20000-2', action: 'CHECK_IN', content: '护理人员已到达并签到', distanceMeters: 35, createTime: '2026-07-02T08:55:00+08:00' },
      { recordId: '20000-3', action: 'START', content: '护理服务已开始', createTime: '2026-07-02T09:00:00+08:00' },
      { recordId: '20000-4', action: 'FINISH', content: '护理服务已结束，已提交服务记录', summary: '完成静脉采血，服务过程正常。', createTime: '2026-07-02T09:50:00+08:00' },
    ],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-07-01T10:00:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付宝支付成功，等待商户派单', '2026-07-01T10:05:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('dispatch', '商户已指派王护理员', '2026-07-01T10:10:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('accept', '护理人员已接单', '2026-07-01T10:20:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_SERVICE),
      createOperationLog('start', '护理服务已开始', '2026-07-02T09:00:00+08:00', ORDER_STATUS.WAITING_SERVICE, ORDER_STATUS.IN_SERVICE),
      createOperationLog('finish', '护理人员已结束服务', '2026-07-02T09:50:00+08:00', ORDER_STATUS.IN_SERVICE, ORDER_STATUS.WAITING_CONFIRM),
      createOperationLog('confirm', '用户确认服务完成', '2026-07-02T10:00:00+08:00', ORDER_STATUS.WAITING_CONFIRM, ORDER_STATUS.COMPLETED),
    ],
  },
  { orderId: 19999, orderNo: '2026070119999', serviceItemId: 301, serviceSpecId: 30103,
    serviceItemName: '艾灸调理', specName: '5次套餐',
    specPrice: 550, totalAmount: 550, status: 1,
    merchantId: 20001, merchantName: '康宁护理中心',
    orderStatus: ORDER_STATUS.WAITING_CONFIRM, paymentStatus: PAYMENT_STATUS.PAID,
    assignmentStatus: ASSIGNMENT_STATUS.ACCEPTED,
    receiverName: '张三', receiverPhone: '13800138000', addressDetail: '北京市朝阳区建国路88号6栋301',
    serviceDate: '2026-07-14', serviceTimeSlot: 'AFTERNOON',
    createTime: '2026-07-01T15:00:00+08:00',
    currentAssignment: { assignmentId: 60002, caregiverId: 50002, caregiverName: '李护理员', caregiverPhone: '138****8002', status: ASSIGNMENT_STATUS.ACCEPTED, acceptedTime: '2026-07-01T15:20:00+08:00' },
    assignments: [
      { assignmentId: 60002, caregiverId: 50002, caregiverName: '李护理员', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-07-01T15:10:00+08:00', acceptedTime: '2026-07-01T15:20:00+08:00' },
    ],
    serviceRecords: [
      { recordId: '19999-1', action: 'CHECK_IN', content: '护理人员已到达并签到', distanceMeters: 48, createTime: '2026-07-14T13:55:00+08:00' },
      { recordId: '19999-2', action: 'START', content: '护理服务已开始', createTime: '2026-07-14T14:00:00+08:00' },
      { recordId: '19999-3', action: 'FINISH', content: '护理服务已结束，等待顾客确认', summary: '完成首次艾灸调理，用户状态良好。', createTime: '2026-07-14T15:00:00+08:00' },
    ],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-07-01T15:00:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付宝支付成功，等待商户派单', '2026-07-01T15:03:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('accept', '护理人员已接单', '2026-07-01T15:20:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_SERVICE),
      createOperationLog('start', '护理服务已开始', '2026-07-14T14:00:00+08:00', ORDER_STATUS.WAITING_SERVICE, ORDER_STATUS.IN_SERVICE),
      createOperationLog('finish', '护理人员已结束服务', '2026-07-14T15:00:00+08:00', ORDER_STATUS.IN_SERVICE, ORDER_STATUS.WAITING_CONFIRM),
    ],
  },
  { orderId: 19998, orderNo: '2026070119998', serviceItemId: 101, serviceSpecId: 10101,
    serviceItemName: '上门输液护理', specName: '单次服务', specPrice: 150, totalAmount: 150, status: 1,
    merchantId: 20001, merchantName: '康宁护理中心', orderStatus: ORDER_STATUS.WAITING_DISPATCH,
    paymentStatus: PAYMENT_STATUS.PAID, assignmentStatus: ASSIGNMENT_STATUS.UNASSIGNED,
    receiverName: '张三', receiverPhone: '13800138000', addressDetail: '北京市朝阳区建国路88号6栋301',
    serviceDate: '2026-07-20', serviceTimeSlot: 'MORNING', createTime: '2026-07-10T09:00:00+08:00',
    currentAssignment: null, assignments: [], serviceRecords: [],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-07-10T09:00:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付宝支付成功，等待商户派单', '2026-07-10T09:03:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
    ],
  },
  { orderId: 19997, orderNo: '2026071119997', serviceItemId: 103, serviceSpecId: 10301,
    serviceItemName: '日常起居照料', specName: '单次服务', specPrice: 120, totalAmount: 120, status: 1,
    merchantId: 20001, merchantName: '康宁护理中心', orderStatus: ORDER_STATUS.WAITING_DISPATCH,
    paymentStatus: PAYMENT_STATUS.PAID, assignmentStatus: ASSIGNMENT_STATUS.WAITING_ACCEPT,
    receiverName: '陈奶奶', receiverPhone: '136****1024', addressDetail: '北京市朝阳区望京西路18号2栋1203',
    serviceDate: '2026-07-16', serviceTimeSlot: 'MORNING', remark: '老人行动较慢，请耐心协助。',
    createTime: '2026-07-11T09:20:00+08:00',
    currentAssignment: { assignmentId: 60003, caregiverId: 50001, caregiverName: '王护理员', caregiverPhone: '138****8001', status: ASSIGNMENT_STATUS.WAITING_ACCEPT, dispatchTime: '2026-07-15T08:30:00+08:00', expireTime: '2026-07-15T10:30:00+08:00' },
    assignments: [
      { assignmentId: 60003, caregiverId: 50001, caregiverName: '王护理员', status: ASSIGNMENT_STATUS.WAITING_ACCEPT, dispatchTime: '2026-07-15T08:30:00+08:00', expireTime: '2026-07-15T10:30:00+08:00' },
    ],
    serviceRecords: [],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-07-11T09:20:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付成功，等待商户派单', '2026-07-11T09:22:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('dispatch', '商户已指派王护理员，等待接单', '2026-07-15T08:30:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_DISPATCH),
    ],
  },
  { orderId: 19996, orderNo: '2026071019996', serviceItemId: 401, serviceSpecId: 40101,
    serviceItemName: '静脉采血', specName: '单次服务', specPrice: 80, totalAmount: 80, status: 1,
    merchantId: 20001, merchantName: '康宁护理中心', orderStatus: ORDER_STATUS.WAITING_SERVICE,
    paymentStatus: PAYMENT_STATUS.PAID, assignmentStatus: ASSIGNMENT_STATUS.ACCEPTED,
    receiverName: '赵先生', receiverPhone: '139****7631', addressDetail: '北京市海淀区中关村南大街12号5单元602',
    serviceDate: '2026-07-15', serviceTimeSlot: 'AFTERNOON', remark: '请提前十分钟电话联系。',
    createTime: '2026-07-10T14:10:00+08:00',
    currentAssignment: { assignmentId: 60004, caregiverId: 50001, caregiverName: '王护理员', caregiverPhone: '138****8001', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-07-10T14:20:00+08:00', acceptedTime: '2026-07-10T14:25:00+08:00' },
    assignments: [
      { assignmentId: 60004, caregiverId: 50001, caregiverName: '王护理员', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-07-10T14:20:00+08:00', acceptedTime: '2026-07-10T14:25:00+08:00' },
    ],
    serviceRecords: [],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-07-10T14:10:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付成功，等待商户派单', '2026-07-10T14:12:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('accept', '护理人员已接单', '2026-07-10T14:25:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_SERVICE),
    ],
  },
  { orderId: 19995, orderNo: '2026070919995', serviceItemId: 102, serviceSpecId: 10201,
    serviceItemName: '压疮护理', specName: '单次服务', specPrice: 180, totalAmount: 180, status: 1,
    merchantId: 20001, merchantName: '康宁护理中心', orderStatus: ORDER_STATUS.IN_SERVICE,
    paymentStatus: PAYMENT_STATUS.PAID, assignmentStatus: ASSIGNMENT_STATUS.ACCEPTED,
    receiverName: '刘先生', receiverPhone: '137****3698', addressDetail: '北京市西城区广安门内大街66号院3号楼',
    serviceDate: '2026-07-15', serviceTimeSlot: 'MORNING', remark: '患者长期卧床，需要更换敷料。',
    createTime: '2026-07-09T11:00:00+08:00',
    currentAssignment: { assignmentId: 60005, caregiverId: 50001, caregiverName: '王护理员', caregiverPhone: '138****8001', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-07-09T11:10:00+08:00', acceptedTime: '2026-07-09T11:18:00+08:00' },
    assignments: [
      { assignmentId: 60005, caregiverId: 50001, caregiverName: '王护理员', status: ASSIGNMENT_STATUS.ACCEPTED, dispatchTime: '2026-07-09T11:10:00+08:00', acceptedTime: '2026-07-09T11:18:00+08:00' },
    ],
    serviceRecords: [
      { recordId: '19995-1', action: 'CHECK_IN', content: '护理人员提交异常签到，等待商户核验', abnormal: true, abnormalReason: '设备定位不可用', reviewStatus: 'PENDING', createTime: '2026-07-15T08:50:00+08:00' },
      { recordId: '19995-2', action: 'START', content: '护理服务已开始', createTime: '2026-07-15T09:00:00+08:00' },
    ],
    operationLogs: [
      createOperationLog('create', '用户创建订单', '2026-07-09T11:00:00+08:00', null, ORDER_STATUS.CREATED),
      createOperationLog('pay', '支付成功，等待商户派单', '2026-07-09T11:03:00+08:00', ORDER_STATUS.CREATED, ORDER_STATUS.WAITING_DISPATCH),
      createOperationLog('accept', '护理人员已接单', '2026-07-09T11:18:00+08:00', ORDER_STATUS.WAITING_DISPATCH, ORDER_STATUS.WAITING_SERVICE),
      createOperationLog('start', '护理服务已开始', '2026-07-15T09:00:00+08:00', ORDER_STATUS.WAITING_SERVICE, ORDER_STATUS.IN_SERVICE),
    ],
  },
]
presetOrders.forEach((o) => orders.push(o))

export function getMockOrder(orderId) {
  return orders.find((order) => order.orderId === Number(orderId)) || null
}

export function getMockOrders() {
  return orders
}

// ========== 接口 Mock ==========

// 1. 获取下单幂等令牌
Mock.mock(/\/api\/v1\/orders\/prepay-token/, 'post', () => {
  const token = 'pt_' + Random.string('lower', 24)
  const expireDate = new Date()
  expireDate.setMinutes(expireDate.getMinutes() + 10) // 10分钟有效
  prepayTokens.set(token, { expireTime: expireDate })

  console.log(`[Mock] 幂等令牌已生成: ${token}`)

  return {
    code: 0,
    message: 'success',
    data: {
      prepayToken: token,
      expireTime: expireDate.toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    },
  }
})

// 2. 创建订单
Mock.mock(/\/api\/v1\/orders$/, 'post', (options) => {
  const idempotentKey = (options.headers || {})['Idempotency-Key'] || ''
  const body = JSON.parse(options.body)

  // 幂等校验
  if (idempotentKey) {
    const existing = orders.find((o) => o._idempotentKey === idempotentKey)
    if (existing) {
      console.log(`[Mock] 幂等命中，返回已有订单: ${existing.orderId}`)
      return {
        code: 0,
        message: '下单成功',
        data: { orderId: existing.orderId, orderNo: existing.orderNo },
      }
    }
  }

  // 校验必填字段
  if (!body.serviceItemId || !body.serviceSpecId || !body.addressId || !body.serviceDate) {
    return { code: 1000, message: '缺少必填参数', data: null }
  }

  const orderId = nextOrderId++
  const orderNo = generateOrderNo()

  const serviceItem = getMockServiceItem(body.serviceItemId)
  const serviceSpec = getMockServiceSpec(body.serviceItemId, body.serviceSpecId)
  const address = getMockAddress(body.addressId)

  if (!serviceItem) return { code: 3003, message: '服务项目已下架', data: null }
  if (!serviceSpec) return { code: 3004, message: '规格已下架', data: null }
  if (!address) return { code: 3006, message: '地址不存在或已被删除', data: null }

  const order = {
    _idempotentKey: idempotentKey,
    orderId,
    orderNo,
    serviceItemId: serviceItem.itemId,
    serviceSpecId: serviceSpec.specId,
    serviceItemName: serviceItem.name,
    serviceCoverImage: serviceItem.coverImage,
    specName: serviceSpec.name,
    specPrice: serviceSpec.price,
    totalAmount: serviceSpec.price,
    status: 0,
    merchantId: serviceItem.merchantId || 20001,
    merchantName: serviceItem.merchantName || '康宁护理中心',
    orderStatus: ORDER_STATUS.CREATED,
    paymentStatus: PAYMENT_STATUS.UNPAID,
    assignmentStatus: ASSIGNMENT_STATUS.UNASSIGNED,
    addressId: address.addressId,
    receiverName: address.receiverName,
    receiverPhone: address.receiverPhone,
    addressDetail: `${address.province}${address.city}${address.district}${address.detailAddress}`,
    serviceDate: body.serviceDate,
    serviceTimeSlot: body.serviceTimeSlot || 'MORNING',
    remark: body.remark || '',
    createTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    currentAssignment: null,
    assignments: [],
    serviceRecords: [],
    operationLogs: [
      createOperationLog(
        'create',
        '用户创建订单',
        new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
        null,
        ORDER_STATUS.CREATED
      ),
    ],
  }
  orders.push(order)

  console.log(`[Mock] 订单已创建: orderId=${orderId}, orderNo=${orderNo}`)

  return {
    code: 0,
    message: '下单成功',
    data: { orderId, orderNo },
  }
})

// 3. 订单列表
Mock.mock(/\/api\/v1\/orders\?/, 'get', (options) => {
  const url = options.url
  const status = getQueryParam(url, 'status')
  const page = parseInt(getQueryParam(url, 'page') || '1')
  const size = Math.min(parseInt(getQueryParam(url, 'size') || '20'), 50)

  let list = [...orders]

  if (status !== null && status !== undefined) {
    list = list.filter((o) => o.status === parseInt(status))
  }

  // 按 createTime 倒序
  list.sort((a, b) => b.orderId - a.orderId)

  const start = (page - 1) * size
  const paged = list.slice(start, start + size).map((o) => ({
    orderId: o.orderId,
    orderNo: o.orderNo,
    serviceItemName: o.serviceItemName,
    serviceItemId: o.serviceItemId,
    serviceSpecId: o.serviceSpecId,
    serviceCoverImage: o.serviceCoverImage,
    specName: o.specName,
    specPrice: o.specPrice,
    totalAmount: o.totalAmount,
    status: o.status,
    orderStatus: o.orderStatus,
    paymentStatus: o.paymentStatus,
    assignmentStatus: o.assignmentStatus,
    merchantId: o.merchantId,
    merchantName: o.merchantName,
    currentAssignment: o.currentAssignment,
    serviceDate: o.serviceDate,
    serviceTimeSlot: o.serviceTimeSlot,
    receiverName: o.receiverName,
    receiverPhone: o.receiverPhone,
    addressDetail: o.addressDetail,
    createTime: o.createTime,
  }))

  return {
    code: 0,
    message: 'success',
    data: { list: paged, total: list.length, page, size },
  }
})

// 4. 订单详情
Mock.mock(/\/api\/v1\/orders\/\d+$/, 'get', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/orders\/(\d+)/)
  const orderId = idMatch ? parseInt(idMatch[1]) : null
  const order = orders.find((o) => o.orderId === orderId)

  if (!order) {
    return { code: 3007, message: '订单不存在', data: null }
  }

  expireAssignmentIfNeeded(order)
  refreshRefund(order)

  // 返回快照版本（不含 _idempotentKey）
  const detail = { ...order }
  delete detail._idempotentKey
  return { code: 0, message: 'success', data: detail }
})

// 5. 取消订单
Mock.mock(/\/api\/v1\/orders\/\d+\/cancel/, 'post', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/orders\/(\d+)\/cancel/)
  const orderId = idMatch ? parseInt(idMatch[1]) : null
  const order = orders.find((o) => o.orderId === orderId)

  if (!order) {
    return { code: 3007, message: '订单不存在', data: null }
  }

  const body = options.body ? JSON.parse(options.body) : {}
  const preview = calculateCancellation(order)
  if (!preview.cancellable) return { code: 3009, message: preview.ruleDescription, data: null }
  order.cancelReason = body.cancelReason || ''
  if (order.currentAssignment) {
    order.currentAssignment.status = ASSIGNMENT_STATUS.CANCELED
    order.assignmentStatus = ASSIGNMENT_STATUS.CANCELED
  }
  order.paymentStatus = preview.refundAmount > 0 ? PAYMENT_STATUS.REFUNDING : PAYMENT_STATUS.UNPAID
  updateOrderStatus(order, ORDER_STATUS.CANCELED, 'cancel', body.cancelReason || '用户取消订单')
  const refund = createRefundForCancellation(order, preview, body.cancelReason || '用户取消订单')

  return {
    code: 0,
    message: '订单已取消',
    data: { orderId: order.orderId, status: order.status, refundStatus: refund.status, refund },
  }
})

// 6. 发起支付（返回模拟支付宝支付参数）
Mock.mock(/\/api\/v1\/orders\/\d+\/pay/, 'post', (options) => {
  const idempotentKey = (options.headers || {})['Idempotency-Key'] || ''
  const idMatch = options.url.match(/\/api\/v1\/orders\/(\d+)\/pay/)
  const orderId = idMatch ? parseInt(idMatch[1]) : null
  const order = orders.find((o) => o.orderId === orderId)

  if (!order) {
    return { code: 3007, message: '订单不存在', data: null }
  }
  if (!idempotentKey) {
    return { code: 1000, message: '支付缺少 Idempotency-Key', data: null }
  }

  if (order.orderStatus !== ORDER_STATUS.CREATED || order.paymentStatus !== PAYMENT_STATUS.UNPAID) {
    return { code: 3009, message: '当前状态不可支付', data: null }
  }

  order.paymentStatus = PAYMENT_STATUS.PAID
  updateOrderStatus(order, ORDER_STATUS.WAITING_DISPATCH, 'pay', '支付成功，等待商户派单')

  console.log(`[Mock] 支付成功: orderId=${orderId}`)

  return {
    code: 0,
    message: 'success',
    data: {
      orderId,
      orderNo: order.orderNo,
      payAmount: order.totalAmount,
      payStatus: 'SUCCESS',
      payChannel: 'alipay',
      mock: true,
      payParams: null,
      tradeNo: 'mock_trade_' + Random.string('number', 16),
    },
  }
})

// 7. 用户确认服务完成（兼容旧 complete 路径）
Mock.mock(/\/api\/v1\/orders\/\d+\/(confirm|complete)/, 'post', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/orders\/(\d+)\/(?:confirm|complete)/)
  const orderId = idMatch ? parseInt(idMatch[1]) : null
  const order = orders.find((item) => item.orderId === orderId)
  if (!order) return { code: 3007, message: '订单不存在', data: null }
  if (order.orderStatus !== ORDER_STATUS.WAITING_CONFIRM) {
    return { code: 3009, message: '护理人员尚未结束服务，当前不可确认', data: null }
  }

  updateOrderStatus(order, ORDER_STATUS.COMPLETED, 'confirm', '用户确认服务完成')
  return { code: 0, message: '服务已完成', data: order }
})

// 8. 商户查询候选护理人员
Mock.mock(/\/api\/v1\/merchants\/orders\/\d+\/candidates(?:\?|$)/, 'get', (options) => {
  const orderId = Number(options.url.match(/\/orders\/(\d+)\/candidates/)?.[1])
  const result = getMerchantOrder(options, orderId)
  if (result.error) return result.error
  const { order } = result
  if (order.orderStatus !== ORDER_STATUS.WAITING_DISPATCH) {
    return { code: 1007, message: '当前订单无需派单', data: null }
  }
  const list = getActiveMerchantCaregivers(result.merchantId).map((item) => ({
    caregiverId: item.caregiverId,
    name: item.name,
    phone: item.phone,
    avatar: item.avatar,
    rating: item.rating,
    completedOrders: item.completedOrders,
    distanceKm: item.caregiverId === 50001 ? 2.4 : 4.1,
    skills: item.skills,
    available: item.available,
    relationId: item.relationId,
    ...evaluateCaregiverForOrder(item.caregiverId, order),
  }))
  if (!list.some((item) => item.eligible)) createOrderException(order, EXCEPTION_TYPE.NO_CAREGIVER, '当前团队没有同时满足排班、区域和接单上限的护理人员，请调整排班、邀请人员或联系顾客改期。')
  return { code: 0, message: 'success', data: { list } }
})

// 9. 商户查询派单记录
Mock.mock(/\/api\/v1\/merchants\/orders\/\d+\/assignments(?:\?|$)/, 'get', (options) => {
  const orderId = Number(options.url.match(/\/orders\/(\d+)\/assignments/)?.[1])
  const result = getMerchantOrder(options, orderId)
  if (result.error) return result.error
  const { order } = result
  return { code: 0, message: 'success', data: { list: order.assignments || [] } }
})

function handleDispatch(options, isRedispatch = false) {
  const orderId = Number(options.url.match(/\/orders\/(\d+)\/(?:dispatch|redispatch)/)?.[1])
  const result = getMerchantOrder(options, orderId)
  if (result.error) return result.error
  const { order, merchantId } = result
  if (order.orderStatus !== ORDER_STATUS.WAITING_DISPATCH) {
    return { code: 1007, message: '当前订单不可派单', data: null }
  }

  const body = JSON.parse(options.body || '{}')
  const caregiver = getActiveMerchantCaregivers(merchantId).find((item) => item.caregiverId === Number(body.caregiverId))
  if (!caregiver?.available) return { code: 5001, message: '护理人员当前不可接单', data: null }
  const availability = evaluateCaregiverForOrder(caregiver.caregiverId, order)
  if (!availability.eligible) return { code: 5005, message: availability.conflictReasons.join('、'), data: { conflicts: availability.conflicts } }

  if (isRedispatch && order.currentAssignment) {
    order.currentAssignment.status = ASSIGNMENT_STATUS.CANCELED
  }

  const assignment = {
    assignmentId: nextAssignmentId++,
    caregiverId: caregiver.caregiverId,
    caregiverName: caregiver.name,
    caregiverPhone: caregiver.phone,
    status: ASSIGNMENT_STATUS.WAITING_ACCEPT,
    dispatchTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    expireTime: new Date(Date.now() + 15 * 60 * 1000).toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    remark: body.remark || '',
  }
  order.assignments.push(assignment)
  order.currentAssignment = assignment
  order.assignmentStatus = ASSIGNMENT_STATUS.WAITING_ACCEPT
  order.operationLogs.push(createOperationLog(
    isRedispatch ? 'redispatch' : 'dispatch',
    `${isRedispatch ? '重新' : ''}指派${caregiver.name}，等待接单`,
    assignment.dispatchTime,
    order.orderStatus,
    order.orderStatus
  ))
  return { code: 0, message: '派单成功', data: assignment }
}

Mock.mock(/\/api\/v1\/merchants\/orders\/\d+\/dispatch$/, 'post', (options) => handleDispatch(options))
Mock.mock(/\/api\/v1\/merchants\/orders\/\d+\/redispatch$/, 'post', (options) => handleDispatch(options, true))

// 商户工作台统计
Mock.mock(/\/api\/v1\/merchants\/dashboard(?:\?|$)/, 'get', (options) => {
  const merchantId = getMerchantIdFromRequest(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const merchantOrders = orders.filter((order) => order.merchantId === merchantId)
  const today = '2026-07-15'
  return {
    code: 0,
    message: 'success',
    data: {
      merchantId,
      merchantName: '康宁护理中心',
      waitingDispatch: merchantOrders.filter((order) => order.orderStatus === ORDER_STATUS.WAITING_DISPATCH && order.assignmentStatus !== ASSIGNMENT_STATUS.WAITING_ACCEPT).length,
      waitingAccept: merchantOrders.filter((order) => order.assignmentStatus === ASSIGNMENT_STATUS.WAITING_ACCEPT).length,
      inService: merchantOrders.filter((order) => order.orderStatus === ORDER_STATUS.IN_SERVICE).length,
      todayServices: merchantOrders.filter((order) => order.serviceDate === today).length,
      completed: merchantOrders.filter((order) => order.orderStatus === ORDER_STATUS.COMPLETED).length,
      totalOrders: merchantOrders.length,
      monthRevenue: merchantOrders.filter((order) => order.paymentStatus === PAYMENT_STATUS.PAID).reduce((sum, order) => sum + Number(order.totalAmount || 0), 0),
    },
  }
})

// 商户订单列表
Mock.mock(/\/api\/v1\/merchants\/orders(?:\?|$)/, 'get', (options) => {
  const merchantId = getMerchantIdFromRequest(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const orderStatus = getQueryParam(options.url, 'orderStatus')
  const assignmentStatus = getQueryParam(options.url, 'assignmentStatus')
  const keyword = getQueryParam(options.url, 'keyword')?.trim()
  const page = Number(getQueryParam(options.url, 'page') || 1)
  const size = Math.min(Number(getQueryParam(options.url, 'size') || 20), 50)
  let list = orders.filter((order) => order.merchantId === merchantId)
  list.forEach(expireAssignmentIfNeeded)
  if (orderStatus) list = list.filter((order) => order.orderStatus === orderStatus)
  if (assignmentStatus) list = list.filter((order) => order.assignmentStatus === assignmentStatus)
  if (keyword) {
    list = list.filter((order) => [order.orderNo, order.serviceItemName, order.receiverName].some((value) => String(value || '').includes(keyword)))
  }
  list.sort((a, b) => b.orderId - a.orderId)
  const start = (page - 1) * size
  return { code: 0, message: 'success', data: { list: list.slice(start, start + size), total: list.length, page, size } }
})

// 商户订单详情
Mock.mock(/\/api\/v1\/merchants\/orders\/\d+$/, 'get', (options) => {
  const orderId = Number(options.url.match(/\/orders\/(\d+)$/)?.[1])
  const result = getMerchantOrder(options, orderId)
  if (result.error) return result.error
  return { code: 0, message: 'success', data: result.order }
})

// 10. 护理人员任务列表和详情
Mock.mock(/\/api\/v1\/caregivers\/tasks(?:\?|$)/, 'get', (options) => {
  const caregiverId = getCaregiverIdFromRequest(options)
  const list = orders.filter((order) => (
    !caregiverId || order.currentAssignment?.caregiverId === caregiverId
  ))
  return { code: 0, message: 'success', data: { list, total: list.length } }
})

Mock.mock(/\/api\/v1\/caregivers\/tasks\/\d+$/, 'get', (options) => {
  const orderId = Number(options.url.match(/\/tasks\/(\d+)/)?.[1])
  const order = getMockOrder(orderId)
  if (!order) return { code: 3007, message: '订单不存在', data: null }
  const caregiverId = getCaregiverIdFromRequest(options)
  if (caregiverId && order.currentAssignment?.caregiverId !== caregiverId) {
    return { code: 3008, message: '无权查看该任务', data: null }
  }
  return { code: 0, message: 'success', data: order }
})

// 11. 护理人员接受或拒绝派单
Mock.mock(/\/api\/v1\/caregivers\/assignments\/\d+\/(accept|reject)/, 'post', (options) => {
  const assignmentId = Number(options.url.match(/\/assignments\/(\d+)/)?.[1])
  const action = options.url.match(/\/(accept|reject)$/)?.[1]
  const order = orders.find((item) => item.currentAssignment?.assignmentId === assignmentId)
  if (!order) return { code: 5002, message: '派单记录不存在', data: null }
  if (order.assignmentStatus !== ASSIGNMENT_STATUS.WAITING_ACCEPT) {
    return { code: 1007, message: '该派单已处理', data: null }
  }

  if (action === 'reject') {
    const body = JSON.parse(options.body || '{}')
    order.currentAssignment.status = ASSIGNMENT_STATUS.REJECTED
    order.currentAssignment.rejectReason = body.reason || '护理人员拒绝接单'
    order.assignmentStatus = ASSIGNMENT_STATUS.REJECTED
    order.operationLogs.push(createOperationLog('reject', '护理人员拒绝接单，等待重新派单', new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'), order.orderStatus, order.orderStatus))
    return { code: 0, message: '已拒绝派单', data: order.currentAssignment }
  }

  order.currentAssignment.status = ASSIGNMENT_STATUS.ACCEPTED
  order.currentAssignment.acceptedTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
  order.assignmentStatus = ASSIGNMENT_STATUS.ACCEPTED
  updateOrderStatus(order, ORDER_STATUS.WAITING_SERVICE, 'accept', '护理人员已接单')
  return { code: 0, message: '接单成功', data: order.currentAssignment }
})

// 12. 护理人员履约动作
Mock.mock(/\/api\/v1\/caregivers\/orders\/\d+\/(depart|check-in|start|finish)/, 'post', (options) => {
  const orderId = Number(options.url.match(/\/orders\/(\d+)/)?.[1])
  const action = options.url.match(/\/(depart|check-in|start|finish)$/)?.[1]
  const order = getMockOrder(orderId)
  if (!order) return { code: 3007, message: '订单不存在', data: null }
  if (order.assignmentStatus !== ASSIGNMENT_STATUS.ACCEPTED) {
    return { code: 1007, message: '护理人员尚未接单', data: null }
  }

  const body = JSON.parse(options.body || '{}')
  if (action === 'depart') {
    if (order.orderStatus !== ORDER_STATUS.WAITING_SERVICE) return { code: 1007, message: '当前不可出发', data: null }
    const record = appendServiceRecord(order, 'DEPART', '护理人员已出发', body)
    order.operationLogs.push(createOperationLog('depart', '护理人员已出发', record.createTime, order.orderStatus, order.orderStatus))
    return { code: 0, message: '已记录出发', data: record }
  }
  if (action === 'check-in') {
    if (order.orderStatus !== ORDER_STATUS.WAITING_SERVICE) return { code: 1007, message: '当前不可签到', data: null }
    const record = appendServiceRecord(order, 'CHECK_IN', '护理人员已到达并签到', body)
    if (body.abnormal) {
      record.content = '护理人员提交异常签到，等待商户核验'
      record.reviewStatus = 'PENDING'
      registerAbnormalCheckIn(order, body)
    }
    order.operationLogs.push(createOperationLog('check-in', record.content, record.createTime, order.orderStatus, order.orderStatus))
    return { code: 0, message: body.abnormal ? '异常签到已提交审核' : '签到成功', data: record }
  }
  if (action === 'start') {
    if (order.orderStatus !== ORDER_STATUS.WAITING_SERVICE) return { code: 1007, message: '当前不可开始服务', data: null }
    if (!order.serviceRecords.some((record) => record.action === 'CHECK_IN')) {
      return { code: 5004, message: '请先完成到达签到', data: null }
    }
    const record = appendServiceRecord(order, 'START', '护理服务已开始', body)
    updateOrderStatus(order, ORDER_STATUS.IN_SERVICE, 'start', '护理服务已开始')
    return { code: 0, message: '服务已开始', data: record }
  }
  if (order.orderStatus !== ORDER_STATUS.IN_SERVICE) return { code: 1007, message: '当前不可结束服务', data: null }
  if (!body.summary?.trim()) return { code: 1000, message: '请填写本次服务摘要', data: null }
  const record = appendServiceRecord(order, 'FINISH', '护理服务已结束，等待顾客确认', body)
  updateOrderStatus(order, ORDER_STATUS.WAITING_CONFIRM, 'finish', '护理人员已结束服务，等待顾客确认')
  return { code: 0, message: '服务记录已提交', data: record }
})

// ========== 工具 ==========
function getQueryParam(url, param) {
  const regex = new RegExp(`[?&]${param}=([^&]*)`)
  const match = url.match(regex)
  return match ? decodeURIComponent(match[1]) : null
}

console.log('[Mock] 订单模块已加载 (order-service / orders)')
