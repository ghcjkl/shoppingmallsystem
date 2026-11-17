<template>
  <div class="checkout-page">
    <el-empty v-if="!cart.items.length" description="没有可结算的商品">
      <el-button type="primary" round @click="$router.push('/home')">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <div class="wrap">
        <!-- 订单商品摘要 -->
        <el-card shadow="never" class="block">
          <template #header><b>商品清单</b></template>
          <div v-for="item in cart.items" :key="cart.keyOf(item)" class="order-item">
            <span class="oi-name">{{ item.productName }}</span>
            <span class="oi-spec">{{ specText(item) }}{{ ingredientText(item) }}</span>
            <span class="oi-qty">x{{ item.quantity }}</span>
            <span class="oi-amount">¥{{ fmtMoney(item.unitPrice * item.quantity) }}</span>
          </div>
          <div class="order-total">
            <span>合计</span>
            <span class="grand">¥{{ fmtMoney(cart.totalAmount) }}</span>
          </div>
        </el-card>

        <!-- 备注 -->
        <el-card shadow="never" class="block">
          <template #header><b>订单备注</b></template>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="如：少冰、不要吸管等（选填）"
          />
        </el-card>

        <!-- 优惠券 -->
        <el-card shadow="never" class="block">
          <template #header><b>优惠券</b></template>
          <el-select
            v-model="selectedCouponId"
            placeholder="选择优惠券"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="c in availableCoupons"
              :key="c.id"
              :label="couponLabel(c)"
              :value="c.id"
            />
          </el-select>
          <div class="coupon-tip" v-if="selectedCouponId">
            已优惠 ¥{{ fmtMoney(couponDiscount) }}
          </div>
          <div class="coupon-tip muted" v-else-if="!availableCoupons.length">
            暂无可用优惠券，可到会员中心领取
          </div>
        </el-card>

        <!-- 积分抵扣 -->
        <el-card shadow="never" class="block" v-if="maxUsablePoints > 0">
          <template #header><b>积分抵扣</b></template>
          <div class="points-line">
            <span>可用积分 {{ app.points || 0 }}，本单最多用 {{ maxUsablePoints }} 积分（抵 ¥{{ fmtMoney(maxUsablePoints / 100) }}）</span>
          </div>
          <el-input-number
            v-model="usePoints"
            :min="0"
            :max="maxUsablePoints"
            :step="100"
            size="small"
          />
          <div class="coupon-tip" v-if="usePoints > 0">
            已用 {{ usePoints }} 积分，抵扣 ¥{{ fmtMoney(pointsAmount) }}
          </div>
        </el-card>

        <!-- 支付方式 -->
        <el-card shadow="never" class="block">
          <template #header><b>支付方式</b></template>
          <el-radio-group v-model="payType" class="pay-types">
            <el-radio :value="1" border>线上支付</el-radio>
            <el-radio :value="2" border>储值支付</el-radio>
            <el-radio :value="3" border>储值 + 线上</el-radio>
          </el-radio-group>
          <div class="balance-line" v-if="balance">
            <span>当前储值余额：本金 ¥{{ fmtMoney(balance.principal) }} ＋ 赠送 ¥{{ fmtMoney(balance.gift) }}</span>
            <span v-if="payType === 2 && totalBalance < cart.totalAmount" class="warn">
              余额不足，将无法完成储值支付
            </span>
          </div>
        </el-card>

        <el-button
          type="primary"
          size="large"
          round
          class="submit-btn"
          :loading="submitting"
          @click="submit"
        >
          提交订单并支付 ¥{{ fmtMoney(finalPayable) }}
        </el-button>
      </div>
    </template>

    <!-- 支付结果弹窗 -->
    <el-dialog v-model="resultVisible" title="下单成功" width="420px">
      <div class="result-list">
        <div v-for="o in paidOrders" :key="o.id" class="result-item">
          <div class="ri-head">
            <el-tag :type="o.orderType === 2 ? 'warning' : 'success'" size="small" round>
              {{ o.orderType === 2 ? '现制饮品' : '标品零食' }}
            </el-tag>
            <span class="ri-no">{{ o.orderNo }}</span>
          </div>
          <div class="ri-amount">实付 ¥{{ fmtMoney(o.payAmount) }}</div>
          <div v-if="o.orderType === 2" class="ri-code">
            取餐码
            <span class="code-num">{{ o.pickupCode }}</span>
            <span class="ri-queue">排队号 {{ o.queueNo }}</span>
          </div>
          <div v-else class="ri-code">到店出示订单号即可取货</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="goOrders">查看订单</el-button>
        <el-button type="primary" @click="$router.push('/home')">继续购物</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder, payOrder } from '../api/order'
