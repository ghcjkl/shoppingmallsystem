package com.shoping.module.order.vo;

import com.shoping.module.order.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {

    private Long id;
    private Long productId;
    private Long skuId;
    private String productName;
    private String skuSpec;
    private String ingredients;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal amount;

    public static OrderItemVO from(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setProductId(item.getProductId());
        vo.setSkuId(item.getSkuId());
        vo.setProductName(item.getProductName());
        vo.setSkuSpec(item.getSkuSpec());
        vo.setIngredients(item.getIngredients());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setAmount(item.getAmount());
        return vo;
    }
}
