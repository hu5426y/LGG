function requireLogin() {
  const token = wx.getStorageSync('campusRunToken')
  if (!token) {
    wx.redirectTo({
      url: '/pages/login/login'
    })
    return false
  }
  return true
}

module.exports = {
  requireLogin
}
