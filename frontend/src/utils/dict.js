// 订单状态字典
export const ORDER_STATUS = {
  1: { text: '待付款', type: 'warning' },
  2: { text: '已支付', type: 'primary' },
  3: { text: '制作中', type: 'primary' },
  4: { text: '待取餐', type: 'success' },
  5: { text: '已完成', type: 'success' },
  6: { text: '已取消', type: 'info' },
  7: { text: '售后中', type: 'danger' }
}

// 排队状态字典
export const QUEUE_STATUS = {
  1: { text: '排队中', type: 'warning' },
  2: { text: '制作中', type: 'primary' },
  3: { text: '待取餐', type: 'success' },
  4: { text: '已完成', type: 'info' },
  5: { text: '已取消', type: 'info' }
}

// 支付方式字典
export const PAY_TYPE = {
  1: '线上支付',
  2: '储值支付',
  3: '混合支付'
}

// 售后状态字典
export const AFTER_SALE_STATUS = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '处理中', type: 'primary' },
  2: { text: '已完成', type: 'success' },
  3: { text: '已拒绝', type: 'danger' }
}

// 售后类型字典
export const AFTER_SALE_TYPE = {
  1: '退款',
  2: '退货退款',
  3: '补发',
  4: '补偿'
}

// 储值流水类型字典
export const TX_TYPE = {
  1: '充值',
  2: '消费',
  3: '退款',
  4: '赠送',
  5: '人工调整'
}
