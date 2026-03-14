function request({ url, method = 'GET', data }) {
  const app = getApp()
  const token = wx.getStorageSync('campusRunToken')
  const apiBaseUrl = app.globalData.apiBaseUrl

  function buildNetworkErrorMessage(err) {
    const rawMessage = (err && err.errMsg) || '网络异常'
    if (rawMessage.includes('timeout')) {
      return `请求超时，请检查接口服务是否启动。当前接口：${apiBaseUrl}`
    }
    if (rawMessage.includes('fail')) {
      if (apiBaseUrl.startsWith('https://')) {
        return `网络不可达，请检查当前接口地址。当前接口：${apiBaseUrl}`
      }
      return `网络不可达，请检查当前接口地址。当前接口：${apiBaseUrl}。真机调试请改成可从手机访问的 HTTPS 公网地址。`
    }
    return rawMessage
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${apiBaseUrl}${url}`,
      method,
      data,
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success(res) {
        const payload = res.data || {}
        if (res.statusCode >= 200 && res.statusCode < 300 && payload.success) {
          resolve(payload.data)
          return
        }
        if (res.statusCode === 401) {
          wx.removeStorageSync('campusRunToken')
          wx.removeStorageSync('campusRunUser')
          getApp().globalData.userInfo = null
        }
        reject(new Error(payload.message || '请求失败'))
      },
      fail(err) {
        reject(new Error(buildNetworkErrorMessage(err)))
      }
    })
  })
}

module.exports = {
  get(url) {
    return request({ url })
  },
  post(url, data) {
    return request({ url, method: 'POST', data })
  }
}
