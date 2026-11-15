package com.shoping.module.order.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.Result;
import com.shoping.module.order.dto.OrderCreateRequest;
import com.shoping.module.order.dto.PayRequest;
import com.shoping.module.order.service.OrderService;
import com.shoping.module.order.vo.OrderVO;
import com.shoping.module.order.vo.QueueVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        // 用户端下单：下单人一律取令牌身份，避免伪造 userId
        request.setUserId(AuthContext.requireUserId());
        return Result.ok(orderService.create(request));
    }

    @PostMapping("/pay")
    public Result<OrderVO> pay(@Valid @RequestBody PayRequest request) {
        // 支付人取令牌身份：顾客只能支付自己的订单，管理员（门店代收）放行
        return Result.ok(orderService.pay(request.getOrderId(), request.getPayType(),
                AuthContext.requireUserId(), AuthContext.isAdmin()));
    }

    @PostMapping("/{orderId}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long orderId) {
        return Result.ok(orderService.cancel(orderId, AuthContext.requireUserId()));
    }

    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        // 顾客只能查自己的订单，管理员可查任意门店订单
        return Result.ok(orderService.detail(orderId, AuthContext.requireUserId(), AuthContext.isAdmin()));
    }

    @GetMapping("/user/{userId}")
    public Result<List<OrderVO>> listByUser(@PathVariable Long userId) {
        return Result.ok(orderService.listByUser(userId));
    }

    @GetMapping("/queue/{storeId}")
    public Result<List<QueueVO>> queueList(@PathVariable Long storeId) {
        return Result.ok(orderService.queueList(storeId));
    }

    @PostMapping("/{orderId}/making")
    public Result<Void> startMaking(@PathVariable Long orderId) {
        orderService.startMaking(orderId);
        return Result.ok();
    }

    @PostMapping("/{orderId}/ready")
    public Result<Void> finishMaking(@PathVariable Long orderId) {
        orderService.finishMaking(orderId);
        return Result.ok();
    }

    @PostMapping("/{orderId}/complete")
    public Result<OrderVO> complete(@PathVariable Long orderId) {
        return Result.ok(orderService.complete(orderId));
    }

    @PostMapping("/pickup/verify")
    public Result<OrderVO> verifyPickup(@RequestParam Long storeId, @RequestParam String pickupCode) {
        return Result.ok(orderService.verifyPickup(storeId, pickupCode));
    }
}
