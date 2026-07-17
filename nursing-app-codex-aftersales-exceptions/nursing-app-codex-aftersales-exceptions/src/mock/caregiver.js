import Mock from 'mockjs'
import { CAREGIVER_AUDIT_STATUS } from '@/constants/caregiver-status.js'
import { ROLES } from '@/constants/roles.js'
import { findMockUserById, grantMockUserRole } from './user.js'
import { getCaregiverSchedule } from './caregiver-schedule-state.js'

const applications = new Map([
  [10001, {
    applicationId: 80001,
    caregiverId: 50001,
    userId: 10001,
    auditStatus: CAREGIVER_AUDIT_STATUS.APPROVED,
    realName: '王护理员',
    gender: 2,
    idCard: '110101199001011234',
    workYears: 6,
    introduction: '熟悉老年生活照护、术后恢复和慢病用药指导。',
    serviceArea: '北京市朝阳区、海淀区',
    skills: ['生活照护', '用药指导', '术后护理'],
    certificates: [{ certificateId: 81001, name: '养老护理员职业技能等级证书', number: 'YLHL-2020-0088', issuer: '北京市职业技能鉴定中心', validUntil: '2030-12-31', imageUrl: '' }],
    merchantName: '康宁护理中心',
    rejectReason: '',
    reviewRecords: [{ recordId: 82001, action: 'APPROVE', operatorName: '平台审核员', remark: '身份及证书核验通过', createTime: '2026-03-12T10:30:00+08:00' }],
    submitTime: '2026-03-10T09:20:00+08:00',
    reviewTime: '2026-03-12T10:30:00+08:00',
    version: 1,
  }],
  [10002, {
    applicationId: 80002,
    caregiverId: 50002,
    userId: 10002,
    auditStatus: CAREGIVER_AUDIT_STATUS.APPROVED,
    realName: '李护理员',
    gender: 2,
    idCard: '110101199202021234',
    workYears: 4,
    introduction: '擅长生活照护、陪诊和康复训练协助。',
    serviceArea: '北京市朝阳区',
    skills: ['生活照护', '陪诊陪护', '康复护理'],
    certificates: [{ certificateId: 81002, name: '养老护理员职业技能等级证书', number: 'YLHL-2022-0126', issuer: '北京市职业技能鉴定中心', validUntil: '2032-06-30', imageUrl: '' }],
    merchantName: '康宁护理中心',
    rejectReason: '',
    reviewRecords: [{ recordId: 82002, action: 'APPROVE', operatorName: '平台审核员', remark: '审核通过', createTime: '2026-04-02T14:00:00+08:00' }],
    submitTime: '2026-03-30T11:00:00+08:00',
    reviewTime: '2026-04-02T14:00:00+08:00',
    version: 1,
  }],
  [10004, {
    applicationId: 80003,
    caregiverId: null,
    userId: 10004,
    auditStatus: CAREGIVER_AUDIT_STATUS.REJECTED,
    realName: '陈晓雨',
    gender: 2,
    idCard: '110101199508081234',
    workYears: 2,
    introduction: '有两年居家照护经验，希望为附近老人提供生活照护服务。',
    serviceArea: '北京市朝阳区',
    skills: ['生活照护', '陪诊陪护'],
    certificates: [{ certificateId: 81003, name: '养老护理员职业技能等级证书', number: 'YLHL-2024-0315', issuer: '', validUntil: '2029-03-15', imageUrl: '' }],
    merchantName: '',
    rejectReason: '请补充证书签发机构，并上传清晰的证书照片。',
    reviewRecords: [
      { recordId: 82004, action: 'REJECT', operatorName: '平台审核员', remark: '请补充证书签发机构，并上传清晰的证书照片。', createTime: '2026-07-14T16:20:00+08:00' },
      { recordId: 82003, action: 'SUBMIT', operatorName: '护理申请人', remark: '提交护理人员认证', createTime: '2026-07-13T09:30:00+08:00' },
    ],
    submitTime: '2026-07-13T09:30:00+08:00',
    reviewTime: '2026-07-14T16:20:00+08:00',
    version: 2,
  }],
])

function getUserId(options) {
  const auth = options.headers?.Authorization || options.headers?.authorization || ''
  return Number(auth.match(/mock_jwt_(\d+)/)?.[1] || 0)
}

function now() {
  return new Date().toISOString().replace(/\.\d{3}Z$/, '+08:00')
}

function clone(value) {
  return value ? JSON.parse(JSON.stringify(value)) : value
}

function refreshMockReview(application) {
  if (application?.auditStatus !== CAREGIVER_AUDIT_STATUS.PENDING_REVIEW) return
  if (Date.now() - new Date(application.submitTime).getTime() < 8000) return
  application.auditStatus = CAREGIVER_AUDIT_STATUS.APPROVED
  application.caregiverId ||= Mock.Random.integer(51000, 59999)
  application.reviewTime = now()
  application.rejectReason = ''
  application.version += 1
  application.reviewRecords.unshift({
    recordId: Mock.Random.integer(83000, 89999),
    action: 'APPROVE',
    operatorName: '模拟平台审核员',
    remark: '身份、资质与服务能力资料完整，审核通过',
    createTime: application.reviewTime,
  })
  grantMockUserRole(application.userId, ROLES.CAREGIVER, application.caregiverId)
}

