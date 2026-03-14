const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { formatDuration, formatPace } = require('../../utils/format')
const {
  normalizePoint,
  normalizeRoutePoints,
  buildMapState,
  formatLocationTimeFromPoints
} = require('../../utils/run-map')

function haversineMeters(pointA, pointB) {
  const toRad = (value) => value * Math.PI / 180
  const earthRadiusMeters = 6371000
  const lat1 = toRad(pointA.latitude)
  const lat2 = toRad(pointB.latitude)
  const deltaLat = toRad(pointB.latitude - pointA.latitude)
  const deltaLng = toRad(pointB.longitude - pointA.longitude)
  const a = Math.sin(deltaLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) ** 2
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return earthRadiusMeters * c
}

function parseServerTime(value) {
  if (!value) {
    return null
  }
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? null : parsed
}

function createRoutePointFromLocation(location) {
  return normalizePoint({
    latitude: location.latitude,
    longitude: location.longitude,
    timestamp: Date.now(),
    accuracy: location.accuracy || null,
    speedMetersPerSecond: typeof location.speed === 'number' && location.speed >= 0 ? location.speed : null
  })
}

function getDefaultState() {
  return {
    sessionId: null,
    running: false,
    paused: false,
    recovering: false,
    recoveredSession: false,
    recoveryHint: '',
    durationSeconds: 0,
    formattedDuration: '00:00:00',
    distanceKm: 0,
    stepCount: 0,
    pace: '--',
    gpsReady: false,
    routePointCount: 0,
    lastLocationTime: '--',
    locationError: '',
    routePoints: [],
    currentPosition: null,
    startedAtMs: null,
    pausedDurationMs: 0,
    pausedStartedAtMs: null,
    ...buildMapState([], null)
  }
}

