import Mock from 'mockjs'
import { NOTIFICATION_CATEGORY } from '@/constants/notification.js'
import { ROLES } from '@/constants/roles.js'

const notifications = [
  { notificationId: 71001, userId: 10001, role: ROLES.CUSTOMER, category: NOTIFICATION_CATEGORY.COMPLAINT, title: '投诉已进入平台仲裁', content: '商户已提交耗材清单和处理说明，平台正在核验双方材料。', read: false, createTime: '2026-07-15T13:20:00+08:00', target: { type: 'CUSTOMER_COMPLAINT', id: 40002 } },
  { notificationId: 71002, userId: 10001, role: ROLES.CUSTOMER, category: NOTIFICATION_CATEGORY.ORDER, title: '护理人员已结束服务', content: '艾灸调理已完成，请查看服务记录并确认服务结果。', read: false, createTime: '2026-07-14T15:02:00+08:00', target: { type: 'CUSTOMER_ORDER', id: 19999 } },
  { notificationId: 71003, userId: 10001, role: ROLES.CUSTOMER, category: NOTIFICATION_CATEGORY.REFUND, title: '退款申请处理中', content: '订单取消申请已提交，退款结果可在订单详情中查看。', read: true, readTime: '2026-07-13T11:30:00+08:00', createTime: '2026-07-13T11:20:00+08:00', target: { type: 'CUSTOMER_ORDER', id: 19998 } },
  { notificationId: 72001, userId: 10001, role: ROLES.CAREGIVER, category: NOTIFICATION_CATEGORY.ASSIGNMENT, title: '你有新的上门任务', content: '日常起居照料，预约 7月16日上午，请及时确认是否接单。', read: false, createTime: '2026-07-15T08:30:00+08:00', target: { type: 'CAREGIVER_TASK', id: 19997 } },
  { notificationId: 72002, userId: 10001, role: ROLES.CAREGIVER, category: NOTIFICATION_CATEGORY.EXCEPTION, title: '异常签到等待商户核验', content: '压疮护理异常签到已提交，商户将在核验后更新结果。', read: false, createTime: '2026-07-15T08:51:00+08:00', target: { type: 'CAREGIVER_TASK', id: 19995 } },
  { notificationId: 72003, userId: 10002, role: ROLES.CAREGIVER, category: NOTIFICATION_CATEGORY.SERVICE, title: '明日服务提醒', content: '艾灸调理预约在明日下午，请提前确认出行安排。', read: false, createTime: '2026-07-13T18:00:00+08:00', target: { type: 'CAREGIVER_TASK', id: 19999 } },
  { notificationId: 73001, userId: 10003, role: ROLES.MERCHANT_MEMBER, category: NOTIFICATION_CATEGORY.EXCEPTION, title: '接单超时需要处理', content: '日常起居照料派单已超时，请重新派单或联系顾客调整时间。', read: false, createTime: '2026-07-15T10:31:00+08:00', target: { type: 'MERCHANT_EXCEPTION', id: 95001 } },
  { notificationId: 73002, userId: 10003, role: ROLES.MERCHANT_MEMBER, category: NOTIFICATION_CATEGORY.COMPLAINT, title: '顾客投诉等待响应', content: '顾客反馈额外耗材收费未提前告知，请提交说明和相关凭证。', read: false, createTime: '2026-07-15T09:45:00+08:00', target: { type: 'MERCHANT_COMPLAINT', id: 40002 } },
  { notificationId: 73003, userId: 10003, role: ROLES.MERCHANT_MEMBER, category: NOTIFICATION_CATEGORY.REVIEW, title: '服务审核已通过', content: '居家用药清单整理已通过平台审核，可在服务管理中查看。', read: true, readTime: '2026-07-14T17:00:00+08:00', createTime: '2026-07-14T16:20:00+08:00', target: { type: 'MERCHANT_SERVICES' } },
]

function getSession(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const token = auth.replace('Bearer ', '')
  const userId = Number(token.match(/mock_jwt_(\d+)/)?.[1] || 0)
  const role = token.match(/mock_jwt_\d+_(CUSTOMER|CAREGIVER|MERCHANT_MEMBER)_/)?.[1] || ''
  return { userId, role }
}
function now() { return new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00') }
function clone(value) { return JSON.parse(JSON.stringify(value)) }
function getQueryParam(url, name) { const match = String(url).match(new RegExp(`[?&]${name}=([^&]*)`)); return match ? decodeURIComponent(match[1]) : '' }
function owned(options) { const session = getSession(options); return notifications.filter((item) => item.userId === session.userId && item.role === session.role) }

Mock.mock(/\/api\/v1\/notifications\/unread-count$/, 'get', (options) => { const list = owned(options); return { code: 0, message: 'success', data: { unreadCount: list.filter((item) => !item.read).length } } })
Mock.mock(/\/api\/v1\/notifications(?:\?|$)/, 'get', (options) => { const category = getQueryParam(options.url, 'category'); const unreadOnly = getQueryParam(options.url, 'unreadOnly') === 'true'; let list = owned(options); if (category) list = list.filter((item) => item.category === category); if (unreadOnly) list = list.filter((item) => !item.read); list = [...list].sort((a, b) => b.createTime.localeCompare(a.createTime)); return { code: 0, message: 'success', data: { list: clone(list), total: list.length, unreadCount: owned(options).filter((item) => !item.read).length } } })
Mock.mock(/\/api\/v1\/notifications\/\d+\/read$/, 'patch', (options) => { const item = owned(options).find((entry) => entry.notificationId === Number(options.url.match(/notifications\/(\d+)/)?.[1])); if (!item) return { code: 9201, message: '消息不存在', data: null }; item.read = true; item.readTime = now(); return { code: 0, message: '已标记为已读', data: clone(item) } })
Mock.mock(/\/api\/v1\/notifications\/read-all$/, 'patch', (options) => { const readTime = now(); owned(options).forEach((item) => { item.read = true; item.readTime = readTime }); return { code: 0, message: '全部消息已读', data: { unreadCount: 0 } } })

console.log('[Mock] 统一通知中心已加载')
