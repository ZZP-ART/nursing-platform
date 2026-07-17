/**
 * Mock — 用户模块（user-service）
 *
 * 对齐 API v1.0：
 * - 发送验证码 /api/v1/users/sms-code
 * - 注册 /api/v1/users/register
 * - 登录 /api/v1/users/login（loginMode: password | sms）
 * - 登出 /api/v1/users/logout
 * - 重置密码 /api/v1/users/password/reset
 * - 个人信息 /api/v1/users/profile (GET/PUT)
 * - 文件上传 /api/v1/files/upload
 */
import Mock from 'mockjs'
import { ROLES } from '@/constants/roles.js'

const Random = Mock.Random

// ========== 模拟用户数据 ==========
const users = new Map()
const tokenBlacklist = new Set() // 登出黑名单

const ROLE_PERMISSIONS = {
  [ROLES.CUSTOMER]: [
    'customer:service:view',
    'customer:order:create',
    'customer:order:view',
  ],
  [ROLES.CAREGIVER]: [
    'caregiver:task:list',
    'caregiver:assignment:accept',
    'caregiver:service:check-in',
  ],
  [ROLES.MERCHANT_MEMBER]: [
    'merchant:service:manage',
    'merchant:order:view',
    'merchant:order:dispatch',
  ],
}

// 预设测试账号（密码: 123456）
const testUser = {
  userId: 10001,
  phone: '13800138000',
  password: '123456',
  nickname: '测试用户',
  avatar: '',
  gender: 0,
  idCard: '110101199001011234',
  status: 0,
  lastLoginTime: '2026-07-01T10:00:00+08:00',
  createTime: '2026-01-15T08:00:00+08:00',
  version: 0,
  roles: [ROLES.CUSTOMER, ROLES.CAREGIVER],
  caregiverId: 50001,
}
users.set('13800138000', testUser)

users.set('13800138001', {
  userId: 10002,
  phone: '13800138001',
  password: '123456',
  nickname: '李护理员',
  avatar: '',
  gender: 2,
  idCard: '110101199202021234',
  status: 0,
  lastLoginTime: '2026-07-01T10:00:00+08:00',
  createTime: '2026-02-01T08:00:00+08:00',
  version: 0,
  roles: [ROLES.CAREGIVER],
  caregiverId: 50002,
})

users.set('13800138002', {
  userId: 10003,
  phone: '13800138002',
  password: '123456',
  nickname: '康宁护理中心',
  avatar: '',
  gender: 0,
  idCard: null,
  status: 0,
  lastLoginTime: '2026-07-01T10:00:00+08:00',
  createTime: '2026-03-01T08:00:00+08:00',
  version: 0,
  roles: [ROLES.MERCHANT_MEMBER],
  merchantId: 20001,
})

users.set('13800138003', {
  userId: 10004,
  phone: '13800138003',
  password: '123456',
  nickname: '护理申请人',
  avatar: '',
  gender: 1,
  idCard: null,
  status: 0,
  lastLoginTime: '2026-07-01T10:00:00+08:00',
  createTime: '2026-07-01T08:00:00+08:00',
  version: 0,
  roles: [ROLES.CUSTOMER],
})

users.set('13800138004', {
  userId: 10005,
  phone: '13800138004',
  password: '123456',
  nickname: '商户申请人',
  avatar: '',
  gender: 1,
  idCard: null,
  status: 0,
  lastLoginTime: '2026-07-01T10:00:00+08:00',
  createTime: '2026-07-01T08:00:00+08:00',
  version: 0,
  roles: [ROLES.CUSTOMER],
})

// ========== 工具函数 ==========

function generateToken(userId, role = ROLES.CUSTOMER) {
  return `mock_jwt_${userId}_${role}_${Date.now()}_${Random.string('lower', 16)}`
}

