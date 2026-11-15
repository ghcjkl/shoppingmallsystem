<template>
  <div class="order-detail-page" v-loading="loading">
    <div v-if="order" class="wrap">
      <button class="back" @click="$router.back()">← 返回</button>

      <!-- 状态卡 -->
      <el-card shadow="never" class="status-card">
        <div class="status-head">
          <el-tag size="small" :type="statusInfo(order.status).type" round>
            {{ statusInfo(order.status).text }}
          </el-tag>
          <span class="status-tip">{{ statusTip }}</span>
        </div>

        <!-- 取餐码（支付后生成，标品与现制通用） -->
        <div v-if="order.pickupCode" class="pickup">
          <div class="pickup-label">取餐码</div>
          <div class="pickup-code">{{ order.pickupCode }}</div>
          <div class="pickup-queue" v-if="order.queueNo">排队号 {{ order.queueNo }}</div>
        </div>
        <div v-else class="pickup muted">支付后生成取餐码</div>

        <!-- 现制排队进度 -->
        <div v-if="order.orderType === 2 && order.queue" class="queue-box">
          <div class="q-title">排队进度</div>
          <div class="q-line">
            <span class="q-no">#{{ order.queue.queueNo }}</span>
            <el-tag size="small" :type="queueStatusInfo(order.queue.status).type" round>
              {{ queueStatusInfo(order.queue.status).text }}
            </el-tag>
            <span class="q-est" v-if="order.queue.estimateMinutes">
              预计约 {{ order.queue.estimateMinutes }} 分钟
            </span>
          </div>
        </div>
      </el-card>

      <!-- 商品明细 -->
      <el-card shadow="never" class="block">
        <template #header><b>商品明细</b></template>
        <div v-for="it in order.items" :key="it.id" class="item">
          <div class="item-main">
            <span class="item-name">{{ it.productName }}</span>
            <span class="item-spec">{{ it.skuSpec }}{{ it.ingredients ? `（${it.ingredients}）` : '' }}</span>
            <span v-if="reviewMap[it.id]" class="item-reviewed">
              已评价 <i class="stars">{{ starText(reviewMap[it.id].rating) }}</i>
            </span>
          </div>
          <span class="item-qty">x{{ it.quantity }}</span>
          <span class="item-amount">¥{{ fmtMoney(it.amount) }}</span>
          <el-button
            v-if="order.status === 5 && !reviewMap[it.id]"
            size="small"
            text
            type="primary"
            @click="openReview(it)"
          >
            评价
          </el-button>
        </div>
      </el-card>

      <!-- 金额与信息 -->
      <el-card shadow="never" class="block">
        <template #header><b>订单信息</b></template>
        <div class="info-row"><span>订单编号</span><span>{{ order.orderNo }}</span></div>
        <div class="info-row"><span>支付方式</span><span>{{ payTypeText }}</span></div>
        <div class="info-row"><span>商品金额</span><span>¥{{ fmtMoney(order.totalAmount) }}</span></div>
        <div class="info-row" v-if="order.balancePaid > 0">
          <span>储值支付</span><span>¥{{ fmtMoney(order.balancePaid) }}</span>
        </div>
        <div class="info-row" v-if="order.cashPaid > 0">
          <span>线上支付</span><span>¥{{ fmtMoney(order.cashPaid) }}</span>
        </div>
        <div class="info-row total"><span>实付</span><span>¥{{ fmtMoney(order.payAmount) }}</span></div>
        <div class="info-row"><span>下单时间</span><span>{{ fmtTime(order.createTime) }}</span></div>
        <div class="info-row" v-if="order.payTime"><span>支付时间</span><span>{{ fmtTime(order.payTime) }}</span></div>
        <div class="info-row" v-if="order.finishTime"><span>完成时间</span><span>{{ fmtTime(order.finishTime) }}</span></div>
        <div class="info-row" v-if="order.remark"><span>备注</span><span>{{ order.remark }}</span></div>
      </el-card>

      <!-- 操作 -->
      <div class="actions">
        <template v-if="order.status === 1">
          <el-button type="primary" round @click="openPay">去支付</el-button>
          <el-button round @click="doCancel">取消订单</el-button>
        </template>
        <template v-else-if="order.status === 2">
          <el-button round @click="doCancel">取消订单</el-button>
        </template>
        <template v-else-if="order.status === 4 && order.orderType === 1">
          <el-button round @click="doCancel">取消订单</el-button>
        </template>
        <el-button v-if="canAfterSale" type="warning" plain round @click="openAfterSale">
          申请售后
        </el-button>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="payVisible" title="支付订单" width="380px">
      <div class="pay-amount">需支付 ¥{{ fmtMoney(order?.payAmount) }}</div>
      <el-radio-group v-model="payType" class="pay-types">
        <el-radio :value="1" border>线上支付</el-radio>
        <el-radio :value="2" border>储值支付</el-radio>
        <el-radio :value="3" border>储值 + 线上</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="doPay">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- 售后弹窗 -->
    <el-dialog v-model="afterSaleVisible" title="申请售后" width="420px">
      <el-form label-position="top">
        <el-form-item label="售后类型">
          <el-select v-model="afterSaleForm.type" style="width: 100%">
            <el-option label="退款" :value="1" />
            <el-option label="退货退款" :value="2" />
            <el-option label="补发" :value="3" />
            <el-option label="补偿" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="afterSaleForm.reason" type="textarea" :rows="3" placeholder="请描述原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="afterSaleVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="doAfterSale">提交</el-button>
      </template>
    </el-dialog>
    <!-- 评价弹窗 -->
    <el-dialog v-model="reviewVisible" title="评价商品" width="420px">
      <div class="rv-target">{{ reviewTarget?.productName }}</div>
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" show-text :texts="RATE_TEXTS" size="large" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="口感、包装、出餐速度……说说你的感受"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="reviewForm.anonymous">匿名评价（不显示昵称）</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, payOrder, cancelOrder } from '../api/order'
import { applyAfterSale } from '../api/aftersale'
import { createReview, getOrderReviews } from '../api/review'
import { readOrderNotice } from '../api/notice'
import { useNoticeStore } from '../stores/notice'
import { useAppStore } from '../stores/app'
import { ORDER_STATUS, QUEUE_STATUS, PAY_TYPE } from '../utils/dict'
import { fmtMoney, fmtTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const notice = useNoticeStore()

const loading = ref(false)
const order = ref(null)
const payVisible = ref(false)
const paying = ref(false)
const payType = ref(1)
const afterSaleVisible = ref(false)
const applying = ref(false)
const afterSaleForm = ref({ type: 1, reason: '' })

const statusInfo = (s) => ORDER_STATUS[s] || { text: '未知', type: 'info' }
const queueStatusInfo = (s) => QUEUE_STATUS[s] || { text: '未知', type: 'info' }
const payTypeText = computed(() => PAY_TYPE[order.value?.payType] || '—')

const statusTip = computed(() => {
  const s = order.value?.status
  const standard = order.value?.orderType === 1
  if (s === 1) return '请尽快完成支付'
  if (s === 2 && standard) return '已支付，等待门店备货'
  if (s === 2) return '已支付，等待门店开始制作'
  if (s === 3) return '门店正在制作中'
  if (s === 4 && standard) return '已备货，请到店出示取餐码取货'
  if (s === 4) return '制作完成，请到店出示取餐码'
  if (s === 5) return '订单已完成，感谢惠顾'
  if (s === 6) return '订单已取消'
  return ''
})

const canAfterSale = computed(() => [2, 3, 4, 5].includes(order.value?.status))

// ===== 商品评价 =====
const RATE_TEXTS = ['很差', '一般', '还行', '不错', '超赞']
const reviewMap = ref({})
const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref(null)
const reviewForm = ref({ rating: 5, content: '', anonymous: false })

const starText = (score) => {
  const n = Math.max(0, Math.min(5, Math.round(Number(score) || 0)))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

const loadReviews = async () => {
  try {
    const list = await getOrderReviews(route.params.id)
    const map = {}
    ;(list || []).forEach((r) => {
      map[r.orderItemId] = r
    })
    reviewMap.value = map
  } catch (e) {
    reviewMap.value = {}
  }
}

const openReview = (item) => {
  reviewTarget.value = item
  reviewForm.value = { rating: 5, content: '', anonymous: false }
  reviewVisible.value = true
}

const submitReview = async () => {
  if (!reviewForm.value.rating) {
    return ElMessage.warning('请先选择评分')
  }
  reviewSubmitting.value = true
  try {
    await createReview({
      orderItemId: reviewTarget.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content || null,
      anonymous: reviewForm.value.anonymous
    })
    ElMessage.success('评价成功，感谢你的反馈')
    reviewVisible.value = false
    await loadReviews()
  } catch (e) {
    /* 已提示 */
  } finally {
    reviewSubmitting.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    order.value = await getOrder(route.params.id)
    loadReviews()
    // 看过订单详情就把该订单的取餐提醒标记已读，红点随之消失
    readOrderNotice(route.params.id)
      .then(() => notice.loadUnread())
      .catch(() => {})
  } catch (e) {
    // 订单不存在或不属于当前用户（后端归属校验）：回订单列表，不停在空白页
    router.replace('/orders')
  } finally {
    loading.value = false
  }
}

const openPay = () => {
  payType.value = 1
  payVisible.value = true
}

const doPay = async () => {
  paying.value = true
  try {
    await payOrder({ orderId: order.value.id, payType: payType.value })
    ElMessage.success('支付成功')
    payVisible.value = false
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    paying.value = false
  }
}

const doCancel = () => {
  ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await cancelOrder(order.value.id, app.userId)
        ElMessage.success('已取消')
        load()
      } catch (e) {
        /* 已提示 */
      }
    })
    .catch(() => {})
}

