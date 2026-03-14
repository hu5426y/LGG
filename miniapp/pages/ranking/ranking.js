const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    ranking: []
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
      const ranking = await request.get('/gamification/ranking')
      this.setData({
        loading: false,
        ranking: safeList(ranking)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  }
})