function maskPhone(phone) {
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function buildUserResponse(user, currentRole = ROLES.CUSTOMER) {
  return {
    userId: user.userId,
    phone: maskPhone(user.phone),
    nickname: user.nickname,
    avatar: user.avatar || null,
    gender: user.gender,
    status: user.status,
    version: user.version || 0,
    roles: user.roles || [ROLES.CUSTOMER],
    currentRole,
    merchantId: user.merchantId || null,
    caregiverId: user.caregiverId || null,
  }
}

function buildAuthResponse(user, currentRole) {
  const roles = user.roles || [ROLES.CUSTOMER]
  return {
    token: generateToken(user.userId, currentRole),
    expireTime: expireTime(),
    roles,
    currentRole,
    permissions: ROLE_PERMISSIONS[currentRole] || [],
    user: buildUserResponse(user, currentRole),
  }
}

function getUserFromToken(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const token = auth.replace('Bearer ', '')
  if (!token || tokenBlacklist.has(token)) return null
  const userId = Number(token.match(/mock_jwt_(\d+)/)?.[1] || 0)
  return findMockUserById(userId)
}

export function findMockUserById(userId) {
  return [...users.values()].find((user) => user.userId === Number(userId)) || null
}

export function grantMockUserRole(userId, role, relatedId = null) {
  const user = findMockUserById(userId)
  if (!user) return null
  if (!user.roles.includes(role)) user.roles.push(role)
  if (role === ROLES.CAREGIVER && relatedId) user.caregiverId = relatedId
  if (role === ROLES.MERCHANT_MEMBER && relatedId) user.merchantId = relatedId
  return user
}

export function getMockMerchantIdByUserId(userId) {
  return findMockUserById(userId)?.merchantId || null
}

// 7天后过期
function expireTime() {
  const d = new Date()
  d.setDate(d.getDate() + 7)
  return d.toISOString().replace(/\.\d{3}Z$/, '+08:00')
}

// ========== 1. 发送短信验证码 ==========
Mock.mock(/\/api\/v1\/users\/sms-code/, 'post', (options) => {
  const idempotencyKey = options.headers?.['Idempotency-Key'] || options.headers?.['idempotency-key']
  const { phone, smsType } = JSON.parse(options.body)
  console.log(`[Mock] 发送验证码 → ${phone}，类型: ${smsType}，验证码: 123456`)

  if (!/^1\d{10}$/.test(phone)) {
    return { code: 1000, message: '手机号格式不正确', data: null }
  }
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(idempotencyKey || '')) {
    return { code: 1000, message: 'Idempotency-Key 必须为 UUID', data: null }
  }

  // 注册时检查手机号已存在
  if (smsType === 'register' && users.has(phone)) {
    return { code: 2003, message: '手机号已被注册', data: null }
  }
  // 登录/重置密码时检查手机号存在
  if ((smsType === 'login' || smsType === 'reset_password') && !users.has(phone)) {
    return { code: 2004, message: '手机号未注册', data: null }
  }

  return {
    code: 0,
    message: '验证码发送任务已受理',
    data: {
      expireSeconds: 300,
      retryAfterSeconds: null,
      requestId: String(Date.now()),
      status: 'PENDING',
    },
  }
})

// ========== 2. 用户注册 ==========
Mock.mock(/\/api\/v1\/users\/register/, 'post', (options) => {
  const { phone, smsCode, password, nickname } = JSON.parse(options.body)

  // 验证码校验（Mock 固定 123456）
  if (smsCode !== '123456') {
    return { code: 2006, message: '验证码错误', data: null }
  }

  // 手机号已注册
  if (users.has(phone)) {
    return { code: 2008, message: '该手机号已注册', data: null }
  }

  // 密码校验：8-32位，包含字母和数字
  if (!password || password.length < 8 || !/[a-zA-Z]/.test(password) || !/\d/.test(password)) {
    return { code: 2009, message: '密码需8-32位，包含字母和数字', data: null }
  }

  const user = {
    userId: Random.integer(10002, 99999),
    phone,
    password,
    nickname: nickname || `用户${phone.slice(-4)}`,
    avatar: null,
    gender: 0,
    idCard: null,
    status: 0,
    lastLoginTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    createTime: new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00'),
    version: 0,
    roles: [ROLES.CUSTOMER],
  }
  users.set(phone, user)

  console.log(`[Mock] 注册成功: ${phone} → userId=${user.userId}`)

  return {
    code: 0,
    message: '注册成功',
    data: {
      ...buildAuthResponse(user, ROLES.CUSTOMER),
    },
  }
})

