/**
 * 网络请求层 — uni.request 封装
 *
 * 对齐后端 API 规范 v1.0：
 * - 统一响应 { code: 0, message, data } — code=0 为成功
 * - 全局错误码 1000-1999 通用 / 2000-2999 用户 / 3000-3999 订单 / 4000-4999 评价投诉
 * - JWT Bearer Token 鉴权，401 + code=1002/1003 跳转登录
 * - 支持 Idempotency-Key 幂等请求头
 * - 支持 PATCH / DELETE 方法
 */
import { clearAuthStorage, getToken } from './storage.js'
import devConfig from '../../config/dev.js'
import prodConfig from '../../config/prod.js'

// ========== 环境配置 ==========
// DEV 环境指向 Gateway，PROD 由 CI/CD 写入
const runtimeConfig = import.meta.env.DEV ? devConfig : prodConfig
const BASE_URL = import.meta.env.VITE_API_BASE_URL || runtimeConfig.BASE_URL
const TIMEOUT = 15000

// ========== 全局错误码映射（按区间） ==========
const ERROR_MESSAGES = {
  // 通用 1000-1999
  1000: '请求参数有误',
  1001: '请求格式错误',
  1002: '登录已过期，请重新登录',
  1003: '账号已在其他设备登录',
  1004: '暂无权限执行此操作',
  1005: '请求的资源不存在',
  1006: '操作冲突，请勿重复提交',
  1007: '暂不满足操作条件',
  1008: '操作过于频繁，请稍后重试',
  1009: '依赖服务暂不可用，请稍后重试',
  // 用户 2000-2999
  2001: '验证码发送过于频繁，请60秒后重试',
  2002: '今日验证码发送次数已达上限',
  2003: '该手机号已被注册',
  2004: '该手机号未注册',
  2006: '验证码错误',
  2007: '验证码已过期',
  2008: '该手机号已注册',
  2009: '密码不符合安全要求',
  2010: '密码错误',
  2011: '账号已被禁用',
  2012: '新密码不能与旧密码相同',
  2013: '身份证号格式不正确',
  2014: '文件大小超过限制',
  2015: '文件为空或保存失败',
  2016: '资料已被更新，请刷新后重试',
  2017: '上传请求冲突，请重新选择文件',
  2018: '文件上传处理中，请稍后重试',
  2019: '短信请求幂等键冲突',
  2020: '请选择有效的登录身份',
  2021: '该账号尚未开通所选身份',
  // 订单 3000-3999
  3001: '下单令牌已过期，请重新提交',
  3002: '该时段已被预约',
  3003: '服务项目已下架',
  3004: '规格已下架',
  3005: '价格已变动，请重新确认',
  3006: '地址不存在或已被删除',
  3007: '订单不存在',
  3008: '无权查看该订单',
  3009: '当前状态不可取消',
  3010: '当前状态不可支付',
  3013: '地址不存在或无权操作',
  // 评价投诉 4000-4999
  4002: '该订单暂不可评价',
  4003: '该订单已评价',
  4006: '投诉记录不存在',
  4007: '无权查看该投诉',
  // 护理人员和派单 5000-5999
  5001: '护理人员当前不可接单',
  5002: '派单记录不存在',
  5004: '请先完成到达签到',
  5005: '护理人员排班、区域或接单上限存在冲突',
  // 商户服务 6000-6999
  6002: '当前审核状态不可编辑',
  6003: '当前服务不可提交审核',
  6004: '服务尚未审核通过',
  // 护理人员认证 7000-7999
  7001: '当前申请状态不可修改',
  7002: '护理人员认证资料不完整',
  7003: '护理人员身份尚未认证通过',
  // 商户入驻 8000-8999
  8001: '当前入驻申请状态不可修改',
  8002: '商户入驻资料不完整',
  8003: '商户入驻尚未审核通过',
  // 商户团队与成员 9000-9999
  9001: '护理人员合作关系不存在',
  9002: '已存在有效合作关系或待确认邀请',
  9003: '当前合作状态不可执行该操作',
  9004: '商户成员不存在',
  9005: '商户负责人不可被停用',
  9101: '异常工单不存在',
  9102: '当前没有可接单护理人员',
  9301: '提现金额超过可用余额',
}

/** 根据错误码获取友好提示 */
function getErrorMessage(code, fallback) {
  return ERROR_MESSAGES[code] || fallback || '服务繁忙，请稍后重试'
}

/** 判断是否为鉴权失败（需跳登录页） */
function isAuthError(code, httpStatus) {
  return httpStatus === 401 || code === 1002 || code === 1003
}

// ========== 请求中的幂等键缓存 ==========
let _idempotentKey = null

