const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { formatDateTime } = require('../../utils/format')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    activityId: '',
    loading: true,
    errorMessage: '',
    activity: null
  },

  onLoad(query) {
    this.setData({
      activityId: query.id || ''
    })
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.loadData()
  },

  async loadData() {
    if (!this.data.activityId) {
      this.setData({
        loading: false,
        errorMessage: '缺少活动 ID'
      })
      return
    }
    this.setData({
      loading: true,
      errorMessage: ''
    })
    try {
      const activities = await request.get('/activities')
      const activity = safeList(activities).find((item) => String(item.id) === String(this.data.activityId))
      if (!activity) {
        throw new Error('未找到对应活动')
      }
      this.setData({
        loading: false,
        activity: {
          ...activity,
          displayStartTime: formatDateTime(activity.startTime),
          displayEndTime: formatDateTime(activity.endTime),
          displayDeadline: formatDateTime(activity.registrationDeadline)
        }
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  async register() {
    if (!this.data.activity?.id) {
      return
    }
    try {
      await request.post(`/activities/${this.data.activity.id}/register`, {})
      wx.showToast({
        title: '报名成功',
        icon: 'success'
      })
      await this.loadData()
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  }
})
