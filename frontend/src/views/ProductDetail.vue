<template>
  <div class="detail-page" v-loading="loading">
    <div v-if="detail" class="wrap">
      <button class="back" @click="$router.back()">← 返回</button>

      <el-card shadow="never" class="top-card">
        <div class="top">
          <div class="cover" :class="'type-' + detail.productType">
            <img v-if="detail.coverImg" :src="detail.coverImg" :alt="detail.name" />
            <span v-else class="cover-emoji">{{ detail.productType === 2 ? '🧋' : '🍿' }}</span>
          </div>
          <div class="info">
            <el-tag size="small" effect="dark" round>{{ detail.productType === 2 ? '现制饮品' : '标品零食' }}</el-tag>
            <h1>{{ detail.name }}</h1>
            <div class="price-line">
              <span class="price">¥{{ fmtMoney(unitPrice) }}</span>
              <span v-if="detail.productType === 2" class="base">基准价 ¥{{ fmtMoney(detail.basePrice) }}</span>
            </div>
            <div class="tags" v-if="tagList.length">
              <el-tag v-for="t in tagList" :key="t" size="small" type="info" effect="plain" round>{{ t }}</el-tag>
            </div>
            <!-- 评分摘要：点击滚到评价区 -->
            <div class="rating-line" @click="scrollToReviews">
              <template v-if="reviewTotal > 0">
                <span class="stars">{{ starText(Number(detail.rating)) }}</span>
                <b>{{ Number(detail.rating).toFixed(1) }}</b>
                <span class="rv-count">{{ reviewTotal }} 条评价</span>
              </template>
              <span v-else class="rv-count">暂无评价</span>
            </div>
            <p class="desc" v-if="detail.detail">{{ detail.detail }}</p>
          </div>
        </div>
      </el-card>

      <!-- SKU 规格选择 -->
      <el-card shadow="never" class="section" v-if="skus.length">
        <template #header><b>规格</b></template>
        <div class="sku-list">
          <button
            v-for="(s, i) in skus"
            :key="s.id"
            class="sku-chip"
            :class="{ active: selectedSkuId === s.id, empty: Number(s.stock || 0) <= 0 }"
            :disabled="Number(s.stock || 0) <= 0"
            @click="selectedSkuId = s.id"
          >
            <span>{{ skuLabel(s, i) }}</span>
            <span v-if="Number(s.stock || 0) <= 0" class="sku-empty">售罄</span>
            <span v-else class="sku-price">¥{{ fmtMoney(s.price) }}</span>
          </button>
        </div>
      </el-card>

      <!-- 制作偏好：温度/甜度/冰量等（仅现制饮品，每组单选） -->
      <el-card shadow="never" class="section" v-if="detail.productType === 2 && makeGroups.length">
        <template #header><b>制作偏好</b></template>
        <div v-for="g in makeGroups" :key="g.id" class="spec-block">
          <div class="spec-label">{{ g.name }}</div>
          <div class="ing-list">
            <button
              v-for="o in g.options"
              :key="o.id"
              class="ing-chip"
              :class="{ active: selectedSpecMap[g.id] === o.id, disabled: isOptionDisabled(g, o) }"
              :title="isOptionDisabled(g, o) ? optionConflictReason(g, o) : ''"
              @click="pickSpecOption(g, o)"
            >
              <span>{{ o.name }}</span>
              <span v-if="Number(o.priceDelta || 0) > 0" class="ing-price">+¥{{ fmtMoney(o.priceDelta) }}</span>
            </button>
          </div>
        </div>
      </el-card>

      <!-- 加料多选（现制） -->
      <el-card shadow="never" class="section" v-if="detail.productType === 2 && ingredients.length">
        <template #header><b>加料（可多选）</b></template>
        <div class="ing-list">
          <button
            v-for="ing in ingredients"
            :key="ing.id"
            class="ing-chip"
            :class="{ active: selectedIngredientIds.includes(ing.id) }"
            @click="toggleIngredient(ing.id)"
          >
            <span>{{ ing.name }}</span>
            <span class="ing-price">+¥{{ fmtMoney(ing.price) }}</span>
          </button>
        </div>
      </el-card>

      <!-- 商品评价 -->
      <el-card id="reviews" shadow="never" class="section">
        <template #header>
          <div class="rv-head">
            <b>商品评价</b>
            <span v-if="reviewTotal" class="rv-total">共 {{ reviewTotal }} 条</span>
          </div>
        </template>

        <div v-if="reviewTotal" class="rv-summary">
          <div class="rv-score">
            <b>{{ Number(reviewData.avgRating).toFixed(1) }}</b>
            <span class="stars">{{ starText(Number(reviewData.avgRating)) }}</span>
            <span class="rv-sub">平均评分</span>
          </div>
          <div class="rv-dist">
            <button
              v-for="row in starRows"
              :key="row.star"
              class="dist-row"
              :class="{ active: filterRating === row.star }"
              @click="toggleFilter(row.star)"
            >
              <span class="dist-label">{{ row.star }} 星</span>
              <span class="dist-bar"><i :style="{ width: row.pct + '%' }" /></span>
              <span class="dist-num">{{ row.count }}</span>
            </button>
          </div>
        </div>

        <div v-if="reviews.length" class="rv-list">
          <div v-for="r in reviews" :key="r.id" class="rv-item">
            <div class="rv-item-head">
              <span class="rv-avatar">{{ (r.nickname || '会').charAt(0) }}</span>
              <span class="rv-name">{{ r.nickname }}</span>
              <span class="stars small">{{ starText(r.rating) }}</span>
              <span class="rv-time">{{ fmtTime(r.createTime) }}</span>
            </div>
            <p v-if="r.content" class="rv-content">{{ r.content }}</p>
            <div v-if="r.reply" class="rv-reply">
              <span class="rv-reply-label">商家回复</span>{{ r.reply }}
            </div>
          </div>
          <div v-if="filterTotal > reviewPageSize" class="rv-more">
            <el-pagination
              small
              background
              layout="prev, pager, next"
              :total="filterTotal"
              :page-size="reviewPageSize"
              :current-page="reviewPage"
              @current-change="changeReviewPage"
            />
          </div>
        </div>
        <el-empty
          v-else
          :description="filterRating ? '该星级暂无评价' : '还没有人评价，等你来抢沙发'"
          :image-size="70"
        />
      </el-card>

      <!-- 底部操作栏 -->
      <div class="footer">
        <div class="qty">
          <span>数量</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQuantity" :disabled="soldOut" />
        </div>
        <div class="foot-actions">
          <span v-if="soldOut" class="sold-tip">该规格暂时售罄</span>
          <span v-else class="total">合计 ¥{{ fmtMoney(unitPrice * quantity) }}</span>
          <el-button type="primary" size="large" round :disabled="soldOut" @click="addToCart">
            {{ soldOut ? '已售罄' : '加入购物车' }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductDetail, getSpecList } from '../api/product'
