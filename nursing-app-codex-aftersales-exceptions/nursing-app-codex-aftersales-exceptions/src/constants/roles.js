export const ROLES = Object.freeze({
  CUSTOMER: 'CUSTOMER',
  CAREGIVER: 'CAREGIVER',
  MERCHANT_MEMBER: 'MERCHANT_MEMBER',
  ADMIN: 'ADMIN',
})

export const APP_LOGIN_ROLES = Object.freeze([
  {
    value: ROLES.CUSTOMER,
    label: '顾客',
    description: '预约护理服务',
    icon: 'account',
  },
  {
    value: ROLES.CAREGIVER,
    label: '护理人员',
    description: '查看上门任务',
    icon: 'server-man',
  },
  {
    value: ROLES.MERCHANT_MEMBER,
    label: '商户',
    description: '管理服务与订单',
    icon: 'home',
  },
])

export const ROLE_HOME_ROUTES = Object.freeze({
  [ROLES.CUSTOMER]: '/pages/index/index',
  [ROLES.CAREGIVER]: '/subpkg-caregiver/home/index',
  [ROLES.MERCHANT_MEMBER]: '/subpkg-merchant/home/index',
})

export function isAppRole(role) {
  return APP_LOGIN_ROLES.some((item) => item.value === role)
}

export function getRoleLabel(role) {
  return APP_LOGIN_ROLES.find((item) => item.value === role)?.label || '未知身份'
}

export function getRoleHomeRoute(role) {
  return ROLE_HOME_ROUTES[role] || ROLE_HOME_ROUTES[ROLES.CUSTOMER]
}

export function navigateToRoleHome(role) {
  const targetRole = isAppRole(role) ? role : ROLES.CUSTOMER
  const url = getRoleHomeRoute(targetRole)
  if (targetRole === ROLES.CUSTOMER) {
    uni.switchTab({ url })
    return
  }
  uni.reLaunch({ url })
}
