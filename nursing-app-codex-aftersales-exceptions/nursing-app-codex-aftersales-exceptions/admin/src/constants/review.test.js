import { describe, expect, it } from 'vitest'
import { REVIEW_STATUS, REVIEW_TYPES, getReviewStatusMeta, getReviewTypeMeta } from './review.js'

describe('admin review metadata', () => {
  it('maps review types', () => {
    expect(getReviewTypeMeta(REVIEW_TYPES.MERCHANT).label).toBe('商户入驻')
  })

  it('maps review status', () => {
    expect(getReviewStatusMeta(REVIEW_STATUS.APPROVED)).toEqual({ label: '已通过', type: 'success' })
  })
})
