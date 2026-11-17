import request from './request'

// 商品分类列表（includeDisabled=true 时含停用分类，管理端用）
export const getCategoryList = (productType, includeDisabled = false) =>
  request.get('/category/list', { params: { productType, includeDisabled } })

// 分类新增 / 改名 / 删除（写操作需要管理员令牌）
export const createCategory = (data) => request.post('/category', data)
export const updateCategory = (data) => request.put('/category', data)
export const deleteCategory = (id) => request.delete(`/category/${id}`)

// 规格组列表（杯型/温度/甜度/冰量）
export const getSpecList = (productType) =>
  request.get('/spec/list', { params: { productType } })

// 商品分页列表
export const getProductPage = (params) => request.get('/product/page', { params })

// 商品详情（含 SKU 与加料）
export const getProductDetail = (id) => request.get(`/product/${id}/detail`)
