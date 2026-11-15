import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'shoping_admin_token'

// 统一请求实例：baseURL 指向 /api，由 vite 代理到后端 localhost:8080
const request = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截器：自动携带管理员令牌
request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：后端统一返回 Result<T> { code, message, data }，code===0 为成功
// 可选请求可在 config 里传 silent: true，失败时不弹全局错误提示（用于辅助性接口）
request.interceptors.response.use(
  (res) => {
    const r = res.data
    // 非标准 Result（如文件流）直接透传
    if (!r || typeof r !== 'object' || !('code' in r)) return r
    if (r.code === 0) return r.data
    if (!res.config?.silent) ElMessage.error(r.message || '请求失败')
    return Promise.reject(new Error(r.message || '请求失败'))
  },
  (err) => {
    // 登录过期/未登录：清除令牌并跳转登录页
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('shoping_admin_user')
      if (window.location.pathname !== '/login') {
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
      }
      return Promise.reject(err)
    }
    if (!err.config?.silent) {
      const msg = err.response?.data?.message || err.message || '网络异常'
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default request
