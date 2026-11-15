package com.shoping.module.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店表（当前业务为单门店，仅一条记录）
 */
@Data
@TableName("store")
public class Store implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门店名称 */
    private String name;

    /** 门店地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 营业时间 */
    private String businessHours;

    /** 状态：1 营业，0 停业 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