// ========== 3. 用户登录 ==========
Mock.mock(/\/api\/v1\/users\/login/, 'post', (options) => {
  const { phone, loginMode, password, smsCode, targetRole = ROLES.CUSTOMER } = JSON.parse(options.body)

  const user = users.get(phone)
  if (!user) {
    return { code: 2004, message: '手机号未注册', data: null }
  }

  // 检查账号状态
  if (user.status === 1) {
    return { code: 2011, message: '账号已被禁用', data: null }
  }

  if (loginMode === 'password') {
    // 密码登录
    if (!password || password !== user.password) {
      return { code: 2010, message: '密码错误', data: null }
    }
  } else if (loginMode === 'sms') {
    // 验证码登录
    if (smsCode !== '123456') {
      return { code: 2006, message: '验证码错误', data: null }
    }
  } else {
    return { code: 1000, message: '请选择登录方式', data: null }
  }

  if (!Object.values(ROLES).includes(targetRole) || targetRole === ROLES.ADMIN) {
    return { code: 2020, message: '请选择有效的登录身份', data: null }
  }

  if (!(user.roles || [ROLES.CUSTOMER]).includes(targetRole)) {
    return { code: 2021, message: '该账号尚未开通所选身份', data: null }
  }

  user.lastLoginTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')

  console.log(`[Mock] 登录成功: ${phone} (${loginMode}, ${targetRole})`)

  return {
    code: 0,
    message: '登录成功',
    data: {
      ...buildAuthResponse(user, targetRole),
    },
  }
})

// ========== 3.1 查询可用身份与切换身份 ==========
Mock.mock(/\/api\/v1\/profile\/roles$/, 'get', (options) => {
  const user = getUserFromToken(options)
  if (!user) return { code: 1002, message: '登录已过期，请重新登录', data: null }
  return {
    code: 0,
    message: 'success',
    data: {
      roles: user.roles || [ROLES.CUSTOMER],
      merchantId: user.merchantId || null,
      caregiverId: user.caregiverId || null,
    },
  }
})

Mock.mock(/\/api\/v1\/auth\/switch-role$/, 'post', (options) => {
  const user = getUserFromToken(options)
  if (!user) return { code: 1002, message: '登录已过期，请重新登录', data: null }
  const { targetRole } = JSON.parse(options.body || '{}')
  if (!Object.values(ROLES).includes(targetRole) || targetRole === ROLES.ADMIN) {
    return { code: 2020, message: '请选择有效的目标身份', data: null }
  }
  if (!(user.roles || []).includes(targetRole)) {
    return { code: 2021, message: '该账号尚未开通所选身份', data: null }
  }
  return {
    code: 0,
    message: '身份切换成功',
    data: buildAuthResponse(user, targetRole),
  }
})

// ========== 4. 用户登出 ==========
Mock.mock(/\/api\/v1\/users\/logout/, 'post', (options) => {
  const auth = options.headers?.Authorization || ''
  const token = auth.replace('Bearer ', '')

  if (!token || token === 'undefined') {
    return { code: 1002, message: '未授权，请先登录', data: null }
  }

  // Token 加入黑名单
  tokenBlacklist.add(token)
  console.log('[Mock] 登出成功，Token 已加入黑名单')

  return { code: 0, message: '登出成功', data: null }
})

// ========== 5. 重置密码 ==========
Mock.mock(/\/api\/v1\/users\/password\/reset/, 'post', (options) => {
  const { phone, smsCode, newPassword } = JSON.parse(options.body)

  const user = users.get(phone)
  if (!user) {
    return { code: 2004, message: '手机号未注册', data: null }
  }

  if (smsCode !== '123456') {
    return { code: 2006, message: '验证码错误', data: null }
  }

  if (!newPassword || newPassword.length < 8 || !/[a-zA-Z]/.test(newPassword) || !/\d/.test(newPassword)) {
    return { code: 2009, message: '密码需8-32位，包含字母和数字', data: null }
  }

  if (newPassword === user.password) {
    return { code: 2012, message: '新密码不能与旧密码相同', data: null }
  }

  user.password = newPassword
  console.log(`[Mock] 密码重置成功: ${phone}`)

  return { code: 0, message: '密码重置成功', data: null }
})