import { getBalance, getUserCoupons } from '../api/user'
import { useCartStore } from '../stores/cart'
import { useAppStore } from '../stores/app'
import { fmtMoney } from '../utils/format'

const router = useRouter()
const cart = useCartStore()
const app = useAppStore()

const remark = ref('')
const payType = ref(1)
const balance = ref(null)
const submitting = ref(false)
const resultVisible = ref(false)
const paidOrders = ref([])
const myCoupons = ref([])
const selectedCouponId = ref(null)
const usePoints = ref(0) // 本单使用的积分（100 的整数倍），只作用于金额最大的那一笔订单

const totalBalance = computed(() =>
  Number(balance.value?.principal || 0) + Number(balance.value?.gift || 0)
)

// 可用券：未使用的券，且范围匹配购物车里的商品类型
const availableCoupons = computed(() => {
  const types = new Set()
  if (cart.standardItems.length) types.add(1)
  if (cart.freshItems.length) types.add(2)
  return myCoupons.value.filter((c) => {
    if (c.status !== 0) return false
    if (c.scope && c.scope !== 0 && !types.has(c.scope)) return false
    // 门槛判断：至少有一笔订单金额满足
    const groups = [
      { type: 1, total: cart.standardItems.reduce((s, i) => s + i.unitPrice * i.quantity, 0) },
      { type: 2, total: cart.freshItems.reduce((s, i) => s + i.unitPrice * i.quantity, 0) }
    ].filter((g) => g.total > 0)
    return groups.some((g) => Number(g.total) >= Number(c.threshold || 0))
  })
})

// 当前选中券
const selectedCoupon = computed(() =>
  myCoupons.value.find((c) => c.id === selectedCouponId.value)
)

// 券优惠金额：按购物车总额计算（券最终作用于满足条件的订单）
const couponDiscount = computed(() => {
  const c = selectedCoupon.value
  if (!c) return 0
  if (c.type === 2) {
    // 折扣券
    return cart.totalAmount * (1 - Number(c.amount))
  }
  return Math.min(Number(c.amount), cart.totalAmount)
})

const payableAmount = computed(() =>
  Math.max(0, cart.totalAmount - couponDiscount.value)
)

// 积分抵扣：100 积分 = 1 元，最多抵券后金额的 20%，且不超过持有积分；取整到 100 的倍数
const maxUsablePoints = computed(() => {
  const byLimit = Math.floor(payableAmount.value * 0.2) * 100
  const byBalance = Math.floor(Number(app.points || 0) / 100) * 100
  return Math.max(0, Math.min(byLimit, byBalance))
})
const pointsAmount = computed(() => Number(usePoints.value || 0) / 100)
const finalPayable = computed(() => Math.max(0, payableAmount.value - pointsAmount.value))

const couponLabel = (c) => {
  if (c.type === 2) return `${c.name}（${Math.round(c.amount * 10)}折）`
  const th = Number(c.threshold) > 0 ? `满${c.threshold}减` : '立减'
  return `${c.name}（${th}${c.amount}）`
}

const ingredientText = (item) => {
  const names = (item.ingredients || []).map((i) => i.name)
  return names.length ? `（${names.join('/')}）` : ''
}

const specText = (item) => {
  const parts = [item.skuLabel]
  const opts = (item.specOptions || []).map((o) => o.name)
  if (opts.length) parts.push(opts.join('/'))
  return parts.join(' / ')
}

