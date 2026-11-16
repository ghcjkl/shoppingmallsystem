package com.shoping.module.user.service.impl;

import com.shoping.module.user.entity.Member;
import com.shoping.module.user.entity.MemberPointRecord;
import com.shoping.module.user.mapper.MemberMapper;
import com.shoping.module.user.mapper.MemberPointRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 会员积分/等级规则单元测试。
 * <p>
 * 只测 MemberServiceImpl 的纯业务规则：Mapper 全部 mock，不起 Spring 容器、不连数据库。
 * 断言方式：捕获传给 updateById / insert 的对象，验证积分、成长值、等级和流水。
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberMapper memberMapper;
    @Mock
    private MemberPointRecordMapper pointRecordMapper;

    private MemberServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MemberServiceImpl(memberMapper, pointRecordMapper);
    }

    /** 构造一个已有会员，避免 getOrCreate 走 insert 分支 */
    private void givenMember(int points, long growth) {
        Member m = new Member();
        m.setId(1L);
        m.setUserId(100L);
        m.setPoints(points);
        m.setGrowthValue(growth);
        m.setLevel(1);
        when(memberMapper.selectOne(any())).thenReturn(m);
    }

    /** 捕获被 updateById 保存的会员 */
    private Member capturedUpdatedMember() {
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberMapper).updateById(captor.capture());
        return captor.getValue();
    }

    /** 捕获写入的积分流水 */
    private MemberPointRecord capturedRecord() {
        ArgumentCaptor<MemberPointRecord> captor = ArgumentCaptor.forClass(MemberPointRecord.class);
        verify(pointRecordMapper).insert(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("onPaid 消费得积分")
    class OnPaid {

        @Test
        @DisplayName("实付 19.90 只加 19 分（金额向下取整）")
        void payTruncatesDecimals() {
            givenMember(0, 0);

            service.onPaid(100L, 500L, new BigDecimal("19.90"));

            Member updated = capturedUpdatedMember();
            assertThat(updated.getPoints()).isEqualTo(19);
            assertThat(updated.getGrowthValue()).isEqualTo(19L);
            assertThat(updated.getLevel()).isEqualTo(1);

            MemberPointRecord record = capturedRecord();
            assertThat(record.getType()).isEqualTo(1);
            assertThat(record.getPoints()).isEqualTo(19);
            assertThat(record.getGrowth()).isEqualTo(19L);
            assertThat(record.getOrderId()).isEqualTo(500L);
            assertThat(record.getRemark()).isEqualTo("消费得积分");
        }

        @Test
        @DisplayName("积分在原值基础上累加")
        void payAccumulates() {
            givenMember(100, 100);

            service.onPaid(100L, 501L, new BigDecimal("25.99"));

            Member updated = capturedUpdatedMember();
            assertThat(updated.getPoints()).isEqualTo(125);
            assertThat(updated.getGrowthValue()).isEqualTo(125L);
        }

        @Test
        @DisplayName("实付为 null 或小于 1 元时不产生任何变更")
        void payIgnoresInvalidAmount() {
            service.onPaid(100L, 502L, null);
            service.onPaid(100L, 503L, new BigDecimal("0.99"));

            verify(memberMapper, never()).updateById(any(Member.class));
            verify(pointRecordMapper, never()).insert(any(MemberPointRecord.class));
        }

        @Test
        @DisplayName("成长值跨过 500 门槛时升级到 V2")
        void payUpgradesLevel() {
            givenMember(499, 499);

            service.onPaid(100L, 504L, new BigDecimal("1.00"));

            assertThat(capturedUpdatedMember().getLevel()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("levelFor 等级边界（通过 onPaid 间接验证）")
    class LevelBoundary {

        @Test
        @DisplayName("各门槛精确值映射到 V1-V5")
        void levelThresholds() {
            assertLevelFor(0L, 1);
            assertLevelFor(499L, 1);
            assertLevelFor(500L, 2);
            assertLevelFor(1499L, 2);
            assertLevelFor(1500L, 3);
            assertLevelFor(2999L, 3);
            assertLevelFor(3000L, 4);
            assertLevelFor(5999L, 4);
            assertLevelFor(6000L, 5);
        }

        /** 每轮清空 mock 调用记录，保证 capturedUpdatedMember 的 verify 只看到本次调用 */
        private void assertLevelFor(long targetGrowth, int expectedLevel) {
            clearInvocations(memberMapper, pointRecordMapper);
            Member m = new Member();
            m.setUserId(100L);
            m.setPoints((int) targetGrowth);
            // 触发 onPaid 加 1 元后成长值正好等于 targetGrowth
            m.setGrowthValue(targetGrowth - 1);
            m.setLevel(1);
            when(memberMapper.selectOne(any())).thenReturn(m);

            service.onPaid(100L, 600L, BigDecimal.ONE);

            assertThat(capturedUpdatedMember().getLevel()).isEqualTo(expectedLevel);
        }
    }

    @Nested
    @DisplayName("onRefund 退款扣回积分")
    class OnRefund {

        @Test
        @DisplayName("退款 9 元：积分和成长值各扣 9")
        void refundDeducts() {
            givenMember(100, 100);

            service.onRefund(100L, 700L, new BigDecimal("9.00"));

            Member updated = capturedUpdatedMember();
            assertThat(updated.getPoints()).isEqualTo(91);
            assertThat(updated.getGrowthValue()).isEqualTo(91L);
            MemberPointRecord record = capturedRecord();
            assertThat(record.getType()).isEqualTo(5);
            assertThat(record.getPoints()).isEqualTo(-9);
            assertThat(record.getGrowth()).isEqualTo(-9L);
        }

        @Test
        @DisplayName("退款金额大于当前积分时只扣到 0，不出现负积分")
        void refundNeverNegative() {
            givenMember(3, 3);

            service.onRefund(100L, 701L, new BigDecimal("9.00"));

            Member updated = capturedUpdatedMember();
            assertThat(updated.getPoints()).isZero();
            assertThat(updated.getGrowthValue()).isZero();
            assertThat(capturedRecord().getPoints()).isEqualTo(-3);
        }

        @Test
        @DisplayName("积分为 0 时退款不写任何流水")
        void refundSkippedWhenNoPoints() {
            givenMember(0, 0);

            service.onRefund(100L, 702L, new BigDecimal("5.00"));

            verify(memberMapper, never()).updateById(any(Member.class));
            verify(pointRecordMapper, never()).insert(any(MemberPointRecord.class));
        }

        @Test
        @DisplayName("退款后成长值跌破门槛会降级")
        void refundDowngradesLevel() {
            givenMember(510, 510);

            service.onRefund(100L, 703L, new BigDecimal("20.00"));

            assertThat(capturedUpdatedMember().getLevel()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("onRecharge 充值得积分")
    class OnRecharge {

        @Test
        @DisplayName("充值 100 加 100 积分和成长值，流水 type=2")
        void rechargeGrantsPoints() {
            givenMember(0, 0);

            service.onRecharge(100L, new BigDecimal("100.00"));

            assertThat(capturedUpdatedMember().getPoints()).isEqualTo(100);
            MemberPointRecord record = capturedRecord();
            assertThat(record.getType()).isEqualTo(2);
            assertThat(record.getOrderId()).isNull();
        }

        @Test
        @DisplayName("充值金额非法时不产生变更")
        void rechargeIgnoresInvalid() {
            service.onRecharge(100L, null);
            service.onRecharge(100L, new BigDecimal("-10"));

            verify(memberMapper, never()).updateById(any(Member.class));
            verify(pointRecordMapper, never()).insert(any(MemberPointRecord.class));
        }
    }

    @Test
    @DisplayName("getOrCreate：查不到时新建 V1 零积分会员")
    void getOrCreateInsertsNewMember() {
        when(memberMapper.selectOne(any())).thenReturn(null);

        Member created = service.getOrCreate(999L);

        assertThat(created.getUserId()).isEqualTo(999L);
        assertThat(created.getLevel()).isEqualTo(1);
        assertThat(created.getPoints()).isZero();
        assertThat(created.getGrowthValue()).isZero();
        verify(memberMapper).insert(created);
    }

    @Test
    @DisplayName("pointRecords 按用户查询流水")
    void pointRecordsQueriesByUser() {
        when(pointRecordMapper.selectList(any())).thenReturn(List.of());

        List<MemberPointRecord> records = service.pointRecords(100L);

        assertThat(records).isEmpty();
        verify(pointRecordMapper).selectList(any());
    }

    @Nested
    @DisplayName("usePoints 积分抵扣")
    class UsePoints {

        @Test
        @DisplayName("积分充足时原子扣减并写 type=3 负数流水")
        void usePointsSuccess() {
            when(memberMapper.deductPoints(100L, 300)).thenReturn(1);

            boolean ok = service.usePoints(100L, 800L, 300);

            assertThat(ok).isTrue();
            verify(memberMapper).deductPoints(100L, 300);
            MemberPointRecord record = capturedRecord();
            assertThat(record.getType()).isEqualTo(3);
            assertThat(record.getPoints()).isEqualTo(-300);
            assertThat(record.getGrowth()).isZero();
            assertThat(record.getOrderId()).isEqualTo(800L);
        }

        @Test
        @DisplayName("积分不足时返回 false，不写流水")
        void usePointsNotEnough() {
            when(memberMapper.deductPoints(100L, 500)).thenReturn(0);

            assertThat(service.usePoints(100L, 801L, 500)).isFalse();
            verify(pointRecordMapper, never()).insert(any(MemberPointRecord.class));
        }

        @Test
        @DisplayName("0 积分直接成功，不产生任何数据库操作")
        void useZeroPoints() {
            assertThat(service.usePoints(100L, 802L, 0)).isTrue();
            verify(memberMapper, never()).deductPoints(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("refundPoints 积分抵扣回退")
    class RefundPoints {

        @Test
        @DisplayName("回退积分写 type=6 正数流水")
        void refundPointsBack() {
            service.refundPoints(100L, 900L, 200);

            verify(memberMapper).increasePoints(100L, 200);
            MemberPointRecord record = capturedRecord();
            assertThat(record.getType()).isEqualTo(6);
            assertThat(record.getPoints()).isEqualTo(200);
            assertThat(record.getOrderId()).isEqualTo(900L);
        }

        @Test
        @DisplayName("回退 0 积分不产生任何操作")
        void refundZero() {
            service.refundPoints(100L, 901L, 0);
            verify(memberMapper, never()).increasePoints(any(), anyInt());
        }
    }
}
