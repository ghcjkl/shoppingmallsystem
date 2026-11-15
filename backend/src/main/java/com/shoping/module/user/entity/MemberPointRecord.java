package com.shoping.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水表（积分/成长值变动明细）
 */
@Data
@TableName("member_point_record")
public class MemberPointRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 类型：1 消费得积分，2 充值得积分，3 积分抵扣，4 人工调整，5 退款扣回 */
    private Integer type;

    /** 积分变动（正加负减） */
    private Integer points;

    /** 成长值变动 */
    private Long growth;

    private Long orderId;

    private String remark;

    private LocalDateTime createTime;
}
