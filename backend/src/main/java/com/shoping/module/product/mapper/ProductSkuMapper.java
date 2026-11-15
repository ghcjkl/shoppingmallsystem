package com.shoping.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shoping.module.product.entity.ProductSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 原子扣减库存：把「够不够」和「扣多少」放进同一条 SQL，由数据库行锁保证互斥。
     * <p>
     * 返回 0 表示库存不足（或规格已下架/删除），调用方据此抛「库存不足」。
     * 相比「先 select 再 updateById」的读改写，能避免两单同时抢最后一件都成功。
     */
    @Update("UPDATE product_sku SET stock = stock - #{qty}, update_time = NOW() "
            + "WHERE id = #{skuId} AND deleted = 0 AND status = 1 AND stock >= #{qty}")
    int deductStock(@Param("skuId") Long skuId, @Param("qty") int qty);

    /** 原子回补库存（取消/售后回滚）：加法本身原子，不会丢失并发增量 */
    @Update("UPDATE product_sku SET stock = stock + #{qty}, update_time = NOW() WHERE id = #{skuId}")
    int increaseStock(@Param("skuId") Long skuId, @Param("qty") int qty);
}
