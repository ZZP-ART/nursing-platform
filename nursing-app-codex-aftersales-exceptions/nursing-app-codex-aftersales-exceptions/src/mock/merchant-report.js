import Mock from 'mockjs'
import { getToken } from '@/utils/storage.js'
import { complaints } from './complaint.js'
import { exceptions } from './aftersales-state.js'
import { getMockOrders } from './order.js'
import { reviews } from './review.js'
import { getMockMerchantIdByUserId } from './user.js'
import { buildMerchantReport } from './merchant-report-state.js'

function merchantIdFrom(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const token = auth.replace('Bearer ', '') || getToken()
  const userId = Number(token.match(/mock_jwt_(\d+)_MERCHANT_MEMBER_/)?.[1] || 0)
  return getMockMerchantIdByUserId(userId)
}

function query(url, name) {
  const value = String(url).match(new RegExp(`[?&]${name}=([^&]*)`))?.[1]
  return value ? decodeURIComponent(value) : ''
}

Mock.mock(/\/api\/v1\/merchant\/reports\/operations(?:\?|$)/, 'get', (options) => {
  const merchantId = merchantIdFrom(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  return { code: 0, message: 'success', data: buildMerchantReport({ merchantId, range: query(options.url, 'range'), orders: getMockOrders(), reviews, complaints, exceptions }) }
})

console.log('[Mock] 商户经营报表与质量指标已加载')
