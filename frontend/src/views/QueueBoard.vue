<template>
  <div class="queue-page">
    <!-- 头部：核销取餐 -->
    <el-card shadow="never" class="verify-card">
      <div class="verify-head">
        <b>核销取餐</b>
        <span class="verify-tip">输入顾客出示的 6 位取餐码</span>
      </div>
      <div class="verify-body">
        <el-input
          v-model="pickupCode"
          class="code-input"
          placeholder="输入取餐码"
          maxlength="8"
          inputmode="numeric"
          clearable
          @input="onCodeInput"
          @keyup.enter="doVerify"
        />
        <el-button type="primary" round :loading="verifying" @click="doVerify">核销</el-button>
      </div>
    </el-card>

    <!-- 标品待取：标品无需制作，付款后即为待取餐 -->
    <el-card shadow="never" class="pickup-card">
      <div class="verify-head">
        <b>标品待取</b>
        <span class="verify-tip">标品付款后即待取，核销后再点完成</span>
        <el-tag v-if="standardOrders.length" size="small" type="warning" round class="pk-badge">
          {{ standardOrders.length }}
        </el-tag>
      </div>
      <el-empty
        v-if="!standardOrders.length"
        description="暂无待取的标品订单"
        :image-size="60"
      />
      <div v-else class="pickup-list">
        <div v-for="o in standardOrders" :key="o.id" class="pickup-row">
          <span class="pk-code">{{ o.pickupCode }}</span>
          <div class="pk-main">
            <div class="pk-no">{{ o.orderNo }}</div>
            <div class="pk-items">{{ itemsText(o) }}</div>
          </div>
          <span class="pk-amount">¥{{ fmtMoney(o.payAmount) }}</span>
          <el-button type="warning" size="small" round @click="finishStandard(o)">
            完成取餐
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 排队看板 -->
    <div class="board" v-loading="loading">
      <el-empty v-if="!loading && !queue.length" description="当前暂无排队订单" />

      <div v-for="q in queue" :key="q.id" class="q-card" :class="'s-' + q.status">
        <div class="q-head">
          <span class="q-no">#{{ q.queueNo }}</span>
          <el-tag size="small" :type="queueStatusInfo(q.status).type" round>
            {{ queueStatusInfo(q.status).text }}
          </el-tag>
          <span class="q-est" v-if="q.estimateMinutes">约 {{ q.estimateMinutes }} 分钟</span>
        </div>

        <div class="q-items">
          <div v-for="it in (q.order?.items || [])" :key="it.id" class="q-item">
            <span class="q-name">{{ it.productName }}</span>
            <span class="q-spec">{{ it.skuSpec }}{{ it.ingredients ? `（${it.ingredients}）` : '' }}</span>
            <span class="q-qty">x{{ it.quantity }}</span>
          </div>
        </div>

        <div class="q-foot">
          <span v-if="q.order?.remark" class="q-remark">备注：{{ q.order.remark }}</span>
          <span class="q-code">取餐码 <b>{{ q.order?.pickupCode || '—' }}</b></span>
        </div>

        <div class="q-actions">
          <el-button
            v-if="q.status === 1"
            type="primary"
            size="small"
            round
            @click="act(q, 'making')"
          >
            开始制作
          </el-button>
          <el-button
            v-if="q.status === 2"
            type="success"
            size="small"
            round
            @click="act(q, 'ready')"
          >
            出餐
          </el-button>
          <el-button
            v-if="q.status === 3"
            type="warning"
            size="small"
            round
            @click="act(q, 'complete')"
          >
            完成取餐
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getQueueList, getOrder, startMaking, finishMaking, completeOrder, verifyPickup } from '../api/order'
import { getStoreOrders } from '../api/admin'
import { useAppStore } from '../stores/app'
import { QUEUE_STATUS } from '../utils/dict'
import { fmtMoney } from '../utils/format'

const app = useAppStore()

const loading = ref(false)
const queue = ref([])
const standardOrders = ref([])
const pickupCode = ref('')
const verifying = ref(false)
let timer = null

const queueStatusInfo = (s) => QUEUE_STATUS[s] || { text: '未知', type: 'info' }

const itemsText = (o) =>
  (o.items || []).map((it) => `${it.productName} x${it.quantity}`).join('、')

