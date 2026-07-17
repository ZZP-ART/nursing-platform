/**
 * Mock — 服务目录模块（catalog-service）
 *
 * 对齐 API v1.0：
 * - GET /api/v1/categories            → 服务分类列表
 * - GET /api/v1/items?categoryId=&page=&size=  → 服务列表（分页）
 * - GET /api/v1/items/{id}            → 服务详情
 * - GET /api/v1/items?keyword=        → 搜索
 *
 * 核心变化：每个服务挂多个规格(specs)，价格在规格上
 */
import Mock from 'mockjs'
import { getMockMerchantIdByUserId } from './user.js'
import {
  SERVICE_AUDIT_STATUS,
  SERVICE_PUBLISH_STATUS,
} from '@/constants/service-status.js'

const Random = Mock.Random
let nextItemId = 601
let nextSpecId = 90001

// ========== 服务分类 ==========
const categories = [
  { categoryId: 1, name: '专业护理', icon: 'https://cdn.nursing.com/icons/category/nurse.png', sortOrder: 1, status: 1 },
  { categoryId: 2, name: '康复理疗', icon: 'https://cdn.nursing.com/icons/category/rehab.png', sortOrder: 2, status: 1 },
  { categoryId: 3, name: '中医养生', icon: 'https://cdn.nursing.com/icons/category/tcm.png', sortOrder: 3, status: 1 },
  { categoryId: 4, name: '专项护理', icon: 'https://cdn.nursing.com/icons/category/special.png', sortOrder: 4, status: 1 },
  { categoryId: 5, name: '心理慰藉', icon: 'https://cdn.nursing.com/icons/category/psych.png', sortOrder: 5, status: 1 },
]

// 辅助：给服务生成规格
function makeSpecs(basePrice, itemId) {
  const specId = (id) => itemId * 100 + id
  return [
    { specId: specId(1), name: '单次服务', price: basePrice, originalPrice: Math.round(basePrice * 1.2), duration: 60, status: 1 },
    { specId: specId(2), name: '3次套餐', price: Math.round(basePrice * 2.7), originalPrice: Math.round(basePrice * 3.2), duration: 60, status: 1 },
    { specId: specId(3), name: '5次套餐', price: Math.round(basePrice * 4.3), originalPrice: Math.round(basePrice * 5.2), duration: 60, status: 1 },
  ]
}

