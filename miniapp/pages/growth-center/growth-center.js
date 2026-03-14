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
    tasks: [],
    planTemplates: [],
    currentPlan: {
      active: false,
      currentDayIndex: 1,
      days: [],
      template: null
    },
    todayCheckin: {
      checkedIn: false,
      streakDays: 0
    },
    checkinHistory: []
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
      const [userData, overview, templates, currentPlan, todayCheckin, checkinHistory] = await Promise.all([
        request.get('/user/me'),
        request.get('/gamification/overview'),
        request.get('/run-plans'),
        request.get('/run-plans/current'),
        request.get('/checkins/today'),
        request.get('/checkins/history')
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
        tasks: safeList(overview?.tasks),
        planTemplates: safeList(templates),
        currentPlan: currentPlan?.template ? currentPlan : {
          active: false,
          currentDayIndex: 1,
          days: [],
          template: null
        },
        todayCheckin: {
          checkedIn: Boolean(todayCheckin?.checkedIn),
          streakDays: safeNumber(todayCheckin?.streakDays, 0)
        },
        checkinHistory: safeList(checkinHistory)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  async selectPlan(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      return
    }
    try {
      await request.post('/run-plans/select', { templateId: id })
      wx.showToast({
        title: '计划已切换',
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
