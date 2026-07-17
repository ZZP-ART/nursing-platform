import Mock from 'mockjs'
import { COMPLAINT_DECISION, COMPLAINT_STATUS, getComplaintStatusMeta } from '@/constants/complaint-status.js'
import { ORDER_STATUS, PAYMENT_STATUS, deriveLegacyStatus } from '@/constants/order-status.js'
import { REFUND_STATUS } from '@/constants/aftersales-status.js'
import { getToken } from '@/utils/storage.js'
import { refunds, now } from './aftersales-state.js'
import { getMockOrder } from './order.js'
import { getMockMerchantIdByUserId } from './user.js'

const COMPLAINT_TYPES = { 1: '服务质量', 2: '服务态度', 3: '乱收费', 4: '其他' }
export const complaints = [
  {
    complaintId: 40001, complaintNo: 'CP202607040001', orderId: 20000, userId: 10001,
    merchantId: 20001, merchantName: '康宁护理中心', type: 1, typeText: '服务质量',
    status: COMPLAINT_STATUS.RESOLVED, content: '护士上门时间比预约晚了将近一小时，而且操作不太规范。',
    images: [], customerEvidence: [], merchantStatement: '护理人员因上一单延时到达，商户已完成内部核查并向顾客致歉。',
    merchantEvidence: [{ name: '护理人员说明', value: '上一单服务延长导致迟到约45分钟' }],
    decision: { action: COMPLAINT_DECISION.PARTIAL_REFUND, label: '部分退款', refundAmount: 20, remark: '迟到属实，退还部分服务费并要求商户整改。' },
    createTime: '2026-07-04T14:00:00+08:00', updateTime: '2026-07-04T16:00:00+08:00',
    tracks: [
      { trackId: 3, action: 'ARBITRATE', operator: '平台仲裁员', content: '核验双方材料后，裁定退还20元并要求商户整改。', createTime: '2026-07-04T16:00:00+08:00' },
      { trackId: 2, action: 'MERCHANT_EVIDENCE', operator: '康宁护理中心', content: '商户已提交迟到原因和护理人员说明。', createTime: '2026-07-04T15:20:00+08:00' },
      { trackId: 1, action: 'SUBMIT', operator: '顾客', content: '投诉已提交，等待商户响应。', createTime: '2026-07-04T14:00:00+08:00' },
    ],
  },
  {
    complaintId: 40002, complaintNo: 'CP202607060002', orderId: 20000, userId: 10001,
    merchantId: 20001, merchantName: '康宁护理中心', type: 3, typeText: '乱收费',
    status: COMPLAINT_STATUS.ADMIN_REVIEW, content: '预约时说好80元一次，但上门后要求额外收取材料费，未提前告知。',
    images: [], customerEvidence: [{ name: '顾客补充说明', value: '现场被要求另付30元材料费' }],
    merchantStatement: '护理人员反馈现场使用了额外耗材，但顾客认为未提前告知，双方未达成一致。',
    merchantEvidence: [{ name: '耗材清单', value: '一次性采血耗材30元' }], decision: null,
    createTime: '2026-07-06T10:00:00+08:00', updateTime: '2026-07-06T11:30:00+08:00',
    tracks: [
      { trackId: 2, action: 'ESCALATE', operator: '康宁护理中心', content: '商户已提交耗材清单，双方存在争议，申请平台仲裁。', createTime: '2026-07-06T11:30:00+08:00' },
      { trackId: 1, action: 'SUBMIT', operator: '顾客', content: '投诉已提交，等待商户响应。', createTime: '2026-07-06T10:00:00+08:00' },
    ],
  },
]

complaints.forEach((complaint) => {
  const order = getMockOrder(complaint.orderId)
  if (!order) return
  order.complaintId = complaint.complaintId
  if (![COMPLAINT_STATUS.RESOLVED, COMPLAINT_STATUS.CLOSED].includes(complaint.status)) {
    order.preDisputeStatus = order.orderStatus
    order.orderStatus = ORDER_STATUS.DISPUTED
    order.status = deriveLegacyStatus(order.orderStatus, order.paymentStatus)
  }
})

let nextComplaintId = 40003
let nextTrackId = 10
const submittedKeys = new Set()