// ========== 服务项目 ==========
const serviceItems = [
  {
    itemId: 101, categoryId: 1, categoryName: '专业护理',
    name: '上门输液护理',
    description: '由持证执业护士上门，为您提供专业的**静脉输液护理**服务。\n\n### 服务流程\n1. 核对医嘱与药品\n2. 评估患者状况\n3. 规范输液操作\n4. 观察反应并记录\n\n### 适用人群\n需长期或临时输液的患者',
    coverImage: 'https://cdn.nursing.com/items/101.jpg',
    images: ['https://cdn.nursing.com/items/101-1.jpg', 'https://cdn.nursing.com/items/101-2.jpg'],
    status: 1, sortOrder: 1,
  },
  {
    itemId: 102, categoryId: 1, categoryName: '专业护理',
    name: '压疮护理',
    description: '针对长期卧床老人的**压疮预防与护理**，包括定期翻身、皮肤清洁、敷药换药。\n\n### 服务内容\n- 压疮风险评估\n- 创面清洁与换药\n- 减压措施指导\n- 营养支持建议',
    coverImage: 'https://cdn.nursing.com/items/102.jpg',
    images: ['https://cdn.nursing.com/items/102-1.jpg'],
    status: 1, sortOrder: 2,
  },
  {
    itemId: 103, categoryId: 1, categoryName: '专业护理',
    name: '日常起居照料',
    description: '协助老人完成起床、洗漱、穿衣、饮食等日常生活起居，确保生活舒适整洁。',
    coverImage: 'https://cdn.nursing.com/items/103.jpg',
    images: [],
    status: 1, sortOrder: 3,
  },
  {
    itemId: 201, categoryId: 2, categoryName: '康复理疗',
    name: '康复运动指导',
    description: '专业康复师根据老人身体状况制定运动方案，一对一指导康复训练。',
    coverImage: 'https://cdn.nursing.com/items/201.jpg',
    images: ['https://cdn.nursing.com/items/201-1.jpg', 'https://cdn.nursing.com/items/201-2.jpg'],
    status: 1, sortOrder: 1,
  },
  {
    itemId: 202, categoryId: 2, categoryName: '康复理疗',
    name: '术后康复护理',
    description: '针对术后恢复期老人的全方位护理，包括切口护理、功能锻炼、营养指导。',
    coverImage: 'https://cdn.nursing.com/items/202.jpg',
    images: [],
    status: 1, sortOrder: 2,
  },
  {
    itemId: 203, categoryId: 2, categoryName: '康复理疗',
    name: '推拿按摩',
    description: '专业推拿师上门提供全身或局部推拿按摩，缓解肌肉酸痛、改善血液循环。',
    coverImage: 'https://cdn.nursing.com/items/203.jpg',
    images: [],
    status: 1, sortOrder: 3,
  },
  {
    itemId: 301, categoryId: 3, categoryName: '中医养生',
    name: '艾灸调理',
    description: '传统艾灸疗法，温经通络，调理脏腑，适用于老年人常见慢性病辅助治疗。',
    coverImage: 'https://cdn.nursing.com/items/301.jpg',
    images: [],
    status: 1, sortOrder: 1,
  },
  {
    itemId: 302, categoryId: 3, categoryName: '中医养生',
    name: '拔罐刮痧',
    description: '中医传统疗法拔罐与刮痧，祛风散寒、活血通络，缓解各种酸痛不适。',
    coverImage: 'https://cdn.nursing.com/items/302.jpg',
    images: [],
    status: 1, sortOrder: 2,
  },
  {
    itemId: 401, categoryId: 4, categoryName: '专项护理',
    name: '静脉采血',
    description: '专业护士上门进行静脉采血，规范操作、安全无菌，免去老人往返医院奔波。',
    coverImage: 'https://cdn.nursing.com/items/401.jpg',
    images: [],
    status: 1, sortOrder: 1,
  },
  {
    itemId: 402, categoryId: 4, categoryName: '专项护理',
    name: 'PICC维护',
    description: '专业护士对PICC置管进行定期维护护理，包括冲管、更换敷料、观察评估。',
    coverImage: 'https://cdn.nursing.com/items/402.jpg',
    images: [],
    status: 1, sortOrder: 2,
  },
  {
    itemId: 403, categoryId: 4, categoryName: '专项护理',
    name: '鼻饲护理',
    description: '为需要鼻饲的老人提供专业鼻饲操作及管道维护，确保营养安全摄入。',
    coverImage: 'https://cdn.nursing.com/items/403.jpg',
    images: [],
    status: 1, sortOrder: 3,
  },
  {
    itemId: 501, categoryId: 5, categoryName: '心理慰藉',
    name: '心理陪伴聊天',
    description: '专业心理咨询师上门陪伴聊天，缓解老人孤独感、焦虑情绪，提供心理支持。',
    coverImage: 'https://cdn.nursing.com/items/501.jpg',
    images: [],
    status: 1, sortOrder: 1,
  },
]

// 基础价格表（itemId → 最低单价）
const basePrices = {
  101: 150, 102: 180, 103: 120,
  201: 220, 202: 300, 203: 168,
  301: 128, 302: 98,
  401: 80,  402: 260, 403: 160,
  501: 150,
}

// 为每个服务生成 specs 和 minPrice
serviceItems.forEach((item) => {
  const bp = basePrices[item.itemId] || 150
  item.specs = makeSpecs(bp, item.itemId)
  item.minPrice = Math.min(...item.specs.map((s) => s.price))
  item.createTime = '2026-07-01T10:00:00+08:00'
  item.updateTime = item.createTime
  item.merchantId = 20001
  item.merchantName = '康宁护理中心'
  item.version = 1
  item.auditStatus = SERVICE_AUDIT_STATUS.APPROVED
  item.publishStatus = SERVICE_PUBLISH_STATUS.PUBLISHED
  item.auditRecords = [
    { recordId: `audit-${item.itemId}-1`, action: 'APPROVE', operatorName: '平台审核员', remark: '服务资料完整，审核通过', createTime: '2026-07-01T09:30:00+08:00' },
  ]
})

