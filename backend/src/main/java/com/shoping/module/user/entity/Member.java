package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员账户表（等级 + 成长值 + 积分）
 */
@Data
@TableName("member")
public class Member implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 会员等级 V1-V5 */
    private Integer level;

    /** 成长值 */
    private Long growthValue;

    /** 积分 */
    private Integer points;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
