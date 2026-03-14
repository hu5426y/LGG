import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

function clearSession() {
  localStorage.removeItem('campus-run-admin-token')
  localStorage.removeItem('campus-run-admin-user')
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus-run-admin-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
      return Promise.reject(new Error('登录已失效，请重新登录'))
    }
    const message = error?.response?.data?.message || error.message || '请求失败'
    return Promise.reject(new Error(message))
  }
)

export default http
