<template>
  <div class="order-list-page" v-loading="loading">
    <el-tabs v-model="activeTab" @tab-change="filter">
      <el-tab-pane label="全部" name="0" />
      <el-tab-pane label="待付款" name="1" />
      <el-tab-pane label="进行中" name="2" />
      <el-tab-pane label="待取餐" name="4" />
      <el-tab-pane label="已完成" name="5" />
      <el-tab-pane label="已取消" name="6" />
    </el-tabs>

    <el-empty v-if="!loading && !list.length" description="暂无订单" />

    <div class="list">
      <el-card v-for="o in list" :key="o.id" shadow="never" class="order-card">
        <div class="oc-head">
          <div class="oc-left">
            <el-tag size="small" :type="o.orderType === 2 ? 'warning' : 'success'" round>
              {{ o.orderType === 2 ? '现制' : '标品' }}
            </el-tag>
            <span class="oc-no">{{ o.orderNo }}</span>
          </div>
          <el-tag size="small" :type="statusInfo(o.status).type" round>
            {{ statusInfo(o.status).text }}
          </el-tag>
        </div>

        <div class="oc-items">
          <div v-for="it in o.items" :key="it.id" class="oc-item">
            <span class="oc-name">{{ it.productName }}</span>
            <span class="oc-spec">{{ it.skuSpec }}{{ it.ingredients ? `（${it.ingredients}）` : '' }}</span>
            <span class="oc-qty">x{{ it.quantity }}</span>
          </div>
        </div>

        <div v-if="o.pickupCode" class="oc-code">
          取餐码 <b>{{ o.pickupCode }}</b>
          <span v-if="o.queueNo">· 排队号 {{ o.queueNo }}</span>
        </div>

        <div class="oc-foot">
          <span class="oc-time">{{ fmtTime(o.createTime) }}</span>
          <span class="oc-amount">实付 ¥{{ fmtMoney(o.payAmount) }}</span>
        </div>

        <div class="oc-actions">
          <template v-if="o.status === 1">
            <el-button size="small" @click="openPay(o)">去支付</el-button>
            <el-button size="small" @click="doCancel(o)">取消订单</el-button>
          </template>
          <template v-else-if="o.status === 2">
            <el-button size="small" @click="doCancel(o)">取消订单</el-button>
          </template>
          <template v-else-if="o.status === 4 && o.orderType === 1">
            <el-button size="small" @click="doCancel(o)">取消订单</el-button>
          </template>
          <el-button size="small" type="primary" plain @click="goDetail(o.id)">查看详情</el-button>
        </div>
      </el-card>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="payVisible" title="支付订单" width="380px">
      <div class="pay-amount">需支付 ¥{{ fmtMoney(currentOrder?.payAmount) }}</div>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrdersByUser, payOrder, cancelOrder } from '../api/order'
import { useAppStore } from '../stores/app'
import { ORDER_STATUS } from '../utils/dict'
import { fmtMoney, fmtTime } from '../utils/format'

const router = useRouter()
const app = useAppStore()

const loading = ref(false)
const activeTab = ref('0')
const all = ref([])
const list = ref([])
const payVisible = ref(false)
const paying = ref(false)
const currentOrder = ref(null)
const payType = ref(1)

const statusInfo = (s) => ORDER_STATUS[s] || { text: '未知', type: 'info' }

const load = async () => {
  loading.value = true
  try {
    all.value = (await getOrdersByUser(app.userId)) || []
    filter()
  } catch (e) {
    all.value = []
    list.value = []
  } finally {
    loading.value = false
  }
}

const filter = () => {
  const t = Number(activeTab.value)
  if (t === 0) list.value = all.value
  else if (t === 2) list.value = all.value.filter((o) => [2, 3].includes(o.status))
  else list.value = all.value.filter((o) => o.status === t)
}

const goDetail = (id) => router.push(`/order/${id}`)

const openPay = (o) => {
  currentOrder.value = o
  payType.value = 1
  payVisible.value = true
}

const doPay = async () => {
  paying.value = true
  try {
    await payOrder({ orderId: currentOrder.value.id, payType: payType.value })
    ElMessage.success('支付成功')
    payVisible.value = false
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    paying.value = false
  }
}

const doCancel = (o) => {
  ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    .then(async () => {
      try {
        await cancelOrder(o.id, app.userId)
        ElMessage.success('已取消')
        load()
      } catch (e) {
        /* 已提示 */
      }
    })
    .catch(() => {})
}

onMounted(load)
</script>

<style scoped>
.order-list-page {
  max-width: 860px;
  margin: 0 auto;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  margin-bottom: 0;
}

.oc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.oc-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.oc-no {
  font-size: 13px;
  color: var(--muted);
}

.oc-items {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.oc-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.oc-name {
  font-weight: 600;
  font-size: 14px;
}

.oc-spec {
  color: var(--muted);
  font-size: 12.5px;
}

.oc-qty {
  margin-left: auto;
  color: var(--ink-soft);
  font-size: 13px;
}

.oc-code {
  margin-top: 12px;
  font-size: 13px;
  color: var(--ink-soft);
}

.oc-code b {
  font-size: 20px;
  letter-spacing: 3px;
  color: var(--accent-deep);
}

.oc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--line);
}

.oc-time {
  font-size: 12px;
  color: var(--muted);
}

.oc-amount {
  font-size: 16px;
  font-weight: 700;
  color: var(--accent-deep);
}

.oc-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
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
