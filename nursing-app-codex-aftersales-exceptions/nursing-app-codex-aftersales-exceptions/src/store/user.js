/**
 * Pinia — 用户状态管理
 *
 * 对齐 API v1.0（user-service）：
 * - 登录/注册返回 { token, expireTime, user }
 * - user 结构：userId, phone(脱敏), nickname, avatar, gender, status
 * - 登出调用后端接口，清除本地存储
 * - 支持获取/修改个人信息、文件上传
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getToken,
  setToken,
  getUserInfo,
  setUserInfo,
  clearAuthStorage,
  isLoggedIn as checkLogin,
} from '@/utils/storage.js'
import http, { createIdempotentKey } from '@/utils/request.js'
import { ROLES } from '@/constants/roles.js'
import { useRoleStore } from '@/store/role.js'

export const useUserStore = defineStore(
  'user',
  () => {
    // ===== 状态 =====
    const token = ref(getToken() || '')
    const userInfo = ref(getUserInfo() || null)
    const isLogin = ref(checkLogin())

    // ===== 计算属性 =====
    const isLoggedIn = computed(() => isLogin.value && !!token.value)
    const phone = computed(() => userInfo.value?.phone || '')
    const userId = computed(() => userInfo.value?.userId || null)

    // ===== 内部方法 =====
    /** 登录/注册成功后保存状态 */
    function _onAuthSuccess(data) {
      const { token: newToken, user, permissions = [] } = data
      const roleStore = useRoleStore()
      const roles = data.roles || user?.roles || [data.currentRole || user?.currentRole || ROLES.CUSTOMER]
      const currentRole = data.currentRole || user?.currentRole || roles[0] || ROLES.CUSTOMER
      const normalizedUser = { ...user, roles, currentRole }

      token.value = newToken
      userInfo.value = normalizedUser
      isLogin.value = true
      setToken(newToken)
      setUserInfo(normalizedUser)
      roleStore.applyAuthSession({ roles, currentRole, permissions })
    }

    // ===== 对外方法 =====

    /** 发送短信验证码 */
    async function sendSmsCode(phoneNumber, smsType) {
      const res = await http.post('/api/v1/users/sms-code', {
        phone: phoneNumber,
        smsType,
      }, {
        idempotencyKey: createIdempotentKey(),
      })
      return res
    }

    /**
     * 登录（密码 / 验证码双模式）
     * @param {string} phoneNumber - 手机号
     * @param {string} credential - 密码或验证码
     * @param {'password'|'sms'} loginMode - 登录方式
     * @param {'CUSTOMER'|'CAREGIVER'|'MERCHANT_MEMBER'} targetRole - 目标登录身份
     */
    async function login(phoneNumber, credential, loginMode = 'password', targetRole = ROLES.CUSTOMER) {
      const body = {
        phone: phoneNumber,
        loginMode,
        targetRole,
      }
      if (loginMode === 'password') {
        body.password = credential
      } else {
        body.smsCode = credential
      }

      const res = await http.post('/api/v1/users/login', body)
      _onAuthSuccess(res.data)
      return res
    }

    /**
     * 注册（手机号 + 验证码 + 密码）
     */
    async function register(phoneNumber, smsCode, password, nickname) {
      const res = await http.post('/api/v1/users/register', {
        phone: phoneNumber,
        smsCode,
        password,
        nickname,
      })
      _onAuthSuccess(res.data)
      return res
    }

    /** 退出登录 */
    function doLogout() {
      // Start revocation while the token is still available to the request layer,
      // but never make the user's local sign-out wait for the network.
      const revokeRequest = token.value
        ? http.post('/api/v1/users/logout', null, {
            idempotentKey: createIdempotentKey('logout'),
          })
        : Promise.resolve()
      token.value = ''
      userInfo.value = null
      isLogin.value = false
      clearAuthStorage()
      useRoleStore().clearSession()
      uni.reLaunch({ url: '/pages/login/login' })
      return revokeRequest.catch(() => undefined)
    }

    /** 获取个人信息（启动时校验 Token 有效性） */
    async function fetchProfile() {
      const res = await http.get('/api/v1/users/profile')
      userInfo.value = { ...userInfo.value, ...res.data }
      setUserInfo(userInfo.value)
      return userInfo.value
    }

    async function fetchRoles() {
      const roleStore = useRoleStore()
      return roleStore.availableRoles
    }

    async function switchRole(targetRole) {
      const res = await http.post('/api/v1/auth/switch-role', { targetRole }, {
        idempotentKey: createIdempotentKey('switch-role'),
      })
      _onAuthSuccess(res.data)
      return res.data
    }

    /** 修改个人信息 */
    async function updateProfile(data) {
      const res = await http.patch('/api/v1/users/profile', {
        ...data,
        version: data.version ?? userInfo.value?.version ?? 0,
      }, {
        idempotentKey: createIdempotentKey('profile-update'),
      })
      // 合并更新本地
      userInfo.value = { ...userInfo.value, ...res.data }
      setUserInfo(userInfo.value)
      return res
    }

    /** 上传文件（头像/评价图片/投诉截图） */
    async function uploadFile(filePath, bizType) {
      const bizTypeMap = {
        review: 'review_image',
        complaint: 'complaint_image',
      }
      const normalizedBizType = bizTypeMap[bizType] || bizType
      const res = await http.upload(
        '/api/v1/files/upload',
        filePath,
        { bizType: normalizedBizType },
        { idempotentKey: createIdempotentKey('file') }
      )
      return res.data
    }

    return {
      token,
      userInfo,
      isLogin,
      isLoggedIn,
      phone,
      userId,
      sendSmsCode,
      login,
      register,
      doLogout,
      fetchProfile,
      fetchRoles,
      switchRole,
      updateProfile,
      uploadFile,
    }
  }
)
