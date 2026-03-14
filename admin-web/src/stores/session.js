import { defineStore } from 'pinia'
import http from '../api/http'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('campus-run-admin-token') || '',
    user: JSON.parse(localStorage.getItem('campus-run-admin-user') || 'null')
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload) {
      const result = await http.post('/auth/login', payload)
      this.token = result.data.token
      this.user = result.data
      localStorage.setItem('campus-run-admin-token', this.token)
      localStorage.setItem('campus-run-admin-user', JSON.stringify(this.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('campus-run-admin-token')
      localStorage.removeItem('campus-run-admin-user')
    }
  }
})
