/**
 * Mock — 评价模块（feedback-service）
 *
 * 对齐 API v1.0：
 * - POST /api/v1/reviews  → 提交评价
 * - GET  /api/v1/reviews  → 评价列表（?itemId=&page=&size=）
 */
import Mock from 'mockjs'
import { getMockOrder } from './order.js'

// ========== 模拟评价数据 ==========
export const reviews = [
  { reviewId: 30001, orderId: 20000, itemId: 401, userId: 10001,
    rating: 5, content: '非常满意！护士技术娴熟，态度也很好，妈妈完全没感到疼。',
    userNickname: '张阿姨', status: 2,
    createTime: '2026-07-02T15:00:00+08:00' },
  { reviewId: 30002, orderId: 20000, itemId: 401, userId: 10002,
    rating: 4, content: '整体不错，就是预约时间稍微延迟了一些，希望能改进。',
    userNickname: '李叔叔', status: 2,
    createTime: '2026-07-02T16:00:00+08:00' },
  { reviewId: 30003, orderId: 20000, itemId: 401, userId: 10003,
    rating: 5, content: '已经是老客户了，平台的服务一直很稳定，会继续推荐给朋友。',
    userNickname: '王女士', status: 2,
    createTime: '2026-07-03T09:00:00+08:00' },
  { reviewId: 30004, orderId: 20000, itemId: 101, userId: 10001,
    rating: 5, content: '护士非常专业，上门输液操作规范，还耐心教了日常注意事项。',
    userNickname: '张阿姨', status: 2,
    createTime: '2026-07-01T11:00:00+08:00' },
  { reviewId: 30005, orderId: 20000, itemId: 101, userId: 10004,
    rating: 3, content: '服务本身不错，但价格偏高了，希望多一些优惠活动。',
    userNickname: '赵先生', status: 2,
    createTime: '2026-07-01T14:00:00+08:00' },
  { reviewId: 30006, orderId: 20000, itemId: 301, userId: 10005,
    rating: 5, content: '艾灸师傅手法很好，老伴的关节疼缓解了不少，会继续坚持治疗。',
    userNickname: '刘奶奶', status: 2,
    createTime: '2026-07-04T10:00:00+08:00' },
  { reviewId: 30007, orderId: 20000, itemId: 201, userId: 10006,
    rating: 4, content: '康复师经验丰富，给了很多实用的康复建议，已经能看到效果了。',
    userNickname: '陈女士', status: 2,
    createTime: '2026-07-03T16:00:00+08:00' },
  { reviewId: 30008, orderId: 20000, itemId: 501, userId: 10007,
    rating: 5, content: '陪聊的小伙子很贴心，我爸跟他聊得特别开心，心情好了很多。',
    userNickname: '孙阿姨', status: 2,
    createTime: '2026-07-05T09:00:00+08:00' },
]

let nextReviewId = 30009
const reviewedOrders = new Set([20000]) // 已评价的订单

// ========== 工具 ==========
function getQueryParam(url, param) {
  const regex = new RegExp(`[?&]${param}=([^&]*)`)
  const match = url.match(regex)
  return match ? decodeURIComponent(match[1]) : null
}

// ========== 接口 Mock ==========

// 1. 提交评价
Mock.mock(/\/api\/v1\/reviews$/, 'post', (options) => {
  const body = JSON.parse(options.body)
  const idempotentKey = (options.headers || {})['Idempotency-Key'] || ''

  if (!idempotentKey) {
    return { code: 1000, message: '缺少 Idempotency-Key', data: null }
  }
  if (!body.orderId || !body.rating || !body.content?.trim()) {
    return { code: 1000, message: '订单ID、评分和评价内容为必填', data: null }
  }

  const order = getMockOrder(body.orderId)
  if (!order) return { code: 1005, message: '订单不存在', data: null }
  if (order.status !== 2) return { code: 4002, message: '该订单暂不可评价', data: null }

  // 一单一评
  if (reviewedOrders.has(body.orderId)) {
    return { code: 4003, message: '该订单已评价', data: null }
  }

  const reviewId = nextReviewId++
  reviews.unshift({
    reviewId,
    orderId: body.orderId,
    itemId: order.serviceItemId,
    userId: 10001,
    rating: body.rating,
    content: body.content || '',
    images: body.images || [],
    userNickname: '测试用户',
    status: 1, // 待审核（MVP 直接通过）
    createTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
  })
  reviewedOrders.add(body.orderId)

  console.log(`[Mock] 评价已提交: reviewId=${reviewId}`)

  return {
    code: 0,
    message: '评价提交成功',
    data: { reviewId },
  }
})

// 2. 评价列表（按服务项目）
Mock.mock(/\/api\/v1\/reviews\?/, 'get', (options) => {
  const url = options.url
  const itemId = getQueryParam(url, 'itemId')
  const page = parseInt(getQueryParam(url, 'page') || '1')
  const size = Math.min(parseInt(getQueryParam(url, 'size') || '20'), 50)

  let list = reviews.filter((r) => r.status === 2) // 仅已展示的

  if (itemId) {
    list = list.filter((r) => r.itemId === parseInt(itemId))
  }

  // 按时间倒序
  list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))

  const start = (page - 1) * size
  const paged = list.slice(start, start + size).map((r) => ({
    reviewId: r.reviewId,
    rating: r.rating,
    content: r.content,
    userNickname: r.userNickname,
    createTime: r.createTime,
  }))

  return {
    code: 0,
    message: 'success',
    data: { list: paged, total: list.length, page, size },
  }
})

console.log('[Mock] 评价模块已加载 (feedback-service / reviews)')
