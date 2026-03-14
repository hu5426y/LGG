const config = require('./config')

App({
  globalData: {
    apiBaseUrl: config.apiBaseUrl || '',
    cloudEnvId: config.cloudEnvId || '',
    cloudService: config.cloudService || '',
    tencentMapKey: config.tencentMapKey || '',
    mapSubKey: config.mapSubKey || '',
    simulationEnabled: Boolean(config.simulationEnabled),
    cloudReady: false,
    cloudError: '',
    userInfo: null
  },

  onLaunch() {
    const user = wx.getStorageSync('campusRunUser')
    if (user) {
      this.globalData.userInfo = user
    }
    this.initCloud()
  },

  initCloud() {
    if (!wx.cloud || !this.globalData.cloudEnvId) {
      this.globalData.cloudReady = false
      return
    }

    try {
      wx.cloud.init({
        env: this.globalData.cloudEnvId,
        traceUser: true
      })
      this.globalData.cloudReady = true
      this.globalData.cloudError = ''
    } catch (error) {
      this.globalData.cloudReady = false
      this.globalData.cloudError = error.message || '云开发初始化失败'
    }
  }
})