const toItemRequest = (item) => ({
  productId: item.productId,
  skuId: item.skuId,
  quantity: item.quantity,
  ingredientIds: (item.ingredients || []).map((i) => i.id),
  specOptionIds: (item.specOptions || []).map((o) => o.id)
})

const submit = async () => {
  submitting.value = true
  try {
    const groups = [
      { orderType: 1, items: cart.standardItems.map(toItemRequest) },
      { orderType: 2, items: cart.freshItems.map(toItemRequest) }
    ].filter((g) => g.items.length)

    if (!groups.length) {
      ElMessage.warning('购物车为空')
      return
    }

    // 积分只能作用于单笔订单：分配给商品金额最大的那笔，且按该单金额重算上限
    const groupAmounts = groups.map((g) =>
      g.items.reduce((s, i) => s + Number(i.unitPrice || 0) * Number(i.quantity || 0), 0))
    const pointsTarget = usePoints.value > 0
      ? groupAmounts.indexOf(Math.max(...groupAmounts))
      : -1

    // 按类型分别下单（后端单订单仅支持一种类型）
    const created = []
    for (let gi = 0; gi < groups.length; gi++) {
      const g = groups[gi]
      // 券只绑定到范围匹配的那一单
      const useCoupon = selectedCoupon.value
        && (!selectedCoupon.value.scope || selectedCoupon.value.scope === 0
          || selectedCoupon.value.scope === g.orderType)
      const order = await createOrder({
        userId: app.userId,
        storeId: app.storeId,
        orderType: g.orderType,
        remark: remark.value || undefined,
        userCouponId: useCoupon ? selectedCouponId.value : undefined,
        usePoints: gi === pointsTarget ? usePoints.value : undefined,
        items: g.items
      })
      created.push(order)
    }

    // 逐笔支付
    const paid = []
    for (const order of created) {
      paid.push(await payOrder({ orderId: order.id, payType: payType.value }))
    }

    paidOrders.value = paid
    cart.clear()
    resultVisible.value = true
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    submitting.value = false
  }
}

const goOrders = () => {
  resultVisible.value = false
  router.push('/orders')
}

onMounted(async () => {
  // 刷新登录用户（含最新积分），否则 maxUsablePoints 用的是旧值
  await app.loadUser()
  try {
    balance.value = await getBalance(app.userId)
  } catch (e) {
    balance.value = null
  }
  try {
    myCoupons.value = (await getUserCoupons(app.userId)) || []
  } catch (e) {
    myCoupons.value = []
  }
})
</script>

<style scoped>
.checkout-page {
  max-width: 760px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  font-size: 14px;
}

.oi-name {
  font-weight: 600;
}

.oi-spec {
  flex: 1;
  color: var(--muted);
  font-size: 12.5px;
}

.oi-qty {
  color: var(--ink-soft);
}

.oi-amount {
  min-width: 70px;
  text-align: right;
  font-weight: 600;
}

.order-total {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}

.grand {
  font-size: 22px;
  font-weight: 800;
  color: var(--accent-deep);
}

.pay-types {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pay-types .el-radio {
  margin-right: 0;
}

.points-line {
  font-size: 13px;
  color: var(--ink-soft);
  margin-bottom: 10px;
}

.balance-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 12px;
  font-size: 12.5px;
  color: var(--muted);
}

.coupon-tip {
  margin-top: 10px;
  font-size: 13px;
  color: var(--accent-deep);
  font-weight: 600;
}

.coupon-tip.muted {
  color: var(--muted);
  font-weight: 400;
}

.warn {
  color: var(--red);
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result-item {
  padding: 14px;
  border-radius: 14px;
  background: #f4f7ff;
}

.ri-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ri-no {
  font-size: 12.5px;
  color: var(--muted);
}

.ri-amount {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 800;
  color: var(--accent-deep);
}

.ri-code {
  margin-top: 8px;
  font-size: 13px;
  color: var(--ink-soft);
}

.code-num {
  display: inline-block;
  margin: 0 8px;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: 4px;
  color: var(--accent-deep);
}

.ri-queue {
  font-size: 12.5px;
  color: var(--muted);
}
</style>