/** 设置全局幂等键（下单流程前调用） */
export function setIdempotentKey(key) {
  _idempotentKey = key
}

/** 获取当前幂等键 */
export function getIdempotentKey() {
  return _idempotentKey
}

/** 清除幂等键 */
export function clearIdempotentKey() {
  _idempotentKey = null
}

// ========== 核心请求方法 ==========

/**
 * 统一请求
 * @param {Object} options - { url, method, data, header, timeout, idempotent }
 * @returns {Promise}
 */
function request(options = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()

    // 构建请求头
    const header = {
      'Content-Type': 'application/json',
      ...options.header,
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }
    // 幂等键：优先使用 options 传入的，其次全局的
    const idempotentKey = options.idempotencyKey || options.idempotentKey || options.idempotent || _idempotentKey
    if (idempotentKey) {
      header['Idempotency-Key'] = idempotentKey
    }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      timeout: options.timeout || TIMEOUT,
      success: (res) => {
        const { statusCode, data } = res

        // 业务成功：code === 0
        if (statusCode === 200 || statusCode === 201) {
          if (data.code === 0) {
            resolve(data)
          } else if (isAuthError(data.code, statusCode)) {
            // 登录态过期 / Token 黑名单
            clearAuthStorage()
            uni.showToast({ title: getErrorMessage(data.code, '请重新登录'), icon: 'none' })
            setTimeout(() => {
              uni.reLaunch({ url: '/pages/login/login' })
            }, 1500)
            reject(data)
          } else {
            // 其他业务错误
            const msg = getErrorMessage(data.code, data.message)
            uni.showToast({ title: msg, icon: 'none' })
            reject(data)
          }
        } else if (isAuthError(data?.code, statusCode)) {
          clearAuthStorage()
          uni.showToast({ title: getErrorMessage(data?.code, '登录已过期，请重新登录'), icon: 'none' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/login' })
          }, 1500)
          reject(data || res)
        } else if (data?.code) {
          uni.showToast({ title: getErrorMessage(data.code, data.message), icon: 'none' })
          reject(data)
        } else if (statusCode >= 500) {
          uni.showToast({ title: '服务器繁忙，请稍后重试', icon: 'none' })
          reject(res)
        } else {
          uni.showToast({ title: `请求异常 (${statusCode})`, icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        const msg = err.errMsg || '网络连接失败'
        if (msg.includes('timeout')) {
          uni.showToast({ title: '请求超时，请重试', icon: 'none' })
        } else if (msg.includes('fail')) {
          uni.showToast({ title: '网络不可用，请检查连接', icon: 'none' })
        }
        reject(err)
      },
    })
  })
}

// ========== 快捷方法 ==========

const http = {
  get(url, data, options = {}) {
    return request({ url, method: 'GET', data, ...options })
  },
  post(url, data, options = {}) {
    return request({ url, method: 'POST', data, ...options })
  },
  put(url, data, options = {}) {
    return request({ url, method: 'PUT', data, ...options })
  },
  patch(url, data, options = {}) {
    return request({ url, method: 'PATCH', data, ...options })
  },
  delete(url, data, options = {}) {
    return request({ url, method: 'DELETE', data, ...options })
  },

  /** 上传文件（multipart/form-data） */
  upload(url, filePath, formData = {}, options = {}) {
    const token = getToken()
    const idempotentKey = options.idempotencyKey || options.idempotentKey || createIdempotentKey('upload')
    return new Promise((resolve, reject) => {
      uni.uploadFile({
        url: BASE_URL + url,
        filePath,
        name: 'file',
        formData,
        header: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          'Idempotency-Key': idempotentKey,
          ...options.header,
        },
        timeout: options.timeout || 30000,
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 0) {
              resolve(data)
            } else {
              uni.showToast({ title: data.message || '上传失败', icon: 'none' })
              reject(data)
            }
          } catch {
            reject(res)
          }
        },
        fail: reject,
      })
    })
  },
}

/** 生成客户端幂等键；短信接口要求 UUID，其他接口也可复用。 */
export function createIdempotentKey(prefix = '') {
  const uuid = globalThis.crypto?.randomUUID?.() ||
    'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
      const random = Math.floor(Math.random() * 16)
      const value = char === 'x' ? random : (random & 0x3) | 0x8
      return value.toString(16)
    })
  return prefix ? `${prefix}-${uuid}`.slice(0, 64) : uuid
}

/** 将后端返回的网关相对资源路径转换为可直接访问的 URL。 */
export function resolveAssetUrl(url) {
  if (!url || /^(https?:|data:|blob:)/i.test(url)) return url || ''
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}

export default http
export { request, BASE_URL }
