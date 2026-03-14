const config = require('./config')

App({
  globalData: {
    apiBaseUrl: config.apiBaseUrl,
    userInfo: null
  },

  onLaunch() {
    const user = wx.getStorageSync('campusRunUser')
    if (user) {
      this.globalData.userInfo = user
    }
  }
})
