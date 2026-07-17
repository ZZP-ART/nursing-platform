import { COMPLAINT_STATUS } from '@/constants/complaint-status.js'
import { EXCEPTION_STATUS } from '@/constants/aftersales-status.js'
import { ORDER_STATUS, PAYMENT_STATUS } from '@/constants/order-status.js'

export const REPORT_RANGES = Object.freeze({ WEEK: '7d', MONTH: '30d', ALL: 'all' })

function round(value, digits = 2) {
  const scale = 10 ** digits
  return Math.round(Number(value || 0) * scale) / scale
}

function percent(numerator, denominator) {
  return denominator ? round((numerator / denominator) * 100, 1) : 0
}

function startOfRange(range, now) {
  if (range === REPORT_RANGES.ALL) return null
  const days = range === REPORT_RANGES.WEEK ? 7 : 30
  const start = new Date(now)
  start.setUTCHours(0, 0, 0, 0)
  start.setUTCDate(start.getUTCDate() - days + 1)
  return start
}

function inRange(value, start) {
  return !start || new Date(value).getTime() >= start.getTime()
}

function buildDailyTrend(orders, start, now) {
  if (!start) return []
  const trend = []
  const cursor = new Date(start)
  while (cursor.getTime() <= now.getTime()) {
    const key = cursor.toISOString().slice(0, 10)
    const dailyOrders = orders.filter((item) => item.createTime?.slice(0, 10) === key)
    trend.push({
      date: key,
      orderCount: dailyOrders.length,
      revenue: round(dailyOrders.filter((item) => item.paymentStatus !== PAYMENT_STATUS.UNPAID).reduce((total, item) => total + Number(item.totalAmount || 0) - Number(item.refund?.status === 'SUCCESS' ? item.refund.refundAmount : 0), 0)),
    })
    cursor.setUTCDate(cursor.getUTCDate() + 1)
  }
  return trend
}

export function buildMerchantReport({ merchantId, range = REPORT_RANGES.MONTH, orders = [], reviews = [], complaints = [], exceptions = [], now = new Date() }) {
  const normalizedRange = Object.values(REPORT_RANGES).includes(range) ? range : REPORT_RANGES.MONTH
  const start = startOfRange(normalizedRange, now)
  const merchantOrders = orders.filter((item) => item.merchantId === merchantId && inRange(item.createTime, start))
  const orderIds = new Set(merchantOrders.map((item) => item.orderId))
  const merchantReviews = reviews.filter((item) => orderIds.has(item.orderId) && item.status === 2)
  const merchantComplaints = complaints.filter((item) => item.merchantId === merchantId && orderIds.has(item.orderId) && inRange(item.createTime, start))
  const merchantExceptions = exceptions.filter((item) => item.merchantId === merchantId && orderIds.has(item.orderId) && inRange(item.createTime, start))
  const paidOrders = merchantOrders.filter((item) => item.paymentStatus !== PAYMENT_STATUS.UNPAID)
  const completedOrders = merchantOrders.filter((item) => item.orderStatus === ORDER_STATUS.COMPLETED)
  const revenue = round(paidOrders.reduce((total, item) => total + Number(item.totalAmount || 0) - Number(item.refund?.status === 'SUCCESS' ? item.refund.refundAmount : 0), 0))
  const ratingTotal = merchantReviews.reduce((total, item) => total + Number(item.rating || 0), 0)
  const unresolvedComplaints = merchantComplaints.filter((item) => ![COMPLAINT_STATUS.RESOLVED, COMPLAINT_STATUS.CLOSED].includes(item.status))
  const unresolvedExceptions = merchantExceptions.filter((item) => ![EXCEPTION_STATUS.RESOLVED, EXCEPTION_STATUS.CLOSED].includes(item.status))
  const serviceMap = new Map()
  merchantOrders.forEach((order) => {
    const item = serviceMap.get(order.serviceItemId) || { itemId: order.serviceItemId, name: order.serviceItemName, orderCount: 0, revenue: 0 }
    item.orderCount += 1
    if (order.paymentStatus !== PAYMENT_STATUS.UNPAID) item.revenue = round(item.revenue + Number(order.totalAmount || 0) - Number(order.refund?.status === 'SUCCESS' ? order.refund.refundAmount : 0))
    serviceMap.set(order.serviceItemId, item)
  })
  const topServices = [...serviceMap.values()].sort((a, b) => b.orderCount - a.orderCount || b.revenue - a.revenue).slice(0, 5)
  const risks = [
    ...unresolvedComplaints.map((item) => ({ type: 'COMPLAINT', id: item.complaintId, title: item.typeText || '投诉待处理', subtitle: item.content, time: item.updateTime || item.createTime, tone: 'danger' })),
    ...unresolvedExceptions.map((item) => ({ type: 'EXCEPTION', id: item.exceptionId, title: item.title, subtitle: item.description, time: item.updateTime || item.createTime, tone: 'warning' })),
  ].sort((a, b) => String(b.time).localeCompare(String(a.time)))

  return {
    range: normalizedRange,
    generatedAt: now.toISOString(),
    overview: {
      orderCount: merchantOrders.length,
      paidOrderCount: paidOrders.length,
      completedOrderCount: completedOrders.length,
      revenue,
      averageOrderAmount: paidOrders.length ? round(revenue / paidOrders.length) : 0,
    },
    quality: {
      completionRate: percent(completedOrders.length, merchantOrders.length),
      averageRating: merchantReviews.length ? round(ratingTotal / merchantReviews.length, 1) : 0,
      positiveRate: percent(merchantReviews.filter((item) => item.rating >= 4).length, merchantReviews.length),
      complaintRate: percent(merchantComplaints.length, merchantOrders.length),
      exceptionRate: percent(merchantExceptions.length, merchantOrders.length),
      reviewCount: merchantReviews.length,
    },
    alerts: { total: risks.length, complaintCount: unresolvedComplaints.length, exceptionCount: unresolvedExceptions.length, list: risks },
    topServices,
    trend: buildDailyTrend(merchantOrders, start, now),
  }
}
