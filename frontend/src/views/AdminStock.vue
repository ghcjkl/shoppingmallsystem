<template>
  <div class="admin-stock">
    <!-- 概览 -->
    <div class="summary">
      <div class="sum-card">
        <span class="sum-label">SKU 总数</span>
        <b class="sum-value">{{ summary.skuTotal }}</b>
        <span class="sum-sub">低库存阈值 ≤ {{ summary.lowThreshold || 10 }} 件</span>
      </div>
      <div class="sum-card warn">
        <span class="sum-label">低库存 SKU</span>
        <b class="sum-value">{{ summary.skuLow }}</b>
        <span class="sum-sub">需要补货</span>
      </div>
      <div class="sum-card danger">
        <span class="sum-label">缺货 SKU</span>
        <b class="sum-value">{{ summary.skuOut }}</b>
        <span class="sum-sub">顾客无法下单</span>
      </div>
      <div class="sum-card">
        <span class="sum-label">原料预警</span>
        <b class="sum-value">{{ summary.materialLow }}<small>/{{ summary.materialTotal }}</small></b>
        <span class="sum-sub">低于安全库存</span>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <!-- 商品库存 -->
      <el-tab-pane label="商品库存" name="sku">
        <div class="toolbar">
          <el-input
            v-model="skuQuery.keyword"
            placeholder="搜索商品名称 / 规格"
            clearable
            style="width: 220px"
            @keyup.enter="reloadSku"
            @clear="reloadSku"
          />
          <el-button type="primary" @click="reloadSku">查询</el-button>
          <el-checkbox v-model="skuQuery.lowOnly" @change="reloadSku">只看低库存</el-checkbox>
          <div class="spacer" />
          <span class="tip">库存按 SKU 展示，含已下架商品</span>
        </div>

        <el-card shadow="never">
          <el-table v-loading="skuLoading" :data="skuList" stripe>
            <el-table-column label="商品" min-width="170" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="pname">{{ row.productName || '-' }}</div>
                <div class="psub">{{ row.productType === 2 ? '现制饮品' : '标品零食' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="规格" width="120">
              <template #default="{ row }">{{ row.skuCode || '默认' }}</template>
            </el-table-column>
            <el-table-column label="售价" width="100">
              <template #default="{ row }">¥{{ fmtMoney(row.price) }}</template>
            </el-table-column>
            <el-table-column label="库存" width="120">
              <template #default="{ row }">
                <span class="stock-num" :class="stockClass(row.stock)">{{ row.stock }}</span>
                <el-tag v-if="row.stock <= 0" size="small" type="danger" round class="stock-tag">缺货</el-tag>
                <el-tag v-else-if="row.stock <= lowThreshold" size="small" type="warning" round class="stock-tag">偏低</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.productStatus === 1 ? 'success' : 'info'" round>
                  {{ row.productStatus === 1 ? '上架' : '下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openAdjust('sku', row)">调整库存</el-button>
                <el-button size="small" @click="quickIn(row)">+10</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pager" v-if="skuTotal > skuQuery.pageSize">
            <el-pagination
              layout="prev, pager, next, total"
              :total="skuTotal"
              :page-size="skuQuery.pageSize"
              :current-page="skuQuery.pageNum"
              @current-change="onSkuPage"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 原料库存 -->
      <el-tab-pane label="原料库存" name="material">
        <div class="toolbar">
          <span class="tip">原料用于现制商品按配方扣减，低于安全库存会在这里预警</span>
        </div>
        <el-card shadow="never">
          <el-table v-loading="materialLoading" :data="materials" stripe>
            <el-table-column prop="name" label="原料" min-width="120" />
            <el-table-column prop="unit" label="单位" width="90" />
            <el-table-column label="当前库存" width="150">
              <template #default="{ row }">
                <span class="stock-num" :class="materialStockClass(row)">{{ fmtQty(row.stock) }}</span>
                <el-tag v-if="materialLow(row)" size="small" type="warning" round class="stock-tag">偏低</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="安全库存" width="110">
              <template #default="{ row }">{{ fmtQty(row.safeStock) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" round>
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" plain @click="openAdjust('material', row)">调整库存</el-button>
                <el-button size="small" @click="quickMaterial(row)">补 20</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 出入库记录 -->
      <el-tab-pane label="出入库记录" name="records">
        <div class="toolbar">
          <el-select v-model="recordQuery.bizType" placeholder="全部对象" clearable style="width: 140px" @change="reloadRecords">
            <el-option label="商品 SKU" :value="1" />
            <el-option label="原料" :value="2" />
          </el-select>
          <el-button @click="reloadRecords">刷新</el-button>
          <div class="spacer" />
          <span class="tip">管理端调整、订单销售扣减与取消回滚都会记录在此</span>
        </div>
        <el-card shadow="never">
          <el-table v-loading="recordLoading" :data="records" stripe>
            <el-table-column label="时间" width="165">
              <template #default="{ row }">{{ row.createTime }}</template>
            </el-table-column>
            <el-table-column label="对象" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag size="small" :type="row.bizType === 1 ? 'primary' : 'warning'" round>
                  {{ row.bizType === 1 ? 'SKU' : '原料' }}
                </el-tag>
                <span class="rec-name">{{ row.targetName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="方式" width="90">
              <template #default="{ row }">{{ changeTypeText(row.changeType) }}</template>
            </el-table-column>
            <el-table-column label="变动" width="100">
              <template #default="{ row }">
                <span :class="Number(row.changeQty) >= 0 ? 'plus' : 'minus'">
                  {{ Number(row.changeQty) > 0 ? '+' : '' }}{{ fmtQty(row.changeQty) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="变动前 → 后" width="150">
              <template #default="{ row }">{{ fmtQty(row.beforeQty) }} → {{ fmtQty(row.afterQty) }}</template>
            </el-table-column>
            <el-table-column label="原因" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.reason || '-' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!recordLoading && !records.length" description="暂无库存变更记录" :image-size="70" />
          <div class="pager" v-if="recordTotal > recordQuery.pageSize">
            <el-pagination
              layout="prev, pager, next, total"
              :total="recordTotal"
              :page-size="recordQuery.pageSize"
              :current-page="recordQuery.pageNum"
              @current-change="onRecordPage"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 调整库存弹窗 -->
    <el-dialog v-model="adjustVisible" title="调整库存" width="440px" :close-on-click-modal="false">
      <div class="adj-head">
        <span class="adj-name">{{ current?.targetName }}</span>
        <span class="adj-stock">当前库存：{{ fmtQty(current?.stock) }} {{ current?.unit || '件' }}</span>
      </div>
      <el-form label-width="84px" label-position="left">
        <el-form-item label="调整方式">
          <el-radio-group v-model="adjustForm.changeType">
            <el-radio-button :value="1">入库</el-radio-button>
            <el-radio-button :value="2">出库</el-radio-button>
            <el-radio-button :value="3">直接设定</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="adjustForm.changeType === 3 ? '目标库存' : '数量'">
          <el-input-number
            v-model="adjustForm.quantity"
            :min="0"
            :precision="adjustMode === 'material' ? 2 : 0"
            :step="adjustMode === 'material' ? 10 : 1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="adjustForm.reason" maxlength="100" placeholder="如 到货入库 / 盘点修正 / 损耗出库" />
        </el-form-item>
      </el-form>
      <p class="tip predict" v-if="adjustForm.changeType !== 3">
        调整后库存：{{ predictAfter }} {{ current?.unit || '件' }}
      </p>
      <p class="tip predict" v-else>直接改为 {{ fmtQty(adjustForm.quantity) }} {{ current?.unit || '件' }}</p>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getSkuStockPage, adjustSkuStock, getStockMaterials, adjustMaterialStock,
  getStockSummary, getStockRecords
} from '../api/admin'
import { fmtMoney } from '../utils/format'

const tab = ref('sku')
const lowThreshold = ref(10)

// 概览
const summary = ref({ skuTotal: 0, skuLow: 0, skuOut: 0, materialTotal: 0, materialLow: 0, lowThreshold: 10 })

// 商品库存
const skuLoading = ref(false)
const skuList = ref([])
const skuTotal = ref(0)
const skuQuery = reactive({ keyword: '', lowOnly: false, pageNum: 1, pageSize: 10 })

// 原料
const materialLoading = ref(false)
const materials = ref([])

// 记录
const recordLoading = ref(false)
const records = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ bizType: null, pageNum: 1, pageSize: 10 })

// 调整弹窗
const adjustVisible = ref(false)
const saving = ref(false)
const adjustMode = ref('sku')
const current = ref(null)
const adjustForm = reactive({ changeType: 1, quantity: 10, reason: '' })

const fmtQty = (v) => {
  if (v === null || v === undefined || v === '') return '0'
  const n = Number(v)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

const changeTypeText = (t) => ({ 1: '入库', 2: '出库', 3: '设定' }[t] || '-')

const stockClass = (stock) => {
  if (stock <= 0) return 'out'
  if (stock <= lowThreshold.value) return 'low'
  return ''
}

const materialLow = (row) => {
  const safe = row.safeStock === null || row.safeStock === undefined ? 10 : Number(row.safeStock)
  return Number(row.stock || 0) <= safe
}

const materialStockClass = (row) => {
  if (Number(row.stock || 0) <= 0) return 'out'
  return materialLow(row) ? 'low' : ''
}

const predictAfter = computed(() => {
  const before = Number(current.value?.stock || 0)
  const qty = Number(adjustForm.quantity || 0)
  if (adjustForm.changeType === 1) return fmtQty(before + qty)
  if (adjustForm.changeType === 2) return fmtQty(before - qty)
  return fmtQty(qty)
})

async function loadSummary() {
  try {
    summary.value = await getStockSummary()
    lowThreshold.value = summary.value.lowThreshold ?? 10
  } catch (e) {
    /* 已提示 */
  }
}

async function loadSku() {
  skuLoading.value = true
  try {
    const page = await getSkuStockPage({
      keyword: skuQuery.keyword || undefined,
      lowOnly: skuQuery.lowOnly || undefined,
      pageNum: skuQuery.pageNum,
      pageSize: skuQuery.pageSize
    })
    skuList.value = page.records || []
    skuTotal.value = page.total || 0
  } catch (e) {
    skuList.value = []
  } finally {
    skuLoading.value = false
  }
}

function reloadSku() {
  skuQuery.pageNum = 1
  loadSku()
}

function onSkuPage(p) {
  skuQuery.pageNum = p
  loadSku()
}

async function loadMaterials() {
  materialLoading.value = true
  try {
    materials.value = (await getStockMaterials()) || []
  } catch (e) {
    materials.value = []
  } finally {
    materialLoading.value = false
  }
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const page = await getStockRecords({
      bizType: recordQuery.bizType ?? undefined,
      pageNum: recordQuery.pageNum,
      pageSize: recordQuery.pageSize
    })
    records.value = page.records || []
    recordTotal.value = page.total || 0
  } catch (e) {
    records.value = []
  } finally {
    recordLoading.value = false
  }
}

function reloadRecords() {
  recordQuery.pageNum = 1
  loadRecords()
}

function onRecordPage(p) {
  recordQuery.pageNum = p
  loadRecords()
}

function onTabChange(name) {
  if (name === 'sku') loadSku()
  else if (name === 'material') loadMaterials()
  else loadRecords()
}

function openAdjust(mode, row) {
  adjustMode.value = mode
  current.value = {
    id: mode === 'sku' ? row.skuId : row.id,
    targetName: mode === 'sku' ? `${row.productName}${row.skuCode ? ' / ' + row.skuCode : ''}` : row.name,
    stock: row.stock,
    unit: mode === 'sku' ? '件' : row.unit || '份'
  }
  adjustForm.changeType = 1
  adjustForm.quantity = mode === 'sku' ? 10 : 20
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function quickIn(row) {
  try {
    await adjustSkuStock(row.skuId, { changeType: 1, quantity: 10, reason: '快捷补货 +10' })
    ElMessage.success('已入库 10 件')
    await Promise.all([loadSku(), loadSummary()])
  } catch (e) {
    /* 已提示 */
  }
}

async function quickMaterial(row) {
  try {
    await adjustMaterialStock(row.id, { changeType: 1, quantity: 20, reason: '快捷补货 +20' })
    ElMessage.success('已补货 20')
    await Promise.all([loadMaterials(), loadSummary()])
  } catch (e) {
    /* 已提示 */
  }
}

async function submitAdjust() {
  const qty = Number(adjustForm.quantity)
  if (Number.isNaN(qty) || qty < 0) return ElMessage.warning('请填写正确的数量')
  if (adjustForm.changeType !== 3 && qty <= 0) return ElMessage.warning('入库/出库数量需大于 0')
  saving.value = true
  try {
    const body = {
      changeType: adjustForm.changeType,
      quantity: qty,
      reason: adjustForm.reason?.trim() || null
    }
    if (adjustMode.value === 'sku') await adjustSkuStock(current.value.id, body)
    else await adjustMaterialStock(current.value.id, body)
    ElMessage.success('库存已更新')
    adjustVisible.value = false
    if (adjustMode.value === 'sku') await Promise.all([loadSku(), loadSummary()])
    else await Promise.all([loadMaterials(), loadSummary()])
    if (tab.value === 'records') loadRecords()
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadSummary()
  loadSku()
  loadMaterials()
})
</script>

<style scoped>
.admin-stock {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.sum-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow: 0 6px 18px rgba(47, 107, 255, 0.07);
}

.sum-label {
  font-size: 12.5px;
  color: var(--muted);
}

.sum-value {
  font-size: 26px;
  line-height: 1.2;
  color: var(--ink);
}

.sum-value small {
  font-size: 14px;
  color: var(--muted);
  font-weight: 500;
}

.sum-sub {
  font-size: 11.5px;
  color: var(--muted);
}

.sum-card.warn .sum-value {
  color: #c77c12;
}

.sum-card.danger .sum-value {
  color: #d5485f;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.spacer {
  flex: 1;
}

.tip {
  font-size: 12px;
  color: var(--muted);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.pname {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink);
}

.psub {
  font-size: 11.5px;
  color: var(--muted);
}

.stock-num {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.stock-num.low {
  color: #c77c12;
}

.stock-num.out {
  color: #d5485f;
}

.stock-tag {
  margin-left: 6px;
}

.rec-name {
  margin-left: 8px;
}

.plus {
  color: #3f9d63;
  font-weight: 700;
}

.minus {
  color: #d5485f;
  font-weight: 700;
}

.adj-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--line);
}

.adj-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
}

.adj-stock {
  font-size: 12.5px;
  color: var(--muted);
}

.predict {
  margin: 4px 0 0;
}
</style>
