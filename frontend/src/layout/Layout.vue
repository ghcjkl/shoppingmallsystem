<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">🧋</div>
        <div class="brand-text">
          <strong>{{ storeName || '食光铺子' }}</strong>
          <span>零食 · 奶茶 · 咖啡</span>
        </div>
      </div>

      <nav class="nav">
        <button
          v-for="item in menus"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="go(item.path)"
        >
          <span class="nav-emoji">{{ item.icon }}</span>
          <span class="nav-label">{{ item.label }}</span>
          <span
            v-if="item.path === '/orders' && notice.unread"
            class="nav-badge nav-red"
            :title="`${notice.unread} 条未读消息`"
          >{{ notice.unread > 99 ? '99+' : notice.unread }}</span>
          <span v-else-if="item.path === '/cart' && cart.count" class="nav-badge">{{ cart.count }}</span>
        </button>

        <template v-if="auth.isAdmin">
          <div class="nav-divider">管理</div>
          <button
            v-for="item in adminMenus"
            :key="item.path"
            type="button"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            @click="go(item.path)"
          >
            <span class="nav-emoji">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
          </button>
        </template>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-title">
          <h2>{{ currentTitle }}</h2>
          <span>{{ storeName || '到店自提 · 无配送' }}</span>
        </div>
        <div class="topbar-right">
          <!-- 站内消息铃铛：有未读取餐提醒时显示红点 -->
          <el-popover
            v-model:visible="noticeVisible"
            placement="bottom-end"
            :width="340"
            trigger="click"
            popper-class="notice-popper"
            @show="notice.loadList()"
          >
            <template #reference>
              <button class="bell-btn" type="button" :class="{ on: notice.unread > 0 }">
                <span class="bell-icon">🔔</span>
                <span v-if="notice.unread > 0" class="bell-badge">{{ notice.unread > 99 ? '99+' : notice.unread }}</span>
              </button>
            </template>

            <div class="notice-panel">
              <div class="np-head">
                <b>消息</b>
                <button
                  v-if="notice.unread > 0"
                  class="np-readall"
                  type="button"
                  @click="notice.markAllRead()"
                >
                  全部已读
                </button>
              </div>

              <div v-if="notice.list.length" class="np-list">
                <button
                  v-for="n in notice.list"
                  :key="n.id"
                  class="np-item"
                  :class="{ unread: n.readFlag === 0 }"
                  type="button"
                  @click="openNotice(n)"
                >
                  <span class="np-dot" />
                  <span class="np-main">
                    <span class="np-title">{{ n.title }}</span>
                    <span class="np-content">{{ n.content }}</span>
                    <span class="np-time">{{ fmtTime(n.createTime) }}</span>
                  </span>
                </button>
              </div>
              <div v-else class="np-empty">暂无消息，订单可取餐时会在这里提醒你</div>
            </div>
          </el-popover>

          <span class="level-tag">V{{ app.level }}</span>
          <span class="user-name">{{ app.nickname }}</span>
          <template v-if="auth.isLoggedIn">
            <span class="admin-name">👤 {{ auth.displayName }}</span>
            <button class="pwd-btn" type="button" @click="openPwd">改密</button>
            <button class="logout-btn" type="button" @click="onLogout">退出</button>
          </template>
          <button v-else class="login-btn" type="button" @click="router.push('/login')">登录</button>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>

    <nav class="mobile-nav">
      <button
        v-for="item in mobileMenus"
        :key="item.path"
        type="button"
        class="mnav-item"
        :class="{ active: isActive(item.path) }"
        @click="go(item.path)"
      >
        <span class="mnav-emoji">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
        <span
          v-if="item.path === '/orders' && notice.unread"
          class="mnav-badge nav-red"
        >{{ notice.unread > 99 ? '99+' : notice.unread }}</span>
        <span v-else-if="item.path === '/cart' && cart.count" class="mnav-badge">{{ cart.count }}</span>
      </button>
    </nav>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="380px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="submitPwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAppStore } from '../stores/app'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { useNoticeStore } from '../stores/notice'
