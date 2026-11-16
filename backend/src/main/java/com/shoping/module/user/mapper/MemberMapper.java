package com.shoping.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shoping.module.user.entity.Member;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 原子扣积分（下单抵扣）：条件更新防止并发重复使用同一批积分。
     * 返回 0 表示积分不足，调用方据此抛「积分不足」。
     */
    @Update("UPDATE member SET points = points - #{points}, "
            + "update_time = NOW() WHERE user_id = #{userId} AND points >= #{points}")
    int deductPoints(@Param("userId") Long userId, @Param("points") int points);

    /** 积分回退（取消订单 / 售后退款）：加法本身原子 */
    @Update("UPDATE member SET points = points + #{points}, "
            + "update_time = NOW() WHERE user_id = #{userId}")
    int increasePoints(@Param("userId") Long userId, @Param("points") int points);
}