serviceItems.push(
  {
    itemId: 598,
    categoryId: 1,
    categoryName: '专业护理',
    name: '居家用药指导',
    description: '护理人员上门核对用药清单，并指导老人安全、规范服药。',
    coverImage: '',
    images: [],
    status: 0,
    sortOrder: 20,
    minPrice: 100,
    merchantId: 20001,
    merchantName: '康宁护理中心',
    version: 1,
    auditStatus: SERVICE_AUDIT_STATUS.DRAFT,
    publishStatus: SERVICE_PUBLISH_STATUS.OFFLINE,
    specs: [{ specId: 89801, name: '单次指导', price: 100, originalPrice: 120, duration: 45, status: 1 }],
    auditRecords: [],
    createTime: '2026-07-12T09:00:00+08:00',
    updateTime: '2026-07-12T09:00:00+08:00',
  },
  {
    itemId: 599,
    categoryId: 2,
    categoryName: '康复理疗',
    name: '居家步态训练',
    description: '康复师上门进行步态评估和辅助行走训练。',
    coverImage: '',
    images: [],
    status: 0,
    sortOrder: 21,
    minPrice: 220,
    merchantId: 20001,
    merchantName: '康宁护理中心',
    version: 2,
    auditStatus: SERVICE_AUDIT_STATUS.REJECTED,
    publishStatus: SERVICE_PUBLISH_STATUS.OFFLINE,
    rejectReason: '服务说明过于简单，请补充适用人群、禁忌事项和风险提示。',
    specs: [{ specId: 89901, name: '60分钟训练', price: 220, originalPrice: 260, duration: 60, status: 1 }],
    auditRecords: [
      { recordId: 'audit-599-1', action: 'REJECT', operatorName: '平台审核员', remark: '服务说明过于简单，请补充适用人群、禁忌事项和风险提示。', createTime: '2026-07-13T15:20:00+08:00' },
    ],
    createTime: '2026-07-12T11:00:00+08:00',
    updateTime: '2026-07-13T15:20:00+08:00',
  },
  {
    itemId: 600,
    categoryId: 4,
    categoryName: '专项护理',
    name: '伤口换药护理',
    description: '由具备资质的护理人员上门进行伤口评估、清洁和规范换药。',
    coverImage: '',
    images: [],
    status: 0,
    sortOrder: 22,
    minPrice: 180,
    merchantId: 20001,
    merchantName: '康宁护理中心',
    version: 1,
    auditStatus: SERVICE_AUDIT_STATUS.PENDING_REVIEW,
    publishStatus: SERVICE_PUBLISH_STATUS.OFFLINE,
    specs: [{ specId: 90000, name: '基础换药', price: 180, originalPrice: 210, duration: 60, status: 1 }],
    auditRecords: [
      { recordId: 'audit-600-1', action: 'SUBMIT', operatorName: '商户运营', remark: '提交平台审核', createTime: '2026-07-15T08:40:00+08:00' },
    ],
    createTime: '2026-07-14T16:00:00+08:00',
    updateTime: '2026-07-15T08:40:00+08:00',
  }
)

export function getMockServiceItem(itemId) {
  return serviceItems.find((item) => item.itemId === Number(itemId) && item.status === 1) || null
}

export function getMockServiceSpec(itemId, specId) {
  const item = getMockServiceItem(itemId)
  return item?.specs.find((spec) => spec.specId === Number(specId) && spec.status === 1) || null
}

// ========== 工具函数 ==========
function getQueryParam(url, param) {
  const regex = new RegExp(`[?&]${param}=([^&]*)`)
  const match = url.match(regex)
  return match ? decodeURIComponent(match[1]) : null
}

/** 构建服务列表项（含 minPrice 和第一个规格） */
function buildListItem(item) {
  return {
    id: item.itemId,
    categoryId: item.categoryId,
    categoryName: item.categoryName,
    name: item.name,
    description: item.description,
    coverImage: item.coverImage,
    sortOrder: item.sortOrder,
    status: item.status,
    minPrice: item.minPrice,
    specs: item.specs.filter((s) => s.status === 1).map((spec) => ({
      ...spec,
      id: spec.specId,
      serviceItemId: item.itemId,
      specId: undefined,
    })),
  }
}

/** 构建服务详情 */
function buildDetail(item) {
  return {
    id: item.itemId,
    categoryId: item.categoryId,
    categoryName: item.categoryName,
    name: item.name,
    description: item.description,
    coverImage: item.coverImage,
    images: item.images,
    sortOrder: item.sortOrder,
    status: item.status,
    specs: item.specs.filter((s) => s.status === 1).map((spec) => ({
      ...spec,
      id: spec.specId,
      serviceItemId: item.itemId,
      specId: undefined,
    })),
    minPrice: item.minPrice,
    createTime: item.createTime,
  }
}

