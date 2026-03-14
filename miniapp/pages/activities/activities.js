const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    activities: [],
    tutorials: []
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.loadData()
  },

  async loadData() {
    this.setData({
      loading: true,
      errorMessage: ''
    })
    try {
      const [activities, tutorials] = await Promise.all([
        request.get('/activities'),
        request.get('/activities/tutorials')
      ])
      this.setData({
        loading: false,
        activities: safeList(activities),
        tutorials: safeList(tutorials)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  async register(event) {
    const { id } = event.currentTarget.dataset
    try {
      await request.post(`/activities/${id}/register`, {})
      wx.showToast({ title: '报名成功', icon: 'success' })
      await this.loadData()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  }
})