function clone(value) { return JSON.parse(JSON.stringify(value)) }
function getQueryParam(url, param) { const match = String(url).match(new RegExp(`[?&]${param}=([^&]*)`)); return match ? decodeURIComponent(match[1]) : null }
function getMerchantId(options) { const auth = options.headers?.Authorization || options.headers?.authorization || ''; const token = auth.replace('Bearer ', '') || getToken(); const userId = Number(token.match(/mock_jwt_(\d+)_MERCHANT_MEMBER_/)?.[1] || 0); return getMockMerchantIdByUserId(userId) }
function appendTrack(complaint, action, operator, content) { complaint.updateTime = now(); complaint.tracks.unshift({ trackId: nextTrackId++, action, operator, content, createTime: complaint.updateTime }) }
function summary(complaint) { const order = getMockOrder(complaint.orderId); return { complaintId: complaint.complaintId, complaintNo: complaint.complaintNo, orderId: complaint.orderId, orderNo: order?.orderNo, serviceItemName: order?.serviceItemName, receiverName: order?.receiverName, merchantName: complaint.merchantName, type: complaint.type, typeText: complaint.typeText, status: complaint.status, statusText: getComplaintStatusMeta(complaint.status).label, content: complaint.content, createTime: complaint.createTime, updateTime: complaint.updateTime } }
function detail(complaint) { return { ...clone(complaint), ...summary(complaint), order: clone(getMockOrder(complaint.orderId)) } }

Mock.mock(/\/api\/v1\/complaints$/, 'post', (options) => {
  const body = JSON.parse(options.body || '{}'); const requestKey = (options.headers || {})['Idempotency-Key'] || ''
  if (!requestKey) return { code: 1000, message: '缺少 Idempotency-Key', data: null }
  if (!body.orderId || !body.type || !body.content?.trim()) return { code: 1000, message: '订单ID、投诉类型和内容为必填', data: null }
  const order = getMockOrder(body.orderId)
  if (!order) return { code: 1005, message: '订单不存在', data: null }
  if (![ORDER_STATUS.IN_SERVICE, ORDER_STATUS.WAITING_CONFIRM, ORDER_STATUS.COMPLETED].includes(order.orderStatus)) return { code: 1000, message: '当前订单阶段不可提交投诉', data: null }
  if (complaints.some((item) => item.orderId === body.orderId && ![COMPLAINT_STATUS.RESOLVED, COMPLAINT_STATUS.CLOSED].includes(item.status))) return { code: 1006, message: '该订单已有处理中投诉', data: null }
  if (submittedKeys.has(requestKey)) return { code: 1006, message: '请勿重复提交投诉', data: null }
  submittedKeys.add(requestKey)
  const complaintId = nextComplaintId++; const createTime = now()
  const complaint = { complaintId, complaintNo: `CP${createTime.slice(0, 10).replaceAll('-', '')}${String(complaintId).slice(-4)}`, orderId: order.orderId, userId: 10001, merchantId: order.merchantId, merchantName: order.merchantName, type: body.type, typeText: COMPLAINT_TYPES[body.type] || '其他', status: COMPLAINT_STATUS.SUBMITTED, content: body.content.trim(), images: body.images || [], customerEvidence: (body.images || []).map((value, index) => ({ name: `顾客截图${index + 1}`, value })), merchantStatement: '', merchantEvidence: [], decision: null, createTime, updateTime: createTime, tracks: [{ trackId: nextTrackId++, action: 'SUBMIT', operator: '顾客', content: '投诉已提交，等待商户响应。', createTime }] }
  complaints.unshift(complaint); order.preDisputeStatus = order.orderStatus; order.orderStatus = ORDER_STATUS.DISPUTED; order.status = deriveLegacyStatus(order.orderStatus, order.paymentStatus); order.complaintId = complaintId
  order.operationLogs.push({ action: 'complaint', remark: '顾客提交投诉，订单进入争议处理', createTime, fromOrderStatus: order.preDisputeStatus, toOrderStatus: ORDER_STATUS.DISPUTED })
  return { code: 0, message: '投诉提交成功', data: { complaintId } }
})

Mock.mock(/\/api\/v1\/complaints(?:\?|$)/, 'get', (options) => { const page = Number(getQueryParam(options.url, 'page') || 1); const size = Math.min(Number(getQueryParam(options.url, 'size') || 20), 50); const list = [...complaints].sort((a, b) => b.updateTime.localeCompare(a.updateTime)); return { code: 0, message: 'success', data: { list: list.slice((page - 1) * size, page * size).map(summary), total: list.length, page, size } } })
Mock.mock(/\/api\/v1\/complaints\/\d+\/tracks$/, 'get', (options) => { const complaint = complaints.find((item) => item.complaintId === Number(options.url.match(/complaints\/(\d+)/)?.[1])); return complaint ? { code: 0, message: 'success', data: detail(complaint) } : { code: 4006, message: '投诉记录不存在', data: null } })

