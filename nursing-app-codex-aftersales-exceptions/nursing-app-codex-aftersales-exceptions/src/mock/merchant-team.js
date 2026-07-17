import Mock from 'mockjs'
import { COOPERATION_STATUS, MEMBER_STATUS } from '@/constants/team-status.js'
import { getMockMerchantIdByUserId } from './user.js'
import { getCaregiverSchedule, getScheduleSummary } from './caregiver-schedule-state.js'

const caregiverRelations = [
  {
    relationId: 71001, merchantId: 20001, caregiverId: 50001, name: '王护理员', phone: '138****8000', rawPhone: '13800138000', avatar: '', status: COOPERATION_STATUS.ACTIVE, available: true, rating: 4.9, completedOrders: 126, joinTime: '2026-03-12 10:30', invitationTime: '2026-03-10 09:00', workYears: 6, serviceAreas: ['朝阳区', '海淀区'], skills: ['生活照护', '用药指导', '术后护理'], certificates: [{ name: '养老护理员职业技能等级证书', number: 'YLHL-2020-0088', validUntil: '2030-12-31', status: 'VALID' }], scheduleSummary: '本周可服务 5 天', currentTaskCount: 2, remark: '核心护理人员', records: [{ action: 'ACCEPT', title: '护理人员接受合作邀请', time: '2026-03-12 10:30', operator: '王护理员' }, { action: 'INVITE', title: '商户发出合作邀请', time: '2026-03-10 09:00', operator: '赵康宁' }],
  },
  {
    relationId: 71002, merchantId: 20001, caregiverId: 50002, name: '李护理员', phone: '138****8001', rawPhone: '13800138001', avatar: '', status: COOPERATION_STATUS.ACTIVE, available: true, rating: 4.8, completedOrders: 98, joinTime: '2026-04-05 14:20', invitationTime: '2026-04-03 11:00', workYears: 4, serviceAreas: ['朝阳区'], skills: ['生活照护', '陪诊陪护', '康复护理'], certificates: [{ name: '养老护理员职业技能等级证书', number: 'YLHL-2022-0126', validUntil: '2032-06-30', status: 'VALID' }], scheduleSummary: '本周可服务 4 天', currentTaskCount: 1, remark: '', records: [{ action: 'ACCEPT', title: '护理人员接受合作邀请', time: '2026-04-05 14:20', operator: '李护理员' }],
  },
  {
    relationId: 71003, merchantId: 20001, caregiverId: 50006, name: '刘佳宁', phone: '137****5628', avatar: '', status: COOPERATION_STATUS.INVITED, available: false, rating: 4.7, completedOrders: 53, joinTime: null, invitationTime: '2026-07-15 09:20', workYears: 5, serviceAreas: ['朝阳区', '东城区'], skills: ['术后护理', '用药指导'], certificates: [{ name: '养老护理员职业技能等级证书', number: 'YLHL-2021-0086', validUntil: '2031-08-30', status: 'VALID' }], scheduleSummary: '等待确认合作', currentTaskCount: 0, remark: '擅长术后护理', records: [{ action: 'INVITE', title: '商户发出合作邀请', time: '2026-07-15 09:20', operator: '赵康宁' }],
  },
  {
    relationId: 71004, merchantId: 20001, caregiverId: 50007, name: '陈晓华', phone: '136****1935', avatar: '', status: COOPERATION_STATUS.SUSPENDED, available: false, rating: 4.6, completedOrders: 41, joinTime: '2026-05-01 09:30', invitationTime: '2026-04-28 10:15', workYears: 3, serviceAreas: ['海淀区'], skills: ['陪诊陪护'], certificates: [{ name: '养老护理员证', number: 'YLHL-2023-0315', validUntil: '2029-03-15', status: 'EXPIRING' }], scheduleSummary: '当前不可接单', currentTaskCount: 0, remark: '证书即将到期，暂时停用', records: [{ action: 'SUSPEND', title: '商户暂停合作', time: '2026-07-10 16:20', operator: '赵康宁', remark: '证书即将到期，等待更新' }],
  },
]

