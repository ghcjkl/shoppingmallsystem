package com.shoping.module.user.controller;

import com.shoping.common.context.AuthContext;
import com.shoping.common.result.Result;
import com.shoping.module.user.entity.Coupon;
import com.shoping.module.user.service.CouponService;
import com.shoping.module.user.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/templates")
    public Result<List<Coupon>> templates(@RequestParam(required = false) Integer scope) {
        return Result.ok(couponService.templates(scope));
    }

    @GetMapping("/user/{userId}")
    public Result<List<UserCouponVO>> listUserCoupons(@PathVariable Long userId) {
        return Result.ok(couponService.listUserCoupons(userId));
    }

    /** 领取优惠券：couponId 券模板 ID，领取人取登录令牌身份 */
    @PostMapping("/receive")
    public Result<UserCouponVO> receive(@RequestParam Long couponId) {
        return Result.ok(couponService.receive(AuthContext.requireUserId(), couponId));
    }
}