function validateApplication(body) {
  if (!body.realName?.trim()) return '请填写真实姓名'
  if (!/^\d{17}[\dXx]$/.test(body.idCard || '')) return '请填写正确的身份证号'
  if (!body.serviceArea?.trim()) return '请填写服务区域'
  if (!Array.isArray(body.skills) || !body.skills.length) return '请至少选择一项服务技能'
  if (!Array.isArray(body.certificates) || !body.certificates.length) return '请至少添加一项资质证书'
  if (body.certificates.some((item) => !item.name?.trim() || !item.number?.trim() || !item.issuer?.trim() || !item.validUntil)) return '请完整填写证书名称、编号、签发机构和有效期'
  return ''
}

function buildApplication(userId, body, current = null) {
  return {
    applicationId: current?.applicationId || Mock.Random.integer(80010, 80999),
    caregiverId: current?.caregiverId || null,
    userId,
    auditStatus: CAREGIVER_AUDIT_STATUS.DRAFT,
    realName: body.realName?.trim() || '',
    gender: Number(body.gender || 0),
    idCard: body.idCard?.trim() || '',
    workYears: Number(body.workYears || 0),
    introduction: body.introduction?.trim() || '',
    serviceArea: body.serviceArea?.trim() || '',
    skills: body.skills || [],
    certificates: (body.certificates || []).map((item) => ({
      certificateId: item.certificateId || Mock.Random.integer(81100, 81999),
      name: item.name?.trim() || '',
      number: item.number?.trim() || '',
      issuer: item.issuer?.trim() || '',
      validUntil: item.validUntil || '',
      imageUrl: item.imageUrl?.trim() || '',
    })),
    merchantName: body.merchantName?.trim() || '',
    rejectReason: current?.rejectReason || '',
    reviewRecords: current?.reviewRecords || [],
    submitTime: current?.submitTime || null,
    reviewTime: current?.reviewTime || null,
    version: (current?.version || 0) + 1,
  }
}

Mock.mock(/\/api\/v1\/caregivers\/application(?:\?|$)/, 'get', (options) => {
  const userId = getUserId(options)
  if (!findMockUserById(userId)) return { code: 1002, message: '请先登录后申请', data: null }
  const application = applications.get(userId)
  refreshMockReview(application)
  return { code: 0, message: 'success', data: clone(application || null) }
})

Mock.mock(/\/api\/v1\/caregivers\/application$/, 'put', (options) => {
  const userId = getUserId(options)
  if (!findMockUserById(userId)) return { code: 1002, message: '请先登录后申请', data: null }
  const current = applications.get(userId)
  if (current && ![CAREGIVER_AUDIT_STATUS.DRAFT, CAREGIVER_AUDIT_STATUS.REJECTED].includes(current.auditStatus)) {
    return { code: 7001, message: '当前审核状态不可修改', data: null }
  }
  const application = buildApplication(userId, JSON.parse(options.body || '{}'), current)
  applications.set(userId, application)
  return { code: 0, message: '草稿已保存', data: clone(application) }
})

Mock.mock(/\/api\/v1\/caregivers\/apply$/, 'post', (options) => {
  const userId = getUserId(options)
  const user = findMockUserById(userId)
  if (!user) return { code: 1002, message: '请先登录后申请', data: null }
  const current = applications.get(userId)
  if (current && ![CAREGIVER_AUDIT_STATUS.DRAFT, CAREGIVER_AUDIT_STATUS.REJECTED].includes(current.auditStatus)) {
    return { code: 7001, message: '当前申请不可重复提交', data: null }
  }
  const body = JSON.parse(options.body || '{}')
  const error = validateApplication(body)
  if (error) return { code: 7002, message: error, data: null }
  const application = buildApplication(userId, body, current)
  application.auditStatus = CAREGIVER_AUDIT_STATUS.PENDING_REVIEW
  application.submitTime = now()
  application.reviewTime = null
  application.rejectReason = ''
  application.reviewRecords.unshift({
    recordId: Mock.Random.integer(83000, 89999),
    action: 'SUBMIT',
    operatorName: user.nickname,
    remark: current?.auditStatus === CAREGIVER_AUDIT_STATUS.REJECTED ? '修改资料后重新提交认证' : '提交护理人员认证',
    createTime: application.submitTime,
  })
  applications.set(userId, application)
  return { code: 0, message: '已提交审核，Mock 将在约8秒后自动通过', data: clone(application) }
})

Mock.mock(/\/api\/v1\/caregiver\/profile$/, 'get', (options) => {
  const userId = getUserId(options)
  const application = applications.get(userId)
  refreshMockReview(application)
  if (!application || application.auditStatus !== CAREGIVER_AUDIT_STATUS.APPROVED) {
    return { code: 7003, message: '护理人员身份尚未认证通过', data: null }
  }
  return {
    code: 0,
    message: 'success',
    data: {
      ...clone(application),
      serviceArea: (getCaregiverSchedule(application.caregiverId)?.serviceAreas || []).join('、') || application.serviceArea,
      rating: userId === 10002 ? 4.8 : 4.9,
      completedOrders: userId === 10002 ? 88 : 126,
      punctualityRate: userId === 10002 ? 97 : 98,
      employmentStatus: 'ACTIVE',
    },
  }
})

console.log('[Mock] 护理人员认证模块已加载')