const merchantMembers = [
  { memberId: 72001, merchantId: 20001, userId: 10003, name: '赵康宁', phone: '138****8002', position: 'OWNER', status: MEMBER_STATUS.ACTIVE, joinTime: '2026-03-01 10:20', permissions: ['*'] },
  { memberId: 72002, merchantId: 20001, userId: 10110, name: '周运营', phone: '139****2018', position: 'OPERATOR', status: MEMBER_STATUS.ACTIVE, joinTime: '2026-04-06 09:00', permissions: ['merchant:service:manage', 'merchant:order:view'] },
  { memberId: 72003, merchantId: 20001, userId: 10111, name: '孙调度', phone: '137****5526', position: 'DISPATCHER', status: MEMBER_STATUS.ACTIVE, joinTime: '2026-04-10 14:00', permissions: ['merchant:order:view', 'merchant:order:dispatch'] },
  { memberId: 72004, merchantId: 20001, userId: 10112, name: '钱财务', phone: '136****6615', position: 'FINANCE', status: MEMBER_STATUS.DISABLED, joinTime: '2026-05-02 10:00', permissions: ['merchant:settlement:view'] },
]

function getMerchantId(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  const userId = Number(auth.match(/mock_jwt_(\d+)_MERCHANT_MEMBER_/)?.[1] || 0)
  return getMockMerchantIdByUserId(userId)
}
function getQuery(url, key) {
  const match = String(url).match(new RegExp(`[?&]${key}=([^&]*)`))
  return match ? decodeURIComponent(match[1].replace(/\+/g, ' ')) : ''
}
function maskPhone(phone) { return phone.length === 11 ? `${phone.slice(0, 3)}****${phone.slice(-4)}` : phone }
function now() { return new Date().toLocaleString('zh-CN', { hour12: false }).replaceAll('/', '-').slice(0, 16) }
function clone(data) { return JSON.parse(JSON.stringify(data)) }
function enrichCaregiver(item) {
  const schedule = getCaregiverSchedule(item.caregiverId)
  if (!schedule) return item
  return { ...item, available: Boolean(item.available && schedule.enabled), serviceAreas: schedule.serviceAreas, scheduleSummary: getScheduleSummary(item.caregiverId), maxDailyOrders: schedule.maxDailyOrders }
}

