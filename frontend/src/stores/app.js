import { defineStore } from 'pinia'
import { getStore } from '../api/store'
import { getUser } from '../api/user'
import { useAuthStore } from './auth'

// 门店默认仍是单门店；用户身份改为从登录态获取（顾客登录后才是真实 userId）
export const CURRENT_STORE_ID = 1

export const useAppStore = defineStore('app', {
  state: () => ({
    user: null,
    store: null
  }),
  getters: {
    // 当前登录用户 ID（未登录时为 null，页面需引导登录）
    userId: () => {
      const auth = useAuthStore()
      return auth.userId
    },
    storeId: () => CURRENT_STORE_ID,
    nickname: (s) => s.user?.nickname || '会员用户',
    phone: (s) => s.user?.phone || '',
    level: (s) => s.user?.level || 1,
    points: (s) => s.user?.points || 0,
    growthValue: (s) => s.user?.growthValue || 0
  },
  actions: {
    async loadUser() {
      const auth = useAuthStore()
      if (!auth.userId) {
        this.user = null
        return
      }
      try {
        this.user = await getUser(auth.userId)
      } catch (e) {
        /* 用户信息加载失败不影响页面 */
      }
    },
    async loadStore() {
      try {
        this.store = await getStore()
      } catch (e) {
        /* 门店信息加载失败不影响页面 */
      }
    },
    async init() {
      await Promise.allSettled([this.loadUser(), this.loadStore()])
    }
  }
})
