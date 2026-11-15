package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值档位规则（充 N 送 M）：赠送金额只由服务端按档位决定，不接受客户端传入
 */
@Data
@TableName("recharge_rule")
public class RechargeRule implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 充值金额（元） */
    private BigDecimal amount;

    /** 赠送金额（元） */
    private BigDecimal gift;

    /** 展示排序，越小越前 */
    private Integer sort;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
