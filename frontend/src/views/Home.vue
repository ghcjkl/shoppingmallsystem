<template>
  <div class="home">
    <!-- 顶部横幅 -->
    <div class="hero">
      <div class="hero-text">
        <h2>食光铺子 · 零食奶茶咖啡</h2>
        <p>新鲜现制，下单即做，排队取餐不等待</p>
        <div class="hero-tags">
          <span>🧋 现制茶饮</span>
          <span>🍿 休闲零食</span>
          <span>☕ 精品咖啡</span>
        </div>
        <button class="hero-btn" :class="{ disabled: storeClosed }" :disabled="storeClosed" @click="scrollToProducts">
          {{ storeClosed ? '暂停营业中' : '立即点单' }}
        </button>
      </div>
      <div class="hero-art">
        <div class="hero-emoji">🧋</div>
        <div class="hero-meta" v-if="store">
          <div class="hero-store">{{ store.name || '食光铺子' }}</div>
          <div class="hero-status" :class="{ off: store.status === 0 }">
            {{ store.status === 0 ? '暂停营业' : '营业中' }}
          </div>
          <div class="hero-addr" v-if="store.address">📍 {{ store.address }}</div>
        </div>
      </div>
    </div>

    <!-- 分类快捷导航 -->
    <div class="section-title">
      <span>分类导航</span>
      <small>点击快速筛选</small>
    </div>
    <div class="cat-cards" v-if="categories.length">
      <button
        class="cat-card"
        :class="{ active: categoryId === null }"
        @click="selectCategory(null)"
      >
        <span class="cat-icon">🏷️</span>
        <span class="cat-name">全部</span>
      </button>
      <button
        v-for="c in categories"
        :key="c.id"
        class="cat-card"
        :class="{ active: categoryId === c.id }"
        @click="selectCategory(c.id)"
      >
        <span class="cat-icon">{{ catIcon(c.name) }}</span>
        <span class="cat-name">{{ c.name }}</span>
      </button>
    </div>

    <!-- 商品类型切换 + 搜索 -->
    <div class="toolbar">
      <el-radio-group v-model="productType" @change="onTypeChange">
        <el-radio-button :value="1">标品零食</el-radio-button>
        <el-radio-button :value="2">现制饮品</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        class="search"
        placeholder="搜索商品名称"
        clearable
        @keyup.enter="reload"
        @clear="reload"
      >
        <template #append>
          <el-button @click="reload">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 商品列表 -->
    <div v-loading="loading" class="grid" id="productList">
      <el-empty v-if="!loading && !products.length" description="暂无商品，请先在后台建品" />
      <div
        v-for="p in products"
        :key="p.id"
        class="product-card"
        :class="{ sold: Number(p.stock || 0) <= 0 }"
        @click="goDetail(p.id)"
      >
        <div class="cover" :class="'type-' + p.productType">
          <img v-if="p.coverImg" :src="p.coverImg" :alt="p.name" />
          <span v-else class="cover-emoji">{{ p.productType === 2 ? '🧋' : '🍿' }}</span>
          <el-tag class="cover-tag" size="small" effect="dark" round>
            {{ p.productType === 2 ? '现制' : '标品' }}
          </el-tag>
          <div v-if="Number(p.stock || 0) <= 0" class="sold-mask">售罄</div>
        </div>
        <div class="p-body">
          <div class="p-name">{{ p.name }}</div>
          <div class="p-sub">
            <span v-if="Number(p.reviewCount || 0) > 0" class="p-rating">
              <i>★</i>{{ Number(p.rating).toFixed(1) }}
              <em>({{ p.reviewCount }})</em>
            </span>
            <span v-else class="p-no-rating">暂无评价</span>
            <span class="p-sub-sep" v-if="subText(p)">·</span>
            {{ subText(p) }}
          </div>
          <div class="p-foot">
            <span class="p-price">¥{{ fmtMoney(p.basePrice) }}</span>
            <span v-if="Number(p.stock || 0) <= 0" class="sold-badge">暂时售罄</span>
            <div v-else class="stepper" :class="{ on: cart.quantityOf(p.id) > 0 }" @click.stop>
              <template v-if="cart.quantityOf(p.id) > 0">
                <button class="step-btn minus" aria-label="减少数量" @click.stop="decrease(p)">−</button>
                <span :key="cart.quantityOf(p.id)" class="step-num">{{ cart.quantityOf(p.id) }}</span>
                <button class="step-btn plus" aria-label="增加数量" @click.stop="increase(p)">＋</button>
              </template>
              <button v-else class="step-btn plus solo" aria-label="加入购物车" @click.stop="increase(p)">＋</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="pager" v-if="total > pageSize">
      <el-pagination
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProductPage, getCategoryList } from '../api/product'
import { useAppStore } from '../stores/app'
import { useCartStore } from '../stores/cart'
import { fmtMoney } from '../utils/format'

