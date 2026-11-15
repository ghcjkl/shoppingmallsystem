import request from './request'

// 发起售后申请
export const applyAfterSale = (data) => request.post('/after-sale/apply', data)

// 用户售后列表
export const getAfterSales = (userId) => request.get(`/after-sale/user/${userId}`)

// 处理售后（门店端）
export const processAfterSale = (data) => request.post('/after-sale/process', data)
