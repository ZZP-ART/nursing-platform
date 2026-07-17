import { describe, expect, it } from 'vitest'
import { EXCEPTION_TYPE, REFUND_STATUS, getExceptionTypeMeta, getRefundStatusMeta } from '@/constants/aftersales-status.js'

describe('aftersales status', () => {
  it('maps refund status', () => {
    expect(getRefundStatusMeta(REFUND_STATUS.PROCESSING).label).toBe('退款处理中')
  })

  it('maps exception type', () => {
    expect(getExceptionTypeMeta(EXCEPTION_TYPE.ABNORMAL_CHECK_IN).label).toBe('异常签到')
  })
})
