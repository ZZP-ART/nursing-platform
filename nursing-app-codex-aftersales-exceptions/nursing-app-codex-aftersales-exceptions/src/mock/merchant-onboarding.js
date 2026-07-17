import Mock from 'mockjs'
import { MERCHANT_AUDIT_STATUS } from '@/constants/merchant-status.js'
import { ROLES } from '@/constants/roles.js'
import { findMockUserById, grantMockUserRole } from './user.js'

const applications = new Map([
  [10003, {
    applicationId: 90001,
    merchantId: 20001,
    userId: 10003,
    auditStatus: MERCHANT_AUDIT_STATUS.APPROVED,
    merchantName: '康宁护理中心',
    merchantType: 'NURSING_ORG',
    creditCode: '91110105MA01KN2026',
    legalRepresentative: '赵康宁',
    contactName: '赵康宁',
    contactPhone: '13800138002',
    contactIdCard: '110101198605061234',
    businessAddress: '北京市朝阳区康宁路88号',
    serviceAreas: '北京市朝阳区、海淀区、东城区',
    businessScope: '居家养老服务、生活照护、陪诊陪护及康复护理服务。',
    licenseImageUrl: 'https://cdn.nursing.com/merchant/20001/license.jpg',
    permitName: '养老机构备案回执',
    permitNumber: 'CYBA-2026-0088',
    permitImageUrl: 'https://cdn.nursing.com/merchant/20001/permit.jpg',
    settlementBank: '中国建设银行北京朝阳支行',
    settlementAccountName: '北京康宁护理服务有限公司',
    settlementAccountNo: '11050123456789001234',
    memberRole: 'OWNER',
    rejectReason: '',
    reviewRecords: [{ recordId: 92001, action: 'APPROVE', operatorName: '平台审核员', remark: '企业主体、营业执照和经营许可核验通过', createTime: '2026-03-01T10:20:00+08:00' }],
    submitTime: '2026-02-27T09:30:00+08:00',
    reviewTime: '2026-03-01T10:20:00+08:00',
    version: 1,
  }],
  [10005, {
    applicationId: 90002,
    merchantId: null,
    userId: 10005,
    auditStatus: MERCHANT_AUDIT_STATUS.REJECTED,
    merchantName: '安心到家护理服务中心',
    merchantType: 'NURSING_ORG',
    creditCode: '91110105MA02AX2026',
    legalRepresentative: '周安心',
    contactName: '周安心',
    contactPhone: '13800138004',
    contactIdCard: '110101198808081234',
    businessAddress: '北京市朝阳区安和街16号',
    serviceAreas: '北京市朝阳区',
    businessScope: '居家生活照护、陪诊陪护和康复护理协助。',
    licenseImageUrl: 'https://cdn.nursing.com/merchant/applications/90002/license.jpg',
    permitName: '养老服务机构备案回执',
    permitNumber: '',
    permitImageUrl: '',
    settlementBank: '',
    settlementAccountName: '',
    settlementAccountNo: '',
    memberRole: null,
    rejectReason: '请补充经营许可或备案回执编号，并上传清晰的附件图片。',
    reviewRecords: [
      { recordId: 92003, action: 'REJECT', operatorName: '平台审核员', remark: '请补充经营许可或备案回执编号，并上传清晰的附件图片。', createTime: '2026-07-14T15:20:00+08:00' },
      { recordId: 92002, action: 'SUBMIT', operatorName: '商户申请人', remark: '提交商户入驻申请', createTime: '2026-07-13T10:00:00+08:00' },
    ],
    submitTime: '2026-07-13T10:00:00+08:00',
    reviewTime: '2026-07-14T15:20:00+08:00',
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
  if (application?.auditStatus !== MERCHANT_AUDIT_STATUS.PENDING_REVIEW) return
  if (Date.now() - new Date(application.submitTime).getTime() < 8000) return
  application.auditStatus = MERCHANT_AUDIT_STATUS.APPROVED
  application.merchantId ||= Mock.Random.integer(21000, 29999)
  application.memberRole = 'OWNER'
  application.reviewTime = now()
  application.rejectReason = ''
  application.version += 1
  application.reviewRecords.unshift({
    recordId: Mock.Random.integer(93000, 99999),
    action: 'APPROVE',
    operatorName: '模拟平台审核员',
    remark: '企业主体、负责人和经营资质资料完整，审核通过',
    createTime: application.reviewTime,
  })
  grantMockUserRole(application.userId, ROLES.MERCHANT_MEMBER, application.merchantId)
}

function validateApplication(body) {
  if (!body.merchantName?.trim()) return '请填写商户名称'
  if (!body.merchantType) return '请选择商户类型'
  if (!/^[0-9A-Z]{18}$/.test(body.creditCode || '')) return '请填写18位统一社会信用代码'
  if (!body.legalRepresentative?.trim()) return '请填写法定代表人'
  if (!body.contactName?.trim() || !/^1\d{10}$/.test(body.contactPhone || '')) return '请填写正确的负责人姓名和手机号'
  if (!/^\d{17}[\dXx]$/.test(body.contactIdCard || '')) return '请填写正确的负责人身份证号'
  if (!body.businessAddress?.trim() || !body.serviceAreas?.trim()) return '请填写经营地址和服务区域'
  if (!body.businessScope?.trim()) return '请填写经营范围'
  if (!body.licenseImageUrl?.trim()) return '请上传营业执照'
  if (!body.permitName?.trim() || !body.permitNumber?.trim() || !body.permitImageUrl?.trim()) return '请完整填写经营许可或备案信息'
  return ''
}

function buildApplication(userId, body, current = null) {
  return {
    applicationId: current?.applicationId || Mock.Random.integer(90010, 90999),
    merchantId: current?.merchantId || null,
    userId,
    auditStatus: MERCHANT_AUDIT_STATUS.DRAFT,
    merchantName: body.merchantName?.trim() || '',
    merchantType: body.merchantType || '',
    creditCode: body.creditCode?.trim().toUpperCase() || '',
    legalRepresentative: body.legalRepresentative?.trim() || '',
    contactName: body.contactName?.trim() || '',
    contactPhone: body.contactPhone?.trim() || '',
    contactIdCard: body.contactIdCard?.trim() || '',
    businessAddress: body.businessAddress?.trim() || '',
    serviceAreas: body.serviceAreas?.trim() || '',
    businessScope: body.businessScope?.trim() || '',
    licenseImageUrl: body.licenseImageUrl?.trim() || '',
    permitName: body.permitName?.trim() || '',
    permitNumber: body.permitNumber?.trim() || '',
    permitImageUrl: body.permitImageUrl?.trim() || '',
    settlementBank: body.settlementBank?.trim() || '',
    settlementAccountName: body.settlementAccountName?.trim() || '',
    settlementAccountNo: body.settlementAccountNo?.trim() || '',
    memberRole: current?.memberRole || null,
    rejectReason: current?.rejectReason || '',
    reviewRecords: current?.reviewRecords || [],
    submitTime: current?.submitTime || null,
    reviewTime: current?.reviewTime || null,
    version: (current?.version || 0) + 1,
  }
}

Mock.mock(/\/api\/v1\/merchants\/application(?:\?|$)/, 'get', (options) => {
  const userId = getUserId(options)
  if (!findMockUserById(userId)) return { code: 1002, message: '请先登录后申请', data: null }
  const application = applications.get(userId)
  refreshMockReview(application)
  return { code: 0, message: 'success', data: clone(application || null) }
})

Mock.mock(/\/api\/v1\/merchants\/application$/, 'put', (options) => {
  const userId = getUserId(options)
  if (!findMockUserById(userId)) return { code: 1002, message: '请先登录后申请', data: null }
  const current = applications.get(userId)
  if (current && ![MERCHANT_AUDIT_STATUS.DRAFT, MERCHANT_AUDIT_STATUS.REJECTED].includes(current.auditStatus)) {
    return { code: 8001, message: '当前审核状态不可修改', data: null }
  }
  const application = buildApplication(userId, JSON.parse(options.body || '{}'), current)
  applications.set(userId, application)
  return { code: 0, message: '草稿已保存', data: clone(application) }
})

Mock.mock(/\/api\/v1\/merchants\/apply$/, 'post', (options) => {
  const userId = getUserId(options)
  const user = findMockUserById(userId)
  if (!user) return { code: 1002, message: '请先登录后申请', data: null }
  const current = applications.get(userId)
  if (current && ![MERCHANT_AUDIT_STATUS.DRAFT, MERCHANT_AUDIT_STATUS.REJECTED].includes(current.auditStatus)) {
    return { code: 8001, message: '当前申请不可重复提交', data: null }
  }
  const body = JSON.parse(options.body || '{}')
  const error = validateApplication(body)
  if (error) return { code: 8002, message: error, data: null }
  const application = buildApplication(userId, body, current)
  application.auditStatus = MERCHANT_AUDIT_STATUS.PENDING_REVIEW
  application.submitTime = now()
  application.reviewTime = null
  application.rejectReason = ''
  application.reviewRecords.unshift({
    recordId: Mock.Random.integer(93000, 99999),
    action: 'SUBMIT',
    operatorName: user.nickname,
    remark: current?.auditStatus === MERCHANT_AUDIT_STATUS.REJECTED ? '修改资料后重新提交入驻申请' : '提交商户入驻申请',
    createTime: application.submitTime,
  })
  applications.set(userId, application)
  return { code: 0, message: '已提交审核，Mock 将在约8秒后自动通过', data: clone(application) }
})

Mock.mock(/\/api\/v1\/merchant\/profile$/, 'get', (options) => {
  const userId = getUserId(options)
  const application = applications.get(userId)
  refreshMockReview(application)
  if (!application || application.auditStatus !== MERCHANT_AUDIT_STATUS.APPROVED) {
    return { code: 8003, message: '商户入驻尚未审核通过', data: null }
  }
  return { code: 0, message: 'success', data: clone(application) }
})

console.log('[Mock] 商户入驻模块已加载')
