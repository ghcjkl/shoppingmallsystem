package com.shoping.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.common.enums.ErrorCode;
import com.shoping.common.exception.BusinessException;
import com.shoping.module.user.entity.Coupon;
import com.shoping.module.user.entity.UserCoupon;
import com.shoping.module.user.mapper.CouponMapper;
import com.shoping.module.user.mapper.UserCouponMapper;
import com.shoping.module.user.service.CouponService;
import com.shoping.module.user.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<Coupon> templates(Integer scope) {
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(scope != null, Coupon::getScope, scope)
                .eq(Coupon::getStatus, 1));
    }

    @Override
    public List<UserCouponVO> listUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getId));
        return userCoupons.stream().map(uc -> {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            UserCouponVO vo = new UserCouponVO();
            vo.setId(uc.getId());
            vo.setCouponId(uc.getCouponId());
            vo.setStatus(uc.getStatus());
            if (coupon != null) {
                vo.setName(coupon.getName());
                vo.setType(coupon.getType());
                vo.setThreshold(coupon.getThreshold());
                vo.setAmount(coupon.getAmount());
                vo.setScope(coupon.getScope());
                vo.setEndTime(coupon.getEndTime());
            }
            return vo;
        }).toList();
    }

    @Override
    public UserCouponVO receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (coupon.getEndTime() != null && LocalDateTime.now().isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        // 同一券模板每人限领一张
        Long exists = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_RECEIVED);
        }
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        userCouponMapper.insert(uc);

        UserCouponVO vo = new UserCouponVO();
        vo.setId(uc.getId());
        vo.setCouponId(couponId);
        vo.setStatus(0);
        vo.setName(coupon.getName());
        vo.setType(coupon.getType());
        vo.setThreshold(coupon.getThreshold());
        vo.setAmount(coupon.getAmount());
        vo.setScope(coupon.getScope());
        vo.setEndTime(coupon.getEndTime());
        return vo;
    }
}
