package com.shoping.module.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变更流水（管理端入库 / 出库 / 直接设定留痕）
 */
@Data
@TableName("stock_record")
public class StockRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对象类型：1 商品SKU 2 原料 */
    private Integer bizType;

    /** 对象 ID（sku_id 或 material_id） */
    private Long targetId;

    /** 对象名称快照：商品名 / 规格 或 原料名 */
    private String targetName;

    /** 变更方式：1 入库 2 出库 3 直接设定 */
    private Integer changeType;

    /** 本次变动量（正数增加、负数减少） */
    private BigDecimal changeQty;

    private BigDecimal beforeQty;

    private BigDecimal afterQty;

    private String reason;

    /** 操作人（管理员用户 ID） */
    private Long operatorId;

    private LocalDateTime createTime;
}
