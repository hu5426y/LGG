const request = require('../../services/request')
const { requireLogin } = require('../../utils/auth')

function safeList(value) {
  return Array.isArray(value) ? value : []
}

Page({
  data: {
    loading: true,
    errorMessage: '',
    activeTab: 'feed',
    posts: [],
    commentsMap: {},
    content: '',
    publishing: false,
    clubs: [],
    selectedClub: null,
    clubMembers: [],
    clubMessages: [],
    loadingClubDetail: false
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
      const [posts, clubs] = await Promise.all([
        request.get('/social/posts'),
        request.get('/social/clubs')
      ])
      this.setData({
        loading: false,
        posts: safeList(posts),
        clubs: safeList(clubs)
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorMessage: error.message
      })
    }
  },

  switchTab(event) {
    this.setData({
      activeTab: event.currentTarget.dataset.tab
    })
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
      await this.loadData()
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
      await this.loadData()
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
      await this.loadData()
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

  async openClub(event) {
    const club = event.currentTarget.dataset.club
    if (!club || !club.id) {
      return
    }
    this.setData({
      selectedClub: club,
      loadingClubDetail: true
    })
    try {
      const [members, messages] = await Promise.all([
        request.get(`/social/clubs/${club.id}/members`),
        request.get(`/social/clubs/${club.id}/messages`)
      ])
      this.setData({
        loadingClubDetail: false,
        clubMembers: safeList(members),
        clubMessages: safeList(messages)
      })
    } catch (error) {
      this.setData({
        loadingClubDetail: false,
        clubMembers: [],
        clubMessages: []
      })
      wx.showToast({
        title: error.message || '请先加入该小队',
        icon: 'none'
      })
    }
  },

  async joinClub(event) {
    const { id } = event.currentTarget.dataset
    try {
      await request.post(`/social/clubs/${id}/join`, {})
      wx.showToast({
        title: '加入成功',
        icon: 'success'
      })
      await this.loadData()
      await this.openClub({ currentTarget: { dataset: { club: this.data.clubs.find((item) => item.id === id) || { id } } } })
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  async leaveClub(event) {
    const { id } = event.currentTarget.dataset
    try {
      await request.post(`/social/clubs/${id}/leave`, {})
      wx.showToast({
        title: '已退出小队',
        icon: 'success'
      })
      this.setData({
        selectedClub: null,
        clubMembers: [],
        clubMessages: []
      })
      await this.loadData()
    } catch (error) {
      wx.showToast({
        title: error.message,
        icon: 'none'
      })
    }
  },

  async sendClubMessage() {
    if (!this.data.selectedClub?.id) {
      return
    }
    const modal = await this.showEditableModal('发送小队消息', '输入本次约跑或通知内容')
    if (!modal.confirm || !modal.content) {
      return
    }
    try {
      await request.post(`/social/clubs/${this.data.selectedClub.id}/messages`, {
        content: modal.content
      })
      wx.showToast({
        title: '消息已发送',
        icon: 'success'
      })
      await this.openClub({ currentTarget: { dataset: { club: this.data.selectedClub } } })
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
