import { describe, expect, it } from 'vitest'
import { COOPERATION_STATUS, getCooperationStatusMeta, getPositionLabel } from '@/constants/team-status.js'

describe('merchant team status', () => {
  it('maps cooperation status metadata', () => {
    expect(getCooperationStatusMeta(COOPERATION_STATUS.ACTIVE)).toEqual({ label: '合作中', tone: 'success' })
  })

  it('maps merchant positions', () => {
    expect(getPositionLabel('DISPATCHER')).toBe('调度员')
  })
})
