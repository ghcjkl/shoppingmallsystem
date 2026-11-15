package com.shoping.module.user.service;

import com.shoping.module.user.entity.Coupon;
import com.shoping.module.user.vo.UserCouponVO;

import java.util.List;

public interface CouponService {

    List<Coupon> templates(Integer scope);

    List<UserCouponVO> listUserCoupons(Long userId);

    /** 领取优惠券：返回领取后的用户券 */
    UserCouponVO receive(Long userId, Long couponId);
}
