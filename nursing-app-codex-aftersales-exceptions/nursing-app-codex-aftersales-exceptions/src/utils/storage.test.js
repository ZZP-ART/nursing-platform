/**
 * 本地存储工具 — 单元测试
 *
 * 覆盖：Token 读写删除、用户信息持久化、登录态判断、超时过期
 */
import { describe, it, expect, beforeEach } from 'vitest'
import {
  getToken,
  setToken,
  removeToken,
  getUserInfo,
  setUserInfo,
  removeUserInfo,
  isLoggedIn,
} from '@/utils/storage.js'

describe('storage — Token 管理', () => {
  beforeEach(() => {
    uni.clearStorageSync()
  })

  it('初始状态下没有 token', () => {
    expect(getToken()).toBe('')
  })

  it('setToken 后 getToken 可读取', () => {
    setToken('test-jwt-token')
    expect(getToken()).toBe('test-jwt-token')
  })

  it('removeToken 后 token 为空', () => {
    setToken('test-jwt-token')
    removeToken()
    expect(getToken()).toBe('')
  })

  it('设置 token 时会记录 loginTime', () => {
    setToken('abc123')
    const loginTime = uni.getStorageSync('loginTime')
    expect(typeof loginTime).toBe('number')
    expect(loginTime).toBeLessThanOrEqual(Date.now())
  })
})

describe('storage — 用户信息', () => {
  beforeEach(() => {
    uni.clearStorageSync()
  })

  it('初始无用户信息返回 null', () => {
    expect(getUserInfo()).toBeNull()
  })

  it('setUserInfo 后 getUserInfo 可读取完整对象', () => {
    const info = { userId: 1, phone: '138****8000', nickname: '测试用户' }
    setUserInfo(info)
    expect(getUserInfo()).toEqual(info)
  })

  it('removeUserInfo 后返回 null', () => {
    setUserInfo({ userId: 1, phone: '138****8000' })
    removeUserInfo()
    expect(getUserInfo()).toBeNull()
  })
})

describe('storage — 登录态判断', () => {
  beforeEach(() => {
    uni.clearStorageSync()
  })

  it('无 token 时 isLoggedIn 返回 false', () => {
    expect(isLoggedIn()).toBe(false)
  })

  it('有 token 且未过期时返回 true', () => {
    setToken('valid-token')
    expect(isLoggedIn()).toBe(true)
  })

  it('token 超过 7 天未登录时返回 false 并清除 token', () => {
    setToken('old-token')
    // 手动将 loginTime 设为 8 天前
    uni.setStorageSync('loginTime', Date.now() - 8 * 24 * 60 * 60 * 1000)
    expect(isLoggedIn()).toBe(false)
    expect(getToken()).toBe('')
  })

  it('token 刚好 6 天时仍有效', () => {
    setToken('recent-token')
    uni.setStorageSync('loginTime', Date.now() - 6 * 24 * 60 * 60 * 1000)
    expect(isLoggedIn()).toBe(true)
  })
})
