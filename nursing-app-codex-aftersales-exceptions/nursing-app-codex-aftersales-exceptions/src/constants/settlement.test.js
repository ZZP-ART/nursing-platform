import { describe, expect, it } from 'vitest'
import { SETTLEMENT_STATUS, getSettlementStatusMeta } from '@/constants/settlement.js'

describe('settlement status', () => {
  it('maps available settlement status', () => {
    expect(getSettlementStatusMeta(SETTLEMENT_STATUS.AVAILABLE).label).toBe('可结算')
  })
})
