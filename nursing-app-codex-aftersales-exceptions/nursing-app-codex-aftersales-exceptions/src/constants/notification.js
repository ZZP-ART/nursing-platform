export const NOTIFICATION_CATEGORY = Object.freeze({
  ORDER: 'ORDER',
  ASSIGNMENT: 'ASSIGNMENT',
  SERVICE: 'SERVICE',
  REVIEW: 'REVIEW',
  REFUND: 'REFUND',
  EXCEPTION: 'EXCEPTION',
  COMPLAINT: 'COMPLAINT',
  SYSTEM: 'SYSTEM',
})

export const NOTIFICATION_CATEGORY_META = Object.freeze({
  [NOTIFICATION_CATEGORY.ORDER]: { label: '订单', icon: 'order', color: '#3A7BF7' },
  [NOTIFICATION_CATEGORY.ASSIGNMENT]: { label: '派单', icon: 'share', color: '#FF8A5C' },
  [NOTIFICATION_CATEGORY.SERVICE]: { label: '履约', icon: 'checkmark-circle', color: '#00B8A9' },
  [NOTIFICATION_CATEGORY.REVIEW]: { label: '审核', icon: 'file-text', color: '#7658D6' },
  [NOTIFICATION_CATEGORY.REFUND]: { label: '退款', icon: 'rmb-circle', color: '#E7833B' },
  [NOTIFICATION_CATEGORY.EXCEPTION]: { label: '异常', icon: 'warning', color: '#E55B63' },
  [NOTIFICATION_CATEGORY.COMPLAINT]: { label: '投诉', icon: 'chat', color: '#D56B34' },
  [NOTIFICATION_CATEGORY.SYSTEM]: { label: '系统', icon: 'bell', color: '#66788C' },
})

export function getNotificationCategoryMeta(category) {
  return NOTIFICATION_CATEGORY_META[category] || NOTIFICATION_CATEGORY_META[NOTIFICATION_CATEGORY.SYSTEM]
}
