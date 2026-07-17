import { describe, expect, it } from 'vitest'
import { CAREGIVER_TABS } from '@/constants/caregiver-navigation.js'
import { MERCHANT_TABS } from '@/constants/merchant-navigation.js'

describe('role navigation', () => {
  it('provides merchants a service tab and a reachable profile tab', () => {
    expect(MERCHANT_TABS.map((tab) => tab.path)).toEqual([
      '/subpkg-merchant/home/index',
      '/subpkg-merchant/services/index',
      '/subpkg-merchant/orders/index',
      '/subpkg-merchant/profile/index',
    ])
  })

  it('provides caregivers a reachable profile tab for account actions', () => {
    expect(CAREGIVER_TABS.map((tab) => tab.path)).toContain('/subpkg-caregiver/profile/index')
    expect(CAREGIVER_TABS).toHaveLength(4)
  })
})