const load = async () => {
  loading.value = true
  try {
    const [qs, stds] = await Promise.all([
      getQueueList(app.storeId).catch(() => []),
      // 标品：状态 4 = 待取餐（辅助查询，取不到不弹全局错误，避免干扰排队看板）
      getStoreOrders({ storeId: app.storeId, status: 4, orderType: 1 }, { silent: true }).catch(() => [])
    ])
    standardOrders.value = stds || []
    const enriched = await Promise.all(
      (qs || []).map(async (q) => {
        try {
          return { ...q, order: await getOrder(q.orderId) }
        } catch (e) {
          return q
        }
      })
    )
    queue.value = enriched
  } catch (e) {
    queue.value = []
    standardOrders.value = []
  } finally {
    loading.value = false
  }
}

// 标品取餐完成：待取餐 → 已完成（无排队记录，直接完结）
const finishStandard = async (o) => {
  try {
    await completeOrder(o.id)
    ElMessage.success(`已完成取餐：${o.orderNo}`)
    load()
  } catch (e) {
    /* 已提示 */
  }
}

const act = async (q, action) => {
  try {
    if (action === 'making') await startMaking(q.orderId)
    else if (action === 'ready') await finishMaking(q.orderId)
    else if (action === 'complete') await completeOrder(q.orderId)
    ElMessage.success('操作成功')
    load()
  } catch (e) {
    /* 已提示 */
  }
}

/** 只保留数字：避免输入法/扫码枪带进空格或字母，也顺手统一全角数字 */
const onCodeInput = (val) => {
  const digits = String(val || '')
    .replace(/[０-９]/g, (ch) => String.fromCharCode(ch.charCodeAt(0) - 0xfee0))
    .replace(/\D/g, '')
    .slice(0, 8)
  pickupCode.value = digits
}

const doVerify = async () => {
  const code = String(pickupCode.value || '').replace(/\D/g, '')
  if (!code) {
    ElMessage.warning('请输入取餐码')
    return
  }
  if (code.length < 4) {
    ElMessage.warning('取餐码至少 4 位，请核对顾客出示的号码')
    return
  }
  verifying.value = true
  try {
    const order = await verifyPickup(app.storeId, code)
    ElMessage.success(`核销成功：${order.orderNo}`)
    pickupCode.value = ''
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    verifying.value = false
  }
}

onMounted(() => {
  app.init()
  load()
  timer = setInterval(load, 5000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.queue-page {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.verify-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.verify-tip {
  font-size: 12px;
  color: var(--muted);
}

.verify-body {
  display: flex;
  gap: 12px;
}

.code-input {
  width: 220px;
}

/* 取餐码要一眼看清 6 位：放大字号、拉开字距、居中，避免窄屏下末位被挤住 */
.code-input :deep(.el-input__inner) {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* 标品待取 */
.pk-badge {
  margin-left: auto;
}

.pickup-list {
  display: flex;
  flex-direction: column;
}

.pickup-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 4px;
}

.pickup-row + .pickup-row {
  border-top: 1px dashed var(--line);
}

.pk-code {
  flex-shrink: 0;
  width: 58px;
  height: 40px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #fff;
  background: linear-gradient(135deg, var(--accent), var(--accent-deep));
}

.pk-main {
  flex: 1;
  min-width: 0;
}

.pk-no {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}

.pk-items {
  margin-top: 2px;
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pk-amount {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent-deep);
}

.board {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  min-height: 100px;
}

.q-card {
  padding: 16px;
  border-radius: 16px;
  background: var(--surface);
  border: 1px solid rgba(30, 79, 214, 0.08);
  box-shadow: var(--shadow-sm);
  border-left: 4px solid var(--accent);
}

.q-card.s-2 {
  border-left-color: #409eff;
}

.q-card.s-3 {
  border-left-color: var(--green);
}

.q-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.q-no {
  font-size: 20px;
  font-weight: 800;
  color: var(--ink);
}

.q-est {
  margin-left: auto;
  font-size: 12px;
  color: var(--muted);
}

.q-items {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.q-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.q-name {
  font-weight: 600;
  font-size: 14px;
}

.q-spec {
  font-size: 12px;
  color: var(--muted);
}

.q-qty {
  margin-left: auto;
  font-size: 13px;
  color: var(--ink-soft);
}

.q-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
  font-size: 12.5px;
  color: var(--ink-soft);
}

.q-remark {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.q-code b {
  font-size: 16px;
  letter-spacing: 2px;
  color: var(--accent-deep);
}

.q-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
