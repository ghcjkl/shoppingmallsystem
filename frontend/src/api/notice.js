import request from './request'

// 未读消息数（前端红点）
export const getUnreadCount = () => request.get('/notice/unread-count', { silent: true })

// 消息分页
export const getNoticeList = (params) => request.get('/notice/list', { params })

// 标记单条已读
export const readNotice = (id) => request.put(`/notice/${id}/read`)

// 全部标记已读
export const readAllNotices = () => request.put('/notice/read-all')

// 进入订单详情时把该订单的提醒标记已读
export const readOrderNotice = (orderId) => request.put(`/notice/order/${orderId}/read`, null, { silent: true })
