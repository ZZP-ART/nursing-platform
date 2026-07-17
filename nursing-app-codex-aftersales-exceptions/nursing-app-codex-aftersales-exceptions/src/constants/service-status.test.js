import { describe, expect, it } from 'vitest'
import {
  SERVICE_AUDIT_STATUS,
  SERVICE_PUBLISH_STATUS,
  canEditService,
  canOfflineService,
  canPublishService,
  canSubmitService,
  getServiceAuditMeta,
} from '@/constants/service-status.js'

describe('service status rules', () => {
  it('草稿和驳回状态可编辑，审核中不可编辑', () => {
    expect(canEditService({ auditStatus: SERVICE_AUDIT_STATUS.DRAFT })).toBe(true)
    expect(canEditService({ auditStatus: SERVICE_AUDIT_STATUS.REJECTED })).toBe(true)
    expect(canEditService({ auditStatus: SERVICE_AUDIT_STATUS.PENDING_REVIEW })).toBe(false)
  })

  it('至少存在一个规格才允许提交审核', () => {
    expect(canSubmitService({ auditStatus: SERVICE_AUDIT_STATUS.DRAFT, specs: [] })).toBe(false)
    expect(canSubmitService({ auditStatus: SERVICE_AUDIT_STATUS.DRAFT, specs: [{ specId: 1 }] })).toBe(true)
  })

  it('审核通过后允许上架，已上架后允许下架', () => {
    expect(canPublishService({ auditStatus: SERVICE_AUDIT_STATUS.APPROVED, publishStatus: SERVICE_PUBLISH_STATUS.OFFLINE })).toBe(true)
    expect(canPublishService({ auditStatus: SERVICE_AUDIT_STATUS.PENDING_REVIEW, publishStatus: SERVICE_PUBLISH_STATUS.OFFLINE })).toBe(false)
    expect(canOfflineService({ publishStatus: SERVICE_PUBLISH_STATUS.PUBLISHED })).toBe(true)
  })

  it('返回审核状态展示信息', () => {
    expect(getServiceAuditMeta(SERVICE_AUDIT_STATUS.REJECTED)).toEqual({ text: '已驳回', tone: 'warning' })
  })
})
