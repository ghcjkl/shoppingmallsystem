package com.shoping.module.order.vo;

import com.shoping.module.order.entity.Orders;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private Integer orderType;
    private Integer status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private Integer payType;
    private BigDecimal balancePaid;
    private BigDecimal cashPaid;
    private String pickupCode;
    private Integer queueNo;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;

    private List<OrderItemVO> items;
    private QueueVO queue;

    public static OrderVO from(Orders order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setStoreId(order.getStoreId());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayType(order.getPayType());
        vo.setBalancePaid(order.getBalancePaid());
        vo.setCashPaid(order.getCashPaid());
        vo.setPickupCode(order.getPickupCode());
        vo.setQueueNo(order.getQueueNo());
        vo.setRemark(order.getRemark());
        vo.setPayTime(order.getPayTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
