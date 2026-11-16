-- =============================================================
-- 零食·奶茶·咖啡商城系统 数据库初始化脚本
-- 适用范围：单门店 · 标品+现制 · 全部到店自提（无配送/快递）
-- 引擎：InnoDB  字符集：utf8mb4  兼容：MySQL 8.x / DataGrip
-- 执行方式：DataGrip 中选中本文件全量执行，或命令行 source 本文件
-- 幂等：表使用 IF NOT EXISTS，种子数据使用 INSERT IGNORE，可重复执行
-- =============================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS shoping
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE shoping;

-- =============================================================
-- 一、门店域
-- =============================================================

-- 门店表（当前业务为单门店，仅一条记录）
CREATE TABLE IF NOT EXISTS store (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name            VARCHAR(64)  NOT NULL COMMENT '门店名称',
    address         VARCHAR(255) NULL COMMENT '门店地址',
    phone           VARCHAR(32)  NULL COMMENT '联系电话',
    business_hours  VARCHAR(128) NULL COMMENT '营业时间',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1营业 0停业',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '门店表';

-- =============================================================
-- 二、商品域（标品零食 / 现制饮品两套模型共用主表，规格由 SKU 承载）
-- =============================================================

-- 商品分类表
CREATE TABLE IF NOT EXISTS category (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id     BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0为顶级',
    name          VARCHAR(64) NOT NULL COMMENT '分类名称',
    product_type  TINYINT     NOT NULL COMMENT '商品类型：1标品零食 2现制饮品',
    sort          INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status        TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_type (product_type)
) ENGINE = InnoDB COMMENT = '商品分类表';

-- 商品 SPU 表
CREATE TABLE IF NOT EXISTS product (
    id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name             VARCHAR(128)  NOT NULL COMMENT '商品名称',
    product_type     TINYINT       NOT NULL COMMENT '商品类型：1标品零食 2现制饮品',
    category_id      BIGINT        NULL COMMENT '分类ID',
    cover_img        VARCHAR(255)  NULL COMMENT '主图URL',
    detail           TEXT          NULL COMMENT '商品详情',
    base_price       DECIMAL(10,2) NULL COMMENT '基准价（现制为默认杯型价）',
    status           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：1上架 0下架',
    net_content      VARCHAR(64)   NULL COMMENT '净含量（标品）',
    shelf_life       VARCHAR(64)   NULL COMMENT '保质期（标品）',
    allergen         VARCHAR(255)  NULL COMMENT '过敏原',
    est_make_minutes INT           NULL COMMENT '预计出餐时长/分钟（现制）',
    calories         INT           NULL COMMENT '热量/千卡（现制）',
    sugar_note       VARCHAR(64)   NULL COMMENT '糖分提示（现制）',
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删 0未删',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_type_status (product_type, status)
) ENGINE = InnoDB COMMENT = '商品SPU表';

-- 规格组表（杯型/温度/甜度/冰量）
CREATE TABLE IF NOT EXISTS spec_group (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    name         VARCHAR(32) NOT NULL COMMENT '规格组名',
    product_type TINYINT     NULL COMMENT '商品类型：1标品 2现制，NULL通用',
    sort         INT         NOT NULL DEFAULT 0 COMMENT '排序',
    create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '规格组表';

-- 规格选项表（中杯/大杯、冰/常温/热…）
CREATE TABLE IF NOT EXISTS spec_option (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id    BIGINT        NOT NULL COMMENT '规格组ID',
    name        VARCHAR(32)   NOT NULL COMMENT '选项名',
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '加价金额',
    sort        INT           NOT NULL DEFAULT 0 COMMENT '排序',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_group (group_id)
) ENGINE = InnoDB COMMENT = '规格选项表';

-- 商品 SKU 表
CREATE TABLE IF NOT EXISTS product_sku (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id  BIGINT        NOT NULL COMMENT '商品ID',
    sku_code    VARCHAR(64)   NULL COMMENT 'SKU编码',
    price       DECIMAL(10,2) NOT NULL COMMENT '售价',
    stock       INT           NOT NULL DEFAULT 0 COMMENT '库存（标品总仓库存）',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1可用 0禁用',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删 0未删',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT = '商品SKU表';

-- SKU 与规格选项关联表（决定一个 SKU 是“中杯/冰/半糖”组合）
CREATE TABLE IF NOT EXISTS sku_spec_option (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    sku_id         BIGINT   NOT NULL COMMENT 'SKU ID',
    spec_option_id BIGINT   NOT NULL COMMENT '规格选项ID',
    PRIMARY KEY (id),
    KEY idx_sku (sku_id),
    KEY idx_option (spec_option_id)
) ENGINE = InnoDB COMMENT = 'SKU规格组合关联表';

-- 加料表（珍珠/椰果/芝士奶盖…，本身也是一种消耗原料的选项）
CREATE TABLE IF NOT EXISTS ingredient (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(32)   NOT NULL COMMENT '加料名',
    price       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '加料价',
    material_id BIGINT        NULL COMMENT '对应原料ID（用于扣减原料库存）',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '加料表';

-- 商品可选加料关联表
CREATE TABLE IF NOT EXISTS product_ingredient (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id    BIGINT        NOT NULL COMMENT '商品ID',
    ingredient_id BIGINT        NOT NULL COMMENT '加料ID',
    price         DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '该商品下加料价（可覆盖默认）',
    PRIMARY KEY (id),
    KEY idx_product (product_id)
) ENGINE = InnoDB COMMENT = '商品可选加料关联表';

-- 原料表（门店原料库存，支付成功后按 BOM 扣减）
CREATE TABLE IF NOT EXISTS material (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)   NOT NULL COMMENT '原料名',
    unit        VARCHAR(16)   NULL COMMENT '单位',
    stock       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '当前库存',
    safe_stock  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '安全库存阈值',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '原料库存表';

-- 商品原料 BOM 表（原料售罄 -> 关联商品自动下架的核心依赖）
CREATE TABLE IF NOT EXISTS product_material (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id  BIGINT        NOT NULL COMMENT '商品ID',
    sku_id      BIGINT        NULL COMMENT 'SKU ID，NULL 表示商品级（所有 SKU）',
    material_id BIGINT        NOT NULL COMMENT '原料ID',
    quantity    DECIMAL(10,3) NOT NULL COMMENT '每份用量',
    PRIMARY KEY (id),
    KEY idx_product (product_id),
    KEY idx_material (material_id)
) ENGINE = InnoDB COMMENT = '商品原料BOM用量表';

-- =============================================================
-- 三、用户 / 会员 / 储值
-- =============================================================

-- 用户表（顾客 + 管理员共用，role 区分）
CREATE TABLE IF NOT EXISTS user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone       VARCHAR(20)  NULL COMMENT '手机号',
    username    VARCHAR(64)  NULL COMMENT '登录用户名（管理员）',
    password    VARCHAR(255) NULL COMMENT '登录密码（BCrypt 加密）',
    role        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0顾客 1管理员',
    nickname    VARCHAR(64)  NULL COMMENT '昵称',
    avatar      VARCHAR(255) NULL COMMENT '头像URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT = '用户表';

-- 会员账户表（等级 + 成长值 + 积分）
CREATE TABLE IF NOT EXISTS member (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '用户ID',
    level        TINYINT  NOT NULL DEFAULT 1 COMMENT '会员等级 V1-V5',
    growth_value BIGINT   NOT NULL DEFAULT 0 COMMENT '成长值',
    points       INT      NOT NULL DEFAULT 0 COMMENT '积分',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB COMMENT = '会员账户表';

-- 积分流水表（积分/成长值变动明细）
CREATE TABLE IF NOT EXISTS member_point_record (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    type        TINYINT      NOT NULL COMMENT '类型：1消费得积分 2充值得积分 3积分抵扣 4人工调整 5退款扣回 6积分抵扣回退',
    points      INT          NOT NULL DEFAULT 0 COMMENT '积分变动（正加负减）',
    growth      BIGINT       NOT NULL DEFAULT 0 COMMENT '成长值变动',
    order_id    BIGINT       NULL COMMENT '关联订单ID',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '积分流水表';

-- 储值账户表（本金与赠送分账，退款时需区分）
CREATE TABLE IF NOT EXISTS balance_account (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT        NOT NULL COMMENT '用户ID',
    principal      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本金余额',
    gift           DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '赠送余额',
    total_recharge DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '累计充值',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB COMMENT = '储值账户表（本金/赠送分账）';

-- 储值流水表（充值/消费/退款/赠送/人工调整，全程可查）
CREATE TABLE IF NOT EXISTS balance_transaction (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id           BIGINT        NOT NULL COMMENT '用户ID',
    type              TINYINT       NOT NULL COMMENT '类型：1充值 2消费 3退款 4赠送 5人工调整',
    principal_amount  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本金变动（正增负减）',
    gift_amount       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '赠送变动（正增负减）',
    balance_principal DECIMAL(10,2) NULL COMMENT '变动后本金余额',
    balance_gift      DECIMAL(10,2) NULL COMMENT '变动后赠送余额',
    order_id          BIGINT        NULL COMMENT '关联订单',
    remark            VARCHAR(255)  NULL COMMENT '备注',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '储值流水表';

-- 充值档位表（充 N 送 M）：赠送金额只由服务端按档位计算，前端不可传
CREATE TABLE IF NOT EXISTS recharge_rule (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    amount      DECIMAL(10,2) NOT NULL COMMENT '充值金额',
    gift        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '赠送金额',
    sort        INT           NOT NULL DEFAULT 0 COMMENT '展示排序',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_amount (amount)
) ENGINE = InnoDB COMMENT = '充值档位表（充N送M）';

-- 优惠券模板表
CREATE TABLE IF NOT EXISTS coupon (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)   NOT NULL COMMENT '券名称',
    type        TINYINT       NOT NULL COMMENT '类型：1满减券 2折扣券 3补偿券 4新客券',
    threshold   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '满减门槛',
    amount      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '减免金额/折扣',
    scope       TINYINT       NOT NULL DEFAULT 0 COMMENT '适用范围：0全场 1标品 2现制',
    start_time  DATETIME      NULL COMMENT '生效时间',
    end_time    DATETIME      NULL COMMENT '失效时间',
    status      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '优惠券模板表';

-- 用户优惠券表
CREATE TABLE IF NOT EXISTS user_coupon (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    coupon_id   BIGINT   NOT NULL COMMENT '券模板ID',
    status      TINYINT  NOT NULL DEFAULT 0 COMMENT '状态：0未用 1已用 2过期',
    order_id    BIGINT   NULL COMMENT '核销订单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '用户优惠券表';

-- =============================================================
-- 四、订单 / 排队
-- =============================================================

-- 订单主表
-- 订单状态机（status）统一编码：
--   1 待付款
--   2 已支付（标品=待取货；现制=已入队）
--   3 制作中（仅现制）
--   4 待取餐/待取货（现制=待取餐；标品=到店待核销）
--   5 已完成
--   6 已取消
--   7 售后中
CREATE TABLE IF NOT EXISTS orders (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_no        VARCHAR(32)   NOT NULL COMMENT '订单号',
    user_id         BIGINT        NOT NULL COMMENT '用户ID',
    store_id        BIGINT        NOT NULL COMMENT '门店ID',
    order_type      TINYINT       NOT NULL COMMENT '订单类型：1标品 2现制',
    status          TINYINT       NOT NULL COMMENT '订单状态（见状态机注释）',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '商品总额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    pay_amount      DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    pay_type        TINYINT       NULL COMMENT '支付方式：1线上 2储值 3混合',
    balance_paid    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '储值支付金额',
    cash_paid       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '线上/现金支付金额',
    pickup_code     VARCHAR(16)   NULL COMMENT '取餐码',
    queue_no        INT           NULL COMMENT '排队号（现制）',
    points_deducted INT           NOT NULL DEFAULT 0 COMMENT '本单使用的积分数量',
    points_amount   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '积分抵扣的金额（100积分=1元）',
    remark          VARCHAR(255)  NULL COMMENT '备注（少冰/不要吸管等）',
    pay_time        DATETIME      NULL COMMENT '支付时间',
    finish_time     DATETIME      NULL COMMENT '完成时间',
    cancel_time     DATETIME      NULL COMMENT '取消时间',
    deleted         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删 0未删',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user (user_id),
    KEY idx_store_status (store_id, status)
) ENGINE = InnoDB COMMENT = '订单主表';

-- 订单明细表（含 SKU/规格/加料快照，防止商品后续变更影响历史订单）
CREATE TABLE IF NOT EXISTS order_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id     BIGINT        NOT NULL COMMENT '订单ID',
    product_id   BIGINT        NOT NULL COMMENT '商品ID',
    sku_id       BIGINT        NULL COMMENT 'SKU ID',
    product_name VARCHAR(128)  NULL COMMENT '商品名快照',
    sku_spec     VARCHAR(255)  NULL COMMENT '规格快照（中杯/冰/半糖）',
    ingredients  VARCHAR(255)  NULL COMMENT '加料快照',
    price        DECIMAL(10,2) NOT NULL COMMENT '成交单价',
    quantity     INT           NOT NULL DEFAULT 1 COMMENT '数量',
    amount       DECIMAL(10,2) NOT NULL COMMENT '小计',
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB COMMENT = '订单明细表';

-- 现制排队表（下单即做核心：支付成功即入队，按支付时间排序）
-- 排队状态（status）：
--   1 排队中 2 制作中 3 待取餐 4 已完成 5 已取消
CREATE TABLE IF NOT EXISTS order_queue (
    id                BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id          BIGINT   NOT NULL COMMENT '订单ID',
    store_id          BIGINT   NOT NULL COMMENT '门店ID',
    queue_no          INT      NOT NULL COMMENT '排队号',
    status            TINYINT  NOT NULL COMMENT '排队状态（见注释）',
    make_start_time   DATETIME NULL COMMENT '开始制作时间',
    make_finish_time  DATETIME NULL COMMENT '制作完成时间',
    estimate_minutes  INT      NULL COMMENT '预计等待分钟',
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order (order_id),
    KEY idx_store_status (store_id, status)
) ENGINE = InnoDB COMMENT = '现制排队表';

-- =============================================================
-- 五、售后
-- =============================================================

-- 售后工单表
CREATE TABLE IF NOT EXISTS after_sale (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT        NOT NULL COMMENT '订单ID',
    order_item_id BIGINT        NULL COMMENT '订单明细ID',
    prev_order_status TINYINT   NULL COMMENT '申请前的订单状态，售后结束（拒绝/完成）时用于恢复',
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    type          TINYINT       NOT NULL COMMENT '类型：1退款 2退货退款 3补发 4补偿',
    reason        VARCHAR(255)  NULL COMMENT '原因',
    image_urls    TEXT          NULL COMMENT '凭证图片（JSON数组）',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已完成 3已拒绝',
    refund_amount DECIMAL(10,2) NULL COMMENT '退款金额',
    result        VARCHAR(255)  NULL COMMENT '处理结果',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '售后工单表';

-- 站内消息表（取餐提醒等，未读数用于前端红点）
CREATE TABLE IF NOT EXISTS user_notice (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '接收用户ID',
    type        TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1取餐提醒 2售后 3系统',
    title       VARCHAR(64)  NOT NULL COMMENT '标题',
    content     VARCHAR(255) NULL COMMENT '内容',
    order_id    BIGINT       NULL COMMENT '关联订单（点击可跳转）',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：1已读 0未读',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_read (user_id, read_flag),
    KEY idx_order (order_id)
) ENGINE = InnoDB COMMENT = '站内消息表';

-- 商品评价表（已完成订单按「订单项」评价，一项一评；隐藏/回复由管理端控制）
CREATE TABLE IF NOT EXISTS product_review (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    order_id      BIGINT       NOT NULL COMMENT '订单ID',
    order_item_id BIGINT       NOT NULL COMMENT '订单明细ID（一项一评）',
    product_id    BIGINT       NOT NULL COMMENT '商品ID',
    user_id       BIGINT       NOT NULL COMMENT '评价用户ID',
    rating        TINYINT      NOT NULL COMMENT '评分：1-5 星',
    content       VARCHAR(500) NULL COMMENT '评价内容',
    anonymous     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名：1匿名 0显示昵称',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1显示 0隐藏（管理端）',
    reply         VARCHAR(255) NULL COMMENT '商家回复',
    reply_time    DATETIME     NULL COMMENT '回复时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_item (order_item_id),
    KEY idx_product (product_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '商品评价表';

-- =============================================================
-- 六、库存流水（管理端入库/出库/设定的操作留痕）
-- =============================================================

CREATE TABLE IF NOT EXISTS stock_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    biz_type     TINYINT       NOT NULL COMMENT '对象类型：1 商品SKU 2 原料',
    target_id    BIGINT        NOT NULL COMMENT '对象ID',
    target_name  VARCHAR(160)  NOT NULL COMMENT '对象名称快照',
    change_type  TINYINT       NOT NULL COMMENT '变更方式：1 入库 2 出库 3 直接设定',
    change_qty   DECIMAL(12,2) NOT NULL COMMENT '本次变动量（正数增加、负数减少）',
    before_qty   DECIMAL(12,2) NOT NULL COMMENT '变更前库存',
    after_qty    DECIMAL(12,2) NOT NULL COMMENT '变更后库存',
    reason       VARCHAR(255)  NULL COMMENT '备注原因',
    operator_id  BIGINT        NULL COMMENT '操作人（管理员用户ID）',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_biz_target (biz_type, target_id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB COMMENT = '库存变更流水';

-- =============================================================
-- 七、种子数据（INSERT IGNORE 保证可重复执行）
-- =============================================================

-- 单门店
INSERT IGNORE INTO store (id, name, address, phone, business_hours, status) VALUES
(1, '旗舰店', '示例地址（按实际填写）', '13800000000', '09:00-22:00', 1);

-- 分类：零食 + 现制 两大顶级分类
INSERT IGNORE INTO category (id, parent_id, name, product_type, sort) VALUES
(1, 0, '零食',   1, 1),
(2, 0, '现制',   2, 2),
(3, 1, '膨化',   1, 1),
(4, 1, '坚果',   1, 2),
(5, 1, '糖巧',   1, 3),
(6, 2, '奶茶',   2, 1),
(7, 2, '咖啡',   2, 2),
(8, 2, '果茶',   2, 3);

-- 规格组
INSERT IGNORE INTO spec_group (id, name, product_type, sort) VALUES
(1, '杯型', 2, 1),
(2, '温度', 2, 2),
(3, '甜度', 2, 3),
(4, '冰量', 2, 4);

-- 规格选项
INSERT IGNORE INTO spec_option (id, group_id, name, price_delta, sort) VALUES
(1,  1, '中杯',   0, 1),
(2,  1, '大杯',   3, 2),
(3,  2, '冰',     0, 1),
(4,  2, '常温',   0, 2),
(5,  2, '热',     0, 3),
(6,  3, '全糖',   0, 1),
(7,  3, '半糖',   0, 2),
(8,  3, '无糖',   0, 3),
(9,  4, '正常冰', 0, 1),
(10, 4, '少冰',   0, 2),
(11, 4, '去冰',   0, 3);

-- 原料
INSERT IGNORE INTO material (id, name, unit, stock, safe_stock, status) VALUES
(1, '珍珠',   '份', 100, 20, 1),
(2, '椰果',   '份', 100, 20, 1),
(3, '芝士',   '份', 50,  10, 1),
(4, '茶底',   '份', 200, 50, 1),
(5, '牛奶',   '份', 200, 50, 1),
(6, '咖啡豆', '份', 80,  15, 1);

-- 加料
INSERT IGNORE INTO ingredient (id, name, price, material_id, status) VALUES
(1, '珍珠',     3, 1, 1),
(2, '椰果',     2, 2, 1),
(3, '芝士奶盖', 5, 3, 1);

-- 商品（标品零食 + 现制饮品）
INSERT IGNORE INTO product (id, name, product_type, category_id, cover_img, detail, base_price, status, net_content, shelf_life, allergen, est_make_minutes, calories, sugar_note) VALUES
(1, '原味薯片',   1, 3, NULL, '薄脆原味薯片，追剧必备',     9.90,  1, '75g',   '9个月',  '小麦',       NULL, NULL, NULL),
(2, '夏威夷果',   1, 4, NULL, '奶油味夏威夷果，颗颗饱满',   29.90, 1, '200g',  '12个月', '坚果',       NULL, NULL, NULL),
(3, '榛仁巧克力', 1, 5, NULL, '香浓榛仁裹丝滑牛奶巧克力',   19.90, 1, '100g',  '12个月', '牛奶、大豆', NULL, NULL, NULL),
(4, '珍珠奶茶',   2, 6, NULL, '经典黑糖珍珠，茶香奶香浓郁', 15.00, 1, NULL, NULL, '牛奶',   5,  380, '甜度可选'),
(5, '美式咖啡',   2, 7, NULL, '现磨咖啡豆，醇苦回甘',       18.00, 1, NULL, NULL, '咖啡因', 3,  10,  '可选加糖'),
(6, '满杯水果茶', 2, 8, NULL, '多种鲜果拼配，清爽解腻',     16.00, 1, NULL, NULL, '无',     6,  220, '甜度可选');

-- SKU：标品一个默认规格；现制中杯/大杯两个规格
INSERT IGNORE INTO product_sku (id, product_id, sku_code, price, stock, status) VALUES
(1, 1, '默认', 9.90,  200, 1),
(2, 2, '默认', 29.90, 150, 1),
(3, 3, '默认', 19.90, 120, 1),
(4, 4, '中杯', 15.00, 100, 1),
(5, 4, '大杯', 18.00, 100, 1),
(6, 5, '中杯', 18.00, 100, 1),
(7, 5, '大杯', 21.00, 100, 1),
(8, 6, '中杯', 16.00, 100, 1),
(9, 6, '大杯', 19.00, 100, 1);

-- 现制可选加料
INSERT IGNORE INTO product_ingredient (id, product_id, ingredient_id, price) VALUES
(1, 4, 1, 3),
(2, 4, 2, 2),
(3, 4, 3, 5),
(4, 5, 3, 5),
(5, 6, 1, 3),
(6, 6, 2, 2);

-- 商品原料配方（BOM）：现制商品支付成功后按此扣减原料库存，sku_id 为 NULL 表示所有规格共用
INSERT IGNORE INTO product_material (id, product_id, sku_id, material_id, quantity) VALUES
(1,  4, NULL, 1, 1),   -- 珍珠奶茶：珍珠 1 份
(2,  4, NULL, 4, 1),   -- 珍珠奶茶：茶底 1 份
(3,  4, NULL, 5, 1),   -- 珍珠奶茶：牛奶 1 份
(4,  5, NULL, 6, 1),   -- 美式咖啡：咖啡豆 1 份
(5,  5, NULL, 4, 1),   -- 美式咖啡：茶底 1 份
(6, 11, NULL, 5, 1),   -- 抹茶拿铁：牛奶 1 份
(7, 11, NULL, 4, 1),   -- 抹茶拿铁：茶底 1 份
(8, 23, NULL, 6, 1),   -- 生椰拿铁：咖啡豆 1 份
(9, 23, NULL, 5, 1);   -- 生椰拿铁：牛奶 1 份

-- 演示顾客（C 端固定 userId=1 使用，无登录）
INSERT IGNORE INTO user (id, phone, username, nickname, role, status) VALUES
(1, '13800000000', NULL, '会员用户', 0, 1);

INSERT IGNORE INTO member (id, user_id, level, growth_value, points) VALUES
(1, 1, 1, 0, 0);

INSERT IGNORE INTO balance_account (id, user_id, principal, gift, total_recharge) VALUES
(1, 1, 0, 0, 0);

-- 优惠券模板（满减/折扣/新客）
INSERT IGNORE INTO coupon (id, name, type, threshold, amount, scope, start_time, end_time, status) VALUES
(1, '满30减5',        1, 30.00, 5.00, 0, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1),
(2, '满50减12',       1, 50.00, 12.00, 0, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1),
(3, '现制饮品9折券',  2, 0.00, 0.90, 2, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1),
(4, '新客立减8元',    4, 0.00, 8.00, 0, '2026-01-01 00:00:00', '2027-12-31 23:59:59', 1);

-- 充值档位（充100送10 / 充300送50 / 充500送100），赠送额由后端按档位决定
INSERT IGNORE INTO recharge_rule (id, amount, gift, sort, status) VALUES
(1, 100.00, 10.00, 1, 1),
(2, 300.00, 50.00, 2, 1),
(3, 500.00, 100.00, 3, 1);

-- 管理员账号由应用启动时自动创建（见 AdminInitializer），默认 admin / 123456
