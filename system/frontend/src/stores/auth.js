import { defineStore } from 'pinia'
import { currentUser, login as loginApi, logout as logoutApi } from '../api/auth'
import { getStoredUser, getToken, removeStoredUser, removeToken, setStoredUser, setToken } from '../utils/token'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getStoredUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    roles: (state) => state.user?.roles || [],
    username: (state) => state.user?.username || ''
  },
  actions: {
    async login(payload) {
      const result = await loginApi(payload)
      setToken(result.token)
      setStoredUser(result)
      this.token = result.token
      this.user = result
      return result
    },
    async refreshCurrentUser() {
      if (!this.token) {
        return null
      }
      const result = await currentUser()
      const user = { ...result, token: this.token }
      setStoredUser(user)
      this.user = user
      return user
    },
    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } finally {
        removeToken()
        removeStoredUser()
        this.token = null
        this.user = null
      }
    },
    hasRole(role) {
      return this.roles.includes(role)
    }
  }
})
