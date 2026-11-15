package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板表
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券名称 */
    private String name;

    /** 类型：1 满减券，2 折扣券，3 补偿券，4 新客券 */
    private Integer type;

    /** 满减门槛 */
    private BigDecimal threshold;

    /** 减免金额/折扣 */
    private BigDecimal amount;

    /** 适用范围：0 全场，1 标品，2 现制 */
    private Integer scope;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private LocalDateTime createTime;
}