import { getProductReviews } from '../api/review'
import { useCartStore } from '../stores/cart'
import { fmtMoney, fmtTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()

const loading = ref(false)
const detail = ref(null)
const selectedSkuId = ref(null)
const selectedIngredientIds = ref([])
// 制作规格组（温度/甜度/冰量…）+ 每组选中项，key 为 groupId
const specGroups = ref([])
const selectedSpecMap = ref({})
const quantity = ref(1)

const skus = computed(() => detail.value?.skus || [])
const ingredients = computed(() => detail.value?.ingredients || [])
// 后端标记了 skuGroup（杯型这类决定 SKU 的组），排除后才是下单时可选的制作偏好
const makeGroups = computed(() =>
  specGroups.value.filter((g) => !g.skuGroup && (g.options || []).length)
)

const selectedSpecOptions = computed(() => {
  const out = []
  for (const g of makeGroups.value) {
    const o = (g.options || []).find((x) => x.id === selectedSpecMap.value[g.id])
    if (o) {
      out.push({
        id: o.id,
        name: o.name,
        priceDelta: Number(o.priceDelta || 0),
        groupName: g.name
      })
    }
  }
  return out
})

// 制作选项跨组互斥（与后端 resolveMakeOptions 规则一致）：
// 温度「热」不能配冰量；温度「冰」不能配「去冰」。按选项名判断，不依赖 ID。
const isHot = () => makeGroups.value.some((g) =>
  g.name.includes('温度') && (g.options || []).find((x) => x.id === selectedSpecMap.value[g.id])?.name?.includes('热'))
const isIced = () => makeGroups.value.some((g) =>
  g.name.includes('温度') && (g.options || []).find((x) => x.id === selectedSpecMap.value[g.id])?.name?.includes('冰'))
const optionConflictReason = (g, o) => {
  if (g.name.includes('冰量')) {
    // 热饮不能加冰，但「去冰」是合理的（热饮本来就无冰）
    const addsIce = o.name.includes('冰') && !o.name.includes('去冰')
    if (isHot() && addsIce) return '「热」不能加冰，请先改温度'
    if (isIced() && o.name.includes('去冰')) return '已选「冰」，不能再选「去冰」'
  }
  if (g.name.includes('温度')) {
    const ice = makeGroups.value.find((x) => x.name.includes('冰量'))
    const iceName = (ice?.options || []).find((x) => x.id === selectedSpecMap.value[ice.id])?.name || ''
    const iceAdds = iceName.includes('冰') && !iceName.includes('去冰')
    if (o.name.includes('热') && iceAdds) return '当前冰量为「' + iceName + '」，请先改冰量'
    if (o.name.includes('冰') && iceName.includes('去冰')) return '当前冰量为「去冰」，请先改冰量'
  }
  return ''
}
const isOptionDisabled = (g, o) => !!optionConflictReason(g, o)
const pickSpecOption = (g, o) => {
  const reason = optionConflictReason(g, o)
  if (reason) {
    ElMessage.warning(reason)
    return
  }
  selectedSpecMap.value[g.id] = o.id
}

const selectedSku = computed(() => skus.value.find((s) => s.id === selectedSkuId.value))

// 当前选中规格的可售库存（未选规格时取全部启用规格合计）
const selectedStock = computed(() => {
  if (selectedSku.value) return Number(selectedSku.value.stock || 0)
  return Number(detail.value?.stock || 0)
})
const soldOut = computed(() => selectedStock.value <= 0)
const maxQuantity = computed(() => Math.max(1, Math.min(99, selectedStock.value)))

const selectedIngredients = computed(() =>
  ingredients.value.filter((i) => selectedIngredientIds.value.includes(i.id))
)

const unitPrice = computed(() => {
  const base = selectedSku.value?.price ?? detail.value?.basePrice ?? 0
  const ingSum = selectedIngredients.value.reduce((s, i) => s + Number(i.price || 0), 0)
  const specSum = selectedSpecOptions.value.reduce((s, o) => s + o.priceDelta, 0)
  return Number(base) + ingSum + specSum
})

const tagList = computed(() => {
  const d = detail.value
  if (!d) return []
  const tags = []
  if (d.productType === 1) {
    if (d.netContent) tags.push(`净含量 ${d.netContent}`)
    if (d.shelfLife) tags.push(`保质期 ${d.shelfLife}`)
  } else {
    if (d.estMakeMinutes) tags.push(`约 ${d.estMakeMinutes} 分钟出餐`)
    if (d.calories) tags.push(`${d.calories} 千卡`)
    if (d.sugarNote) tags.push(d.sugarNote)
  }
  if (d.allergen) tags.push(`过敏原：${d.allergen}`)
  return tags
})

const skuLabel = (s, i) => {
  if (s.skuCode) return s.skuCode
  return skus.value.length > 1 ? `规格 ${i + 1}` : '默认规格'
}

const toggleIngredient = (id) => {
  const idx = selectedIngredientIds.value.indexOf(id)
  if (idx >= 0) selectedIngredientIds.value.splice(idx, 1)
  else selectedIngredientIds.value.push(id)
}

const addToCart = () => {
  if (detail.value.productType === 1 && !selectedSku.value) {
    ElMessage.warning('请选择规格')
    return
  }
  if (soldOut.value) {
    ElMessage.warning('该商品已售罄')
    return
  }
  if (quantity.value > selectedStock.value) {
    ElMessage.warning(`库存仅剩 ${selectedStock.value} 件`)
    return
  }
  cart.add({
    productId: detail.value.id,
    productType: detail.value.productType,
    productName: detail.value.name,
    coverImg: detail.value.coverImg,
    skuId: selectedSku.value?.id ?? null,
    skuLabel: selectedSku.value ? skuLabel(selectedSku.value, skus.value.indexOf(selectedSku.value)) : '默认',
    skuPrice: Number(selectedSku.value?.price ?? detail.value.basePrice ?? 0),
    specOptions: selectedSpecOptions.value.map((o) => ({ ...o })),
    ingredients: selectedIngredients.value.map((i) => ({ id: i.id, name: i.name, price: Number(i.price || 0) })),
    unitPrice: unitPrice.value,
    quantity: quantity.value
  })
  ElMessage.success('已加入购物车')
  router.push('/cart')
}

// ===== 商品评价 =====
const reviewData = ref({ avgRating: 0, total: 0, filterTotal: 0, starDist: {}, records: [] })
const reviewPage = ref(1)
const reviewPageSize = 5
const filterRating = ref(null)

const reviews = computed(() => reviewData.value.records || [])
const reviewTotal = computed(() => Number(reviewData.value.total || 0))
const filterTotal = computed(() => Number(reviewData.value.filterTotal || 0))

/** 用实心/空心星表现评分，四舍五入到整星 */
const starText = (score) => {
  const n = Math.max(0, Math.min(5, Math.round(Number(score) || 0)))
  return '★'.repeat(n) + '☆'.repeat(5 - n)
}

const starRows = computed(() => {
  const dist = reviewData.value.starDist || {}
  const total = reviewTotal.value || 1
  return [5, 4, 3, 2, 1].map((star) => {
    const count = Number(dist[star] ?? dist[String(star)] ?? 0)
    return { star, count, pct: Math.max(count > 0 ? 4 : 0, (count / total) * 100) }
  })
})

const loadReviews = async () => {
  try {
    reviewData.value = await getProductReviews(route.params.id, {
      pageNum: reviewPage.value,
      pageSize: reviewPageSize,
      rating: filterRating.value || undefined
    })
  } catch (e) {
    reviewData.value = { avgRating: 0, total: 0, filterTotal: 0, starDist: {}, records: [] }
  }
}

const toggleFilter = (star) => {
  filterRating.value = filterRating.value === star ? null : star
  reviewPage.value = 1
  loadReviews()
}

const changeReviewPage = (page) => {
  reviewPage.value = page
  loadReviews()
}

const scrollToReviews = () => {
  document.querySelector('#reviews')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(async () => {
  loading.value = true
  try {
    detail.value = await getProductDetail(route.params.id)
    // 默认选中第一个可用 SKU
    if (skus.value.length) {
      // 默认选中第一个有货规格，全部售罄时退回第一个（按钮会被禁用）
      const firstInStock = skus.value.find((s) => Number(s.stock || 0) > 0)
      selectedSkuId.value = (firstInStock || skus.value[0]).id
    }
    if (detail.value.productType === 2) {
      // 制作偏好每组默认取第一项（冰 / 全糖 / 正常冰），保证每单都带明确的制作参数
      const groups = (await getSpecList(2)) || []
      specGroups.value = groups.filter((g) => !g.skuGroup && (g.options || []).length)
      const defaults = {}
      specGroups.value.forEach((g) => { defaults[g.id] = g.options[0].id })
      selectedSpecMap.value = defaults
    }
  } catch (e) {
    /* 已由拦截器提示 */
  } finally {
    loading.value = false
  }
  loadReviews()
})
</script>

<style scoped>
.detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding-bottom: 90px;
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

.top {
  display: flex;
  gap: 22px;
}

.cover {
  flex-shrink: 0;
  width: 260px;
  height: 220px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  overflow: hidden;
}

.cover.type-1 {
  background: linear-gradient(135deg, #dbe7ff, #aac5ff);
}

.cover.type-2 {
  background: linear-gradient(135deg, #dfe8ff, #c4d6ff);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-emoji {
  font-size: 90px;
}

.info {
  flex: 1;
  min-width: 0;
}

.info h1 {
  margin: 12px 0 8px;
  font-size: 24px;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.price {
  font-size: 26px;
  font-weight: 800;
  color: var(--accent-deep);
}

.base {
  font-size: 13px;
  color: var(--muted);
  text-decoration: line-through;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.desc {
  margin-top: 14px;
  font-size: 13px;
  color: var(--muted);
  line-height: 1.8;
}

.sku-list,
.ing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.spec-block + .spec-block {
  margin-top: 16px;
}

.spec-label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
}

.sku-chip,
.ing-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  color: var(--ink-soft);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}

.sku-chip.active,
.ing-chip.active {
  color: var(--accent-deep);
  border-color: var(--accent);
  background: #eef3ff;
  font-weight: 600;
}

.ing-chip.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  text-decoration: line-through;
}

.sku-chip.empty {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f4f5f8;
}

.sku-empty {
  font-size: 12px;
  color: #d94a4a;
  font-weight: 600;
}

.sold-tip {
  font-size: 15px;
  font-weight: 600;
  color: #d94a4a;
}

.sku-price {
  color: var(--accent-deep);
  font-weight: 700;
}

.ing-price {
  font-size: 12px;
  color: var(--muted);
}

.footer {
  position: fixed;
  left: 232px;
  right: 20px;
  bottom: 16px;
  z-index: 25;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-radius: 18px;
  background: rgba(255, 253, 249, 0.96);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 34px rgba(26, 43, 74, 0.16);
  backdrop-filter: blur(18px);
}

.qty {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--ink-soft);
  font-size: 14px;
}

.foot-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.total {
  font-size: 20px;
  font-weight: 800;
  color: var(--accent-deep);
}

/* ===== 评分与评价 ===== */
.rating-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--muted);
}

