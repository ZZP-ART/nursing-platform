import { DISPATCH_CONFLICT, getDispatchConflictText } from '@/constants/caregiver-schedule.js'

const baseDays = [
  { date: '2026-07-16', slots: ['MORNING', 'AFTERNOON'], onLeave: false, leaveReason: '' },
  { date: '2026-07-17', slots: ['AFTERNOON', 'EVENING'], onLeave: false, leaveReason: '' },
  { date: '2026-07-18', slots: [], onLeave: true, leaveReason: '个人事务' },
  { date: '2026-07-19', slots: ['MORNING', 'AFTERNOON'], onLeave: false, leaveReason: '' },
  { date: '2026-07-20', slots: ['MORNING', 'AFTERNOON'], onLeave: false, leaveReason: '' },
  { date: '2026-07-21', slots: ['MORNING'], onLeave: false, leaveReason: '' },
  { date: '2026-07-22', slots: ['AFTERNOON'], onLeave: false, leaveReason: '' },
]

const schedules = new Map([
  [50001, { caregiverId: 50001, enabled: true, maxDailyOrders: 3, serviceAreas: ['朝阳区', '海淀区'], dailyTaskCounts: { '2026-07-16': 1, '2026-07-20': 1 }, days: baseDays }],
  [50002, { caregiverId: 50002, enabled: true, maxDailyOrders: 2, serviceAreas: ['朝阳区'], dailyTaskCounts: { '2026-07-20': 0 }, days: baseDays.map((day) => day.date === '2026-07-20' ? { ...day, slots: ['AFTERNOON'] } : { ...day, slots: [...day.slots] }) }],
])

export function cloneSchedule(value) { return value == null ? value : JSON.parse(JSON.stringify(value)) }

export function getCaregiverSchedule(caregiverId) {
  return schedules.get(Number(caregiverId)) || null
}

export function saveCaregiverSchedule(caregiverId, payload) {
  const current = getCaregiverSchedule(caregiverId) || { caregiverId: Number(caregiverId), dailyTaskCounts: {} }
  const schedule = {
    ...current,
    enabled: payload.enabled !== false,
    maxDailyOrders: Math.max(1, Math.min(Number(payload.maxDailyOrders || 1), 10)),
    serviceAreas: [...new Set((payload.serviceAreas || []).map((item) => String(item).trim()).filter(Boolean))],
    days: (payload.days || []).map((day) => ({ date: day.date, slots: day.onLeave ? [] : [...new Set(day.slots || [])], onLeave: Boolean(day.onLeave), leaveReason: day.onLeave ? String(day.leaveReason || '').trim() : '' })),
  }
  schedules.set(Number(caregiverId), schedule)
  return schedule
}

function addressMatches(address, areas) {
  return areas.some((area) => String(address || '').includes(area.replace(/^北京市/, '')))
}

export function evaluateCaregiverForOrder(caregiverId, order) {
  const schedule = getCaregiverSchedule(caregiverId)
  const conflicts = []
  if (!schedule?.enabled) conflicts.push(DISPATCH_CONFLICT.DISABLED)
  const day = schedule?.days.find((item) => item.date === order.serviceDate)
  if (day?.onLeave) conflicts.push(DISPATCH_CONFLICT.ON_LEAVE)
  else if (!day?.slots.includes(order.serviceTimeSlot)) conflicts.push(DISPATCH_CONFLICT.TIME_UNAVAILABLE)
  if (!addressMatches(order.addressDetail, schedule?.serviceAreas || [])) conflicts.push(DISPATCH_CONFLICT.AREA_MISMATCH)
  if (Number(schedule?.dailyTaskCounts?.[order.serviceDate] || 0) >= Number(schedule?.maxDailyOrders || 1)) conflicts.push(DISPATCH_CONFLICT.DAILY_LIMIT)
  return { eligible: conflicts.length === 0, conflicts, conflictReasons: conflicts.map(getDispatchConflictText), serviceAreas: schedule?.serviceAreas || [], maxDailyOrders: schedule?.maxDailyOrders || 1, dailyTaskCount: Number(schedule?.dailyTaskCounts?.[order.serviceDate] || 0), scheduledSlots: day?.slots || [], onLeave: Boolean(day?.onLeave) }
}

export function getScheduleSummary(caregiverId) {
  const schedule = getCaregiverSchedule(caregiverId)
  if (!schedule?.enabled) return '已暂停接单'
  const availableDays = schedule.days.filter((day) => !day.onLeave && day.slots.length).length
  return `未来7天可服务 ${availableDays} 天`
}
