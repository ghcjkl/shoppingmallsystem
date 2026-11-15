import { defineStore } from 'pinia'
import { getUnreadCount, getNoticeList, readNotice, readAllNotices } from '../api/notice'
import { useAuthStore } from './auth'

// 轮询定时器放模块级，避免被 Pinia 响应式包装
let timer = null
const POLL_MS = 30000

export const useNoticeStore = defineStore('notice', {
  state: () => ({
    unread: 0,
    list: [],
    loading: false
  }),
  actions: {
    /** 拉未读数：静默失败（后端未重启时不影响页面） */
    async loadUnread() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.unread = 0
        return
      }
      try {
        const res = await getUnreadCount()
        this.unread = Number(res?.count || 0)
      } catch (e) {
        /* 静默 */
      }
    },

    async loadList() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) {
        this.list = []
        return
      }
      this.loading = true
      try {
        const res = await getNoticeList({ pageNum: 1, pageSize: 20 })
        this.list = res?.records || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },

    async markRead(notice) {
      if (!notice || notice.readFlag === 1) return
      try {
        await readNotice(notice.id)
        notice.readFlag = 1
        this.unread = Math.max(0, this.unread - 1)
      } catch (e) {
        /* 已提示 */
      }
    },

    async markAllRead() {
      if (!this.unread) return
      try {
        await readAllNotices()
        this.list.forEach((n) => (n.readFlag = 1))
        this.unread = 0
      } catch (e) {
        /* 已提示 */
      }
    },

    /** 30 秒轮询未读数，页面隐藏时跳过 */
    startPolling() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) return
      this.loadUnread()
      if (timer) return
      timer = setInterval(() => {
        if (document.visibilityState === 'visible') this.loadUnread()
      }, POLL_MS)
    },

    stopPolling() {
      if (timer) {
        clearInterval(timer)
        timer = null
      }
    }
  }
})
