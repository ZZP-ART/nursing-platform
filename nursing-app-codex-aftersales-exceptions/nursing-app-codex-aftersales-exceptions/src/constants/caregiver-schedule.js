export const SCHEDULE_SLOTS = Object.freeze([
  { value: 'MORNING', label: '上午', time: '08:00-12:00' },
  { value: 'AFTERNOON', label: '下午', time: '13:00-17:00' },
  { value: 'EVENING', label: '晚上', time: '18:00-21:00' },
])

export const DISPATCH_CONFLICT = Object.freeze({
  DISABLED: 'DISABLED',
  ON_LEAVE: 'ON_LEAVE',
  TIME_UNAVAILABLE: 'TIME_UNAVAILABLE',
  AREA_MISMATCH: 'AREA_MISMATCH',
  DAILY_LIMIT: 'DAILY_LIMIT',
})

export const DISPATCH_CONFLICT_TEXT = Object.freeze({
  [DISPATCH_CONFLICT.DISABLED]: '护理人员已暂停接单',
  [DISPATCH_CONFLICT.ON_LEAVE]: '预约当天已请假',
  [DISPATCH_CONFLICT.TIME_UNAVAILABLE]: '预约时段未开放',
  [DISPATCH_CONFLICT.AREA_MISMATCH]: '服务地址超出服务区域',
  [DISPATCH_CONFLICT.DAILY_LIMIT]: '当天接单已达上限',
})

export function getDispatchConflictText(code) {
  return DISPATCH_CONFLICT_TEXT[code] || '当前不可派单'
}