Mock.mock(/\/api\/v1\/merchant\/complaints(?:\?|$)/, 'get', (options) => { const merchantId = getMerchantId(options); if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }; const status = getQueryParam(options.url, 'status'); let list = complaints.filter((item) => item.merchantId === merchantId); if (status !== null && status !== '') list = list.filter((item) => item.status === Number(status)); return { code: 0, message: 'success', data: { list: list.map(summary), total: list.length } } })
Mock.mock(/\/api\/v1\/merchant\/complaints\/\d+$/, 'get', (options) => { const merchantId = getMerchantId(options); const complaint = complaints.find((item) => item.complaintId === Number(options.url.match(/complaints\/(\d+)$/)?.[1]) && item.merchantId === merchantId); return complaint ? { code: 0, message: 'success', data: detail(complaint) } : { code: 4007, message: '无权查看该投诉', data: null } })
Mock.mock(/\/api\/v1\/merchant\/complaints\/\d+\/(resolve|escalate)$/, 'post', (options) => {
  const merchantId = getMerchantId(options); const [, id, action] = options.url.match(/complaints\/(\d+)\/(resolve|escalate)$/) || []; const complaint = complaints.find((item) => item.complaintId === Number(id) && item.merchantId === merchantId)
  if (!complaint) return { code: 4007, message: '无权处理该投诉', data: null }
  if (![COMPLAINT_STATUS.SUBMITTED, COMPLAINT_STATUS.MERCHANT_PROCESSING].includes(complaint.status)) return { code: 1007, message: '当前投诉不可由商户处理', data: null }
  const body = JSON.parse(options.body || '{}'); if (!body.statement?.trim()) return { code: 1000, message: '请填写商户处理说明', data: null }
  complaint.merchantStatement = body.statement.trim(); complaint.merchantEvidence = body.evidence || []
  if (action === 'resolve') { complaint.status = COMPLAINT_STATUS.RESOLVED; complaint.decision = { action: COMPLAINT_DECISION.NO_REFUND, label: '协商解决', refundAmount: 0, remark: body.statement.trim() }; const order = getMockOrder(complaint.orderId); order.orderStatus = order.preDisputeStatus || ORDER_STATUS.COMPLETED; order.status = deriveLegacyStatus(order.orderStatus, order.paymentStatus); appendTrack(complaint, 'MERCHANT_RESOLVE', '康宁护理中心', '商户提交处理方案，顾客投诉已协商解决。') }
  else { complaint.status = COMPLAINT_STATUS.ADMIN_REVIEW; appendTrack(complaint, 'ESCALATE', '康宁护理中心', '商户已提交说明和凭证，申请平台仲裁。') }
  return { code: 0, message: action === 'resolve' ? '投诉已协商解决' : '已提交平台仲裁', data: detail(complaint) }
})

export function arbitrateComplaint(complaint, body, operator = '平台仲裁员') {
  const order = getMockOrder(complaint.orderId); const refundAmount = Math.max(0, Math.min(Number(body.refundAmount || 0), Number(order?.totalAmount || 0)))
  complaint.status = COMPLAINT_STATUS.RESOLVED; complaint.decision = { action: body.decision, label: body.decision === COMPLAINT_DECISION.FULL_REFUND ? '全额退款' : body.decision === COMPLAINT_DECISION.PARTIAL_REFUND ? '部分退款' : '不予退款', refundAmount, remark: body.remark }; appendTrack(complaint, 'ARBITRATE', operator, body.remark)
  if (order) {
    order.orderStatus = body.decision === COMPLAINT_DECISION.FULL_REFUND ? ORDER_STATUS.CLOSED : (order.preDisputeStatus || ORDER_STATUS.COMPLETED)
    if (refundAmount > 0) { const refund = { refundId: `RF${order.orderId}${Date.now()}`, orderId: order.orderId, status: REFUND_STATUS.PROCESSING, totalAmount: Number(order.totalAmount), refundAmount, deductionAmount: Number(order.totalAmount) - refundAmount, reason: `投诉仲裁：${body.remark}`, applyTime: now(), successTime: null, mockCompleteAt: Date.now() + 8000, finalPaymentStatus: refundAmount < Number(order.totalAmount) ? PAYMENT_STATUS.PARTIALLY_REFUNDED : PAYMENT_STATUS.REFUNDED, records: [{ action: 'APPLY', title: '平台仲裁退款已提交', remark: body.remark, time: now() }] }; refunds.set(order.orderId, refund); order.refund = refund; order.paymentStatus = PAYMENT_STATUS.REFUNDING }
    order.status = deriveLegacyStatus(order.orderStatus, order.paymentStatus); order.complaintId = complaint.complaintId; order.operationLogs.push({ action: 'complaint-arbitration', remark: body.remark, createTime: now(), fromOrderStatus: ORDER_STATUS.DISPUTED, toOrderStatus: order.orderStatus })
  }
  return detail(complaint)
}

console.log('[Mock] 投诉举证与仲裁模块已加载')
