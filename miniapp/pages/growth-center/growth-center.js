const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

function safeNumber(value, fallback = 0) {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    profile: {
      displayName: '',
      points: 0,
      levelValue: 1,
      totalDistanceKm: 0
    },
    badges: [],
    tasks: []
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
      const [userData, overview] = await Promise.all([
        request.get('/user/me'),
        request.get('/gamification/overview')
      ])
      this.setData({
        loading: false,
        profile: {
          displayName: userData?.profile?.displayName || '',
          points: safeNumber(userData?.profile?.points, 0),
          levelValue: safeNumber(userData?.profile?.levelValue, 1),
          totalDistanceKm: safeNumber(userData?.profile?.totalDistanceKm, 0)
        },
        badges: safeList(overview?.badges),
        tasks: safeList(overview?.tasks)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  }
})
