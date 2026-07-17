import { getActiveRole, getPermissions, getToken } from '@/utils/storage.js'
import { getRoleHomeRoute } from '@/constants/roles.js'

export function hasRole(role) {
  return getActiveRole() === role
}

export function hasPermission(permission) {
  const permissions = getPermissions()
  return permissions.includes('*') || permissions.includes(permission)
}

export function requireRole(roles) {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return false
  }

  const allowedRoles = Array.isArray(roles) ? roles : [roles]
  const currentRole = getActiveRole()
  if (allowedRoles.includes(currentRole)) return true

  uni.showToast({ title: '当前身份无权访问该页面', icon: 'none' })
  setTimeout(() => {
    uni.reLaunch({ url: getRoleHomeRoute(currentRole) })
  }, 800)
  return false
}

export function requirePermission(permission) {
  if (hasPermission(permission)) return true
  uni.showToast({ title: '暂无权限执行此操作', icon: 'none' })
  return false
}
