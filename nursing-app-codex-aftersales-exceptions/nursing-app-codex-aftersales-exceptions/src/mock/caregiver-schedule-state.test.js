import { describe, expect, it } from 'vitest'
import { evaluateCaregiverForOrder, saveCaregiverSchedule } from '@/mock/caregiver-schedule-state.js'
import { DISPATCH_CONFLICT } from '@/constants/caregiver-schedule.js'

describe('caregiver schedule mock state', () => {
  it('blocks a caregiver when the order area is outside the service range', () => {
    const result = evaluateCaregiverForOrder(50002, { serviceDate: '2026-07-20', serviceTimeSlot: 'AFTERNOON', addressDetail: '北京市海淀区中关村' })
    expect(result.eligible).toBe(false)
    expect(result.conflicts).toContain(DISPATCH_CONFLICT.AREA_MISMATCH)
  })

  it('persists leave days and clears their time slots', () => {
    const schedule = saveCaregiverSchedule(50999, { enabled: true, maxDailyOrders: 2, serviceAreas: ['朝阳区'], days: [{ date: '2026-07-20', slots: ['MORNING'], onLeave: true, leaveReason: '培训' }] })
    expect(schedule.days[0].slots).toEqual([])
    expect(schedule.days[0].leaveReason).toBe('培训')
  })
})
