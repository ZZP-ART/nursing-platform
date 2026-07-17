/**
 * Mock — 地址管理模块（order-service）
 *
 * 对齐 API v1.0：
 * - GET    /api/v1/addresses              → 地址列表
 * - POST   /api/v1/addresses              → 新增地址
 * - PUT    /api/v1/addresses/{id}         → 编辑地址
 * - DELETE /api/v1/addresses/{id}         → 删除地址（逻辑删除）
 * - PUT    /api/v1/addresses/{id}/default → 设置默认地址
 */
import Mock from 'mockjs'

// ========== 模拟地址数据 ==========
const addresses = [
  {
    addressId: 5001,
    userId: 10001,
    receiverName: '张三',
    receiverPhone: '13800138000',
    tag: '家',
    province: '北京市',
    city: '北京市',
    district: '朝阳区',
    detailAddress: '建国路88号6栋301',
    isDefault: 1,
  },
  {
    addressId: 5002,
    userId: 10001,
    receiverName: '张三',
    receiverPhone: '13800138000',
    tag: '公司',
    province: '北京市',
    city: '北京市',
    district: '海淀区',
    detailAddress: '中关村大街1号8层',
    isDefault: 0,
  },
  {
    addressId: 5003,
    userId: 10001,
    receiverName: '李四',
    receiverPhone: '13900139000',
    tag: '其他',
    province: '上海市',
    city: '上海市',
    district: '浦东新区',
    detailAddress: '张江高科技园区碧波路690号',
    isDefault: 0,
  },
]

let nextId = 5004

// ========== 工具 ==========
function getUserAddresses() {
  return addresses.filter((a) => a.isDeleted !== 1)
}

export function getMockAddress(addressId) {
  return addresses.find((address) => address.addressId === Number(addressId) && address.isDeleted !== 1) || null
}

// ========== 接口 Mock ==========

// 1. 地址列表
Mock.mock(/\/api\/v1\/addresses$/, 'get', () => {
  const list = getUserAddresses()
  // 默认地址排首位
  list.sort((a, b) => b.isDefault - a.isDefault || b.addressId - a.addressId)
  return {
    code: 0,
    message: 'success',
    data: list,
  }
})

// 2. 新增地址
Mock.mock(/\/api\/v1\/addresses$/, 'post', (options) => {
  const body = JSON.parse(options.body)

  if (!body.receiverName || !body.receiverPhone || !body.detailAddress) {
    return { code: 1000, message: '收件人姓名、电话、详细地址为必填', data: null }
  }

  // 如果设为默认，其他地址默认取消
  if (body.isDefault === 1) {
    getUserAddresses().forEach((a) => { a.isDefault = 0 })
  }

  const addr = {
    addressId: nextId++,
    userId: 10001,
    receiverName: body.receiverName,
    receiverPhone: body.receiverPhone,
    tag: body.tag || '家',
    province: body.province || '',
    city: body.city || '',
    district: body.district || '',
    detailAddress: body.detailAddress,
    isDefault: body.isDefault || 0,
  }
  addresses.push(addr)

  console.log(`[Mock] 地址已添加: addressId=${addr.addressId}`)

  return {
    code: 0,
    message: '地址添加成功',
    data: { addressId: addr.addressId },
  }
})

// 3. 编辑地址
Mock.mock(/\/api\/v1\/addresses\/\d+$/, 'patch', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/addresses\/(\d+)/)
  const addressId = idMatch ? parseInt(idMatch[1]) : null
  const body = JSON.parse(options.body)
  const addr = addresses.find((a) => a.addressId === addressId && a.isDeleted !== 1)

  if (!addr) {
    return { code: 1005, message: '地址不存在', data: null }
  }

  // 如果设为默认，其他地址默认取消
  if (body.isDefault === 1) {
    getUserAddresses().forEach((a) => { a.isDefault = 0 })
  }

  Object.keys(body).forEach((key) => {
    if (body[key] !== undefined) addr[key] = body[key]
  })

  console.log(`[Mock] 地址已修改: addressId=${addressId}`)

  return { code: 0, message: '地址修改成功', data: null }
})

// 4. 删除地址
Mock.mock(/\/api\/v1\/addresses\/\d+/, 'delete', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/addresses\/(\d+)/)
  const addressId = idMatch ? parseInt(idMatch[1]) : null
  const addr = addresses.find((a) => a.addressId === addressId && a.isDeleted !== 1)

  if (!addr) {
    return { code: 1005, message: '地址不存在', data: null }
  }

  addr.isDeleted = 1
  console.log(`[Mock] 地址已删除: addressId=${addressId}`)

  return { code: 0, message: '地址已删除', data: null }
})

// 5. 设置默认地址
Mock.mock(/\/api\/v1\/addresses\/\d+\/default/, 'put', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/addresses\/(\d+)\/default/)
  const addressId = idMatch ? parseInt(idMatch[1]) : null
  const addr = addresses.find((a) => a.addressId === addressId && a.isDeleted !== 1)

  if (!addr) {
    return { code: 1005, message: '地址不存在', data: null }
  }

  getUserAddresses().forEach((a) => { a.isDefault = 0 })
  addr.isDefault = 1

  console.log(`[Mock] 默认地址已设置: addressId=${addressId}`)

  return { code: 0, message: '默认地址设置成功', data: null }
})

console.log('[Mock] 地址模块已加载 (order-service / addresses)')