function getMerchantIdFromRequest(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const userId = Number(auth.match(/mock_jwt_(\d+)_MERCHANT_MEMBER_/)?.[1])
  return getMockMerchantIdByUserId(userId)
}

function getManagedService(options, itemId) {
  const merchantId = getMerchantIdFromRequest(options)
  if (!merchantId) return { error: { code: 1004, message: '暂无商户权限', data: null } }
  const item = serviceItems.find((service) => service.itemId === Number(itemId))
  if (!item) return { error: { code: 1005, message: '服务项目不存在', data: null } }
  if (item.merchantId !== merchantId) return { error: { code: 1004, message: '无权操作该服务', data: null } }
  return { item, merchantId }
}

function buildManagedService(item) {
  return {
    ...item,
    specs: item.specs.map((spec) => ({ ...spec, serviceItemId: item.itemId })),
  }
}

function validateServiceBody(body) {
  if (!body.name?.trim()) return '请填写服务名称'
  if (!body.categoryId) return '请选择服务分类'
  if (!body.description?.trim() || body.description.trim().length < 10) return '服务说明至少10个字符'
  if (!Array.isArray(body.specs) || !body.specs.length) return '至少添加一个服务规格'
  for (const spec of body.specs) {
    if (!spec.name?.trim() || Number(spec.price) <= 0 || Number(spec.duration) <= 0) {
      return '请完整填写规格名称、价格和服务时长'
    }
  }
  return ''
}

function normalizeSpecs(specs = []) {
  return specs.map((spec) => ({
    specId: spec.specId || nextSpecId++,
    name: spec.name.trim(),
    price: Number(spec.price),
    originalPrice: Number(spec.originalPrice || spec.price),
    duration: Number(spec.duration),
    status: 1,
  }))
}

function resolvePendingReviews() {
  const now = Date.now()
  serviceItems.forEach((item) => {
    if (item.auditStatus !== SERVICE_AUDIT_STATUS.PENDING_REVIEW || !item.reviewReadyAt) return
    if (now < item.reviewReadyAt) return
    item.auditStatus = SERVICE_AUDIT_STATUS.APPROVED
    item.rejectReason = ''
    item.updateTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
    item.auditRecords.unshift({
      recordId: `audit-${item.itemId}-${Random.string('lower', 6)}`,
      action: 'APPROVE',
      operatorName: '模拟平台审核员',
      remark: '服务资料符合要求，审核通过',
      createTime: item.updateTime,
    })
    delete item.reviewReadyAt
  })
}

// ========== 接口 Mock ==========

// 1. 服务分类列表
Mock.mock(/\/api\/v1\/categories$/, 'get', () => {
  return {
    code: 0,
    message: 'success',
    data: categories.filter((c) => c.status === 1).map((category) => ({
      ...category,
      parentId: 0,
    })),
  }
})

// 2. 服务项目列表 + 搜索（同一端点 /api/v1/items，通过参数区分）
Mock.mock(/\/api\/v1\/items(\?|$)/, 'get', (options) => {
  const url = options.url
  const categoryId = getQueryParam(url, 'categoryId')
  const keyword = getQueryParam(url, 'keyword')
  const cursor = getQueryParam(url, 'cursor')
  const size = getQueryParam(url, 'size') || '20'

  let list = serviceItems.filter((s) => s.status === 1)

  // 分类筛选
  if (categoryId) {
    const catId = parseInt(categoryId)
    // 检查分类是否存在
    if (!categories.find((c) => c.categoryId === catId)) {
      return { code: 1005, message: '分类不存在', data: null }
    }
    list = list.filter((s) => s.categoryId === catId)
  }

  // 关键词搜索
  if (keyword) {
    if (!keyword.trim()) {
      return { code: 1000, message: '关键词不能为空', data: null }
    }
    const kw = keyword.toLowerCase()
    list = list.filter(
      (s) =>
        s.name.toLowerCase().includes(kw) ||
        (s.description && s.description.toLowerCase().includes(kw))
    )
  }

  // 按 sortOrder 排序
  list.sort((a, b) => a.sortOrder - b.sortOrder)

  const offset = cursor?.startsWith('cursor_') ? Number(cursor.slice(7)) || 0 : 0
  const pageSize = Math.min(parseInt(size) || 20, 50)
  const pageList = list.slice(offset, offset + pageSize).map(buildListItem)
  const nextOffset = offset + pageList.length
  const hasNext = nextOffset < list.length

  return {
    code: 0,
    message: 'success',
    data: {
      list: pageList,
      size: pageSize,
      hasNext,
      nextCursor: hasNext ? `cursor_${nextOffset}` : null,
    },
  }
})