const router = useRouter()
const app = useAppStore()
const cart = useCartStore()

const store = computed(() => app.store)
const storeClosed = computed(() => store.value?.status === 0)
const productType = ref(1)
const keyword = ref('')
const categoryId = ref(null)
const categories = ref([])
const products = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const subText = (p) => {
  if (p.productType === 1) return p.netContent ? `净含量 ${p.netContent}` : '标品零食'
  return p.sugarNote || `约 ${p.estMakeMinutes || 15} 分钟出餐`
}

// 分类名 -> 图标（按关键词匹配，兜底用标签图标）
const CATEGORY_ICONS = [
  { keys: ['零食', '膨化', '薯片', '饼干', '坚果'], icon: '🍿' },
  { keys: ['糖', '巧', '甜', '糕点'], icon: '🍫' },
  { keys: ['茶', '果', '饮', '奶茶', '西柚', '柠檬'], icon: '🧋' },
  { keys: ['咖啡', '拿铁', '抹茶', '奶'], icon: '☕' },
  { keys: ['面包', '烘焙'], icon: '🥐' },
  { keys: ['冰', '雪糕', '冰淇淋'], icon: '🍦' }
]
const catIcon = (name) => {
  const hit = CATEGORY_ICONS.find((item) => item.keys.some((k) => name?.includes(k)))
  return hit ? hit.icon : '🏷️'
}

const scrollToProducts = () => {
  document.getElementById('productList')?.scrollIntoView({ behavior: 'smooth' })
}

const loadCategories = async () => {
  try {
    categories.value = (await getCategoryList(productType.value)) || []
  } catch (e) {
    categories.value = []
  }
}

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await getProductPage({
      productType: productType.value,
      categoryId: categoryId.value ?? undefined,
      keyword: keyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    products.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    products.value = []
  } finally {
    loading.value = false
  }
}

const reload = () => {
  pageNum.value = 1
  loadProducts()
}

const onTypeChange = () => {
  categoryId.value = null
  loadCategories()
  reload()
}

const selectCategory = (id) => {
  categoryId.value = id
  reload()
}

const onPageChange = (p) => {
  pageNum.value = p
  loadProducts()
}

const goDetail = (id) => router.push(`/product/${id}`)

// 卡片步进器：点「＋」直接按默认规格加入购物车，点「−」减少一件
const increase = (p) => {
  const stock = Number(p.stock || 0)
  if (stock <= 0) {
    ElMessage.warning('该商品已售罄')
    return
  }
  if (cart.quantityOf(p.id) >= stock) {
    ElMessage.warning(`库存仅剩 ${stock} 件`)
    return
  }
  const price = Number(p.basePrice || 0)
  cart.add({
    productId: p.id,
    productType: p.productType,
    productName: p.name,
    coverImg: p.coverImg,
    skuId: null,
    skuLabel: '默认规格',
    skuPrice: price,
    ingredients: [],
    unitPrice: price,
    quantity: 1
  })
  ElMessage({ message: '已加入购物车', type: 'success', duration: 1100, grouping: true })
}

const decrease = (p) => cart.decreaseByProduct(p.id)

onMounted(async () => {
  await app.init()
  await loadCategories()
  await loadProducts()
})
</script>

<style scoped>
.home {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
}

/* 光斑背景：为毛玻璃提供可透出的色彩 */
.home::before {
  content: "";
  position: absolute;
  inset: -40px;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(42% 32% at 12% 6%, rgba(47, 107, 255, 0.20), transparent 60%),
    radial-gradient(38% 30% at 92% 12%, rgba(122, 165, 255, 0.22), transparent 62%),
    radial-gradient(46% 36% at 50% 88%, rgba(91, 127, 255, 0.16), transparent 64%);
}

.home > * {
  position: relative;
  z-index: 1;
}

/* 顶部横幅（毛玻璃） */
.hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 34px 36px;
  margin-bottom: 22px;
  border-radius: 22px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(120deg, rgba(47, 107, 255, 0.72) 0%, rgba(30, 79, 214, 0.78) 55%, rgba(91, 127, 255, 0.66) 100%);
  backdrop-filter: blur(18px) saturate(160%);
  -webkit-backdrop-filter: blur(18px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 18px 44px rgba(47, 107, 255, 0.30);
}

