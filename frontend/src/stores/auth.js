import { defineStore } from 'pinia'
import { login as apiLogin, register as apiRegister } from '../api/auth'

const TOKEN_KEY = 'shoping_admin_token'
const USER_KEY = 'shoping_admin_user'

// 登录状态：令牌 + 用户信息（管理员 role=1，顾客 role=0），持久化到 localStorage
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 1,
    userId: (s) => s.user?.userId || null,
    displayName: (s) => s.user?.nickname || s.user?.username || '用户'
  },
  actions: {
    async login(username, password) {
      const data = await apiLogin({ username, password })
      this.persist(data)
      return data
    },
    async register(payload) {
      const data = await apiRegister(payload)
      this.persist(data)
      return data
    },
    persist(data) {
      this.token = data.token
      this.user = data
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