// ========== 6. 获取个人信息 ==========
Mock.mock(/\/api\/v1\/users\/profile/, 'get', (options) => {
  const auth = options.headers?.Authorization || ''
  const token = auth.replace('Bearer ', '')

  if (!token || tokenBlacklist.has(token)) {
    return { code: 1002, message: '未授权，请先登录', data: null }
  }

  // 简单解析：mock token 里包含 userId
  const userIdMatch = token.match(/mock_jwt_(\d+)/)
  const userId = userIdMatch ? parseInt(userIdMatch[1]) : 10001
  const roleMatch = token.match(/mock_jwt_\d+_(CUSTOMER|CAREGIVER|MERCHANT_MEMBER)_/)
  const currentRole = roleMatch?.[1] || ROLES.CUSTOMER

  // 找到对应用户
  let user = null
  for (const u of users.values()) {
    if (u.userId === userId) { user = u; break }
  }
  if (!user) user = testUser

  // 脱敏身份证号
  const maskedIdCard = user.idCard
    ? user.idCard.slice(0, 3) + '***********' + user.idCard.slice(-4)
    : null

  return {
    code: 0,
    message: 'success',
    data: {
      userId: user.userId,
      phone: maskPhone(user.phone),
      nickname: user.nickname,
      avatar: user.avatar,
      gender: user.gender,
      idCard: maskedIdCard,
      status: user.status,
      lastLoginTime: user.lastLoginTime,
      createTime: user.createTime,
      version: user.version || 0,
      roles: user.roles || [ROLES.CUSTOMER],
      currentRole,
      merchantId: user.merchantId || null,
      caregiverId: user.caregiverId || null,
    },
  }
})

// ========== 7. 修改个人信息 ==========
Mock.mock(/\/api\/v1\/users\/profile/, 'patch', (options) => {
  const auth = options.headers?.Authorization || ''
  const token = auth.replace('Bearer ', '')

  if (!token || tokenBlacklist.has(token)) {
    return { code: 1002, message: '未授权，请先登录', data: null }
  }

  const userIdMatch = token.match(/mock_jwt_(\d+)/)
  const userId = userIdMatch ? parseInt(userIdMatch[1]) : 10001

  const body = JSON.parse(options.body || '{}')

  let user = null
  for (const u of users.values()) {
    if (u.userId === userId) { user = u; break }
  }
  if (!user) user = testUser

  if (body.version === undefined || body.version === null) {
    return { code: 1000, message: 'version 为必填字段', data: null }
  }
  if (body.version !== (user.version || 0)) {
    return { code: 2016, message: '资料版本冲突，请刷新后重试', data: null }
  }

  // 更新字段
  if (body.nickname !== undefined) {
    if (body.nickname.length < 2 || body.nickname.length > 16) {
      return { code: 1000, message: '昵称长度需2-16个字符', data: null }
    }
    user.nickname = body.nickname
  }
  if (body.avatar !== undefined) user.avatar = body.avatar
  if (body.gender !== undefined) user.gender = body.gender
  if (body.idCard !== undefined) {
    if (!/^\d{17}[\dXx]$/.test(body.idCard)) {
      return { code: 2013, message: '身份证号格式不正确', data: null }
    }
    user.idCard = body.idCard
  }
  user.version = (user.version || 0) + 1

  console.log(`[Mock] 个人信息已更新: userId=${userId}`)

  return {
    code: 0,
    message: '修改成功',
    data: {
      userId: user.userId,
      nickname: user.nickname,
      avatar: user.avatar,
      gender: user.gender,
      version: user.version,
    },
  }
})

// ========== 8. 文件上传 ==========
Mock.mock(/\/api\/v1\/files\/upload/, 'post', () => {
  const fileName = Random.string('lower', 10) + '.jpg'
  const fileUrl = `https://cdn.nursing.com/${new Date().toISOString().slice(0, 10).replace(/-/g, '/')}/${fileName}`

  console.log(`[Mock] 文件上传成功 → ${fileUrl}`)

  return {
    code: 0,
    message: '上传成功',
    data: {
      fileUrl,
      fileName,
      fileSize: Random.integer(10240, 204800),
      bizType: 'avatar',
    },
  }
})

console.log('[Mock] 用户模块已加载 (user-service v1.0)')
