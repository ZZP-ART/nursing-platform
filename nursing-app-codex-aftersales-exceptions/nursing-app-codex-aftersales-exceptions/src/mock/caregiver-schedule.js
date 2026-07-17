import Mock from 'mockjs'
import { findMockUserById } from './user.js'
import { cloneSchedule, getCaregiverSchedule, saveCaregiverSchedule } from './caregiver-schedule-state.js'

function getCaregiverId(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const userId = Number(auth.match(/mock_jwt_(\d+)_CAREGIVER_/)?.[1] || 0)
  return findMockUserById(userId)?.caregiverId || null
}

Mock.mock(/\/api\/v1\/caregiver\/schedule$/, 'get', (options) => {
  const caregiverId = getCaregiverId(options)
  if (!caregiverId) return { code: 7003, message: '护理人员身份尚未认证通过', data: null }
  return { code: 0, message: 'success', data: cloneSchedule(getCaregiverSchedule(caregiverId)) }
})

Mock.mock(/\/api\/v1\/caregiver\/schedule$/, 'put', (options) => {
  const caregiverId = getCaregiverId(options)
  if (!caregiverId) return { code: 7003, message: '护理人员身份尚未认证通过', data: null }
  const body = JSON.parse(options.body || '{}')
  if (!Array.isArray(body.serviceAreas) || !body.serviceAreas.length) return { code: 1000, message: '请至少选择一个服务区域', data: null }
  if (!Array.isArray(body.days) || body.days.some((day) => !/^\d{4}-\d{2}-\d{2}$/.test(day.date || ''))) return { code: 1000, message: '排班日期格式不正确', data: null }
  return { code: 0, message: '排班与服务区域已保存', data: cloneSchedule(saveCaregiverSchedule(caregiverId, body)) }
})

console.log('[Mock] 护理人员排班与服务区域已加载')
