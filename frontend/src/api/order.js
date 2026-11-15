import request from './request'

// 创建订单
export const createOrder = (data) => request.post('/order', data)

// 支付订单：payType 1线上 2储值 3混合
export const payOrder = (data) => request.post('/order/pay', data)

// 取消订单
export const cancelOrder = (orderId, userId) =>
  request.post(`/order/${orderId}/cancel`, null, { params: { userId } })

// 订单详情
export const getOrder = (orderId) => request.get(`/order/${orderId}`)

// 用户订单列表
export const getOrdersByUser = (userId) => request.get(`/order/user/${userId}`)

// 门店排队列表（制作中/待取餐/排队中）
export const getQueueList = (storeId) => request.get(`/order/queue/${storeId}`)

// 开始制作
export const startMaking = (orderId) => request.post(`/order/${orderId}/making`)

// 制作完成（出餐）
export const finishMaking = (orderId) => request.post(`/order/${orderId}/ready`)

// 完成取餐
export const completeOrder = (orderId) => request.post(`/order/${orderId}/complete`)

// 核销取餐（按取餐码）
export const verifyPickup = (storeId, pickupCode) =>
  request.post('/order/pickup/verify', null, { params: { storeId, pickupCode } })
