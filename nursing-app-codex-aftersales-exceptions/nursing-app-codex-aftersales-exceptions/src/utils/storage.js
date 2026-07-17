/**
 * 本地存储工具 — Token 持久化与用户数据管理
 */
const KEYS = {
  TOKEN: 'token',
  REFRESH_TOKEN: 'refreshToken',
  USER_INFO: 'userInfo',
  LOGIN_TIME: 'loginTime',
  ACTIVE_ROLE: 'activeRole',
  AVAILABLE_ROLES: 'availableRoles',
  PERMISSIONS: 'permissions',
  LAST_LOGIN_ROLE: 'lastLoginRole',
}

/** 设置存储 */
function set(key, value) {
  try {
    uni.setStorageSync(key, value)
  } catch (e) {
    console.error(`[Storage] 写入失败: ${key}`, e)
  }
}

/** 读取存储 */
function get(key, defaultValue = null) {
  try {
    const value = uni.getStorageSync(key)
    return value !== '' && value !== undefined ? value : defaultValue
  } catch (e) {
    console.error(`[Storage] 读取失败: ${key}`, e)
    return defaultValue
  }
}

/** 删除存储 */
function remove(key) {
  try {
    uni.removeStorageSync(key)
  } catch (e) {
    console.error(`[Storage] 删除失败: ${key}`, e)
  }
}

// ========== Token 相关 ==========

export function getToken() {
  return get(KEYS.TOKEN, '')
}

export function setToken(token) {
  set(KEYS.TOKEN, token)
  set(KEYS.LOGIN_TIME, Date.now())
}

export function removeToken() {
  remove(KEYS.TOKEN)
  remove(KEYS.REFRESH_TOKEN)
  remove(KEYS.LOGIN_TIME)
}

// ========== 用户信息 ==========

export function getUserInfo() {
  return get(KEYS.USER_INFO, null)
}

export function setUserInfo(info) {
  set(KEYS.USER_INFO, info)
}

export function removeUserInfo() {
  remove(KEYS.USER_INFO)
}

// ========== 角色会话 ==========

export function getActiveRole() {
  return get(KEYS.ACTIVE_ROLE, '')
}

export function setActiveRole(role) {
  set(KEYS.ACTIVE_ROLE, role)
}

export function getAvailableRoles() {
  return get(KEYS.AVAILABLE_ROLES, [])
}

export function setAvailableRoles(roles) {
  set(KEYS.AVAILABLE_ROLES, Array.isArray(roles) ? roles : [])
}

export function getPermissions() {
  return get(KEYS.PERMISSIONS, [])
}

export function setPermissions(permissions) {
  set(KEYS.PERMISSIONS, Array.isArray(permissions) ? permissions : [])
}

export function getLastLoginRole() {
  return get(KEYS.LAST_LOGIN_ROLE, '')
}

export function setLastLoginRole(role) {
  set(KEYS.LAST_LOGIN_ROLE, role)
}

export function removeRoleSession() {
  remove(KEYS.ACTIVE_ROLE)
  remove(KEYS.AVAILABLE_ROLES)
  remove(KEYS.PERMISSIONS)
}

export function clearAuthStorage() {
  removeToken()
  removeUserInfo()
  removeRoleSession()
}

// ========== 登录态检查 ==========

/** 判断是否已登录 */
export function isLoggedIn() {
  const token = getToken()
  if (!token) return false
  // 简易超时判断（7天未登录则失效）
  const loginTime = get(KEYS.LOGIN_TIME, 0)
  if (Date.now() - loginTime > 7 * 24 * 60 * 60 * 1000) {
    removeToken()
    return false
  }
  return true
}

/** 退出登录 */
export function logout() {
  clearAuthStorage()
  // 跳转到登录页
  uni.reLaunch({ url: '/pages/login/login' })
}

export default {
  getToken,
  setToken,
  removeToken,
  getUserInfo,
  setUserInfo,
  getActiveRole,
  setActiveRole,
  getAvailableRoles,
  setAvailableRoles,
  getPermissions,
  setPermissions,
  getLastLoginRole,
  setLastLoginRole,
  removeRoleSession,
  clearAuthStorage,
  isLoggedIn,
  logout,
}
