import { describe, expect, it } from 'vitest'
import { MERCHANT_AUDIT_STATUS, getMerchantAuditMeta } from '@/constants/merchant-status.js'

describe('merchant status', () => {
  it('returns merchant review metadata', () => {
    expect(getMerchantAuditMeta(MERCHANT_AUDIT_STATUS.PENDING_REVIEW)).toEqual(expect.objectContaining({
      label: '审核中',
      tone: 'primary',
    }))
  })

  it('falls back to not applied', () => {
    expect(getMerchantAuditMeta('UNKNOWN').label).toBe('未入驻')
  })
})
