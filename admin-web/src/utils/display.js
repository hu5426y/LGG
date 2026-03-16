const badgeRuleTypeLabels = {
  TOTAL_DISTANCE: '累计里程',
  RUN_COUNT: '跑步次数',
  POINTS: '积分',
  CHECKIN_STREAK: '连续打卡',
  PLAN_COMPLETION: '计划完成次数',
  SQUAD_PARTICIPATION: '跑团参与'
}

const auditActionLabels = {
  IMPORT_STUDENTS: '导入学生',
  UPDATE_STUDENT_STATUS: '更新学生状态',
  REVIEW_POST: '审核动态',
  SAVE_BADGE: '保存勋章',
  SAVE_ACTIVITY: '保存活动',
  SAVE_TUTORIAL: '保存教程'
}

const auditTargetTypeLabels = {
  sys_user: '学生账号',
  feed_post: '动态',
  badge: '勋章',
  activity: '活动',
  tutorial: '教程'
}

const reviewActionLabels = {
  APPROVE: '通过',
  REJECT: '拒绝',
  OFFLINE: '下架',
  FEATURE: '置顶'
}

export function badgeRuleTypeLabel(value) {
  return badgeRuleTypeLabels[value] || value || '-'
}

export function auditActionLabel(value) {
  return auditActionLabels[value] || value || '-'
}

export function auditTargetTypeLabel(value) {
  return auditTargetTypeLabels[value] || value || '-'
}

export function formatAuditDetail(action, detail) {
  if (action === 'REVIEW_POST' && typeof detail === 'string' && detail.includes(':')) {
    const [reviewAction, remark] = detail.split(/:(.+)/)
    const actionLabel = reviewActionLabels[reviewAction] || reviewAction
    return remark ? `${actionLabel}：${remark}` : actionLabel
  }
  return detail || '-'
}
