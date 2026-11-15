import request from './request'

// 门店：单门店，获取当前门店信息
export const getStore = () => request.get('/store')

// 管理端：保存门店信息与营业状态（写操作需要管理员令牌）
export const updateStore = (data) => request.put('/store', data)
