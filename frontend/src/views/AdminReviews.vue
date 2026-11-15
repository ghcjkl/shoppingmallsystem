<template>
  <div class="admin-reviews">
    <!-- 筛选 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索商品名 / 评价内容"
        clearable
        style="width: 240px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #append>
          <el-button @click="search">搜索</el-button>
        </template>
      </el-input>
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 130px" @change="search">
        <el-option label="显示中" :value="1" />
        <el-option label="已隐藏" :value="0" />
      </el-select>
      <el-select v-model="rating" placeholder="全部星级" clearable style="width: 120px" @change="search">
        <el-option v-for="s in [5, 4, 3, 2, 1]" :key="s" :label="`${s} 星`" :value="s" />
      </el-select>
      <span class="tip">隐藏后前台不再展示，评分统计同步剔除</span>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column label="商品" min-width="130">
          <template #default="{ row }">{{ row.productName || `商品 ${row.productId}` }}</template>
        </el-table-column>
        <el-table-column label="评分" width="110">
          <template #default="{ row }">
            <span class="stars">{{ starText(row.rating) }}</span>
            <span class="score">{{ row.rating }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="220">
          <template #default="{ row }">
            <div class="content">{{ row.content || '（只打分，未写内容）' }}</div>
            <div v-if="row.reply" class="reply-line">回复：{{ row.reply }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="{ row }">{{ row.nickname }}</template>
        </el-table-column>
        <el-table-column label="时间" width="168">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              size="small"
              @change="(v) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="openReply(row)">
              {{ row.reply ? '改回复' : '回复' }}
            </el-button>
            <el-button size="small" text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="tip">暂无评价</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="changePage"
        />
      </div>
    </el-card>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="商家回复" width="440px">
      <div class="reply-target">
        <span class="stars">{{ starText(current?.rating) }}</span>
        {{ current?.content || '（只打分，未写内容）' }}
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="3"
        maxlength="255"
        show-word-limit
        placeholder="回复会展示在商品详情页的评价下方，留空可清除回复"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReply">保存回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewPage, setReviewStatus, replyReview, deleteReview } from '../api/admin'
import { fmtTime } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const status = ref(null)
const rating = ref(null)
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10

const replyVisible = ref(false)
const current = ref(null)
const replyText = ref('')

const starText = (score) => {
  const n = Math.max(0, Math.min(5, Math.round(Number(score) || 0)))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

async function load() {
  loading.value = true
  try {
    const res = await getReviewPage({
      keyword: keyword.value.trim() || undefined,
      status: status.value === null || status.value === '' ? undefined : status.value,
      rating: rating.value === null || rating.value === '' ? undefined : rating.value,
      pageNum: pageNum.value,
      pageSize
    })
    rows.value = res.records || []
    total.value = Number(res.total || 0)
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

function changePage(page) {
  pageNum.value = page
  load()
}

async function toggleStatus(row, enabled) {
  try {
    await setReviewStatus(row.id, enabled ? 1 : 0)
    row.status = enabled ? 1 : 0
    ElMessage.success(enabled ? '已在前台显示' : '已隐藏，评分统计同步剔除')
  } catch (e) {
    /* 已提示 */
  }
}

function openReply(row) {
  current.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}

async function submitReply() {
  saving.value = true
  try {
    const res = await replyReview(current.value.id, replyText.value.trim())
    current.value.reply = res?.reply || null
    ElMessage.success(replyText.value.trim() ? '回复已保存' : '回复已清除')
    replyVisible.value = false
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.productName || '该商品'}」的这条评价？删除后用户可重新评价。`,
      '删除评价',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteReview(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    /* 已提示 */
  }
}

onMounted(load)
</script>

<style scoped>
.admin-reviews {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tip {
  font-size: 12px;
  color: var(--muted);
}

.stars {
  color: #f0a020;
  letter-spacing: 1px;
  font-size: 13px;
}

.score {
  margin-left: 6px;
  font-size: 12px;
  color: var(--muted);
}

.content {
  font-size: 13px;
  color: var(--ink);
  line-height: 1.6;
  word-break: break-word;
}

.reply-line {
  margin-top: 4px;
  font-size: 12px;
  color: var(--accent-deep);
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.reply-target {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f6f9ff;
  font-size: 13px;
  color: var(--ink-soft);
  line-height: 1.6;
}
</style>
