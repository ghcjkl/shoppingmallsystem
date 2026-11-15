<template>
  <div class="admin-balance">
    <!-- 搜索 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索手机号 / 昵称 / 用户名"
        clearable
        style="width: 260px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button @click="search">搜索</el-button>
        </template>
      </el-input>
      <span class="tip">留空显示最近 20 位用户</span>
      <div class="spacer" />
      <el-button type="primary" plain @click="openRules">充值档位设置</el-button>
    </div>

    <!-- 用户列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="userId" label="ID" width="70" />
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="本金" width="110">
          <template #default="{ row }">¥{{ fmtMoney(row.principal) }}</template>
        </el-table-column>
        <el-table-column label="赠送" width="110">
          <template #default="{ row }">¥{{ fmtMoney(row.gift) }}</template>
        </el-table-column>
        <el-table-column label="合计" width="120">
          <template #default="{ row }">
            <b class="total">¥{{ fmtMoney(row.total) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="累计充值" width="120">
          <template #default="{ row }">¥{{ fmtMoney(row.totalRecharge) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 0 ? 'danger' : 'success'" round>
              {{ row.status === 0 ? '禁用' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">修改金额</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 修改金额弹层 -->
    <el-dialog
      v-model="editVisible"
      :title="`修改储值金额 · ${current?.nickname || current?.phone || ('用户 ' + current?.userId)}`"
      width="560px"
      :close-on-click-modal="false"
    >
      <div class="balance-summary">
        <div class="b-item"><span>本金</span><b>¥{{ fmtMoney(current?.principal) }}</b></div>
        <div class="b-item"><span>赠送</span><b>¥{{ fmtMoney(current?.gift) }}</b></div>
        <div class="b-item"><span>合计</span><b class="total">¥{{ fmtMoney(current?.total) }}</b></div>
      </div>

      <el-divider content-position="left">增减金额</el-divider>
      <div class="adjust-row">
        <el-input-number v-model="adjustForm.principalDelta" :precision="2" style="width: 150px" />
        <span class="lbl">本金变动</span>
        <el-input-number v-model="adjustForm.giftDelta" :precision="2" style="width: 150px" />
        <span class="lbl">赠送变动</span>
        <el-button type="primary" :loading="saving" @click="submitAdjust">保存增减</el-button>
      </div>
      <p class="tip">正数增加、负数扣减；扣成负数会被拒绝</p>

      <el-divider content-position="left">直接设定</el-divider>
      <div class="adjust-row">
        <el-input-number v-model="setForm.principal" :min="0" :precision="2" style="width: 150px" />
        <span class="lbl">本金余额</span>
        <el-input-number v-model="setForm.gift" :min="0" :precision="2" style="width: 150px" />
        <span class="lbl">赠送余额</span>
        <el-button :loading="saving" @click="submitSet">保存设定</el-button>
      </div>
      <p class="tip">按差额写人工调整流水</p>

      <el-form-item label="备注" style="margin-top: 8px">
        <el-input v-model="remark" placeholder="调整原因，会写入流水" />
      </el-form-item>

      <el-divider content-position="left">最近流水</el-divider>
      <el-table v-loading="txLoading" :data="transactions" size="small" max-height="240" stripe>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ TX_TYPE[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="本金变动" width="100">
          <template #default="{ row }">{{ fmtMoney(row.principalAmount) }}</template>
        </el-table-column>
        <el-table-column label="赠送变动" width="100">
          <template #default="{ row }">{{ fmtMoney(row.giftAmount) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="editVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 充值档位管理：前台只展示「充 N 到账 N+M」，赠送额由这里配置 -->
    <el-dialog v-model="rulesVisible" title="充值档位设置" width="740px">
      <div class="rule-head">
        <span class="tip">前台只能选启用的档位，赠送金额由后端按档位结算，客户端无法指定</span>
        <el-button size="small" type="primary" @click="openRuleForm()">＋ 新增档位</el-button>
      </div>
      <el-table v-loading="ruleLoading" :data="rules" size="small" stripe>
        <el-table-column label="充值金额" width="100">
          <template #default="{ row }">¥{{ fmtMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="赠送金额" width="100">
          <template #default="{ row }">¥{{ fmtMoney(row.gift) }}</template>
        </el-table-column>
        <el-table-column label="到账金额" width="110">
          <template #default="{ row }">
            <b class="total">¥{{ fmtMoney(row.total) }}</b>
          </template>
        </el-table-column>
        <el-table-column label="前台展示" min-width="170">
          <template #default="{ row }">
            <span class="rule-preview-text">充 ¥{{ fmtMoney(row.amount) }} 到账 ¥{{ fmtMoney(row.total) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="60" prop="sort" align="center" />
        <el-table-column label="启用" width="64" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              size="small"
              @change="(v) => toggleRule(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openRuleForm(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="tip">还没有档位。未配置时前台可自定义金额，且无赠送。</div>
        </template>
      </el-table>
      <template #footer>
        <el-button @click="rulesVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑档位 -->
    <el-dialog
      v-model="ruleFormVisible"
      :title="ruleForm.id ? '编辑充值档位' : '新增充值档位'"
      width="380px"
      append-to-body
    >
      <el-form label-position="top">
        <el-form-item label="充值金额（元）">
          <el-input-number v-model="ruleForm.amount" :min="0.01" :max="100000" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赠送金额（元）">
          <el-input-number v-model="ruleForm.gift" :min="0" :max="100000" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序（越小越靠前）">
          <el-input-number v-model="ruleForm.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <div class="rule-preview">
        前台展示：充 ¥{{ fmtMoney(ruleForm.amount) }} 到账 ¥{{ fmtMoney((Number(ruleForm.amount) || 0) + (Number(ruleForm.gift) || 0)) }}
      </div>
      <template #footer>
        <el-button @click="ruleFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSaving" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  searchBalanceUsers, getAdminBalance, getAdminTransactions, adjustBalance, setBalance,
  getRechargeRules, createRechargeRule, updateRechargeRule, updateRechargeRuleStatus, deleteRechargeRule
} from '../api/admin'
import { fmtMoney, fmtTime } from '../utils/format'
import { TX_TYPE } from '../utils/dict'

const loading = ref(false)
const saving = ref(false)
const txLoading = ref(false)
const keyword = ref('')
const users = ref([])

const editVisible = ref(false)
const current = ref(null)
const transactions = ref([])
const remark = ref('')
const adjustForm = reactive({ principalDelta: 0, giftDelta: 0 })
const setForm = reactive({ principal: 0, gift: 0 })

async function search() {
  loading.value = true
  try {
    users.value = await searchBalanceUsers(keyword.value.trim() || undefined)
  } catch (e) {
    users.value = []
  } finally {
    loading.value = false
  }
}

async function openEdit(row) {
  current.value = row
  remark.value = ''
  adjustForm.principalDelta = 0
  adjustForm.giftDelta = 0
  editVisible.value = true
  await Promise.all([loadAccount(), loadTransactions()])
}

async function loadAccount() {
  try {
    current.value = await getAdminBalance(current.value.userId)
    setForm.principal = current.value.principal
    setForm.gift = current.value.gift
  } catch (e) {
    /* 已提示 */
  }
}

async function loadTransactions() {
  txLoading.value = true
  try {
    const txs = await getAdminTransactions(current.value.userId)
    transactions.value = txs.slice(0, 10)
  } catch (e) {
    transactions.value = []
  } finally {
    txLoading.value = false
  }
}

async function submitAdjust() {
  if (!adjustForm.principalDelta && !adjustForm.giftDelta) {
    return ElMessage.warning('请输入变动金额')
  }
  saving.value = true
  try {
    await adjustBalance(current.value.userId, {
      principalDelta: adjustForm.principalDelta,
      giftDelta: adjustForm.giftDelta,
      remark: remark.value || null
    })
    ElMessage.success('已调整')
    remark.value = ''
    adjustForm.principalDelta = 0
    adjustForm.giftDelta = 0
    await Promise.all([loadAccount(), loadTransactions(), search()])
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function submitSet() {
  saving.value = true
  try {
    await setBalance(current.value.userId, {
      principal: setForm.principal,
      gift: setForm.gift,
      remark: remark.value || null
    })
    ElMessage.success('已设定')
    remark.value = ''
    await Promise.all([loadAccount(), loadTransactions(), search()])
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(search)

// ===== 充值档位 =====
const rulesVisible = ref(false)
const ruleLoading = ref(false)
const ruleSaving = ref(false)
const rules = ref([])
const ruleFormVisible = ref(false)
const ruleForm = reactive({ id: null, amount: 100, gift: 10, sort: 0, enabled: true })

async function openRules() {
  rulesVisible.value = true
  await loadRules()
}

async function loadRules() {
  ruleLoading.value = true
  try {
    rules.value = (await getRechargeRules()) || []
  } catch (e) {
    rules.value = []
  } finally {
    ruleLoading.value = false
  }
}

function openRuleForm(row) {
  if (row) {
    ruleForm.id = row.id
    ruleForm.amount = Number(row.amount)
    ruleForm.gift = Number(row.gift)
    ruleForm.sort = row.sort || 0
    ruleForm.enabled = row.status === 1
  } else {
    ruleForm.id = null
    ruleForm.amount = 100
    ruleForm.gift = 10
    ruleForm.sort = rules.value.length + 1
    ruleForm.enabled = true
  }
  ruleFormVisible.value = true
}

async function submitRule() {
  if (!ruleForm.amount || ruleForm.amount <= 0) return ElMessage.warning('请输入充值金额')
  ruleSaving.value = true
  const payload = {
    amount: ruleForm.amount,
    gift: ruleForm.gift || 0,
    sort: ruleForm.sort || 0,
    status: ruleForm.enabled ? 1 : 0
  }
  try {
    if (ruleForm.id) {
      await updateRechargeRule(ruleForm.id, payload)
      ElMessage.success('档位已更新')
    } else {
      await createRechargeRule(payload)
      ElMessage.success('档位已新增')
    }
    ruleFormVisible.value = false
    await loadRules()
  } catch (e) {
    /* 已提示 */
  } finally {
    ruleSaving.value = false
  }
}

async function toggleRule(row, enabled) {
  try {
    await updateRechargeRuleStatus(row.id, enabled ? 1 : 0)
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '已启用' : '已停用')
  } catch (e) {
    /* 已提示 */
  }
}

async function removeRule(row) {
  try {
    await ElMessageBox.confirm(
      `删除「充 ¥${fmtMoney(row.amount)} 送 ¥${fmtMoney(row.gift)}」档位？已充值记录不受影响。`,
      '删除档位',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteRechargeRule(row.id)
    ElMessage.success('已删除')
    await loadRules()
  } catch (e) {
    /* 已提示 */
  }
}
</script>

<style scoped>
.admin-balance { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 12px; }
.spacer { flex: 1; }
.rule-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.rule-head .tip { flex: 1; line-height: 1.5; }
.rule-preview-text { font-size: 13px; color: var(--ink-soft); }
.rule-preview {
  margin-top: 4px; padding: 10px 12px; border-radius: 10px;
  background: rgba(47, 107, 255, 0.06); font-size: 13px; color: var(--accent-deep);
}
.tip { font-size: 12px; color: var(--muted); }
.total { color: var(--accent-deep); }
.balance-summary {
  display: flex; gap: 12px; margin-bottom: 8px;
}
.b-item {
  flex: 1; padding: 14px 16px; border-radius: 14px;
  background: var(--fill, #f4f7ff); display: flex; flex-direction: column; gap: 4px;
}
.b-item span { font-size: 12px; color: var(--muted); }
.b-item b { font-size: 20px; }
.adjust-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.lbl { font-size: 13px; color: var(--ink-soft); }
</style>
