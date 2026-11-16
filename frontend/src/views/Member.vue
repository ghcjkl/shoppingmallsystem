<template>
  <div class="member-page">
    <!-- 用户信息 -->
    <el-card shadow="never" class="user-card">
      <div class="user-info">
        <div class="avatar">{{ avatarChar }}</div>
        <div class="meta">
          <h2>{{ app.nickname }}</h2>
          <p>{{ app.phone || '未绑定手机号' }}</p>
        </div>
        <div class="level">
          <span class="level-badge">V{{ app.level }}</span>
          <span>成长值 {{ app.growthValue }}</span>
        </div>
      </div>
      <div class="stats">
        <div class="stat">
          <span class="stat-num">{{ app.points }}</span>
          <span class="stat-label">积分</span>
        </div>
        <div class="stat">
          <span class="stat-num">{{ fmtMoney(totalBalance) }}</span>
          <span class="stat-label">储值余额</span>
        </div>
        <div class="stat">
          <span class="stat-num">{{ couponCount }}</span>
          <span class="stat-label">优惠券</span>
        </div>
      </div>
    </el-card>

    <!-- 储值账户 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="block-head">
          <b>储值账户</b>
          <el-button size="small" type="primary" round @click="openRecharge">充值</el-button>
        </div>
      </template>
      <div class="balance-detail" v-if="balance">
        <div class="bal-item">
          <span>本金</span>
          <b>¥{{ fmtMoney(balance.principal) }}</b>
        </div>
        <div class="bal-item">
          <span>赠送</span>
          <b>¥{{ fmtMoney(balance.gift) }}</b>
        </div>
        <div class="bal-item">
          <span>累计充值</span>
          <b>¥{{ fmtMoney(balance.totalRecharge) }}</b>
        </div>
      </div>
      <el-empty v-else description="暂无储值账户" :image-size="60" />
    </el-card>

    <!-- 积分明细 -->
    <el-card shadow="never" class="block">
      <template #header>
        <div class="block-head">
          <b>积分明细</b>
          <span class="head-tip">消费、充值均自动累计积分与成长值</span>
        </div>
      </template>
      <div class="point-note">
        <span class="note-main">100 积分可抵扣 ¥1，结算页可使用；抵扣不超过订单金额的 20%</span>
        <span class="note-sub">成长值满 500 / 1500 / 3000 / 6000 依次升至 V2 / V3 / V4 / V5</span>
      </div>
      <div v-if="pointRecords.length" class="point-list">
        <div v-for="r in pointRecords" :key="r.id" class="point-item">
          <div class="pt-left">
            <span class="pt-type">{{ pointTypeText(r.type) }}</span>
            <span class="pt-time">{{ fmtTime(r.createTime) }}</span>
          </div>
          <div class="pt-right">
            <span class="pt-points" :class="{ minus: r.points < 0 }">{{ signed(r.points) }} 积分</span>
            <span class="pt-growth" :class="{ minus: r.growth < 0 }">成长值 {{ signed(r.growth) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无积分记录，下单或充值后自动累计" :image-size="60" />
    </el-card>

    <!-- 快捷入口 -->
    <!-- 快捷入口：门店看板仅管理员可见，顾客侧换成领券中心 -->
    <div class="quick-grid">
      <button v-if="auth.isAdmin" class="quick" @click="$router.push('/queue')">🏪 门店看板</button>
      <button class="quick" @click="$router.push('/orders')">📋 我的订单</button>
      <button v-if="!auth.isAdmin" class="quick" @click="scrollToCoupons">🎫 领券中心</button>
    </div>

    <!-- 领券中心 -->
    <el-card shadow="never" class="block coupon-block">
      <template #header><b>领券中心</b></template>
      <div v-if="templates.length" class="coupon-list">
        <div v-for="t in templates" :key="t.id" class="coupon">
          <div class="cp-left">
            <div class="cp-amount" v-if="t.type === 2">{{ Math.round(t.amount * 10) }}折</div>
            <div class="cp-amount" v-else>¥{{ fmtMoney(t.amount) }}</div>
            <div class="cp-threshold" v-if="Number(t.threshold) > 0">
              满 ¥{{ fmtMoney(t.threshold) }} 可用
            </div>
          </div>
          <div class="cp-right">
            <div class="cp-name">{{ t.name }}</div>
            <div class="cp-scope">{{ scopeText(t.scope) }}</div>
          </div>
          <el-button
            size="small"
            type="primary"
            round
            :disabled="receivedIds.includes(t.id)"
            @click="doReceive(t)"
          >
            {{ receivedIds.includes(t.id) ? '已领取' : '领取' }}
          </el-button>
        </div>
      </div>
      <el-empty v-else description="暂无可领取的优惠券" :image-size="60" />
    </el-card>

    <!-- 优惠券 -->
    <el-card shadow="never" class="block">
      <template #header><b>我的优惠券</b></template>
      <div v-if="coupons.length" class="coupon-list">
        <div v-for="c in coupons" :key="c.id" class="coupon">
          <div class="cp-left">
            <div class="cp-amount">¥{{ fmtMoney(c.amount) }}</div>
            <div class="cp-threshold" v-if="Number(c.threshold) > 0">
              满 ¥{{ fmtMoney(c.threshold) }} 可用
            </div>
          </div>
          <div class="cp-right">
            <div class="cp-name">{{ c.name }}</div>
            <div class="cp-scope">{{ scopeText(c.scope) }}</div>
          </div>
          <el-tag size="small" :type="c.status === 0 ? 'success' : 'info'" round>
            {{ c.status === 0 ? '未使用' : c.status === 1 ? '已使用' : '已过期' }}
          </el-tag>
        </div>
      </div>
      <el-empty v-else description="暂无优惠券" :image-size="60" />
    </el-card>

    <!-- 储值流水 -->
    <el-card shadow="never" class="block">
      <template #header><b>储值流水</b></template>
      <div v-if="transactions.length" class="tx-list">
        <div v-for="t in transactions" :key="t.id" class="tx-item">
          <div class="tx-left">
            <div class="tx-type">{{ txTypeText(t.type) }}</div>
            <div class="tx-time">{{ fmtTime(t.createTime) }}</div>
          </div>
          <div class="tx-right">
            <div class="tx-amount" :class="txAmountClass(t)">
              {{ txAmount(t) }}
            </div>
            <div class="tx-bal">余额 ¥{{ fmtMoney(Number(t.balancePrincipal) + Number(t.balanceGift)) }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无流水" :image-size="60" />
    </el-card>

    <!-- 售后记录 -->
    <el-card shadow="never" class="block">
      <template #header><b>售后记录</b></template>
      <div v-if="afterSales.length" class="as-list">
        <div v-for="a in afterSales" :key="a.id" class="as-item">
          <div class="as-left">
            <el-tag size="small" :type="afterSaleStatusInfo(a.status).type" round>
              {{ afterSaleStatusInfo(a.status).text }}
            </el-tag>
            <span class="as-type">{{ afterSaleTypeText(a.type) }}</span>
          </div>
          <div class="as-mid">{{ a.reason || '—' }}</div>
          <div class="as-right">
            <span v-if="a.refundAmount != null">退款 ¥{{ fmtMoney(a.refundAmount) }}</span>
            <span class="as-time">{{ fmtTime(a.createTime) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无售后记录" :image-size="60" />
    </el-card>

    <!-- 充值弹窗：只选档位，赠送额由后端计算，前端不提交 gift -->
    <el-dialog v-model="rechargeVisible" title="储值充值" width="420px">
      <template v-if="rechargeRules.length">
        <div class="rule-tip">选择充值档位，赠送金额以实际到账为准</div>
        <div class="rule-list">
          <div
            v-for="r in rechargeRules"
            :key="r.id"
            class="rule-card"
            :class="{ active: rechargeAmount === Number(r.amount) }"
            @click="rechargeAmount = Number(r.amount)"
          >
            <div class="rule-main">
              <span class="rule-amount">充 ¥{{ fmtMoney(r.amount) }}</span>
              <span class="rule-get">到账 ¥{{ fmtMoney(r.total) }}</span>
            </div>
            <span v-if="Number(r.gift) > 0" class="rule-gift">送 ¥{{ fmtMoney(r.gift) }}</span>
          </div>
        </div>
      </template>
      <template v-else>
        <el-form label-position="top">
          <el-form-item label="充值金额（元）">
            <el-input-number v-model="rechargeAmount" :min="1" :max="100000" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-form>
        <div class="rule-tip">当前未配置充值档位，按充值金额原额到账</div>
      </template>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBalance, recharge, getTransactions, getUserCoupons, getCouponTemplates, receiveCoupon, getPointRecords, getRechargeRules } from '../api/user'
import { getAfterSales } from '../api/aftersale'
import { useAppStore } from '../stores/app'
import { useAuthStore } from '../stores/auth'
import { AFTER_SALE_STATUS, AFTER_SALE_TYPE, TX_TYPE } from '../utils/dict'
import { fmtMoney, fmtTime } from '../utils/format'

const router = useRouter()
const app = useAppStore()
const auth = useAuthStore()

// 顾客侧快捷入口：滚动到领券中心
const scrollToCoupons = () => {
  document.querySelector('.coupon-block')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const balance = ref(null)
const coupons = ref([])
const templates = ref([])
const transactions = ref([])
const afterSales = ref([])
const pointRecords = ref([])
const rechargeVisible = ref(false)
const recharging = ref(false)
const rechargeRules = ref([])
const rechargeAmount = ref(100)

const totalBalance = computed(() =>
  Number(balance.value?.principal || 0) + Number(balance.value?.gift || 0)
)
const couponCount = computed(() => coupons.value.length)
const avatarChar = computed(() => (app.nickname || '会').charAt(0))
const receivedIds = computed(() => coupons.value.map((c) => c.couponId))

const scopeText = (s) => ({ 0: '全场通用', 1: '标品零食', 2: '现制饮品' }[s] || '—')
const txTypeText = (t) => TX_TYPE[t] || '—'
const pointTypeText = (t) => ({ 1: '消费得积分', 2: '充值得积分', 3: '积分抵扣', 4: '人工调整', 5: '退款扣回积分', 6: '积分抵扣回退' }[t] || '—')
const signed = (v) => {
  const n = Number(v || 0)
  return (n > 0 ? '+' : n < 0 ? '−' : '') + Math.abs(n)
}
const afterSaleStatusInfo = (s) => AFTER_SALE_STATUS[s] || { text: '未知', type: 'info' }
const afterSaleTypeText = (t) => AFTER_SALE_TYPE[t] || '—'

const txAmount = (t) => {
  const v = Number(t.principalAmount || 0) + Number(t.giftAmount || 0)
  return (v >= 0 ? '+' : '') + v.toFixed(2)
}
const txAmountClass = (t) => {
  const v = Number(t.principalAmount || 0) + Number(t.giftAmount || 0)
  return v >= 0 ? 'pos' : 'neg'
}

const load = async () => {
  await app.init()
  const userId = app.userId
  try { balance.value = await getBalance(userId) } catch (e) { balance.value = null }
  try { coupons.value = (await getUserCoupons(userId)) || [] } catch (e) { coupons.value = [] }
  try { templates.value = (await getCouponTemplates()) || [] } catch (e) { templates.value = [] }
  try { transactions.value = (await getTransactions(userId)) || [] } catch (e) { transactions.value = [] }
  try { afterSales.value = (await getAfterSales(userId)) || [] } catch (e) { afterSales.value = [] }
  try { pointRecords.value = (await getPointRecords(userId)) || [] } catch (e) { pointRecords.value = [] }
  try { rechargeRules.value = (await getRechargeRules()) || [] } catch (e) { rechargeRules.value = [] }
}

const doReceive = async (coupon) => {
  try {
    await receiveCoupon(coupon.id)
    ElMessage.success('领取成功')
    // 刷新我的券与领取状态
    coupons.value = (await getUserCoupons(app.userId)) || []
  } catch (e) {
    /* 已提示 */
  }
}

const openRecharge = () => {
  // 默认选中第一个档位（有档位时不允许自定义金额）
  rechargeAmount.value = rechargeRules.value.length
    ? Number(rechargeRules.value[0].amount)
    : 100
  rechargeVisible.value = true
}

const doRecharge = async () => {
  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    return ElMessage.warning('请选择充值档位')
  }
  const hit = rechargeRules.value.find((r) => Number(r.amount) === Number(rechargeAmount.value))
  recharging.value = true
  try {
    // 只提交金额，赠送额由后端按档位计算
    balance.value = await recharge({ userId: app.userId, amount: rechargeAmount.value })
    ElMessage.success(hit ? `充值成功，到账 ¥${fmtMoney(hit.total)}` : '充值成功')
    rechargeVisible.value = false
    // 充值会同时累计积分与成长值，一并刷新
    await Promise.all([
      app.loadUser(),
      getTransactions(app.userId).then((r) => (transactions.value = r || [])).catch(() => {}),
      getPointRecords(app.userId).then((r) => (pointRecords.value = r || [])).catch(() => {})
    ])
  } catch (e) {
    /* 已提示 */
  } finally {
    recharging.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.member-page {
  max-width: 860px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  background:
    linear-gradient(120deg, rgba(47, 107, 255, 0.12), transparent 55%),
    var(--surface);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  background: var(--accent);
}

.meta h2 {
  font-size: 18px;
}

.meta p {
  font-size: 12.5px;
  color: var(--muted);
  margin-top: 3px;
}

.level {
  margin-left: auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  font-size: 12px;
  color: var(--muted);
}

.level-badge {
  padding: 3px 12px;
  border-radius: 999px;
  font-weight: 700;
  color: var(--accent-deep);
  background: #eef3ff;
}

.stats {
  display: flex;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 20px;
  font-weight: 800;
  color: var(--ink);
}

.stat-label {
  font-size: 12px;
  color: var(--muted);
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.head-tip {
  font-size: 12px;
  color: var(--muted);
}

.point-note {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: #eef3ff;
  border-left: 3px solid var(--accent);
}

.note-main {
  font-size: 13px;
  font-weight: 600;
  color: var(--accent-deep);
}

.note-sub {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.5;
}

.point-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.point-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f4f7ff;
}

.pt-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pt-type {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.pt-time {
  font-size: 12px;
  color: var(--muted);
}

.pt-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pt-points {
  font-size: 15px;
  font-weight: 700;
  color: var(--accent-deep);
}

.pt-points.minus {
  color: #d94a4a;
}

.pt-growth {
  font-size: 12px;
  color: var(--muted);
}

.pt-growth.minus {
  color: #d94a4a;
}

.balance-detail {
  display: flex;
  gap: 12px;
}

.bal-item {
  flex: 1;
  padding: 14px;
  border-radius: 12px;
  background: #f4f7ff;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bal-item span {
  font-size: 12px;
  color: var(--muted);
}

.bal-item b {
  font-size: 18px;
  color: var(--accent-deep);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick {
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
  font-size: 15px;
  font-family: inherit;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: transform 0.15s ease;
}

.quick:hover {
  transform: translateY(-2px);
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coupon {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 14px;
  background: linear-gradient(120deg, #eef3ff, #dce7ff);
}

.cp-left {
  min-width: 90px;
}

.cp-amount {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent-deep);
}

.cp-threshold {
  font-size: 11px;
  color: var(--muted);
}

.cp-right {
  flex: 1;
  min-width: 0;
}

.cp-name {
  font-weight: 600;
  font-size: 14px;
}

.cp-scope {
  font-size: 12px;
  color: var(--muted);
  margin-top: 3px;
}

.tx-list,
.as-list {
  display: flex;
  flex-direction: column;
}

.tx-item,
.as-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
}

.tx-item + .tx-item,
.as-item + .as-item {
  border-top: 1px solid var(--line);
}

.tx-type {
  font-weight: 600;
  font-size: 14px;
}

.tx-time,
.as-time {
  font-size: 12px;
  color: var(--muted);
  margin-top: 3px;
}

.tx-right {
  text-align: right;
}

.tx-amount {
  font-size: 15px;
  font-weight: 700;
}

.tx-amount.pos {
  color: var(--green);
}

.tx-amount.neg {
  color: var(--ink);
}

.tx-bal {
  font-size: 12px;
  color: var(--muted);
  margin-top: 3px;
}

.as-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.as-type {
  font-size: 13px;
  color: var(--ink-soft);
}

.as-mid {
  flex: 1;
  margin: 0 12px;
  font-size: 13px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.as-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 3px;
  font-size: 13px;
}

.rule-tip {
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 10px;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.rule-card:hover {
  border-color: var(--accent);
}

.rule-card.active {
  border-color: var(--accent);
  background: rgba(47, 107, 255, 0.06);
}

.rule-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.rule-amount {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.rule-get {
  font-size: 13px;
  color: var(--accent-deep);
}

.rule-gift {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: var(--accent);
  border-radius: 999px;
  padding: 2px 8px;
}

@media (max-width: 768px) {
  .balance-detail {
    flex-direction: column;
  }

  .stats .stat-num {
    font-size: 17px;
  }
}
</style>
