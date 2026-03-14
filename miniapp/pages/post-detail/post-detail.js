const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    postId: '',
    loading: true,
    errorMessage: '',
    post: null,
    comments: []
  },

  onLoad(query) {
    this.setData({
      postId: query.id || ''
    })
  },

  async onShow() {
    if (!requireLogin()) {
      return
    }
    await this.loadData()
  },

  async loadData() {
    if (!this.data.postId) {
      this.setData({
        loading: false,
        errorMessage: '缺少动态 ID'
      })
      return
    }
    this.setData({
      loading: true,
      errorMessage: ''
    })
    try {
      const posts = await request.get('/social/posts')
      const post = safeList(posts).find((item) => String(item.id) === String(this.data.postId))
      if (!post) {
        throw new Error('未找到对应动态')
      }
      const comments = await request.get(`/social/posts/${this.data.postId}/comments`).catch(() => [])
      this.setData({
        loading: false,
        post,
        comments: safeList(comments)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  async likePost() {
    if (!this.data.post?.id) {
      return
    }
    try {
      await request.post(`/social/posts/${this.data.post.id}/like`, {})
      await this.loadData()
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  async commentPost() {
    if (!this.data.post?.id) {
      return
    }
    const modal = await this.showEditableModal('发表评论', '输入评论内容')
    if (!modal.confirm || !modal.content) {
      return
    }
    try {
      await request.post(`/social/posts/${this.data.post.id}/comments`, {
        content: modal.content
      })
      await this.loadData()
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  async reportPost() {
    if (!this.data.post?.id) {
      return
    }
    const modal = await this.showEditableModal('举报原因', '请输入举报原因')
    if (!modal.confirm || !modal.content) {
      return
    }
    try {
      await request.post(`/social/posts/${this.data.post.id}/report`, {
        reason: modal.content
      })
      wx.showToast({
        title: '举报成功',
        icon: 'success'
      })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
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
