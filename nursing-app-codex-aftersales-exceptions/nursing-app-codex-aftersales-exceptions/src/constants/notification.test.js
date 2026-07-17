import { describe, expect, it } from 'vitest'
import { NOTIFICATION_CATEGORY, getNotificationCategoryMeta } from '@/constants/notification.js'

describe('notification category', () => {
  it('maps business categories to display metadata', () => {
    expect(getNotificationCategoryMeta(NOTIFICATION_CATEGORY.REFUND).label).toBe('退款')
    expect(getNotificationCategoryMeta('UNKNOWN').label).toBe('系统')
  })
})
