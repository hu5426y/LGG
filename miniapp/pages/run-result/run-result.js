const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { formatDuration, formatDateTime, formatPaceSeconds } = require('../../utils/format')
const { normalizeRoutePoints, sampleRoutePoints, buildMapState } = require('../../utils/run-map')

Page({
  data: {
    runId: '',
    loading: true,
    errorMessage: '',
    run: null,
    routePoints: [],
    routePointCount: 0,
    ...buildMapState([], null)
  },

  onLoad(query) {
    this.setData({
      runId: query.id || ''
    })
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.loadData()
  },

  async loadData() {
    if (!this.data.runId) {
      this.setData({
        loading: false,
        errorMessage: '缺少跑步记录 ID'
      })
      return
    }
    this.setData({
      loading: true,
      errorMessage: ''
    })
    try {
      const detail = await request.get(`/runs/${this.data.runId}`)
      const detailRoutePoints = normalizeRoutePoints(detail?.routePoints)
      const routePoints = sampleRoutePoints(detailRoutePoints, 200)
      const routePointCount = Number(detail?.routePointCount || detailRoutePoints.length || 0)
      const mapState = buildMapState(routePoints, routePoints[routePoints.length - 1] || null)
      this.setData({
        loading: false,
        routePoints,
        routePointCount,
        run: {
          ...detail,
          displayDuration: formatDuration(Number(detail?.durationSeconds || 0)),
          displayFinishedAt: formatDateTime(detail?.finishedAt || detail?.startedAt),
          displayPace: formatPaceSeconds(Number(detail?.avgPaceSeconds || 0))
        },
        ...mapState
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  goRunPage() {
    wx.switchTab({
      url: '/pages/run/run'
    })
  },

  goProfilePage() {
    wx.switchTab({
      url: '/pages/profile/profile'
    })
  }
})
