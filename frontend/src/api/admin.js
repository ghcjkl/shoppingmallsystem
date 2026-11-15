import request from './request'

// ===== 管理端：商品管理（含图片上传）=====

// 商品分页（含已下架，可按状态筛选）
export const getAdminProductPage = (params) => request.get('/admin/product/page', { params })

// 商品详情（含禁用 SKU）
export const getAdminProduct = (id) => request.get(`/admin/product/${id}`)

// 新增商品
export const createProduct = (data) => request.post('/admin/product', data)

// 编辑商品
export const updateProduct = (id, data) => request.put(`/admin/product/${id}`, data)

// 上下架：status 1 上架 0 下架
export const updateProductStatus = (id, status) =>
  request.put(`/admin/product/${id}/status`, null, { params: { status } })

// 删除商品
export const deleteProduct = (id) => request.delete(`/admin/product/${id}`)

// 上传商品图片，返回 { url, originalName, size }
export const uploadProductImage = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/product/image', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ===== 管理端：储值账户 =====

// 搜索用户及其储值（手机号/昵称/用户名）
export const searchBalanceUsers = (keyword) =>
  request.get('/admin/balance/users', { params: { keyword } })

// 单个用户储值账户
export const getAdminBalance = (userId) => request.get(`/admin/balance/${userId}`)

// 用户储值流水
export const getAdminTransactions = (userId) => request.get(`/admin/balance/${userId}/transactions`)

// 增减金额：正数增加、负数减少
export const adjustBalance = (userId, data) =>
  request.post(`/admin/balance/${userId}/adjust`, data)

// 直接设定金额
export const setBalance = (userId, data) => request.put(`/admin/balance/${userId}`, data)

// ===== 管理端：售后处理 =====

// 售后单列表（status 可选：0 待处理 1 处理中 2 已完成 3 已拒绝）
export const getAdminAfterSales = (status) =>
  request.get('/admin/after-sale/list', { params: { status } })

// 处理售后（同意退款/拒绝等）
export const processAfterSale = (data) => request.post('/admin/after-sale/process', data)

// ===== 管理端：门店订单 =====

// 门店订单列表（status 可选，orderType：1 标品 / 2 现制）
// config 可传 { silent: true }，用在看板这类“取不到也不该弹错”的辅助查询
export const getStoreOrders = (params, config = {}) =>
  request.get('/admin/order/list', { params, ...config })

// ===== 管理端：库存 =====

// 商品库存分页（按 SKU），lowOnly=true 只看低库存
export const getSkuStockPage = (params) => request.get('/admin/stock/sku/page', { params })

// 调整某个 SKU 的库存：changeType 1 入库 / 2 出库 / 3 设定
export const adjustSkuStock = (skuId, data) =>
  request.post(`/admin/stock/sku/${skuId}/adjust`, data)

// 原料库存列表
export const getStockMaterials = () => request.get('/admin/stock/material/list')

// 调整原料库存（参数同上）
export const adjustMaterialStock = (materialId, data) =>
  request.post(`/admin/stock/material/${materialId}/adjust`, data)

// 库存概览
export const getStockSummary = () => request.get('/admin/stock/summary')

// 库存变更流水（bizType：1 商品SKU 2 原料）
export const getStockRecords = (params) => request.get('/admin/stock/records', { params })

// ===== 管理端：销售看板 =====

// days：7 / 14 / 30
export const getSalesDashboard = (params) => request.get('/admin/stats/dashboard', { params })

// ===== 管理端：充值档位（充 N 送 M） =====

// 全部档位（含停用）
export const getRechargeRules = () => request.get('/admin/recharge-rule')

export const createRechargeRule = (data) => request.post('/admin/recharge-rule', data)

export const updateRechargeRule = (id, data) => request.put(`/admin/recharge-rule/${id}`, data)

// status：1 启用 / 0 停用
export const updateRechargeRuleStatus = (id, status) =>
  request.put(`/admin/recharge-rule/${id}/status`, null, { params: { status } })

export const deleteRechargeRule = (id) => request.delete(`/admin/recharge-rule/${id}`)

// ===== 管理端：商品评价 =====

// 评价分页：productId / status(1显示 0隐藏) / rating / keyword
export const getReviewPage = (params) => request.get('/admin/review/page', { params })

// status：1 显示 / 0 隐藏
export const setReviewStatus = (id, status) =>
  request.put(`/admin/review/${id}/status`, null, { params: { status } })

// 商家回复，传空串清除
export const replyReview = (id, reply) => request.put(`/admin/review/${id}/reply`, { reply })

export const deleteReview = (id) => request.delete(`/admin/review/${id}`)
