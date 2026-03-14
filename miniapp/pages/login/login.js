const request = require('../../services/request')

Page({
  data: {
    username: '',
    password: '',
    loading: false,
    errorMessage: '',
    requestMode: 'http',
    targetTitle: '',
    targetValue: '',
    targetHelper: '',
    targetWarningMessage: '',
    devAccounts: [
      {
        label: '学生账号',
        username: '20230001',
        password: '123456'
      },
      {
        label: '管理员账号',
        username: 'admin',
        password: 'admin123'
      }
    ]
  },

  onShow() {
    const token = wx.getStorageSync('campusRunToken')
    const target = request.describeRequestTarget()

    this.setData({
      requestMode: target.mode,
      targetTitle: target.title,
      targetValue: target.value,
      targetHelper: target.helper,
      targetWarningMessage: target.warningMessage
    })

    if (token) {
      wx.switchTab({
        url: '/pages/home/home'
      })
    }
  },

  handleInput(event) {
    const { field } = event.currentTarget.dataset
    this.setData({
      [field]: event.detail.value
    })
  },

  fillAccount(event) {
    const { username, password } = event.currentTarget.dataset
    this.setData({
      username,
      password,
      errorMessage: ''
    })
  },

  storeLogin(data) {
    wx.setStorageSync('campusRunToken', data.token)
    wx.setStorageSync('campusRunUser', data)
    getApp().globalData.userInfo = data
  },

  async handlePasswordLogin() {
    if (this.data.loading) {
      return
    }

    if (!this.data.username || !this.data.password) {
      this.setData({
        errorMessage: '请输入账号和密码后再登录'
      })
      wx.showToast({
        title: '请输入账号和密码',
        icon: 'none'
      })
      return
    }

    this.setData({
      loading: true,
      errorMessage: ''
    })

    try {
      const data = await request.post('/auth/login', {
        username: this.data.username,
        password: this.data.password
      })
      this.storeLogin(data)
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      })
      wx.switchTab({
        url: '/pages/home/home'
      })
    } catch (error) {
      this.setData({
        errorMessage: error.message
      })
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  copyTargetValue() {
    wx.setClipboardData({
      data: this.data.targetValue
    })
  }
})
