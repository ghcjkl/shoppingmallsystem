package com.shoping.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细表（含 SKU/规格/加料快照，防止商品后续变更影响历史订单）
 */
@Data
@TableName("order_item")
public class OrderItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** SKU ID */
    private Long skuId;

    /** 商品名快照 */
    private String productName;

    /** 规格快照（中杯/冰/半糖） */
    private String skuSpec;

    /** 加料快照 */
    private String ingredients;

    /** 成交单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal amount;
}
