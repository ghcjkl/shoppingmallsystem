package com.shoping.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 *
 * 订单状态机（status）统一编码：
 *   1 待付款
 *   2 已支付（标品=待取货；现制=已入队）
 *   3 制作中（仅现制）
 *   4 待取餐/待取货（现制=待取餐；标品=到店待核销）
 *   5 已完成
 *   6 已取消
 *   7 售后中
 */
@Data
@TableName("orders")
public class Orders implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 门店 ID */
    private Long storeId;

    /** 订单类型：1 标品，2 现制 */
    private Integer orderType;

    /** 订单状态（见状态机注释） */
    private Integer status;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付方式：1 线上，2 储值，3 混合 */
    private Integer payType;

    /** 储值支付金额 */
    private BigDecimal balancePaid;

    /** 线上/现金支付金额 */
    private BigDecimal cashPaid;

    /** 取餐码 */
    private String pickupCode;

    /** 排队号（现制） */
    private Integer queueNo;

    /** 备注（少冰/不要吸管等） */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
