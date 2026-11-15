import request from './request'

// 用户信息（含会员等级/成长值/积分）
export const getUser = (userId) => request.get(`/user/${userId}`)

// 积分流水明细
export const getPointRecords = (userId) => request.get(`/user/${userId}/points`)

// 储值账户余额（本金/赠送/累计充值）
export const getBalance = (userId) => request.get(`/balance/${userId}`)

// 储值充值（只传金额，赠送额由后端按档位计算）
export const recharge = (data) => request.post('/balance/recharge', data)

// 充值档位：{ amount, gift, total }，前端只展示「充 N 到账 N+M」
export const getRechargeRules = () => request.get('/balance/rules')

// 储值流水
export const getTransactions = (userId) => request.get(`/balance/${userId}/transactions`)

// 优惠券模板
export const getCouponTemplates = (scope) =>
  request.get('/coupon/templates', { params: { scope } })

// 用户优惠券
export const getUserCoupons = (userId) => request.get(`/coupon/user/${userId}`)

// 领取优惠券
export const receiveCoupon = (couponId) =>
  request.post('/coupon/receive', null, { params: { couponId } })
