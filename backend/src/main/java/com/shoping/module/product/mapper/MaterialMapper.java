package com.shoping.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shoping.module.product.entity.Material;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 原子扣减原料库存，返回 0 表示余量不够。
     * 现制订单并发下单时原料同样会超扣，必须和 SKU 一样走条件更新。
     */
    @Update("UPDATE material SET stock = stock - #{qty}, update_time = NOW() "
            + "WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 原子回补原料库存 */
    @Update("UPDATE material SET stock = stock + #{qty}, update_time = NOW() WHERE id = #{id}")
    int increaseStock(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
