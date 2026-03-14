function analyzeApiBaseUrl(apiBaseUrl = '') {
  const normalized = String(apiBaseUrl || '')
  const isHttps = normalized.startsWith('https://')
  const hostMatch = normalized.match(/^https?:\/\/([^/:]+)/)
  const host = hostMatch ? hostMatch[1] : ''
  const isLoopback = host === '127.0.0.1' || host === 'localhost'
  const isPrivateIp =
    /^10\./.test(host) ||
    /^192\.168\./.test(host) ||
    /^172\.(1[6-9]|2\d|3[0-1])\./.test(host)

  return {
    isHttps,
    host,
    isLoopback,
    isPrivateIp,
    unsuitableForRealDevice: !isHttps || isLoopback || isPrivateIp
  }
}

function getRequestTarget() {
  const app = getApp()
  const globalData = (app && app.globalData) || {}
  const cloudEnvId = globalData.cloudEnvId || ''
  const cloudService = globalData.cloudService || ''
  const apiBaseUrl = globalData.apiBaseUrl || ''
  const cloudSupported = Boolean(wx.cloud && typeof wx.cloud.callContainer === 'function')
  const useCloudContainer = Boolean(cloudEnvId && cloudService && cloudSupported)

  return {
    mode: useCloudContainer ? 'cloud' : 'http',
    cloudEnvId,
    cloudService,
    apiBaseUrl
  }
}

function describeRequestTarget() {
  const target = getRequestTarget()
  if (target.mode === 'cloud') {
    return {
      mode: 'cloud',
      title: '当前模式',
      value: `微信云托管 / ${target.cloudService}`,
      helper: `云环境：${target.cloudEnvId}`,
      warningMessage: ''
    }
  }

  const apiInfo = analyzeApiBaseUrl(target.apiBaseUrl)
  return {
    mode: 'http',
    title: '当前接口',
    value: target.apiBaseUrl,
    helper: '真机调试建议改成 HTTPS 公网地址，避免手机请求无响应。',
    warningMessage: apiInfo.unsuitableForRealDevice
      ? `当前接口是 ${target.apiBaseUrl}，真机调试时手机可能无法访问。建议改成手机可访问的 HTTPS 地址。`
      : ''
  }
}

function buildHttpNetworkErrorMessage(apiBaseUrl, err) {
  const rawMessage = (err && err.errMsg) || '网络异常'
  const apiInfo = analyzeApiBaseUrl(apiBaseUrl)

  if (rawMessage.includes('timeout')) {
    if (apiInfo.unsuitableForRealDevice) {
      return `请求超时，当前接口为 ${apiBaseUrl}，不适合真机调试。请改成手机可访问的 HTTPS 地址。`
    }
    return `请求超时，请检查接口服务是否已启动。当前接口：${apiBaseUrl}`
  }

  if (rawMessage.includes('fail')) {
    if (apiInfo.unsuitableForRealDevice) {
      return `当前接口 ${apiBaseUrl} 多半只能在开发者工具中访问，真机调试时手机通常无法访问。请改成手机可访问的 HTTPS 公网地址。`
    }
    return `网络不可达，请检查当前接口地址。当前接口：${apiBaseUrl}`
  }

  return rawMessage
}

function buildCloudNetworkErrorMessage(target, err) {
  const rawMessage = (err && err.errMsg) || '云托管请求失败'
  if (rawMessage.includes('timeout')) {
    return `云托管请求超时，请检查服务 ${target.cloudService} 是否已启动，环境是否选择为 ${target.cloudEnvId}。`
  }
  return `云托管请求失败，请检查环境 ${target.cloudEnvId} 下的服务 ${target.cloudService} 是否已发布。${rawMessage}`
}

function normalizeResponsePayload(res) {
  if (res && typeof res === 'object' && 'data' in res) {
    return res.data || {}
  }
  return {}
}

function requestByHttp({ url, method, data, token, target }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${target.apiBaseUrl}${url}`,
      method,
      data,
      timeout: 8000,
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success(res) {
        const payload = normalizeResponsePayload(res)
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
        reject(new Error(buildHttpNetworkErrorMessage(target.apiBaseUrl, err)))
      }
    })
  })
}

function requestByCloudContainer({ url, method, data, token, target }) {
  return new Promise((resolve, reject) => {
    wx.cloud.callContainer({
      config: {
        env: target.cloudEnvId
      },
      path: `/api${url}`,
      method,
      data,
      header: {
        'X-WX-SERVICE': target.cloudService,
        Authorization: token ? `Bearer ${token}` : ''
      },
      success(res) {
        const payload = normalizeResponsePayload(res)
        const statusCode = res.statusCode || 200
        if (statusCode >= 200 && statusCode < 300 && payload.success) {
          resolve(payload.data)
          return
        }

        if (statusCode === 401) {
          wx.removeStorageSync('campusRunToken')
          wx.removeStorageSync('campusRunUser')
          getApp().globalData.userInfo = null
        }

        reject(new Error(payload.message || '云托管请求失败'))
      },
      fail(err) {
        reject(new Error(buildCloudNetworkErrorMessage(target, err)))
      }
    })
  })
}

function request({ url, method = 'GET', data }) {
  const token = wx.getStorageSync('campusRunToken')
  const target = getRequestTarget()

  if (target.mode === 'cloud') {
    return requestByCloudContainer({ url, method, data, token, target })
  }

  return requestByHttp({ url, method, data, token, target })
}

module.exports = {
  analyzeApiBaseUrl,
  describeRequestTarget,
  get(url) {
    return request({ url })
  },
  post(url, data) {
    return request({ url, method: 'POST', data })
  }
}
