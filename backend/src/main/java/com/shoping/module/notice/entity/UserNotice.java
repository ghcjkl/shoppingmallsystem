package com.shoping.module.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息：取餐提醒等，未读数用于前端红点
 */
@Data
@TableName("user_notice")
public class UserNotice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 1 取餐提醒，2 售后，3 系统 */
    private Integer type;

    private String title;

    private String content;

    private Long orderId;

    /** 1 已读，0 未读 */
    private Integer readFlag;

    private LocalDateTime createTime;
}
