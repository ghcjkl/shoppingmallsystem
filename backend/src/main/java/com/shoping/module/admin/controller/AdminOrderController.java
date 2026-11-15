package com.shoping.module.admin.controller;

import com.shoping.common.result.Result;
import com.shoping.module.order.service.OrderService;
import com.shoping.module.order.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端门店订单：按状态/类型查门店订单（排队看板用来看「标品待取餐」等）
 */
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 门店订单列表
     *
     * @param storeId   门店 ID
     * @param status    订单状态，空为全部
     * @param orderType 1 标品 / 2 现制，空为全部
     */
    @GetMapping("/list")
    public Result<List<OrderVO>> list(@RequestParam Long storeId,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) Integer orderType) {
        return Result.ok(orderService.listByStore(storeId, status, orderType));
    }
}
