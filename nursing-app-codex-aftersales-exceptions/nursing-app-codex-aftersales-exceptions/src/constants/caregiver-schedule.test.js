import { describe, expect, it } from 'vitest'
import { DISPATCH_CONFLICT, SCHEDULE_SLOTS, getDispatchConflictText } from '@/constants/caregiver-schedule.js'

describe('caregiver schedule constants', () => {
  it('defines service slots and dispatch conflict messages', () => {
    expect(SCHEDULE_SLOTS.map((item) => item.value)).toContain('MORNING')
    expect(getDispatchConflictText(DISPATCH_CONFLICT.AREA_MISMATCH)).toBe('服务地址超出服务区域')
  })
})
