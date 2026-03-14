const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { navigateTo, switchTab, openBannerTarget } = require('../../utils/navigation')
const { buildMapState, normalizeRoutePoints, normalizePoint } = require('../../utils/run-map')

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
    mapSubKey: '',
    homeData: {
      banners: [],
      hotPosts: [],
      recommendedActivities: [],
      tutorials: [],
      leaderboard: []
    },
    profile: {
      displayName: '',
      totalDistanceKm: 0,
      points: 0,
      levelValue: 1
    },
    overview: {
      badges: [],
      tasks: []
    },
    todayCheckin: {
      checkedIn: false,
      streakDays: 0
    },
    currentPlan: {
      active: false,
      currentDayIndex: 1,
      template: null,
      days: []
    },
    latestRun: null,
    ...buildMapState([], null)
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
      errorMessage: '',
      mapSubKey: getApp().globalData.mapSubKey || ''
    })
    try {
      const [homeData, userData, overview, todayCheckin, currentPlan] = await Promise.all([
        request.get('/public/home'),
        request.get('/user/me'),
        request.get('/gamification/overview'),
        request.get('/checkins/today'),
        request.get('/run-plans/current')
      ])
      const recentRuns = safeList(userData?.recentRuns)
      const latestRunSummary = recentRuns[0] || null
      let latestRunDetail = null
      if (latestRunSummary?.id) {
        latestRunDetail = await request.get(`/runs/${latestRunSummary.id}`).catch(() => null)
      }
      await this.applyMapPreview(latestRunDetail)
      this.setData({
        loading: false,
        homeData: {
          banners: safeList(homeData?.banners),
          hotPosts: safeList(homeData?.hotPosts),
          recommendedActivities: safeList(homeData?.recommendedActivities),
          tutorials: safeList(homeData?.tutorials),
          leaderboard: safeList(homeData?.leaderboard)
        },
        profile: {
          displayName: userData?.profile?.displayName || '',
          totalDistanceKm: safeNumber(userData?.profile?.totalDistanceKm, 0),
          points: safeNumber(userData?.profile?.points, 0),
          levelValue: safeNumber(userData?.profile?.levelValue, 1)
        },
        overview: {
          badges: safeList(overview?.badges),
          tasks: safeList(overview?.tasks)
        },
        todayCheckin: {
          checkedIn: Boolean(todayCheckin?.checkedIn),
          streakDays: safeNumber(todayCheckin?.streakDays, 0)
        },
        currentPlan: currentPlan?.template
          ? currentPlan
          : { active: false, currentDayIndex: 1, template: null, days: [] },
        latestRun: latestRunDetail
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  async applyMapPreview(latestRunDetail) {
    const routePoints = normalizeRoutePoints(latestRunDetail?.routePoints)
    if (routePoints.length) {
      this.setData({
        ...buildMapState(routePoints, routePoints[routePoints.length - 1])
      })
      return
    }
    try {
      const location = await new Promise((resolve, reject) => {
        wx.getLocation({
          type: 'gcj02',
          success: resolve,
          fail: reject
        })
      })
      const point = normalizePoint({
        latitude: location.latitude,
        longitude: location.longitude,
        timestamp: Date.now()
      })
      this.setData({
        ...buildMapState([], point)
      })
    } catch (error) {
      this.setData({
        ...buildMapState([], null)
      })
    }
  },

  goRun() {
    switchTab('/pages/run/run')
  },

  goActivities() {
    switchTab('/pages/activities/activities')
  },

  goSocial() {
    switchTab('/pages/social/social')
  },

  goGrowthCenter() {
    navigateTo('/pages/growth-center/growth-center')
  },

  goRanking() {
    navigateTo('/pages/ranking/ranking')
  },

  openBanner(event) {
    const { linkType, linkTarget } = event.currentTarget.dataset
    const opened = openBannerTarget(linkType, linkTarget)
    if (!opened) {
      wx.showToast({
        title: '该内容即将开放',
        icon: 'none'
      })
    }
  },

  openPostDetail(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      return
    }
    navigateTo(`/pages/post-detail/post-detail?id=${id}`)
  },

  openActivityDetail(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      return
    }
    navigateTo(`/pages/activity-detail/activity-detail?id=${id}`)
  },

  openTutorialDetail(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      return
    }
    navigateTo(`/pages/tutorial-detail/tutorial-detail?id=${id}`)
  }
})
