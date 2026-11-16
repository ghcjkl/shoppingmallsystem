-- 积分抵扣迁移：orders 表加两个字段（可重复执行）
-- 规则：100 积分 = 1 元；单笔订单最多用积分抵扣实付的 20%；与优惠券可叠加
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS points_deducted INT NOT NULL DEFAULT 0 COMMENT '本单使用的积分数量'
        AFTER queue_no,
    ADD COLUMN IF NOT EXISTS points_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '积分抵扣的金额（100积分=1元）'
        AFTER points_deducted;
