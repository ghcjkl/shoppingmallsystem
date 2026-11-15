package com.shoping.module.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类表
 */
@Data
@TableName("category")
public class Category implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 ID，0 为顶级 */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 商品类型：1 标品零食，2 现制饮品 */
    private Integer productType;

    /** 排序 */
    private Integer sort;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 该分类下的商品数（非表字段，查询时填充） */
    @TableField(exist = false)
    private Integer productCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
