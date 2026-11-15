import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
      { path: 'product/:id', component: () => import('../views/ProductDetail.vue'), meta: { title: '商品详情' } },
      { path: 'cart', component: () => import('../views/Cart.vue'), meta: { title: '购物车', requiresAuth: true } },
      { path: 'checkout', component: () => import('../views/Checkout.vue'), meta: { title: '确认下单', requiresAuth: true } },
      { path: 'orders', component: () => import('../views/OrderList.vue'), meta: { title: '我的订单', requiresAuth: true } },
      { path: 'order/:id', component: () => import('../views/OrderDetail.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'member', component: () => import('../views/Member.vue'), meta: { title: '会员中心', requiresAuth: true } },
      { path: 'queue', component: () => import('../views/QueueBoard.vue'), meta: { title: '门店看板', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/products', component: () => import('../views/AdminProducts.vue'), meta: { title: '商品管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/balance', component: () => import('../views/AdminBalance.vue'), meta: { title: '储值管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/aftersale', component: () => import('../views/AdminAfterSale.vue'), meta: { title: '售后处理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/store', component: () => import('../views/AdminStore.vue'), meta: { title: '门店设置', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/stock', component: () => import('../views/AdminStock.vue'), meta: { title: '库存管理', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/stats', component: () => import('../views/AdminStats.vue'), meta: { title: '销售看板', requiresAuth: true, requiresAdmin: true } },
      { path: 'admin/reviews', component: () => import('../views/AdminReviews.vue'), meta: { title: '商品评价', requiresAuth: true, requiresAdmin: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：
// - 需登录（购物车/下单/订单/会员）→ 未登录跳 /login
// - 需管理员（门店看板/管理）→ 未登录跳 /login；已登录但不是管理员回首页并提示，不清登录态
router.beforeEach((to) => {
  const token = localStorage.getItem('shoping_admin_token')
  const user = JSON.parse(localStorage.getItem('shoping_admin_user') || 'null')

  if (to.path === '/login') {
    // 已登录访问登录页：管理员去看板，顾客去首页
    if (token) return user?.role === 1 ? '/queue' : '/home'
    return true
  }
  if (to.meta.requiresAuth && !token) {
    return '/login'
  }
  if (to.meta.requiresAdmin && user?.role !== 1) {
    ElMessage.warning('该页面仅门店管理员可访问')
    return '/home'
  }
  return true
})

export default router
