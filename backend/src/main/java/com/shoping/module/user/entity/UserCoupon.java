package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券表
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 券模板 ID */
    private Long couponId;

    /** 状态：0 未用，1 已用，2 过期 */
    private Integer status;

    /** 核销订单 ID */
    private Long orderId;

    private LocalDateTime createTime;
}