import { changePassword } from '../api/auth'
import { fmtTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const cart = useCartStore()
const auth = useAuthStore()
const notice = useNoticeStore()

// ===== 站内消息（取餐提醒红点） =====
const noticeVisible = ref(false)

const openNotice = async (n) => {
  await notice.markRead(n)
  noticeVisible.value = false
  if (n.orderId) router.push(`/order/${n.orderId}`)
}

const onLogout = () => {
  notice.stopPolling()
  notice.unread = 0
  notice.list = []
  auth.logout()
  ElMessage.success('已退出登录')
  router.push('/home')
}

// 修改密码
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value === pwdForm.oldPassword) callback(new Error('新密码不能与原密码相同'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const openPwd = () => {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdFormRef.value?.clearValidate()
  pwdVisible.value = true
}

const submitPwd = async () => {
  await pwdFormRef.value.validate()
  pwdSaving.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    pwdVisible.value = false
    ElMessage.success('密码修改成功，请重新登录')
    // 改密后旧令牌失效风险，强制重新登录
    setTimeout(() => {
      auth.logout()
      router.push('/login')
    }, 800)
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    pwdSaving.value = false
  }
}

const storeName = computed(() => app.store?.name || '')

const menus = [
  { path: '/home', label: '首页', icon: '🏠' },
  { path: '/cart', label: '购物车', icon: '🛒' },
  { path: '/orders', label: '我的订单', icon: '📋' },
  { path: '/member', label: '会员中心', icon: '👤' }
]

// 门店看板是门店运营页面（含核销、出餐），只放在管理员分组里
const adminMenus = [
  { path: '/queue', label: '门店看板', icon: '🏪' },
  { path: '/admin/stats', label: '销售看板', icon: '📈' },
  { path: '/admin/products', label: '商品管理', icon: '📦' },
  { path: '/admin/stock', label: '库存管理', icon: '📊' },
  { path: '/admin/balance', label: '储值管理', icon: '💰' },
  { path: '/admin/aftersale', label: '售后处理', icon: '↩️' },
  { path: '/admin/reviews', label: '商品评价', icon: '💬' },
  { path: '/admin/store', label: '门店设置', icon: '⚙️' }
]

const mobileMenus = [
  { path: '/home', label: '首页', icon: '🏠' },
  { path: '/cart', label: '购物车', icon: '🛒' },
  { path: '/orders', label: '订单', icon: '📋' },
  { path: '/member', label: '我的', icon: '👤' }
]

const currentTitle = computed(() => route.meta.title || '食光铺子')

// 首页、订单详情、商品详情、管理页等子路径需要高亮对应菜单
const isActive = (path) => {
  if (path === '/orders') return route.path.startsWith('/order')
  if (path === '/admin/products') return route.path === '/admin/products'
  if (path === '/admin/stock') return route.path === '/admin/stock'
  if (path === '/admin/balance') return route.path === '/admin/balance'
  if (path === '/admin/aftersale') return route.path === '/admin/aftersale'
  if (path === '/admin/store') return route.path === '/admin/store'
  if (path === '/admin/stats') return route.path === '/admin/stats'
  return route.path === path
}

const go = (path) => router.push(path)

onMounted(() => {
  app.init()
  notice.startPolling()
})

onUnmounted(() => {
  notice.stopPolling()
})
</script>

<style scoped>
.shell {
  min-height: 100vh;
  padding-left: 232px;
  background:
    radial-gradient(58% 46% at 100% 0%, rgba(47, 107, 255, 0.14), transparent 62%),
    radial-gradient(50% 44% at 0% 100%, rgba(47, 107, 255, 0.08), transparent 60%),
    var(--app-bg);
  background-attachment: fixed;
}

.sidebar {
  position: fixed;
  top: 16px;
  left: 16px;
  bottom: 16px;
  z-index: 30;
  width: 200px;
  display: flex;
  flex-direction: column;
  padding: 22px 14px 16px;
  overflow: hidden;
  border-radius: 24px;
  background: linear-gradient(165deg, #1e3a8a 0%, #1a2f6d 55%, #14234f 100%);
  box-shadow: 0 24px 48px rgba(20, 35, 79, 0.28);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 4px 8px 22px;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-size: 21px;
  background: var(--accent);
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.32);
}

.brand-text strong {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 15px;
  line-height: 1.25;
  color: #f2f6ff;
}

.brand-text span {
  font-size: 11px;
  color: rgba(242, 246, 255, 0.5);
}

.nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-divider {
  margin: 12px 8px 4px;
  padding-top: 10px;
  border-top: 1px solid rgba(242, 246, 255, 0.14);
  font-size: 11px;
  color: rgba(242, 246, 255, 0.4);
  letter-spacing: 2px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 12px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: rgba(242, 246, 255, 0.76);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-item.active {
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 10px 22px rgba(47, 107, 255, 0.28);
}

.nav-emoji {
  font-size: 17px;
}

.nav-badge {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  color: #1e4fd6;
  background: #d6e4ff;
}

/* 未读消息红点 */
.nav-badge.nav-red {
  color: #fff;
  background: #e5484d;
  box-shadow: 0 0 0 2px rgba(229, 72, 77, 0.18);
}

/* 顶栏消息铃铛 */
.bell-btn {
  position: relative;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
  display: grid;
  place-items: center;
  font-size: 16px;
  line-height: 1;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.bell-btn:hover {
  border-color: var(--accent);
}

.bell-btn.on {
  border-color: rgba(229, 72, 77, 0.4);
  box-shadow: 0 0 0 3px rgba(229, 72, 77, 0.1);
}

.bell-icon {
  filter: grayscale(0.15);
}

.bell-badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  border-radius: 9px;
  background: #e5484d;
  color: #fff;
  font-size: 10.5px;
  font-weight: 700;
  line-height: 17px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(229, 72, 77, 0.4);
}

.main {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 16px;
  z-index: 20;
  height: 62px;
  margin: 16px 20px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 0 24px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 14px 34px rgba(26, 43, 74, 0.08);
  backdrop-filter: blur(16px);
}

.topbar-title h2 {
  font-size: 17px;
}

.topbar-title span {
  font-size: 12px;
  color: var(--muted);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.level-tag {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: var(--accent-deep);
  background: var(--accent-light-9, #eef3ff);
}

.user-name {
  font-size: 13px;
  color: var(--ink-soft);
}

.admin-name {
  font-size: 13px;
  color: var(--accent-deep);
  font-weight: 600;
}

.logout-btn,
.login-btn,
.pwd-btn {
  border: none;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

.pwd-btn {
  color: var(--accent-deep);
  background: var(--accent-light-9, #eef3ff);
  font-weight: 600;
}

.pwd-btn:hover {
  opacity: 0.75;
}

.logout-btn {
  color: var(--ink-soft);
  background: var(--fill, #f4f7ff);
}

.logout-btn:hover {
  opacity: 0.75;
}

.login-btn {
  color: #fff;
  font-weight: 600;
  background: var(--accent);
  box-shadow: 0 6px 16px rgba(47, 107, 255, 0.26);
}

.login-btn:hover {
  opacity: 0.9;
}

.content {
  padding: 22px 20px 48px;
}

.mobile-nav {
  display: none;
}

@media (max-width: 960px) {
  .shell {
    padding-left: 84px;
  }

  .sidebar {
    width: 62px;
    padding: 18px 8px 14px;
    border-radius: 20px;
    align-items: center;
  }

  .brand {
    justify-content: center;
    padding: 4px 0 18px;
  }

  .brand-text,
  .nav-label {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 12px 0;
  }

  .nav-badge {
    position: absolute;
    top: 4px;
    right: 2px;
    margin: 0;
  }
}

@media (max-width: 768px) {
  .shell {
    padding-left: 0;
  }

  .sidebar {
    display: none;
  }

  .topbar {
    top: 8px;
    height: 54px;
    margin: 8px 10px 0;
    padding: 0 10px 0 14px;
    border-radius: 14px;
  }

  .topbar-title h2 {
    font-size: 16px;
  }

  .level-tag {
    display: none;
  }

  .content {
    padding: 14px 12px calc(88px + env(safe-area-inset-bottom));
  }

  .mobile-nav {
    position: fixed;
    left: 10px;
    right: 10px;
    bottom: calc(8px + env(safe-area-inset-bottom));
    z-index: 40;
    display: flex;
    padding: 6px 4px;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.94);
    border: 1px solid rgba(255, 255, 255, 0.9);
    box-shadow: 0 16px 34px rgba(26, 43, 74, 0.16);
    backdrop-filter: blur(18px);
  }

  .mnav-item {
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    padding: 5px 0 4px;
    border: none;
    border-radius: 14px;
    background: transparent;
    color: #7d8ba8;
    font-family: inherit;
    font-size: 10.5px;
    cursor: pointer;
  }

  .mnav-item.active {
    color: var(--accent-deep);
    background: #eef3ff;
    font-weight: 600;
  }

  .mnav-emoji {
    font-size: 18px;
  }

  .mnav-badge {
    position: absolute;
    top: -2px;
    right: 22%;
    min-width: 17px;
    height: 17px;
    padding: 0 4px;
    border-radius: 9px;
    display: grid;
    place-items: center;
    font-size: 10px;
    font-weight: 700;
    color: #fff;
    background: var(--red);
  }
}
</style>

<style>
/* el-popover 内容渲染到 body，需用非 scoped 样式 */
.notice-popper.el-popover {
  padding: 0 !important;
  border-radius: 14px !important;
  overflow: hidden;
  border: 1px solid rgba(47, 107, 255, 0.12) !important;
  box-shadow: 0 12px 32px rgba(24, 43, 84, 0.16) !important;
}

.notice-popper .np-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2fb;
}

.notice-popper .np-head b {
  font-size: 14px;
  color: #1a2b4a;
}

.notice-popper .np-readall {
  border: none;
  background: transparent;
  color: #2f6bff;
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;
}

.notice-popper .np-readall:hover {
  text-decoration: underline;
}

.notice-popper .np-list {
  max-height: 320px;
  overflow-y: auto;
}

.notice-popper .np-item {
  width: 100%;
  display: flex;
  gap: 9px;
  padding: 11px 14px;
  border: none;
  border-bottom: 1px solid #f3f6fd;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}

.notice-popper .np-item:last-child {
  border-bottom: none;
}

.notice-popper .np-item:hover {
  background: #f7faff;
}

.notice-popper .np-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: transparent;
}

.notice-popper .np-item.unread .np-dot {
  background: #e5484d;
  box-shadow: 0 0 0 3px rgba(229, 72, 77, 0.14);
}

.notice-popper .np-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.notice-popper .np-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a2b4a;
}

.notice-popper .np-item.unread .np-title {
  color: #1e4fd6;
}

.notice-popper .np-content {
  font-size: 12px;
  color: #6d7c99;
  line-height: 1.5;
  word-break: break-word;
}

.notice-popper .np-time {
  font-size: 11px;
  color: #a6b2c9;
}

.notice-popper .np-empty {
  padding: 26px 14px;
  text-align: center;
  font-size: 12.5px;
  color: #8b98b3;
}
</style>
