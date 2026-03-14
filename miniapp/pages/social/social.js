const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    posts: [],
    commentsMap: {},
    content: '',
    publishing: false
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.loadPosts()
  },

  async loadPosts() {
    this.setData({
      loading: true,
      errorMessage: ''
    })
    try {
      const posts = await request.get('/social/posts')
      this.setData({
        loading: false,
        posts: safeList(posts)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  handleInput(event) {
    this.setData({
      content: event.detail.value
    })
  },

  async publishPost() {
    if (this.data.publishing) {
      return
    }
    if (!this.data.content) {
      wx.showToast({ title: '请输入内容', icon: 'none' })
      return
    }
    this.setData({ publishing: true })
    try {
      await request.post('/social/posts', {
        content: this.data.content
      })
      this.setData({ content: '' })
      wx.showToast({ title: '发布成功', icon: 'success' })
      await this.loadPosts()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    } finally {
      this.setData({ publishing: false })
    }
  },

  async likePost(event) {
    const { id } = event.currentTarget.dataset
    try {
      await request.post(`/social/posts/${id}/like`, {})
      await this.loadPosts()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async loadComments(event) {
    const { id } = event.currentTarget.dataset
    try {
      const comments = await request.get(`/social/posts/${id}/comments`)
      this.setData({
        [`commentsMap.${id}`]: safeList(comments)
      })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async commentPost(event) {
    const { id } = event.currentTarget.dataset
    const modal = await this.showEditableModal('发表评论', '输入评论内容')
    if (!modal.confirm || !modal.content) {
      return
    }
    try {
      await request.post(`/social/posts/${id}/comments`, {
        content: modal.content
      })
      await this.loadComments({ currentTarget: { dataset: { id } } })
      await this.loadPosts()
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  async reportPost(event) {
    const { id } = event.currentTarget.dataset
    const modal = await this.showEditableModal('举报原因', '请输入举报原因')
    if (!modal.confirm || !modal.content) {
      return
    }
    try {
      await request.post(`/social/posts/${id}/report`, {
        reason: modal.content
      })
      wx.showToast({ title: '举报成功', icon: 'success' })
    } catch (error) {
      wx.showToast({ title: error.message, icon: 'none' })
    }
  },

  showEditableModal(title, placeholderText) {
    return new Promise((resolve) => {
      wx.showModal({
        title,
        editable: true,
        placeholderText,
        success(res) {
          resolve(res)
        },
        fail() {
          resolve({ confirm: false, content: '' })
        }
      })
    })
  }
})
