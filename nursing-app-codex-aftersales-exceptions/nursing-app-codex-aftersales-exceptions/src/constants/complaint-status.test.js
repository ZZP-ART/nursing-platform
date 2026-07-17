import { describe, expect, it } from 'vitest'
import { COMPLAINT_STATUS, getComplaintStatusMeta } from '@/constants/complaint-status.js'

describe('complaint status', () => {
  it('keeps legacy resolved status while adding arbitration', () => {
    expect(COMPLAINT_STATUS.RESOLVED).toBe(2)
    expect(getComplaintStatusMeta(COMPLAINT_STATUS.ADMIN_REVIEW).label).toBe('平台仲裁中')
  })
})