// 3. 服务详情（需要精确匹配 /api/v1/items/{id}，不跟列表/搜索冲突）
Mock.mock(/\/api\/v1\/items\/\d+$/, 'get', (options) => {
  const idMatch = options.url.match(/\/api\/v1\/items\/(\d+)/)
  const itemId = idMatch ? parseInt(idMatch[1]) : null

  if (!itemId) {
    return { code: 1000, message: '参数错误', data: null }
  }

  const item = serviceItems.find((s) => s.itemId === itemId && s.status === 1)
  if (!item) {
    return { code: 1005, message: '服务项目不存在', data: null }
  }

  return {
    code: 0,
    message: 'success',
    data: buildDetail(item),
  }
})

// 4. 商户服务列表
Mock.mock(/\/api\/v1\/merchants\/services(?:\?|$)/, 'get', (options) => {
  resolvePendingReviews()
  const merchantId = getMerchantIdFromRequest(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const auditStatus = getQueryParam(options.url, 'auditStatus')
  const publishStatus = getQueryParam(options.url, 'publishStatus')
  const keyword = getQueryParam(options.url, 'keyword')?.trim()
  let list = serviceItems.filter((item) => item.merchantId === merchantId)
  if (auditStatus) list = list.filter((item) => item.auditStatus === auditStatus)
  if (publishStatus) list = list.filter((item) => item.publishStatus === publishStatus)
  if (keyword) list = list.filter((item) => item.name.includes(keyword))
  list.sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime))
  const summary = {
    total: serviceItems.filter((item) => item.merchantId === merchantId).length,
    draft: serviceItems.filter((item) => item.merchantId === merchantId && item.auditStatus === SERVICE_AUDIT_STATUS.DRAFT).length,
    pending: serviceItems.filter((item) => item.merchantId === merchantId && item.auditStatus === SERVICE_AUDIT_STATUS.PENDING_REVIEW).length,
    approved: serviceItems.filter((item) => item.merchantId === merchantId && item.auditStatus === SERVICE_AUDIT_STATUS.APPROVED).length,
    rejected: serviceItems.filter((item) => item.merchantId === merchantId && item.auditStatus === SERVICE_AUDIT_STATUS.REJECTED).length,
    published: serviceItems.filter((item) => item.merchantId === merchantId && item.publishStatus === SERVICE_PUBLISH_STATUS.PUBLISHED).length,
  }
  return { code: 0, message: 'success', data: { list: list.map(buildManagedService), summary } }
})

// 5. 商户服务详情
Mock.mock(/\/api\/v1\/merchants\/services\/\d+$/, 'get', (options) => {
  resolvePendingReviews()
  const itemId = Number(options.url.match(/\/services\/(\d+)$/)?.[1])
  const result = getManagedService(options, itemId)
  if (result.error) return result.error
  return { code: 0, message: 'success', data: buildManagedService(result.item) }
})

