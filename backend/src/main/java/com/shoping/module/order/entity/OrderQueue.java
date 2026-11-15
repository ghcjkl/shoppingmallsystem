package com.shoping.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 现制排队表（下单即做核心：支付成功即入队，按支付时间排序）
 *
 * 排队状态（status）：
 *   1 排队中，2 制作中，3 待取餐，4 已完成，5 已取消
 */
@Data
@TableName("order_queue")
public class OrderQueue implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID */
    private Long orderId;

    /** 门店 ID */
    private Long storeId;

    /** 排队号 */
    private Integer queueNo;

    /** 排队状态（见注释） */
    private Integer status;

    /** 开始制作时间 */
    private LocalDateTime makeStartTime;

    /** 制作完成时间 */
    private LocalDateTime makeFinishTime;

    /** 预计等待分钟 */
    private Integer estimateMinutes;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
