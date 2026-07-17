import { describe, expect, it } from 'vitest'
import { CAREGIVER_AUDIT_STATUS, getCaregiverAuditMeta } from '@/constants/caregiver-status.js'

describe('caregiver status', () => {
  it('returns the audit display metadata', () => {
    expect(getCaregiverAuditMeta(CAREGIVER_AUDIT_STATUS.REJECTED)).toEqual(expect.objectContaining({
      label: '已驳回',
      tone: 'danger',
    }))
  })

  it('falls back to not applied for unknown status', () => {
    expect(getCaregiverAuditMeta('UNKNOWN').label).toBe('未申请')
  })
})