Mock.mock(/\/api\/v1\/merchant\/caregivers(?:\?|$)/, 'get', (options) => {
  const merchantId = getMerchantId(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const status = getQuery(options.url, 'status')
  const keyword = getQuery(options.url, 'keyword')
  let list = caregiverRelations.filter((item) => item.merchantId === merchantId).map(enrichCaregiver)
  if (status) list = list.filter((item) => item.status === status)
  if (keyword) list = list.filter((item) => `${item.name}${item.phone}${item.skills.join('')}`.includes(keyword))
  const all = caregiverRelations.filter((item) => item.merchantId === merchantId)
  return { code: 0, message: 'success', data: { list: clone(list), total: list.length, summary: { total: all.length, active: all.filter((item) => item.status === COOPERATION_STATUS.ACTIVE).length, invited: all.filter((item) => item.status === COOPERATION_STATUS.INVITED).length, suspended: all.filter((item) => item.status === COOPERATION_STATUS.SUSPENDED).length, available: all.filter((item) => item.status === COOPERATION_STATUS.ACTIVE && item.available).length } } }
})

Mock.mock(/\/api\/v1\/merchant\/caregivers\/\d+$/, 'get', (options) => {
  const merchantId = getMerchantId(options)
  const relationId = Number(options.url.match(/caregivers\/(\d+)$/)?.[1])
  const relation = caregiverRelations.find((item) => item.relationId === relationId && item.merchantId === merchantId)
  return relation ? { code: 0, message: 'success', data: clone(enrichCaregiver(relation)) } : { code: 9001, message: '护理人员合作关系不存在', data: null }
})

Mock.mock(/\/api\/v1\/merchant\/caregivers\/invite$/, 'post', (options) => {
  const merchantId = getMerchantId(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const body = JSON.parse(options.body || '{}')
  if (!/^1\d{10}$/.test(body.phone || '')) return { code: 1000, message: '请输入正确的护理人员手机号', data: null }
  if (caregiverRelations.some((item) => item.merchantId === merchantId && item.rawPhone === body.phone && item.status !== COOPERATION_STATUS.LEFT)) return { code: 9002, message: '已存在有效合作关系或待确认邀请', data: null }
  const relation = { relationId: Mock.Random.integer(71100, 71999), merchantId, caregiverId: Mock.Random.integer(50100, 50999), name: body.name?.trim() || `护理人员${body.phone.slice(-4)}`, phone: maskPhone(body.phone), rawPhone: body.phone, avatar: '', status: COOPERATION_STATUS.INVITED, available: false, rating: 0, completedOrders: 0, joinTime: null, invitationTime: now(), workYears: Number(body.workYears || 0), serviceAreas: body.serviceAreas || [], skills: body.skills || [], certificates: [], scheduleSummary: '等待确认合作', currentTaskCount: 0, remark: body.remark?.trim() || '', records: [{ action: 'INVITE', title: '商户发出合作邀请', time: now(), operator: '商户负责人', remark: body.remark?.trim() || '' }] }
  caregiverRelations.unshift(relation)
  return { code: 0, message: '合作邀请已发送', data: clone(relation) }
})

Mock.mock(/\/api\/v1\/merchant\/caregivers\/\d+\/(suspend|resume)$/, 'post', (options) => {
  const merchantId = getMerchantId(options)
  const [, relationId, action] = options.url.match(/caregivers\/(\d+)\/(suspend|resume)$/) || []
  const relation = caregiverRelations.find((item) => item.relationId === Number(relationId) && item.merchantId === merchantId)
  if (!relation) return { code: 9001, message: '护理人员合作关系不存在', data: null }
  if (relation.status === COOPERATION_STATUS.INVITED) return { code: 9003, message: '待确认邀请不可执行该操作', data: null }
  const body = JSON.parse(options.body || '{}')
  relation.status = action === 'suspend' ? COOPERATION_STATUS.SUSPENDED : COOPERATION_STATUS.ACTIVE
  relation.available = action === 'resume'
  relation.records.unshift({ action: action.toUpperCase(), title: action === 'suspend' ? '商户暂停合作' : '商户恢复合作', time: now(), operator: '商户负责人', remark: body.reason || '' })
  return { code: 0, message: action === 'suspend' ? '已暂停合作' : '已恢复合作', data: clone(relation) }
})

Mock.mock(/\/api\/v1\/merchant\/members(?:\?|$)/, 'get', (options) => {
  const merchantId = getMerchantId(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const list = merchantMembers.filter((item) => item.merchantId === merchantId)
  return { code: 0, message: 'success', data: { list: clone(list), total: list.length } }
})

Mock.mock(/\/api\/v1\/merchant\/members\/invite$/, 'post', (options) => {
  const merchantId = getMerchantId(options)
  if (!merchantId) return { code: 1004, message: '暂无商户权限', data: null }
  const body = JSON.parse(options.body || '{}')
  if (!/^1\d{10}$/.test(body.phone || '') || !['OPERATOR', 'DISPATCHER', 'FINANCE'].includes(body.position)) return { code: 1000, message: '请填写正确的成员手机号和岗位', data: null }
  const member = { memberId: Mock.Random.integer(72100, 72999), merchantId, userId: Mock.Random.integer(10200, 10999), name: body.name?.trim() || `成员${body.phone.slice(-4)}`, phone: maskPhone(body.phone), position: body.position, status: MEMBER_STATUS.ACTIVE, joinTime: now(), permissions: [] }
  merchantMembers.unshift(member)
  return { code: 0, message: '成员邀请已发送', data: clone(member) }
})

Mock.mock(/\/api\/v1\/merchant\/members\/\d+\/(disable|enable)$/, 'post', (options) => {
  const merchantId = getMerchantId(options)
  const [, memberId, action] = options.url.match(/members\/(\d+)\/(disable|enable)$/) || []
  const member = merchantMembers.find((item) => item.memberId === Number(memberId) && item.merchantId === merchantId)
  if (!member) return { code: 9004, message: '商户成员不存在', data: null }
  if (member.position === 'OWNER') return { code: 9005, message: '负责人不可被停用', data: null }
  member.status = action === 'disable' ? MEMBER_STATUS.DISABLED : MEMBER_STATUS.ACTIVE
  return { code: 0, message: '成员状态已更新', data: clone(member) }
})

export function getActiveMerchantCaregivers(merchantId) {
  return caregiverRelations.filter((item) => item.merchantId === merchantId && item.status === COOPERATION_STATUS.ACTIVE).map(enrichCaregiver)
}

console.log('[Mock] 商户团队管理模块已加载')