.hero-text h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.hero-text p {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 16px;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.hero-tags span {
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(4px);
}

.hero-btn {
  border: none;
  padding: 10px 24px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: #1e4fd6;
  background: #fff;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.16);
  transition: transform 0.15s ease;
}

.hero-btn:hover {
  transform: translateY(-2px);
}

.hero-btn.disabled,
.hero-btn:disabled {
  cursor: not-allowed;
  color: rgba(30, 79, 214, 0.55);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: none;
}

.hero-btn.disabled:hover,
.hero-btn:disabled:hover {
  transform: none;
}

.hero-art {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
}

.hero-emoji {
  font-size: 72px;
  line-height: 1;
  filter: drop-shadow(0 8px 16px rgba(0, 0, 0, 0.2));
}

.hero-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.hero-store {
  font-size: 15px;
  font-weight: 600;
}

.hero-status {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(63, 157, 99, 0.95);
}

.hero-status.off {
  background: rgba(213, 72, 95, 0.95);
}

.hero-addr {
  font-size: 12px;
  opacity: 0.85;
}

/* 分类导航 */
.section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 4px 2px 12px;
}

.section-title span {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}

.section-title small {
  font-size: 12px;
  color: var(--muted);
}

.cat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.cat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(14px) saturate(150%);
  -webkit-backdrop-filter: blur(14px) saturate(150%);
  box-shadow: 0 6px 18px rgba(47, 107, 255, 0.06);
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}

.cat-card:hover {
  border-color: var(--accent-light);
  background: rgba(255, 255, 255, 0.75);
  box-shadow: 0 8px 22px rgba(47, 107, 255, 0.12);
}

.cat-card.active {
  border-color: var(--accent);
  background: rgba(47, 107, 255, 0.88);
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.28);
}

.cat-icon {
  font-size: 26px;
  line-height: 1;
}

.cat-name {
  font-size: 13px;
  color: var(--ink-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.cat-card.active .cat-name {
  color: #fff;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.search {
  width: 260px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.product-card {
  overflow: hidden;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(12px) saturate(140%);
  -webkit-backdrop-filter: blur(12px) saturate(140%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 6px 18px rgba(47, 107, 255, 0.07);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
}

.cover {
  position: relative;
  height: 150px;
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
  font-size: 56px;
}

.cover-tag {
  position: absolute;
  top: 10px;
  left: 10px;
}

/* 售罄：封面灰化 + 中间标签，右下角不再显示加购按钮 */
.product-card.sold {
  cursor: pointer;
}

.product-card.sold .cover img {
  filter: grayscale(100%);
  opacity: 0.55;
}

.sold-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #fff;
  background: rgba(26, 43, 74, 0.35);
}

.sold-badge {
  font-size: 12px;
  color: var(--muted);
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(26, 43, 74, 0.06);
}

.p-body {
  padding: 12px 14px 14px;
}

.p-name {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.p-sub {
  margin-top: 3px;
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.p-rating {
  color: #e8960f;
  font-weight: 600;
}

.p-rating i {
  font-style: normal;
  margin-right: 1px;
}

.p-rating em {
  font-style: normal;
  font-weight: 400;
  font-size: 11px;
  color: var(--muted);
  margin-left: 2px;
}

.p-no-rating {
  color: #b6c0d4;
}

.p-sub-sep {
  margin: 0 4px;
  color: #c8d2e6;
}

.p-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.p-price {
  font-size: 17px;
  font-weight: 700;
  color: var(--accent-deep);
}

/* 右下角步进器：0 件时只显示圆形「＋」，加购后展开「− 数量 ＋」 */
.stepper {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  border-radius: 999px;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.stepper.on {
  background: rgba(47, 107, 255, 0.08);
  border: 1px solid rgba(47, 107, 255, 0.16);
}

.step-btn {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 50%;
  font-family: inherit;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
  color: #fff;
  background: var(--accent);
  transition: transform 0.12s ease, background 0.15s ease;
}

.step-btn:hover {
  background: var(--accent-deep);
}

.step-btn:active {
  transform: scale(0.9);
}

.step-btn.minus {
  color: var(--accent-deep);
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(47, 107, 255, 0.28);
}

.step-btn.minus:hover {
  color: #fff;
  background: var(--accent);
}

.step-btn.solo {
  width: 28px;
  height: 28px;
  font-size: 18px;
  box-shadow: 0 4px 10px rgba(47, 107, 255, 0.35);
}

.step-btn.solo:hover {
  transform: translateY(-1px) scale(1.06);
}

.step-num {
  min-width: 20px;
  text-align: center;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--accent-deep);
  font-variant-numeric: tabular-nums;
  animation: num-pop 0.18s ease;
}

@keyframes num-pop {
  from {
    transform: scale(0.6);
    opacity: 0.3;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 26px 22px;
  }

  .hero-art {
    flex-direction: row;
    width: 100%;
    justify-content: flex-start;
    gap: 14px;
    text-align: left;
  }

  .hero-emoji {
    font-size: 52px;
  }

  .hero-meta {
    align-items: flex-start;
  }

  .cat-cards {
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  .cat-card {
    padding: 12px 4px;
  }

  .cat-icon {
    font-size: 22px;
  }

  .search {
    width: 100%;
  }

  .grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .cover {
    height: 120px;
  }
}
</style>
