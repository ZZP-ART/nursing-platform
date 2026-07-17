import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { ROLES, getRoleHomeRoute, isAppRole, navigateToRoleHome } from '@/constants/roles.js'
import {
  getActiveRole,
  setActiveRole,
  getAvailableRoles,
  setAvailableRoles,
  getPermissions,
  setPermissions,
  getLastLoginRole,
  setLastLoginRole,
  removeRoleSession,
} from '@/utils/storage.js'

export const useRoleStore = defineStore('role', () => {
  const currentRole = ref(getActiveRole() || '')
  const availableRoles = ref(getAvailableRoles())
  const permissions = ref(getPermissions())
  const lastLoginRole = ref(
    isAppRole(getLastLoginRole()) ? getLastLoginRole() : ROLES.CUSTOMER
  )

  const isCustomer = computed(() => currentRole.value === ROLES.CUSTOMER)
  const isCaregiver = computed(() => currentRole.value === ROLES.CAREGIVER)
  const isMerchant = computed(() => currentRole.value === ROLES.MERCHANT_MEMBER)
  const homeRoute = computed(() => getRoleHomeRoute(currentRole.value))

  function applyAuthSession({ roles = [], currentRole: role, permissions: grants = [] }) {
    const normalizedRoles = Array.from(new Set(roles.filter(Boolean)))
    const normalizedRole = role || normalizedRoles[0] || ROLES.CUSTOMER

    currentRole.value = normalizedRole
    availableRoles.value = normalizedRoles.includes(normalizedRole)
      ? normalizedRoles
      : [normalizedRole, ...normalizedRoles]
    permissions.value = Array.from(new Set(grants.filter(Boolean)))
    lastLoginRole.value = isAppRole(normalizedRole) ? normalizedRole : ROLES.CUSTOMER

    setActiveRole(currentRole.value)
    setAvailableRoles(availableRoles.value)
    setPermissions(permissions.value)
    setLastLoginRole(lastLoginRole.value)
  }

  function rememberLoginRole(role) {
    if (!isAppRole(role)) return
    lastLoginRole.value = role
    setLastLoginRole(role)
  }

  function hasRole(role) {
    return availableRoles.value.includes(role)
  }

  function hasPermission(permission) {
    return permissions.value.includes('*') || permissions.value.includes(permission)
  }

  function goToWorkspace() {
    navigateToRoleHome(currentRole.value)
  }

  function clearSession() {
    currentRole.value = ''
    availableRoles.value = []
    permissions.value = []
    removeRoleSession()
  }

  return {
    currentRole,
    availableRoles,
    permissions,
    lastLoginRole,
    isCustomer,
    isCaregiver,
    isMerchant,
    homeRoute,
    applyAuthSession,
    rememberLoginRole,
    hasRole,
    hasPermission,
    goToWorkspace,
    clearSession,
  }
})
