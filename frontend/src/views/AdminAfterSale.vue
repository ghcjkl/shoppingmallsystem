<template>
  <div class="admin-aftersale">
    <!-- 状态筛选 -->
    <div class="toolbar">
      <el-radio-group v-model="statusFilter" @change="load">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待处理</el-radio-button>
        <el-radio-button :value="1">处理中</el-radio-button>
        <el-radio-button :value="2">已完成</el-radio-button>
        <el-radio-button :value="3">已拒绝</el-radio-button>
      </el-radio-group>
      <span class="tip">共 {{ list.length }} 单</span>
    </div>

    <!-- 售后单列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="工单号" width="90" />
        <el-table-column prop="orderId" label="订单号" width="90" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ AFTER_SALE_TYPE[row.type] || '—' }}</template>
        </el-table-column>
        <el-table-column label="原因" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || '—' }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="110">
          <template #default="{ row }">
            {{ row.refundAmount != null ? '¥' + fmtMoney(row.refundAmount) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusInfo(row.status).type" round>
              {{ statusInfo(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 1"
              size="small"
              type="primary"
              @click="openProcess(row)"
            >
              处理
            </el-button>
            <span v-else class="muted">已结束</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无售后工单" :image-size="80" />
    </el-card>

    <!-- 处理弹层 -->
    <el-dialog
      v-model="processVisible"
      title="处理售后"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="as-detail">
        <div class="d-row"><span>工单号</span><b>#{{ current?.id }}</b></div>
        <div class="d-row"><span>售后类型</span><b>{{ AFTER_SALE_TYPE[current?.type] || '—' }}</b></div>
        <div class="d-row"><span>申请原因</span><b>{{ current?.reason || '—' }}</b></div>
        <div class="d-row"><span>订单号</span><b>{{ current?.orderId }}</b></div>
      </div>

      <el-divider content-position="left">处理结果</el-divider>
      <el-radio-group v-model="processForm.status" class="proc-opts">
        <el-radio :value="2" border>同意并退款</el-radio>
        <el-radio :value="3" border>拒绝</el-radio>
      </el-radio-group>

      <el-form-item v-if="processForm.status === 2" label="退款金额" style="margin-top: 14px">
        <div class="amount-box">
          <el-input-number
            v-model="processForm.refundAmount"
            :min="0"
            :max="orderPaid || 100000"
            :precision="2"
            style="width: 180px"
          />
          <div class="hint">
            <template v-if="orderPaid > 0">
              订单实付 ¥{{ fmtMoney(orderPaid) }}，退款不得超过该金额（已退部分会累计计算，后端二次校验）
            </template>
            <template v-else>按退款金额同步扣回该用户已获得的积分与成长值</template>
          </div>
        </div>
      </el-form-item>

      <el-form-item v-if="processForm.status === 2 && restockable" label="退回库存">
        <div class="restock">
          <el-checkbox v-model="processForm.restock">商品退回，库存同步回滚</el-checkbox>
          <div class="hint">
            按售后数量把库存加回仓库（现制商品同时回补原料），并写入「出入库记录」
          </div>
        </div>
      </el-form-item>

      <el-form-item label="处理说明" style="margin-top: 8px">
        <el-input v-model="processForm.result" type="textarea" :rows="2" placeholder="选填" />
      </el-form-item>

      <template #footer>
        <el-button @click="processVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitProcess">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminAfterSales, processAfterSale } from '../api/admin'
import { getOrder } from '../api/order'
import { fmtMoney, fmtTime } from '../utils/format'
import { AFTER_SALE_STATUS, AFTER_SALE_TYPE } from '../utils/dict'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const statusFilter = ref(null)

const processVisible = ref(false)
const current = ref(null)
/** 该工单对应订单的实付金额，用于给退款输入框设上限并给出提示 */
const orderPaid = ref(0)
const processForm = reactive({ status: 2, refundAmount: 0, result: '', restock: true })

// 只有「退款 / 退货退款」涉及库存，补发、补偿不动库存
const restockable = computed(() => [1, 2].includes(current.value?.type))

const statusInfo = (s) => AFTER_SALE_STATUS[s] || { text: '未知', type: 'info' }

const load = async () => {
  loading.value = true
  try {
    list.value = (await getAdminAfterSales(statusFilter.value)) || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const openProcess = (row) => {
  current.value = row
  processForm.status = 2
  processForm.refundAmount = row.refundAmount != null ? Number(row.refundAmount) : 0
  processForm.result = ''
  processForm.restock = true
  processVisible.value = true
  // 取订单实付作为退款上限参考；失败静默，不阻塞处理
  orderPaid.value = 0
  if (row.orderId) {
    getOrder(row.orderId)
      .then((o) => {
        orderPaid.value = Number(o?.payAmount || 0)
      })
      .catch(() => {})
  }
}

const submitProcess = async () => {
  if (!processForm.status) {
    ElMessage.warning('请选择处理结果')
    return
  }
  const willRestock =
    processForm.status === 2 && restockable.value && processForm.restock
  saving.value = true
  try {
    await processAfterSale({
      id: current.value.id,
      status: processForm.status,
      refundAmount: processForm.status === 2 ? processForm.refundAmount : null,
      restock: processForm.status === 2 ? processForm.restock : null,
      result: processForm.result || undefined
    })
    ElMessage.success(willRestock ? '处理成功，库存已回滚' : '处理成功')
    processVisible.value = false
    load()
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-aftersale {
  max-width: 1100px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tip {
  font-size: 13px;
  color: var(--muted);
}

.muted {
  color: var(--muted);
  font-size: 13px;
}

.as-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 6px 2px;
}

.d-row {
  display: flex;
  gap: 12px;
  font-size: 14px;
}

.d-row span {
  width: 80px;
  color: var(--muted);
  flex-shrink: 0;
}

.d-row b {
  font-weight: 600;
  color: var(--ink);
}

.proc-opts {
  display: flex;
  gap: 12px;
}

.proc-opts .el-radio {
  margin-right: 0;
}

.restock {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.amount-box {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hint {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.5;
}
</style>
