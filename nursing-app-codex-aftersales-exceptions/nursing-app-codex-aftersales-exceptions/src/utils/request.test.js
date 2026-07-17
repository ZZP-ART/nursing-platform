/**
 * 网络请求层 — 单元测试
 *
 * 覆盖：错误码映射、幂等键管理、HTTP 快捷方法导出
 */
import { describe, it, expect, beforeEach } from 'vitest'
import http, {
  setIdempotentKey,
  getIdempotentKey,
  clearIdempotentKey,
  request,
  BASE_URL,
  createIdempotentKey,
  resolveAssetUrl,
} from '@/utils/request.js'

describe('request — 错误码映射', () => {
  // 模拟 request 的基础行为 — 测试 Mock 的 uni.request
  // 因为 request 使用 uni.request，所以在此测试 Mock 集成

  it('BASE_URL 为开发环境地址', () => {
    expect(BASE_URL).toBe('/backend')
  })

  it('http 对象包含所有 HTTP 快捷方法', () => {
    expect(typeof http.get).toBe('function')
    expect(typeof http.post).toBe('function')
    expect(typeof http.put).toBe('function')
    expect(typeof http.patch).toBe('function')
    expect(typeof http.delete).toBe('function')
    expect(typeof http.upload).toBe('function')
  })

  it('生成 UUID 格式幂等键', () => {
    expect(createIdempotentKey()).toMatch(/^[0-9a-f-]{36}$/i)
  })

  it('相对资源路径自动拼接网关地址', () => {
    expect(resolveAssetUrl('/uploads/a.png')).toBe('/backend/uploads/a.png')
  })

  it('http.get 调用 uni.request 并返回 Promise', async () => {
    uni.request.mockImplementationOnce((opts) => {
      opts.success({ statusCode: 200, data: { code: 0, message: 'success', data: { ok: true } } })
    })
    const res = await http.get('/api/v1/categories')
    expect(res.code).toBe(0)
    expect(res.data).toEqual({ ok: true })
  })

  it('http.post 携带正确的 method POST', async () => {
    uni.request.mockImplementationOnce((opts) => {
      expect(opts.method).toBe('POST')
      opts.success({ statusCode: 201, data: { code: 0, message: 'ok', data: null } })
    })
    await http.post('/api/v1/users/login', { phone: '13800138000', loginMode: 'password', password: '123456' })
  })

  it('http.get 将游标参数作为 query data 传入', async () => {
    uni.request.mockImplementationOnce((opts) => {
      expect(opts.data).toEqual({ cursor: 'cursor_20', size: 20 })
      opts.success({ statusCode: 200, data: { code: 0, message: 'ok', data: { list: [], total: 0 } } })
    })
    await http.get('/api/v1/items', { cursor: 'cursor_20', size: 20 })
  })

  it('业务错误码（非 auth）时 reject', async () => {
    uni.request.mockImplementationOnce((opts) => {
      opts.success({ statusCode: 200, data: { code: 2006, message: '验证码错误', data: null } })
    })
    await expect(http.post('/api/v1/users/login', {})).rejects.toEqual(
      expect.objectContaining({ code: 2006 }),
    )
    expect(uni.showToast).toHaveBeenCalledWith(
      expect.objectContaining({ title: '验证码错误' }),
    )
  })
})

describe('request — 幂等键管理', () => {
  beforeEach(() => {
    clearIdempotentKey()
  })

  it('初始状态幂等键为 null', () => {
    expect(getIdempotentKey()).toBeNull()
  })

  it('setIdempotentKey 设置后可读取', () => {
    setIdempotentKey('idem-abc-123')
    expect(getIdempotentKey()).toBe('idem-abc-123')
  })

  it('clearIdempotentKey 后为 null', () => {
    setIdempotentKey('idem-xyz')
    clearIdempotentKey()
    expect(getIdempotentKey()).toBeNull()
  })

  it('幂等键会添加到请求头', async () => {
    setIdempotentKey('idem-test-456')
    uni.request.mockImplementationOnce((opts) => {
      expect(opts.header['Idempotency-Key']).toBe('idem-test-456')
      opts.success({ statusCode: 200, data: { code: 0, message: 'ok', data: null } })
    })
    await http.post('/api/v1/orders', { itemId: 1 })
  })

  it('options 传入的幂等键优先于全局', async () => {
    setIdempotentKey('global-key')
    uni.request.mockImplementationOnce((opts) => {
      expect(opts.header['Idempotency-Key']).toBe('inline-key')
      opts.success({ statusCode: 200, data: { code: 0, message: 'ok', data: null } })
    })
    // 通过 request 直接调用以传入 idempotent 选项
    await request({ url: '/api/v1/orders', method: 'POST', data: {}, idempotent: 'inline-key' })
  })

  it('短信接口支持 Idempotency-Key 请求头', async () => {
    uni.request.mockImplementationOnce((opts) => {
      expect(opts.header['Idempotency-Key']).toBe('00000000-0000-4000-8000-000000000001')
      opts.success({ statusCode: 200, data: { code: 0, message: 'ok', data: null } })
    })
    await http.post('/api/v1/users/sms-code', {}, {
      idempotencyKey: '00000000-0000-4000-8000-000000000001',
    })
  })
})
