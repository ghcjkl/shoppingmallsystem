import request from './request'

// 提交评价（仅限自己已完成订单中的商品，一项一评）
export const createReview = (data) => request.post('/review', data)

// 商品评价列表（公开）：{ avgRating, total, starDist, records, pageNum, pageSize }
export const getProductReviews = (productId, params) =>
  request.get(`/review/product/${productId}`, { params })

// 某订单下我已提交的评价（订单详情判断「已评价」）
export const getOrderReviews = (orderId) => request.get(`/review/order/${orderId}`)
