package com.shoping.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shoping.module.user.entity.Member;
import com.shoping.module.user.entity.MemberPointRecord;
import com.shoping.module.user.mapper.MemberMapper;
import com.shoping.module.user.mapper.MemberPointRecordMapper;
import com.shoping.module.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员积分/等级实现。
 * <p>
 * 规则：
 * <ul>
 *   <li>消费 1 元 = 1 积分 + 1 成长值；充值 1 元 = 1 积分 + 1 成长值（金额向下取整）。</li>
 *   <li>等级按成长值自动升级：V1 0，V2 500，V3 1500，V4 3000，V5 6000。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final long[] LEVEL_GROWTH = {0L, 500L, 1500L, 3000L, 6000L};

    private final MemberMapper memberMapper;
    private final MemberPointRecordMapper pointRecordMapper;

    @Override
    public Member getOrCreate(Long userId) {
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getUserId, userId));
        if (member == null) {
            member = new Member();
            member.setUserId(userId);
            member.setLevel(1);
            member.setGrowthValue(0L);
            member.setPoints(0);
            memberMapper.insert(member);
        }
        return member;
    }

    @Override
    @Transactional
    public void onPaid(Long userId, Long orderId, BigDecimal payAmount) {
        int amount = payAmount == null ? 0 : payAmount.intValue();
        if (amount <= 0) {
            return;
        }
        Member member = getOrCreate(userId);
        member.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + amount);
        member.setGrowthValue((member.getGrowthValue() == null ? 0L : member.getGrowthValue()) + amount);
        member.setLevel(levelFor(member.getGrowthValue()));
        memberMapper.updateById(member);
        insertRecord(userId, 1, amount, (long) amount, orderId, "消费得积分");
    }

    @Override
    @Transactional
    public void onRecharge(Long userId, BigDecimal amount) {
        int amt = amount == null ? 0 : amount.intValue();
        if (amt <= 0) {
            return;
        }
        Member member = getOrCreate(userId);
        member.setPoints((member.getPoints() == null ? 0 : member.getPoints()) + amt);
        member.setGrowthValue((member.getGrowthValue() == null ? 0L : member.getGrowthValue()) + amt);
        member.setLevel(levelFor(member.getGrowthValue()));
        memberMapper.updateById(member);
        insertRecord(userId, 2, amt, (long) amt, null, "充值得积分");
    }

    @Override
    @Transactional
    public void onRefund(Long userId, Long orderId, BigDecimal refundAmount) {
        int amount = refundAmount == null ? 0 : refundAmount.intValue();
        if (amount <= 0) {
            return;
        }
        Member member = getOrCreate(userId);
        int currentPoints = member.getPoints() == null ? 0 : member.getPoints();
        long currentGrowth = member.getGrowthValue() == null ? 0L : member.getGrowthValue();
        int deductPoints = Math.min(amount, Math.max(currentPoints, 0));
        long deductGrowth = Math.min(amount, Math.max(currentGrowth, 0L));
        if (deductPoints == 0 && deductGrowth == 0) {
            return;
        }
        member.setPoints(currentPoints - deductPoints);
        member.setGrowthValue(currentGrowth - deductGrowth);
        member.setLevel(levelFor(member.getGrowthValue()));
        memberMapper.updateById(member);
        insertRecord(userId, 5, -deductPoints, -deductGrowth, orderId, "退款扣回积分");
    }

    @Override
    public List<MemberPointRecord> pointRecords(Long userId) {
        return pointRecordMapper.selectList(new LambdaQueryWrapper<MemberPointRecord>()
                .eq(MemberPointRecord::getUserId, userId)
                .orderByDesc(MemberPointRecord::getId));
    }

    private int levelFor(long growth) {
        int level = 1;
        for (int i = 1; i < LEVEL_GROWTH.length; i++) {
            if (growth >= LEVEL_GROWTH[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    private void insertRecord(Long userId, int type, int points, long growth, Long orderId, String remark) {
        MemberPointRecord record = new MemberPointRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setPoints(points);
        record.setGrowth(growth);
        record.setOrderId(orderId);
        record.setRemark(remark);
        pointRecordMapper.insert(record);
    }
}
