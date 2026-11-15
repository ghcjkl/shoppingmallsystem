package com.shoping.module.order.vo;

import com.shoping.module.order.entity.OrderQueue;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QueueVO {

    private Long id;
    private Long orderId;
    private Long storeId;
    private Integer queueNo;
    private Integer status;
    private LocalDateTime makeStartTime;
    private LocalDateTime makeFinishTime;
    private Integer estimateMinutes;
    private LocalDateTime createTime;
    /** 关联订单明细（商品名/规格快照/加料/取餐码/备注），供门店看板直接渲染 */
    private OrderVO order;

    public static QueueVO from(OrderQueue queue) {
        QueueVO vo = new QueueVO();
        vo.setId(queue.getId());
        vo.setOrderId(queue.getOrderId());
        vo.setStoreId(queue.getStoreId());
        vo.setQueueNo(queue.getQueueNo());
        vo.setStatus(queue.getStatus());
        vo.setMakeStartTime(queue.getMakeStartTime());
        vo.setMakeFinishTime(queue.getMakeFinishTime());
        vo.setEstimateMinutes(queue.getEstimateMinutes());
        vo.setCreateTime(queue.getCreateTime());
        return vo;
    }
}
