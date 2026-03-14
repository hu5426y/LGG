const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')
const { formatDuration, formatPace } = require('../../utils/format')
const {
  normalizePoint,
  normalizeRoutePoints,
  buildMapState,
  formatLocationTimeFromPoints
} = require('../../utils/run-map')
const { createRunSession, appendRunPoints, finalizeRunSession } = require('../../utils/cloud-run')
const { ROUTES, PACES, buildSimulatedRoute } = require('../../utils/run-simulator')

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

function createSensorSummary() {
  return {
    sampleCount: 0,
    activeSampleCount: 0,
    totalMagnitude: 0,
    maxMagnitude: 0
  }
}

function getSimulationEnabled() {
  const app = getApp()
  return Boolean(app?.globalData?.simulationEnabled)
}

function getMapSubKey() {
  const app = getApp()
  return app?.globalData?.mapSubKey || ''
}

function getModeFromSource(source) {
  return typeof source === 'string' && source.includes('simulated') ? 'SIMULATED' : 'REAL'
}

function summarizeSensorState(sensorState) {
  const sampleCount = sensorState.sampleCount || 0
  const activeSampleCount = sensorState.activeSampleCount || 0
  const averageMagnitude = sampleCount ? sensorState.totalMagnitude / sampleCount : 0
  const motionConfidence = sampleCount ? activeSampleCount / sampleCount : 0
  return {
    sampleCount,
    activeSampleCount,
    averageMagnitude: Number(averageMagnitude.toFixed(3)),
    maxMagnitude: Number((sensorState.maxMagnitude || 0).toFixed(3)),
    motionConfidence: Number(motionConfidence.toFixed(2))
  }
}

function getDefaultState() {
  return {
    sessionId: null,
    cloudSessionId: '',
    runMode: 'REAL',
    running: false,
    paused: false,
    recovering: false,
    recoveredSession: false,
    recoveryHint: '',
    durationSeconds: 0,
    formattedDuration: '00:00:00',
    distanceKm: 0,
    estimatedStepCount: 0,
    calories: 0,
    pace: '--',
    gpsReady: false,
    routePointCount: 0,
    lastLocationTime: '--',
    locationError: '',
    cloudStatus: '本地缓存中',
    motionConfidence: 0,
    motionLabel: '待采集',
    sensorSampleCount: 0,
    routePoints: [],
    currentPosition: null,
    startedAtMs: null,
    pausedDurationMs: 0,
    pausedStartedAtMs: null,
    simulationEnabled: getSimulationEnabled(),
    mapSubKey: getMapSubKey(),
    simulationName: '',
    simulationCompleted: false,
    ...buildMapState([], null)
  }
}

