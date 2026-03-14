function navigateTo(url) {
  if (!url) {
    return false
  }
  wx.navigateTo({ url })
  return true
}

function switchTab(url) {
  if (!url) {
    return false
  }
  wx.switchTab({ url })
  return true
}

function openBannerTarget(linkType, linkTarget) {
  const type = (linkType || '').toUpperCase()
  const target = linkTarget || ''

  if (type === 'TASK') {
    return navigateTo('/pages/growth-center/growth-center')
  }
  if (type === 'ACTIVITY') {
    return target
      ? navigateTo(`/pages/activity-detail/activity-detail?id=${target}`)
      : switchTab('/pages/activities/activities')
  }
  if (type === 'POST') {
    return target
      ? navigateTo(`/pages/post-detail/post-detail?id=${target}`)
      : switchTab('/pages/social/social')
  }
  if (type === 'TUTORIAL') {
    return target
      ? navigateTo(`/pages/tutorial-detail/tutorial-detail?id=${target}`)
      : switchTab('/pages/activities/activities')
  }
  if (type === 'RANKING') {
    return navigateTo('/pages/ranking/ranking')
  }
  if (type === 'RUN') {
    return switchTab('/pages/run/run')
  }
  if (type === 'SOCIAL') {
    return switchTab('/pages/social/social')
  }
  if (type === 'ACTIVITY_LIST') {
    return switchTab('/pages/activities/activities')
  }
  return false
}

module.exports = {
  navigateTo,
  switchTab,
  openBannerTarget
}