Page({
  data: getDefaultState(),

  onLoad() {
    this.locationListener = this.handleLocationChange.bind(this)
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.restoreRunState()
  },

  onUnload() {
    this.stopTicker()
    this.stopLocationTracking()
  },

  onHide() {
    this.stopTicker()
    this.persistState()
  },

  persistState() {
    const routePoints = normalizeRoutePoints(this.data.routePoints)
    wx.setStorageSync('campusRunCurrentRun', {
      sessionId: this.data.sessionId,
      running: this.data.running,
      paused: this.data.paused,
      recoveredSession: this.data.recoveredSession,
      recoveryHint: this.data.recoveryHint,
      durationSeconds: this.data.durationSeconds,
      formattedDuration: this.data.formattedDuration,
      distanceKm: Number(this.data.distanceKm.toFixed(3)),
      stepCount: this.data.stepCount,
      pace: this.data.pace,
      gpsReady: this.data.gpsReady,
      routePointCount: routePoints.length,
      lastLocationTime: this.data.lastLocationTime,
      locationError: this.data.locationError,
      routePoints,
      currentPosition: this.data.currentPosition,
      startedAtMs: this.data.startedAtMs,
      pausedDurationMs: this.data.pausedDurationMs,
      pausedStartedAtMs: this.data.pausedStartedAtMs
    })
  },

  async restoreRunState() {
    this.setData({
      recovering: true,
      locationError: ''
    })

    const saved = wx.getStorageSync('campusRunCurrentRun')
    if (saved && saved.sessionId) {
      this.applySavedRun(saved)
      this.setData({ recovering: false })
      if (saved.running && !saved.paused) {
        this.startTicker()
        await this.startLocationTracking().catch(() => {})
      }
      return
    }

    const recovered = await this.recoverCurrentRun().catch((error) => {
      this.setData({
        locationError: error.message
      })
      return false
    })

    if (!recovered) {
      await this.refreshLocationPreviewSilently()
      this.setData({
        recovering: false,
        recoveredSession: false,
        recoveryHint: ''
      })
    }
  },

  applySavedRun(saved) {
    const routePoints = normalizeRoutePoints(saved.routePoints)
    const currentPosition = normalizePoint(saved.currentPosition) || routePoints[routePoints.length - 1] || null
    const running = Boolean(saved.running)
    const durationSeconds = Number(saved.durationSeconds || 0)
    const distanceKm = Number(saved.distanceKm || 0)
    this.setData({
      sessionId: saved.sessionId,
      running,
      paused: Boolean(saved.paused),
      recoveredSession: running,
      recoveryHint: running ? '检测到上次未结束跑步，你可以继续、结束上传或放弃本次记录。' : '',
      durationSeconds,
      formattedDuration: saved.formattedDuration || formatDuration(durationSeconds),
      distanceKm,
      stepCount: Number(saved.stepCount || 0),
      pace: saved.pace || formatPace(distanceKm, durationSeconds),
      gpsReady: Boolean(saved.gpsReady || routePoints.length || currentPosition),
      routePointCount: routePoints.length,
      lastLocationTime: routePoints.length ? formatLocationTimeFromPoints(routePoints) : '--',
      locationError: saved.locationError || '',
      routePoints,
      currentPosition,
      startedAtMs: saved.startedAtMs || null,
      pausedDurationMs: Number(saved.pausedDurationMs || 0),
      pausedStartedAtMs: saved.pausedStartedAtMs || null,
      ...buildMapState(routePoints, currentPosition)
    })
    this.persistState()
  },

  computeDurationSeconds(now = Date.now()) {
    if (!this.data.startedAtMs) {
      return 0
    }
    const currentPaused = this.data.paused && this.data.pausedStartedAtMs
      ? (now - this.data.pausedStartedAtMs)
      : 0
    return Math.max(0, Math.floor((now - this.data.startedAtMs - this.data.pausedDurationMs - currentPaused) / 1000))
  },

  updateDerivedMetrics() {
    const durationSeconds = this.computeDurationSeconds()
    this.setData({
      durationSeconds,
      formattedDuration: formatDuration(durationSeconds),
      pace: formatPace(this.data.distanceKm, durationSeconds),
      stepCount: Math.max(this.data.stepCount, Math.round(this.data.distanceKm * 1350))
    })
    this.persistState()
  },

  startTicker() {
    this.stopTicker()
    this.timer = setInterval(() => {
      this.updateDerivedMetrics()
    }, 1000)
  },

  stopTicker() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },

  async getLocationOnce() {
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: 'gcj02',
        isHighAccuracy: true,
        success: resolve,
        fail: reject
      })
    })
  },

  async ensureLocationReady() {
    try {
      return await this.getLocationOnce()
    } catch (error) {
      await new Promise((resolve, reject) => {
        wx.authorize({
          scope: 'scope.userLocation',
          success: resolve,
          fail: reject
        })
      }).catch(async () => {
        await new Promise((resolve) => {
          wx.showModal({
            title: '需要定位权限',
            content: '真实跑步记录需要开启定位权限，请在设置中允许定位后重试。',
            showCancel: false,
            success: resolve
          })
        })
        throw new Error('未获得定位权限')
      })

      return this.getLocationOnce().catch((locationError) => {
        throw new Error(locationError.errMsg || '定位初始化失败')
      })
    }
  },

  async refreshLocationPreviewSilently() {
    try {
      const location = await this.getLocationOnce()
      const currentPosition = createRoutePointFromLocation(location)
      this.setData({
        currentPosition,
        gpsReady: true,
        locationError: '',
        ...buildMapState(this.data.routePoints, currentPosition)
      })
    } catch (error) {
      // Ignore preview failures when the page is only trying to render the current map state.
    }
  },

  async startLocationTracking() {
    if (this.locationTracking) {
      return
    }
    await new Promise((resolve, reject) => {
      wx.startLocationUpdate({
        success: resolve,
        fail: reject
      })
    }).catch((error) => {
      throw new Error(error.errMsg || '开启定位失败')
    })

    if (typeof wx.offLocationChange === 'function') {
      wx.offLocationChange(this.locationListener)
    }
    wx.onLocationChange(this.locationListener)
    this.locationTracking = true
    this.setData({ locationError: '' })
  },

  stopLocationTracking() {
    if (!this.locationTracking) {
      return
    }
    if (typeof wx.offLocationChange === 'function') {
      wx.offLocationChange(this.locationListener)
    }
    if (typeof wx.stopLocationUpdate === 'function') {
      wx.stopLocationUpdate({})
    }
    this.locationTracking = false
  },

  applyRecoveredRun(run) {
    const routePoints = normalizeRoutePoints(run.routePoints)
    const paused = run.state === 'PAUSED'
    const distanceKm = Number(run.distanceKm || 0)
    const durationSeconds = Number(run.durationSeconds || 0)
    const currentPosition = routePoints[routePoints.length - 1] || null
    this.setData({
      sessionId: run.id,
      running: run.state === 'RUNNING' || run.state === 'PAUSED',
      paused,
      recovering: false,
      recoveredSession: true,
      recoveryHint: '检测到上次未结束跑步，你可以继续、结束上传或放弃本次记录。',
      durationSeconds,
      formattedDuration: formatDuration(durationSeconds),
      distanceKm,
      stepCount: Number(run.stepCount || 0),
      pace: formatPace(distanceKm, durationSeconds),
      gpsReady: Boolean(routePoints.length || currentPosition),
      routePointCount: routePoints.length,
      lastLocationTime: routePoints.length ? formatLocationTimeFromPoints(routePoints) : '--',
      locationError: '',
      routePoints,
      currentPosition,
      startedAtMs: parseServerTime(run.startedAt),
      pausedDurationMs: 0,
      pausedStartedAtMs: paused ? parseServerTime(run.pausedAt) : null,
      ...buildMapState(routePoints, currentPosition)
    })
    this.persistState()
  },

  async recoverCurrentRun(showToast = false) {
    const current = await request.get('/runs/current')
    if (!current || !current.id) {
      return false
    }
    this.applyRecoveredRun(current)
    if (current.state === 'RUNNING') {
      this.startTicker()
      await this.startLocationTracking().catch(() => {})
    }
    await this.refreshLocationPreviewSilently()
    if (showToast) {
      wx.showToast({
        title: '已恢复未结束跑步',
        icon: 'none'
      })
    }
    return true
  },

  handleLocationChange(location) {
    if (!this.data.running || this.data.paused) {
      return
    }

    const point = createRoutePointFromLocation(location)
    const routePoints = normalizeRoutePoints(this.data.routePoints).slice()
    const previousPoint = routePoints[routePoints.length - 1]

    if (previousPoint) {
      const deltaDistance = haversineMeters(previousPoint, point)
      const deltaMillis = point.timestamp - previousPoint.timestamp
      if (deltaDistance < 3 && deltaMillis < 3000) {
        this.setData({
          currentPosition: point,
          gpsReady: true,
          lastLocationTime: new Date(point.timestamp).toLocaleTimeString('zh-CN', { hour12: false }),
          ...buildMapState(routePoints, point)
        })
        return
      }

      routePoints.push(point)
      this.setData({
        routePoints,
        currentPosition: point,
        distanceKm: Number((this.data.distanceKm + deltaDistance / 1000).toFixed(3)),
        routePointCount: routePoints.length,
        gpsReady: true,
        lastLocationTime: new Date(point.timestamp).toLocaleTimeString('zh-CN', { hour12: false }),
        ...buildMapState(routePoints, point)
      })
    } else {
      routePoints.push(point)
      this.setData({
        routePoints,
        currentPosition: point,
        routePointCount: routePoints.length,
        gpsReady: true,
        lastLocationTime: new Date(point.timestamp).toLocaleTimeString('zh-CN', { hour12: false }),
        ...buildMapState(routePoints, point)
      })
    }

    this.updateDerivedMetrics()
  },

  seedRouteWithLocation(location) {
    const point = createRoutePointFromLocation(location)
    const routePoints = point ? [point] : []
    this.setData({
      routePoints,
      currentPosition: point,
      gpsReady: Boolean(point),
      routePointCount: routePoints.length,
      lastLocationTime: point ? new Date(point.timestamp).toLocaleTimeString('zh-CN', { hour12: false }) : '--',
      ...buildMapState(routePoints, point)
    })
  },

  isActiveRunConflict(message) {
    return typeof message === 'string' && (
      message.includes('未结束') ||
      (message.includes('跑步记录') && message.includes('请先完成'))
    )
  },

  async discardRunSession(runId, successMessage = '已放弃当前跑步') {
    await request.post(`/runs/${runId}/discard`, {})
    this.stopTicker()
    this.stopLocationTracking()
    this.setData(getDefaultState())
    wx.removeStorageSync('campusRunCurrentRun')
    await this.refreshLocationPreviewSilently()
    if (successMessage) {
      wx.showToast({
        title: successMessage,
        icon: 'success'
      })
    }
  },

  async handleActiveRunConflict() {
    const current = await request.get('/runs/current').catch(() => null)
    if (!current || !current.id) {
      wx.showToast({
        title: '检测到未结束跑步，请稍后重试',
        icon: 'none'
      })
      return 'none'
    }

    const modalResult = await new Promise((resolve) => {
      wx.showModal({
        title: '检测到未结束跑步',
        content: '你上次的跑步还没有结束。你可以恢复继续，也可以放弃并不记录上次跑步。',
        confirmText: '放弃上次',
        cancelText: '恢复查看',
        success: resolve
      })
    })

    if (modalResult.confirm) {
      await this.discardRunSession(current.id, '')
      wx.showToast({
        title: '已放弃上次跑步，正在重新开始',
        icon: 'none'
      })
      return 'discarded'
    }

    this.applyRecoveredRun(current)
    if (current.state === 'RUNNING') {
      this.startTicker()
      await this.startLocationTracking().catch(() => {})
    }
    await this.refreshLocationPreviewSilently()
    return 'recovered'
  },

  async startRun() {
    if (this.data.recovering) {
      return
    }

    try {
      const location = await this.ensureLocationReady()
      const systemInfo = wx.getSystemInfoSync()
      const result = await request.post('/runs/start', {
        source: 'wechat-miniapp',
        deviceModel: systemInfo.model || '',
        devicePlatform: systemInfo.platform || '',
        clientVersion: systemInfo.version || '',
        simulated: false
      })

      this.setData({
        ...getDefaultState(),
        sessionId: result.id,
        running: true,
        startedAtMs: Date.now()
      })
      this.seedRouteWithLocation(location)
      this.persistState()
      await this.startLocationTracking()
      this.startTicker()
      wx.showToast({ title: '开始跑步', icon: 'success' })
    } catch (error) {
      if (this.isActiveRunConflict(error.message)) {
        const resolution = await this.handleActiveRunConflict()
        if (resolution === 'discarded') {
          return this.startRun()
        }
        if (resolution === 'recovered') {
          return
        }
      }

      this.setData({
        locationError: error.message
      })
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async pauseRun() {
    if (!this.data.sessionId) {
      wx.showToast({ title: '当前没有进行中的跑步', icon: 'none' })
      return
    }

    try {
      await request.post(`/runs/${this.data.sessionId}/pause`, {})
      this.stopTicker()
      this.stopLocationTracking()
      this.setData({
        paused: true,
        running: true,
        recoveredSession: true,
        recoveryHint: '这次跑步已暂停，你可以继续、结束上传或放弃本次记录。',
        pausedStartedAtMs: Date.now()
      })
      this.persistState()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async resumeRun() {
    if (!this.data.sessionId) {
      wx.showToast({ title: '当前没有可继续的跑步', icon: 'none' })
      return
    }

    try {
      await this.ensureLocationReady()
      await request.post(`/runs/${this.data.sessionId}/resume`, {})
      const now = Date.now()
      const pausedDurationMs = this.data.pausedDurationMs +
        (this.data.pausedStartedAtMs ? (now - this.data.pausedStartedAtMs) : 0)

      this.setData({
        paused: false,
        running: true,
        recoveredSession: true,
        recoveryHint: '检测到上次未结束跑步，你可以继续、结束上传或放弃本次记录。',
        pausedDurationMs,
        pausedStartedAtMs: null,
        locationError: ''
      })
      await this.startLocationTracking()
      this.persistState()
      this.startTicker()
    } catch (error) {
      this.setData({ locationError: error.message })
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async finishRun() {
    if (!this.data.sessionId) {
      wx.showToast({ title: '当前没有可结束的跑步', icon: 'none' })
      return
    }

    const runId = this.data.sessionId
    try {
      this.stopTicker()
      this.stopLocationTracking()
      const durationSeconds = this.computeDurationSeconds()
      const routePoints = normalizeRoutePoints(this.data.routePoints)
      await request.post(`/runs/${runId}/finish`, {
        distanceKm: Number(this.data.distanceKm.toFixed(3)),
        durationSeconds,
        stepCount: Math.max(this.data.stepCount, Math.round(this.data.distanceKm * 1350)),
        routePoints
      })
      this.setData(getDefaultState())
      wx.removeStorageSync('campusRunCurrentRun')
      wx.navigateTo({
        url: `/pages/run-result/run-result?id=${runId}`
      })
    } catch (error) {
      this.persistState()
      wx.showToast({ title: this.formatRunError(error.message), icon: 'none' })
    }
  },

  async discardRun() {
    if (!this.data.sessionId) {
      wx.showToast({ title: '当前没有可放弃的跑步', icon: 'none' })
      return
    }

    const confirmed = await new Promise((resolve) => {
      wx.showModal({
        title: '放弃当前跑步',
        content: '放弃后这次未结束的跑步会被删除，确定继续吗？',
        success: (res) => resolve(Boolean(res.confirm))
      })
    })
    if (!confirmed) {
      return
    }

    try {
      await this.discardRunSession(this.data.sessionId)
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  formatRunError(message) {
    if (message === '跑步时长过短，未达到有效记录门槛') {
      return '有效跑步至少需要持续 5 分钟'
    }
    if (message === '跑步距离过短，未达到有效记录门槛') {
      return '有效跑步至少需要完成 0.5 公里'
    }
    if (message === '轨迹点过少，请开启定位后重试') {
      return '轨迹点不足，请确认定位已开启并保持移动'
    }
    return message
  }
})