Page({
  data: getDefaultState(),

  onLoad() {
    this.locationListener = this.handleLocationChange.bind(this)
    this.accelerometerListener = this.handleAccelerometerChange.bind(this)
    this.sensorState = createSensorSummary()
    this.pendingCloudPoints = []
    this.cloudBatchIndex = 0
    this.lastCloudFlushAt = 0
    this.simulationQueue = []
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
    this.stopMotionTracking()
    this.stopSimulationPlayback()
  },

  onHide() {
    this.stopTicker()
    this.persistState()
  },

  persistState() {
    const routePoints = normalizeRoutePoints(this.data.routePoints)
    wx.setStorageSync('campusRunCurrentRun', {
      sessionId: this.data.sessionId,
      cloudSessionId: this.data.cloudSessionId,
      runMode: this.data.runMode,
      running: this.data.running,
      paused: this.data.paused,
      recoveredSession: this.data.recoveredSession,
      recoveryHint: this.data.recoveryHint,
      durationSeconds: this.data.durationSeconds,
      formattedDuration: this.data.formattedDuration,
      distanceKm: Number(this.data.distanceKm.toFixed(3)),
      estimatedStepCount: this.data.estimatedStepCount,
      calories: this.data.calories,
      pace: this.data.pace,
      gpsReady: this.data.gpsReady,
      routePointCount: routePoints.length,
      lastLocationTime: this.data.lastLocationTime,
      locationError: this.data.locationError,
      cloudStatus: this.data.cloudStatus,
      motionConfidence: this.data.motionConfidence,
      motionLabel: this.data.motionLabel,
      sensorSampleCount: this.data.sensorSampleCount,
      routePoints,
      currentPosition: this.data.currentPosition,
      startedAtMs: this.data.startedAtMs,
      pausedDurationMs: this.data.pausedDurationMs,
      pausedStartedAtMs: this.data.pausedStartedAtMs,
      simulationName: this.data.simulationName,
      simulationCompleted: this.data.simulationCompleted
    })
  },

  async restoreRunState() {
    this.setData({
      recovering: true,
      locationError: '',
      simulationEnabled: getSimulationEnabled()
    })

    const saved = wx.getStorageSync('campusRunCurrentRun')
    if (saved && saved.sessionId) {
      this.applySavedRun(saved)
      this.setData({ recovering: false })
      if (saved.running && !saved.paused) {
        this.startTicker()
        if (saved.runMode === 'REAL') {
          await this.startLocationTracking().catch(() => {})
          await this.startMotionTracking().catch(() => {})
        }
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
      cloudSessionId: saved.cloudSessionId || '',
      runMode: saved.runMode || 'REAL',
      running,
      paused: Boolean(saved.paused),
      recoveredSession: running,
      recoveryHint: running ? '检测到上次未结束跑步，你可以继续、结束上传或放弃本次记录。' : '',
      durationSeconds,
      formattedDuration: saved.formattedDuration || formatDuration(durationSeconds),
      distanceKm,
      estimatedStepCount: Number(saved.estimatedStepCount || 0),
      calories: Number(saved.calories || 0),
      pace: saved.pace || formatPace(distanceKm, durationSeconds),
      gpsReady: Boolean(saved.gpsReady || routePoints.length || currentPosition),
      routePointCount: routePoints.length,
      lastLocationTime: routePoints.length ? formatLocationTimeFromPoints(routePoints) : '--',
      locationError: saved.locationError || '',
      cloudStatus: saved.cloudStatus || '本地缓存中',
      motionConfidence: Number(saved.motionConfidence || 0),
      motionLabel: saved.motionLabel || '待采集',
      sensorSampleCount: Number(saved.sensorSampleCount || 0),
      routePoints,
      currentPosition,
      startedAtMs: saved.startedAtMs || null,
      pausedDurationMs: Number(saved.pausedDurationMs || 0),
      pausedStartedAtMs: saved.pausedStartedAtMs || null,
      simulationEnabled: getSimulationEnabled(),
      mapSubKey: getMapSubKey(),
      simulationName: saved.simulationName || '',
      simulationCompleted: Boolean(saved.simulationCompleted),
      ...buildMapState(routePoints, currentPosition)
    })
    this.persistState()
  },

  computeDurationSeconds(now = Date.now()) {
    if (this.data.runMode === 'SIMULATED') {
      const routePoints = normalizeRoutePoints(this.data.routePoints)
      if (routePoints.length > 1) {
        return Math.max(0, Math.floor((routePoints[routePoints.length - 1].timestamp - routePoints[0].timestamp) / 1000))
      }
    }
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
    const calories = Math.round(this.data.distanceKm * 60)
    this.setData({
      durationSeconds,
      formattedDuration: formatDuration(durationSeconds),
      pace: formatPace(this.data.distanceKm, durationSeconds),
      calories,
      estimatedStepCount: Math.max(this.data.estimatedStepCount, Math.round(this.data.distanceKm * 1350))
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
      // ignore
    }
  },

  async startLocationTracking() {
    if (this.data.runMode !== 'REAL' || this.locationTracking) {
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

  async startMotionTracking() {
    if (this.motionTracking || typeof wx.startAccelerometer !== 'function') {
      return
    }
    await new Promise((resolve, reject) => {
      wx.startAccelerometer({
        interval: 'game',
        success: resolve,
        fail: reject
      })
    }).catch(() => null)
    if (typeof wx.offAccelerometerChange === 'function') {
      wx.offAccelerometerChange(this.accelerometerListener)
    }
    if (typeof wx.onAccelerometerChange === 'function') {
      wx.onAccelerometerChange(this.accelerometerListener)
      this.motionTracking = true
    }
  },

  stopMotionTracking() {
    if (!this.motionTracking) {
      return
    }
    if (typeof wx.offAccelerometerChange === 'function') {
      wx.offAccelerometerChange(this.accelerometerListener)
    }
    if (typeof wx.stopAccelerometer === 'function') {
      wx.stopAccelerometer({})
    }
    this.motionTracking = false
  },

  handleAccelerometerChange(event) {
    if (!this.data.running || this.data.paused) {
      return
    }
    const magnitude = Math.sqrt((event.x || 0) ** 2 + (event.y || 0) ** 2 + (event.z || 0) ** 2)
    this.sensorState.sampleCount += 1
    this.sensorState.totalMagnitude += magnitude
    this.sensorState.maxMagnitude = Math.max(this.sensorState.maxMagnitude, magnitude)
    if (magnitude >= 1.05) {
      this.sensorState.activeSampleCount += 1
    }
    const summary = summarizeSensorState(this.sensorState)
    this.setData({
      motionConfidence: summary.motionConfidence,
      motionLabel: summary.motionConfidence >= 0.6 ? '运动稳定' : (summary.motionConfidence >= 0.3 ? '轻度波动' : '采集偏弱'),
      sensorSampleCount: summary.sampleCount
    })
  },

  async createCloudSession(mode, firstPoint) {
    try {
      const cloudSessionId = await createRunSession({
        runId: this.data.sessionId,
        mode,
        firstPoint,
        device: wx.getDeviceInfo ? wx.getDeviceInfo() : {},
        mapSubKey: getApp().globalData.mapSubKey || ''
      })
      this.setData({
        cloudSessionId,
        cloudStatus: cloudSessionId ? '云端会话已创建' : '本地缓存中'
      })
    } catch (error) {
      this.setData({
        cloudStatus: '云端不可用，已降级本地缓存'
      })
    }
  },

  queueCloudPoint(point) {
    if (!point) {
      return
    }
    this.pendingCloudPoints.push(point)
    this.flushCloudPoints()
  },

  async flushCloudPoints(force = false) {
    if (!this.pendingCloudPoints.length) {
      return
    }
    const now = Date.now()
    const dueByCount = this.pendingCloudPoints.length >= 10
    const dueByTime = now - this.lastCloudFlushAt >= 15000
    if (!force && !dueByCount && !dueByTime) {
      return
    }
    const currentBatch = this.pendingCloudPoints.splice(0, this.pendingCloudPoints.length)
    try {
      await appendRunPoints(this.data.cloudSessionId, this.cloudBatchIndex, currentBatch)
      this.cloudBatchIndex += 1
      this.lastCloudFlushAt = now
      this.setData({
        cloudStatus: this.data.cloudSessionId ? '已同步轨迹到云端' : '已写入本地缓存'
      })
    } catch (error) {
      this.pendingCloudPoints = currentBatch.concat(this.pendingCloudPoints)
      this.setData({
        cloudStatus: '云同步失败，稍后重试'
      })
    }
  },

  async finalizeCloudSession(status) {
    await this.flushCloudPoints(true)
    const summary = summarizeSensorState(this.sensorState)
    try {
      await finalizeRunSession(this.data.cloudSessionId, {
        status,
        distanceKm: Number(this.data.distanceKm.toFixed(3)),
        durationSeconds: this.computeDurationSeconds(),
        estimatedStepCount: this.data.estimatedStepCount,
        routePointCount: this.data.routePointCount,
        motionConfidence: summary.motionConfidence,
        sensorSummary: summary
      })
      this.setData({
        cloudStatus: status === 'FINISHED' ? '云端会话已完成' : '云端会话已更新'
      })
    } catch (error) {
      this.setData({
        cloudStatus: '云端总结写入失败'
      })
    }
  },

  applyRecoveredRun(run) {
    const routePoints = normalizeRoutePoints(run.routePoints)
    const paused = run.state === 'PAUSED'
    const distanceKm = Number(run.distanceKm || 0)
    const durationSeconds = Number(run.durationSeconds || 0)
    const currentPosition = routePoints[routePoints.length - 1] || null
    const runMode = getModeFromSource(run.source)
    this.setData({
      sessionId: run.id,
      cloudSessionId: run.cloudSessionId || '',
      runMode,
      running: run.state === 'RUNNING' || run.state === 'PAUSED',
      paused,
      recovering: false,
      recoveredSession: true,
      recoveryHint: '检测到上次未结束跑步，你可以继续、结束上传或放弃本次记录。',
      durationSeconds,
      formattedDuration: formatDuration(durationSeconds),
      distanceKm,
      estimatedStepCount: Number(run.estimatedStepCount || 0),
      calories: Number(run.calories || 0),
      pace: formatPace(distanceKm, durationSeconds),
      gpsReady: Boolean(routePoints.length || currentPosition),
      routePointCount: routePoints.length,
      lastLocationTime: routePoints.length ? formatLocationTimeFromPoints(routePoints) : '--',
      locationError: '',
      cloudStatus: run.cloudSessionId ? '已恢复云端会话' : '恢复到本地缓存',
      motionConfidence: Number(run.motionConfidence || 0),
      motionLabel: Number(run.motionConfidence || 0) > 0.6 ? '运动稳定' : '采集中',
      sensorSampleCount: 0,
      routePoints,
      currentPosition,
      startedAtMs: parseServerTime(run.startedAt),
      pausedDurationMs: 0,
      pausedStartedAtMs: paused ? parseServerTime(run.pausedAt) : null,
      simulationEnabled: getSimulationEnabled(),
      mapSubKey: getMapSubKey(),
      simulationName: runMode === 'SIMULATED' ? '模拟路线' : '',
      simulationCompleted: false,
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
      if (this.data.runMode === 'REAL') {
        await this.startLocationTracking().catch(() => {})
        await this.startMotionTracking().catch(() => {})
      }
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

  appendRoutePoint(point) {
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
    this.queueCloudPoint(point)
    this.updateDerivedMetrics()
  },

  handleLocationChange(location) {
    if (!this.data.running || this.data.paused || this.data.runMode !== 'REAL') {
      return
    }
    const point = createRoutePointFromLocation(location)
    this.appendRoutePoint(point)
  },

  seedRouteWithPoint(point) {
    const routePoints = point ? [point] : []
    this.setData({
      routePoints,
      currentPosition: point,
      gpsReady: Boolean(point),
      routePointCount: routePoints.length,
      lastLocationTime: point ? new Date(point.timestamp).toLocaleTimeString('zh-CN', { hour12: false }) : '--',
      ...buildMapState(routePoints, point)
    })
    if (point) {
      this.queueCloudPoint(point)
    }
  },

  async chooseStartMode() {
    if (!this.data.simulationEnabled) {
      return { mode: 'REAL' }
    }
    const result = await new Promise((resolve) => {
      wx.showActionSheet({
        itemList: ['真实跑步', '模拟跑步'],
        success: (res) => resolve(res.tapIndex === 1 ? { mode: 'SIMULATED' } : { mode: 'REAL' }),
        fail: () => resolve({ mode: 'REAL' })
      })
    })
    if (result.mode !== 'SIMULATED') {
      return result
    }
    const routeChoice = await new Promise((resolve) => {
      wx.showActionSheet({
        itemList: ROUTES.map((item) => item.name),
        success: (res) => resolve(ROUTES[res.tapIndex] || ROUTES[0]),
        fail: () => resolve(ROUTES[0])
      })
    })
    const paceChoice = await new Promise((resolve) => {
      wx.showActionSheet({
        itemList: PACES.map((item) => item.label),
        success: (res) => resolve(PACES[res.tapIndex] || PACES[0]),
        fail: () => resolve(PACES[0])
      })
    })
    return {
      mode: 'SIMULATED',
      routeId: routeChoice.id,
      paceId: paceChoice.id,
      simulationName: `${routeChoice.name} · ${paceChoice.label}`
    }
  },

  async startRun() {
    if (this.data.recovering) {
      return
    }

    try {
      const choice = await this.chooseStartMode()
      if (choice.mode === 'SIMULATED') {
        return this.startSimulatedRun(choice)
      }
      return this.startRealRun()
    } catch (error) {
      this.setData({
        locationError: error.message
      })
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async startRealRun() {
    const location = await this.ensureLocationReady()
    const systemInfo = wx.getSystemInfoSync()
    const result = await request.post('/runs/start', {
      source: 'wechat-miniapp',
      deviceModel: systemInfo.model || '',
      devicePlatform: systemInfo.platform || '',
      clientVersion: systemInfo.version || '',
      mode: 'REAL',
      simulated: false
    })
    this.sensorState = createSensorSummary()
    this.pendingCloudPoints = []
    this.cloudBatchIndex = 0
    this.lastCloudFlushAt = 0
    const firstPoint = createRoutePointFromLocation(location)
    this.setData({
      ...getDefaultState(),
      sessionId: result.id,
      runMode: 'REAL',
      running: true,
      startedAtMs: Date.now(),
      simulationEnabled: getSimulationEnabled()
      ,mapSubKey: getMapSubKey()
    })
    this.seedRouteWithPoint(firstPoint)
    await this.createCloudSession('REAL', firstPoint)
    this.persistState()
    await this.startLocationTracking()
    await this.startMotionTracking()
    this.startTicker()
    wx.showToast({ title: '开始跑步', icon: 'success' })
  },

  async startSimulatedRun(choice) {
    const systemInfo = wx.getSystemInfoSync()
    const result = await request.post('/runs/start', {
      source: 'wechat-miniapp',
      deviceModel: systemInfo.model || '',
      devicePlatform: systemInfo.platform || '',
      clientVersion: systemInfo.version || '',
      mode: 'SIMULATED',
      simulated: true
    })
    this.sensorState = createSensorSummary()
    this.pendingCloudPoints = []
    this.cloudBatchIndex = 0
    this.lastCloudFlushAt = 0
    const simulated = buildSimulatedRoute(choice.routeId, choice.paceId)
    const [firstPoint, ...rest] = simulated.points
    this.simulationQueue = rest
    this.setData({
      ...getDefaultState(),
      sessionId: result.id,
      runMode: 'SIMULATED',
      running: true,
      startedAtMs: firstPoint.timestamp,
      simulationName: choice.simulationName || simulated.route.name,
      simulationEnabled: getSimulationEnabled()
      ,mapSubKey: getMapSubKey()
    })
    this.seedRouteWithPoint(firstPoint)
    await this.createCloudSession('SIMULATED', firstPoint)
    this.persistState()
    this.startTicker()
    this.startSimulationPlayback()
    wx.showToast({ title: '模拟跑步已开始', icon: 'success' })
  },

  startSimulationPlayback() {
    this.stopSimulationPlayback()
    this.simulationTimer = setInterval(() => {
      if (!this.data.running || this.data.paused) {
        return
      }
      const nextPoint = this.simulationQueue.shift()
      if (!nextPoint) {
        this.stopSimulationPlayback()
        this.setData({
          simulationCompleted: true,
          cloudStatus: '模拟轨迹已生成，可结束上传'
        })
        return
      }
      this.sensorState.sampleCount += 1
      this.sensorState.activeSampleCount += 1
      this.sensorState.totalMagnitude += 1.16
      this.sensorState.maxMagnitude = Math.max(this.sensorState.maxMagnitude, 1.22)
      const summary = summarizeSensorState(this.sensorState)
      this.setData({
        motionConfidence: summary.motionConfidence,
        motionLabel: '模拟运动',
        sensorSampleCount: summary.sampleCount
      })
      this.appendRoutePoint(nextPoint)
    }, 800)
  },

  stopSimulationPlayback() {
    if (this.simulationTimer) {
      clearInterval(this.simulationTimer)
      this.simulationTimer = null
    }
  },

  isActiveRunConflict(message) {
    return typeof message === 'string' && (
      message.includes('未结束') ||
      (message.includes('跑步记录') && message.includes('请先完成'))
    )
  },

  async discardRunSession(runId, successMessage = '已放弃当前跑步') {
    await request.post(`/runs/${runId}/discard`, {})
    await this.finalizeCloudSession('DISCARDED')
    this.stopTicker()
    this.stopLocationTracking()
    this.stopMotionTracking()
    this.stopSimulationPlayback()
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
      if (this.data.runMode === 'REAL') {
        await this.startLocationTracking().catch(() => {})
        await this.startMotionTracking().catch(() => {})
      }
    }
    await this.refreshLocationPreviewSilently()
    return 'recovered'
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
      this.stopMotionTracking()
      this.stopSimulationPlayback()
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
      if (this.data.runMode === 'REAL') {
        await this.ensureLocationReady()
      }
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
      if (this.data.runMode === 'REAL') {
        await this.startLocationTracking()
        await this.startMotionTracking()
      } else if (this.simulationQueue.length) {
        this.startSimulationPlayback()
      }
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
      this.stopMotionTracking()
      this.stopSimulationPlayback()
      const durationSeconds = this.computeDurationSeconds()
      const routePoints = normalizeRoutePoints(this.data.routePoints)
      const sensorSummary = summarizeSensorState(this.sensorState)
      await this.finalizeCloudSession('FINISHED')
      await request.post(`/runs/${runId}/finish`, {
        distanceKm: Number(this.data.distanceKm.toFixed(3)),
        durationSeconds,
        estimatedStepCount: Math.max(this.data.estimatedStepCount, Math.round(this.data.distanceKm * 1350)),
        cloudSessionId: this.data.cloudSessionId || '',
        sensorSummary: JSON.stringify(sensorSummary),
        motionConfidence: sensorSummary.motionConfidence,
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
