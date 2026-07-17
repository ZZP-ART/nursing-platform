import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useRoleStore } from '@/store/role.js'
import { ROLES } from '@/constants/roles.js'
import {
  getActiveRole,
  getAvailableRoles,
  getLastLoginRole,
  getPermissions,
} from '@/utils/storage.js'

describe('roleStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    uni.clearStorageSync()
  })

  it('默认使用顾客作为登录页预选身份', () => {
    const store = useRoleStore()
    expect(store.currentRole).toBe('')
    expect(store.lastLoginRole).toBe(ROLES.CUSTOMER)
  })

  it('保存登录返回的角色会话和权限', () => {
    const store = useRoleStore()
    store.applyAuthSession({
      roles: [ROLES.CUSTOMER, ROLES.CAREGIVER],
      currentRole: ROLES.CAREGIVER,
      permissions: ['caregiver:task:list'],
    })

    expect(store.currentRole).toBe(ROLES.CAREGIVER)
    expect(store.isCaregiver).toBe(true)
    expect(store.hasRole(ROLES.CUSTOMER)).toBe(true)
    expect(store.hasPermission('caregiver:task:list')).toBe(true)
    expect(getActiveRole()).toBe(ROLES.CAREGIVER)
    expect(getAvailableRoles()).toEqual([ROLES.CUSTOMER, ROLES.CAREGIVER])
    expect(getPermissions()).toEqual(['caregiver:task:list'])
    expect(getLastLoginRole()).toBe(ROLES.CAREGIVER)
  })

  it('清理会话时保留上次登录身份偏好', () => {
    const store = useRoleStore()
    store.applyAuthSession({
      roles: [ROLES.MERCHANT_MEMBER],
      currentRole: ROLES.MERCHANT_MEMBER,
      permissions: ['merchant:order:view'],
    })

    store.clearSession()

    expect(store.currentRole).toBe('')
    expect(store.availableRoles).toEqual([])
    expect(store.permissions).toEqual([])
    expect(getActiveRole()).toBe('')
    expect(getLastLoginRole()).toBe(ROLES.MERCHANT_MEMBER)
  })

  it('按当前身份进入正确工作台', () => {
    const store = useRoleStore()
    store.applyAuthSession({
      roles: [ROLES.MERCHANT_MEMBER],
      currentRole: ROLES.MERCHANT_MEMBER,
    })

    store.goToWorkspace()

    expect(uni.reLaunch).toHaveBeenCalledWith({ url: '/subpkg-merchant/home/index' })
  })
})