const openAfterSale = () => {
  afterSaleForm.value = { type: 1, reason: '' }
  afterSaleVisible.value = true
}

const doAfterSale = async () => {
  applying.value = true
  try {
    await applyAfterSale({
      orderId: order.value.id,
      orderItemId: order.value.items?.[0]?.id ?? null,
      userId: app.userId,
      type: afterSaleForm.value.type,
      reason: afterSaleForm.value.reason || undefined,
      imageUrls: '[]'
    })
    ElMessage.success('售后申请已提交')
    afterSaleVisible.value = false
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    applying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail-page {
  max-width: 760px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.back {
  align-self: flex-start;
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.status-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tip {
  font-size: 13px;
  color: var(--muted);
}

.pickup {
  margin-top: 20px;
  text-align: center;
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(135deg, #eef3ff, #dce7ff);
}

.pickup.muted {
  color: var(--muted);
  background: #f4f7ff;
}

.pickup-label {
  font-size: 13px;
  color: var(--ink-soft);
}

.pickup-code {
  margin: 6px 0;
  font-size: 44px;
  font-weight: 900;
  letter-spacing: 8px;
  color: var(--accent-deep);
}

.pickup-queue {
  font-size: 13px;
  color: var(--muted);
}

.queue-box {
  margin-top: 16px;
  padding: 14px;
  border-radius: 14px;
  background: #f4f7ff;
}

.q-title {
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 8px;
}

.q-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.q-no {
  font-size: 18px;
  font-weight: 800;
  color: var(--ink);
}

.q-est {
  font-size: 12.5px;
  color: var(--muted);
}

.item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.item-name {
  font-weight: 600;
  font-size: 14px;
}

.item-spec {
  font-size: 12.5px;
  color: var(--muted);
}

.item-qty {
  color: var(--ink-soft);
}

.item-amount {
  min-width: 70px;
  text-align: right;
  font-weight: 600;
}

.item-reviewed {
  font-size: 12px;
  color: var(--muted);
}

.item-reviewed .stars {
  color: #f0a020;
  font-style: normal;
  letter-spacing: 0;
}

.rv-target {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 13.5px;
  color: var(--ink-soft);
}

.info-row.total {
  font-weight: 700;
  color: var(--ink);
}

.info-row.total span:last-child {
  color: var(--accent-deep);
  font-size: 16px;
}

.actions {
  display: flex;
  gap: 10px;
}

.pay-amount {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  color: var(--accent-deep);
}

.pay-types {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pay-types .el-radio {
  margin-right: 0;
}
</style>
