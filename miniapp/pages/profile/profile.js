const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { formatDuration, formatDateTime, formatPaceSeconds } = require('../../utils/format')

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
      college: '',
      className: '',
      points: 0,
      levelValue: 1,
      totalDistanceKm: 0
    },
    recentRuns: [],
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
      const recentRuns = safeList(userData?.recentRuns).map((run) => ({
        ...run,
        displayDuration: formatDuration(safeNumber(run.durationSeconds, 0)),
        displayFinishedAt: formatDateTime(run.finishedAt),
        displayPace: formatPaceSeconds(safeNumber(run.avgPaceSeconds, 0))
      }))
      this.setData({
        loading: false,
        profile: {
          displayName: userData?.profile?.displayName || '',
          college: userData?.profile?.college || '',
          className: userData?.profile?.className || '',
          points: safeNumber(userData?.profile?.points, 0),
          levelValue: safeNumber(userData?.profile?.levelValue, 1),
          totalDistanceKm: safeNumber(userData?.profile?.totalDistanceKm, 0)
        },
        recentRuns,
        badges: safeList(overview?.badges),
        tasks: safeList(overview?.tasks)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  viewRunDetail(event) {
    const { runId } = event.currentTarget.dataset
    if (!runId) {
      return
    }
    wx.navigateTo({
      url: `/pages/run-result/run-result?id=${runId}`
    })
  },

  logout() {
    wx.removeStorageSync('campusRunToken')
    wx.removeStorageSync('campusRunUser')
    wx.removeStorageSync('campusRunCurrentRun')
    getApp().globalData.userInfo = null
    wx.redirectTo({
      url: '/pages/login/login'
    })
  }
})