// 6. 创建服务草稿
Mock.mock(/\/api\/v1\/merchants\/services$/, 'post', (options) => {
  const merchantId = getMerchantIdFromRequest(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const body = JSON.parse(options.body || '{}')
  const validationMessage = validateServiceBody(body)
  if (validationMessage) return { code: 1000, message: validationMessage, data: null }
  const category = categories.find((item) => item.categoryId === Number(body.categoryId))
  if (!category) return { code: 1005, message: '服务分类不存在', data: null }
  const now = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
  const specs = normalizeSpecs(body.specs)
  const item = {
    itemId: nextItemId++,
    categoryId: category.categoryId,
    categoryName: category.name,
    name: body.name.trim(),
    description: body.description.trim(),
    coverImage: body.coverImage || '',
    images: body.images || [],
    status: 0,
    sortOrder: serviceItems.length + 1,
    minPrice: Math.min(...specs.map((spec) => spec.price)),
    merchantId,
    merchantName: '康宁护理中心',
    version: 1,
    auditStatus: SERVICE_AUDIT_STATUS.DRAFT,
    publishStatus: SERVICE_PUBLISH_STATUS.OFFLINE,
    specs,
    auditRecords: [],
    createTime: now,
    updateTime: now,
  }
  serviceItems.push(item)
  return { code: 0, message: '服务草稿已创建', data: buildManagedService(item) }
})

// 7. 修改服务草稿或驳回服务
Mock.mock(/\/api\/v1\/merchants\/services\/\d+$/, 'put', (options) => {
  const itemId = Number(options.url.match(/\/services\/(\d+)$/)?.[1])
  const result = getManagedService(options, itemId)
  if (result.error) return result.error
  const { item } = result
  if (![SERVICE_AUDIT_STATUS.DRAFT, SERVICE_AUDIT_STATUS.REJECTED].includes(item.auditStatus)) {
    return { code: 6002, message: '当前审核状态不可编辑', data: null }
  }
  const body = JSON.parse(options.body || '{}')
  const validationMessage = validateServiceBody(body)
  if (validationMessage) return { code: 1000, message: validationMessage, data: null }
  const category = categories.find((entry) => entry.categoryId === Number(body.categoryId))
  if (!category) return { code: 1005, message: '服务分类不存在', data: null }
  item.categoryId = category.categoryId
  item.categoryName = category.name
  item.name = body.name.trim()
  item.description = body.description.trim()
  item.coverImage = body.coverImage || ''
  item.images = body.images || []
  item.specs = normalizeSpecs(body.specs)
  item.minPrice = Math.min(...item.specs.map((spec) => spec.price))
  item.version += 1
  item.auditStatus = SERVICE_AUDIT_STATUS.DRAFT
  item.rejectReason = ''
  item.updateTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
  return { code: 0, message: '服务草稿已保存', data: buildManagedService(item) }
})

// 8. 提交审核
Mock.mock(/\/api\/v1\/merchants\/services\/\d+\/submit$/, 'post', (options) => {
  const itemId = Number(options.url.match(/\/services\/(\d+)\/submit/)?.[1])
  const result = getManagedService(options, itemId)
  if (result.error) return result.error
  const { item } = result
  if (![SERVICE_AUDIT_STATUS.DRAFT, SERVICE_AUDIT_STATUS.REJECTED].includes(item.auditStatus)) {
    return { code: 6003, message: '当前服务不可提交审核', data: null }
  }
  const validationMessage = validateServiceBody(item)
  if (validationMessage) return { code: 1000, message: validationMessage, data: null }
  const now = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
  item.auditStatus = SERVICE_AUDIT_STATUS.PENDING_REVIEW
  item.publishStatus = SERVICE_PUBLISH_STATUS.OFFLINE
  item.status = 0
  item.reviewReadyAt = Date.now() + 8000
  item.updateTime = now
  item.auditRecords.unshift({ recordId: `audit-${item.itemId}-${Random.string('lower', 6)}`, action: 'SUBMIT', operatorName: '商户运营', remark: '提交平台审核', createTime: now })
  return { code: 0, message: '已提交审核，Mock 将在约8秒后自动通过', data: buildManagedService(item) }
})

// 9. 上架与下架
Mock.mock(/\/api\/v1\/merchants\/services\/\d+\/publish$/, 'post', (options) => {
  resolvePendingReviews()
  const itemId = Number(options.url.match(/\/services\/(\d+)\/publish/)?.[1])
  const result = getManagedService(options, itemId)
  if (result.error) return result.error
  const { item } = result
  if (item.auditStatus !== SERVICE_AUDIT_STATUS.APPROVED) return { code: 6004, message: '服务尚未审核通过', data: null }
  item.publishStatus = SERVICE_PUBLISH_STATUS.PUBLISHED
  item.status = 1
  item.updateTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
  return { code: 0, message: '服务已上架', data: buildManagedService(item) }
})

Mock.mock(/\/api\/v1\/merchants\/services\/\d+\/offline$/, 'post', (options) => {
  const itemId = Number(options.url.match(/\/services\/(\d+)\/offline/)?.[1])
  const result = getManagedService(options, itemId)
  if (result.error) return result.error
  const { item } = result
  item.publishStatus = SERVICE_PUBLISH_STATUS.OFFLINE
  item.status = 0
  item.updateTime = new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
  return { code: 0, message: '服务已下架', data: buildManagedService(item) }
})

console.log('[Mock] 服务目录模块已加载 (catalog-service v1.0)')
