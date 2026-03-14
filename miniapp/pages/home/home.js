const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { navigateTo, switchTab, openBannerTarget } = require('../../utils/navigation')

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
    }
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
      const [homeData, profile, overview] = await Promise.all([
        request.get('/public/home'),
        request.get('/user/me'),
        request.get('/gamification/overview')
      ])
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
          displayName: profile?.profile?.displayName || '',
          totalDistanceKm: safeNumber(profile?.profile?.totalDistanceKm, 0),
          points: safeNumber(profile?.profile?.points, 0),
          levelValue: safeNumber(profile?.profile?.levelValue, 1)
        },
        overview: {
          badges: safeList(overview?.badges),
          tasks: safeList(overview?.tasks)
        }
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
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
