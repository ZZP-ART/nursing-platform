/**
 * Mock 数据入口 — 仅开发环境加载
 * 后端未就绪时模拟所有 API 响应
 *
 * 模块覆盖：
 * - user-service:     用户认证、个人信息、文件上传
 * - catalog-service:  服务分类、服务项目、搜索
 * - order-service:    订单、地址、支付
 * - feedback-service: 评价、投诉
 */
import Mock from 'mockjs'
import './user.js'
import './caregiver.js'
import './caregiver-schedule.js'
import './merchant-onboarding.js'
import './merchant-team.js'
import './service.js'
import './address.js'
import './order.js'
import './aftersales.js'
import './review.js'
import './complaint.js'
import './notification.js'
import './settlement.js'
import './merchant-report.js'

// 全局 Mock 配置
Mock.setup({
  timeout: '200-500', // 模拟网络延迟 200-500ms
})

console.log('[Mock] 全部模块已启用 — 拦截所有 API 请求')