.rating-line b {
  font-size: 15px;
  color: var(--ink);
}

.rating-line:hover .rv-count {
  color: var(--accent);
}

.stars {
  color: #f0a020;
  letter-spacing: 1px;
  font-size: 14px;
}

.stars.small {
  font-size: 12px;
  letter-spacing: 0;
}

.rv-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.rv-total {
  font-size: 12px;
  color: var(--muted);
}

.rv-summary {
  display: flex;
  gap: 28px;
  align-items: center;
  padding: 4px 0 16px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 4px;
}

.rv-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 96px;
}

.rv-score b {
  font-size: 32px;
  line-height: 1.1;
  color: var(--ink);
}

.rv-sub {
  font-size: 11.5px;
  color: var(--muted);
}

.rv-dist {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
  color: var(--muted);
  text-align: left;
}

.dist-row:hover {
  background: rgba(47, 107, 255, 0.06);
}

.dist-row.active {
  background: rgba(47, 107, 255, 0.1);
  color: var(--accent-deep);
}

.dist-label {
  width: 34px;
  flex-shrink: 0;
}

.dist-bar {
  flex: 1;
  height: 7px;
  border-radius: 999px;
  background: #eef2fb;
  overflow: hidden;
}

.dist-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: #f0b429;
  transition: width 0.3s ease;
}

.dist-num {
  width: 30px;
  text-align: right;
  flex-shrink: 0;
}

.rv-list {
  display: flex;
  flex-direction: column;
}

.rv-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--line);
}

.rv-item:last-child {
  border-bottom: none;
}

.rv-item-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.rv-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  color: #fff;
  background: linear-gradient(135deg, #2f6bff, #5b8def);
  flex-shrink: 0;
}

.rv-name {
  font-size: 13px;
  color: var(--ink-soft);
}

.rv-time {
  margin-left: auto;
  font-size: 11.5px;
  color: var(--muted);
}

.rv-content {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink);
  white-space: pre-wrap;
}

.rv-reply {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f6f9ff;
  font-size: 12.5px;
  color: var(--ink-soft);
  line-height: 1.6;
}

.rv-reply-label {
  color: var(--accent-deep);
  font-weight: 600;
  margin-right: 6px;
}

.rv-more {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}

@media (max-width: 768px) {
  .top {
    flex-direction: column;
  }

  .cover {
    width: 100%;
    height: 200px;
  }

  .footer {
    left: 10px;
    right: 10px;
    bottom: calc(78px + env(safe-area-inset-bottom));
    flex-direction: column;
    gap: 12px;
  }

  .foot-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
