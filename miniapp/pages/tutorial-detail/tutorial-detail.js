const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    tutorialId: '',
    loading: true,
    errorMessage: '',
    tutorial: null
  },

  onLoad(query) {
    this.setData({
      tutorialId: query.id || ''
    })
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.loadData()
  },

  async loadData() {
    if (!this.data.tutorialId) {
      this.setData({
        loading: false,
        errorMessage: '缺少教程 ID'
      })
      return
    }
    this.setData({
      loading: true,
      errorMessage: ''
    })
    try {
      const tutorials = await request.get('/activities/tutorials')
      const tutorial = safeList(tutorials).find((item) => String(item.id) === String(this.data.tutorialId))
      if (!tutorial) {
        throw new Error('未找到对应教程')
      }
      this.setData({
        loading: false,
        tutorial
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  }
})
