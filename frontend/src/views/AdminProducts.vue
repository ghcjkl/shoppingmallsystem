<template>
  <div class="admin-products">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-select v-model="query.productType" placeholder="全部类型" clearable style="width: 130px" @change="reload">
        <el-option label="标品零食" :value="1" />
        <el-option label="现制饮品" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="reload">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索商品名称"
        clearable
        style="width: 200px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-button type="primary" @click="reload">查询</el-button>
      <div class="spacer" />
      <el-button @click="openCats">🗂 分类管理</el-button>
      <el-button type="primary" @click="openForm(null)">＋ 添加商品</el-button>
    </div>

    <!-- 商品表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="products" stripe>
        <el-table-column label="图片" width="72">
          <template #default="{ row }">
            <el-image
              v-if="row.coverImg"
              :src="row.coverImg"
              fit="cover"
              style="width: 48px; height: 48px; border-radius: 8px"
            />
            <div v-else class="thumb-empty">{{ row.productType === 2 ? '🧋' : '🍿' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.productType === 2 ? 'warning' : 'info'" round>
              {{ row.productType === 2 ? '现制' : '标品' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="基准价" width="100">
          <template #default="{ row }">¥{{ fmtMoney(row.basePrice) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" round>
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row.id)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > query.pageSize">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="query.pageSize"
          :current-page="query.pageNum"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹层 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? `编辑商品 #${editingId}` : '添加商品'"
      width="680px"
      top="5vh"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="90px" label-position="left">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="如 珍珠奶茶" maxlength="128" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="商品类型" required>
            <el-radio-group v-model="form.productType">
              <el-radio-button :value="1">标品零食</el-radio-button>
              <el-radio-button :value="2">现制饮品</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">上架</el-radio-button>
              <el-radio-button :value="0">下架</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" clearable placeholder="未分类" style="width: 100%">
              <el-option
                v-for="c in categories"
                :key="c.id"
                :label="`${c.name}（${c.productType === 1 ? '标品' : '现制'}）`"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="基准价" required>
            <el-input-number v-model="form.basePrice" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </div>

        <el-form-item label="商品图片">
          <div class="upload-row">
            <el-upload
              :show-file-list="false"
              :http-request="doUpload"
              accept="image/*"
            >
              <el-image
                v-if="form.coverImg"
                :src="form.coverImg"
                fit="cover"
                class="cover-preview"
              />
              <div v-else class="cover-uploader">＋ 上传图片</div>
            </el-upload>
            <el-input v-model="form.coverImg" placeholder="图片地址（上传后自动回填）" />
          </div>
        </el-form-item>

        <el-form-item label="商品详情">
          <el-input v-model="form.detail" type="textarea" :rows="2" placeholder="口味、卖点等" />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="净含量">
            <el-input v-model="form.netContent" placeholder="如 75g" />
          </el-form-item>
          <el-form-item label="保质期">
            <el-input v-model="form.shelfLife" placeholder="如 9个月" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="过敏原">
            <el-input v-model="form.allergen" placeholder="如 牛奶、坚果" />
          </el-form-item>
          <el-form-item label="出餐分钟">
            <el-input-number v-model="form.estMakeMinutes" :min="0" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="热量(千卡)">
            <el-input-number v-model="form.calories" :min="0" style="width: 100%" />
          </el-form-item>
          <el-form-item label="糖分提示">
            <el-input v-model="form.sugarNote" placeholder="如 甜度可选" />
          </el-form-item>
        </div>

        <el-form-item v-if="!editingId" label="默认库存">
          <el-input-number v-model="form.defaultStock" :min="0" style="width: 180px" />
          <span class="tip">无规格时按此库存自动建「默认」规格</span>
        </el-form-item>

        <div class="sku-head">
          <b>规格 / SKU</b>
          <el-button size="small" @click="addSkuRow">＋ 添加规格行</el-button>
        </div>
        <div v-for="(sku, idx) in form.skus" :key="idx" class="sku-row">
          <el-input v-model="sku.skuCode" placeholder="规格名，如 中杯" style="width: 130px" />
          <el-input-number v-model="sku.price" :min="0" :precision="2" placeholder="售价" style="width: 120px" />
          <el-input-number v-model="sku.stock" :min="0" placeholder="库存" style="width: 110px" />
          <el-switch v-model="sku.status" :active-value="1" :inactive-value="0" inline-prompt
                     active-text="启" inactive-text="禁" style="margin: 0 8px" />
          <el-button size="small" type="danger" circle @click="form.skus.splice(idx, 1)">✕</el-button>
        </div>
        <p class="tip">不填规格时，系统自动建一条「默认」规格</p>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类管理弹层 -->
    <el-dialog v-model="catVisible" title="分类管理" width="640px" :close-on-click-modal="false">
      <div class="cat-bar">
        <el-radio-group v-model="catType" @change="loadCats">
          <el-radio-button :value="1">标品零食</el-radio-button>
          <el-radio-button :value="2">现制饮品</el-radio-button>
        </el-radio-group>
        <div class="cat-add">
          <el-input
            v-model="newCatName"
            placeholder="新分类名称，如 冰淇淋"
            maxlength="32"
            clearable
            @keyup.enter="addCat"
          />
          <el-button type="primary" :loading="catSaving" @click="addCat">新增</el-button>
        </div>
      </div>

      <el-table v-loading="catLoading" :data="catList" size="small" max-height="360">
        <el-table-column label="分类名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" maxlength="32" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="{ row }">
            <el-input-number
              v-model="row.sort"
              :min="0"
              size="small"
              controls-position="right"
              style="width: 82px"
            />
          </template>
        </el-table-column>
        <el-table-column label="商品数" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'cat-used': row.productCount > 0 }">{{ row.productCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="132" align="right">
          <template #default="{ row }">
            <el-button size="small" @click="saveCat(row)">保存</el-button>
            <el-button size="small" type="danger" @click="removeCat(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p class="tip cat-tip">
        停用的分类不会出现在顾客端；分类下还有商品时不能删除，需先把商品改到别的分类或删除商品。
      </p>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminProductPage, getAdminProduct, createProduct, updateProduct,
  updateProductStatus, deleteProduct, uploadProductImage
} from '../api/admin'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '../api/product'
import { fmtMoney } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const formVisible = ref(false)
const editingId = ref(null)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', productType: null, status: null })
const form = reactive(emptyForm())

// 分类管理
const catVisible = ref(false)
const catType = ref(1)
const catList = ref([])
const catLoading = ref(false)
const catSaving = ref(false)
const newCatName = ref('')

function emptyForm() {
  return {
    name: '', productType: 1, status: 1, categoryId: null, basePrice: null,
    coverImg: '', detail: '', netContent: '', shelfLife: '', allergen: '',
    estMakeMinutes: null, calories: null, sugarNote: '', defaultStock: 0, skus: []
  }
}

const categoryName = (id) => {
  const c = categories.value.find((x) => x.id === id)
  return c ? c.name : '-'
}

async function load() {
  loading.value = true
  try {
    const page = await getAdminProductPage({
      pageNum: query.pageNum, pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      productType: query.productType ?? undefined,
      status: query.status ?? undefined
    })
    products.value = page.records
    total.value = page.total
  } catch (e) {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function onPageChange(p) {
  query.pageNum = p
  load()
}

async function openForm(id) {
  editingId.value = id
  Object.assign(form, emptyForm())
  if (id) {
    try {
      const d = await getAdminProduct(id)
      Object.assign(form, {
        name: d.name, productType: d.productType, status: d.status,
        categoryId: d.categoryId, basePrice: d.basePrice, coverImg: d.coverImg || '',
        detail: d.detail || '', netContent: d.netContent || '', shelfLife: d.shelfLife || '',
        allergen: d.allergen || '', estMakeMinutes: d.estMakeMinutes, calories: d.calories,
        sugarNote: d.sugarNote || '', defaultStock: 0,
        skus: (d.skus || []).map((s) => ({ id: s.id, skuCode: s.skuCode, price: s.price, stock: s.stock, status: s.status }))
      })
    } catch (e) {
      return
    }
  }
  formVisible.value = true
}

function addSkuRow() {
  form.skus.push({ id: null, skuCode: '', price: null, stock: 0, status: 1 })
}

async function doUpload({ file, onSuccess, onError }) {
  try {
    const result = await uploadProductImage(file)
    form.coverImg = result.url
    onSuccess(result)
    ElMessage.success('图片已上传')
  } catch (e) {
    onError(e)
  }
}

async function save() {
  if (!form.name?.trim()) return ElMessage.warning('请填写商品名称')
  if (form.basePrice === null || form.basePrice === undefined) return ElMessage.warning('请填写基准价')
  saving.value = true
  const body = {
    name: form.name.trim(),
    productType: form.productType,
    status: form.status,
    categoryId: form.categoryId || null,
    basePrice: form.basePrice,
    coverImg: form.coverImg || null,
    detail: form.detail || null,
    netContent: form.netContent || null,
    shelfLife: form.shelfLife || null,
    allergen: form.allergen || null,
    estMakeMinutes: form.estMakeMinutes,
    calories: form.calories,
    sugarNote: form.sugarNote || null,
    defaultStock: form.defaultStock,
    skus: form.skus.map((s) => ({
      id: s.id || null,
      skuCode: s.skuCode || null,
      price: s.price,
      stock: s.stock,
      status: s.status
    }))
  }
  try {
    if (editingId.value) {
      await updateProduct(editingId.value, body)
      ElMessage.success('商品已更新')
    } else {
      await createProduct(body)
      ElMessage.success('商品已添加')
    }
    formVisible.value = false
    load()
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {  const next = row.status === 1 ? 0 : 1
  try {
    await updateProductStatus(row.id, next)
    ElMessage.success(next === 1 ? '已上架' : '已下架')
    load()
  } catch (e) {
    /* 已提示 */
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除后商品「${row.name}」将不可再下单，确认删除？`, '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteProduct(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    /* 已提示 */
  }
}

// ===== 分类管理 =====
async function refreshCategoryOptions() {
  try {
    categories.value = (await getCategoryList()) || []
  } catch (e) {
    /* 已提示 */
  }
}

async function loadCats() {
  catLoading.value = true
  try {
    catList.value = (await getCategoryList(catType.value, true)) || []
  } catch (e) {
    catList.value = []
  } finally {
    catLoading.value = false
  }
}

function openCats() {
  catType.value = query.productType || 1
  newCatName.value = ''
  catVisible.value = true
  loadCats()
}

async function addCat() {
  const name = newCatName.value.trim()
  if (!name) return ElMessage.warning('请填写分类名称')
  if (catList.value.some((c) => c.name === name)) return ElMessage.warning('该分类已存在')
  catSaving.value = true
  try {
    await createCategory({
      name,
      productType: catType.value,
      status: 1,
      sort: catList.value.length
    })
    newCatName.value = ''
    ElMessage.success('分类已新增')
    await loadCats()
    await refreshCategoryOptions()
  } catch (e) {
    /* 已提示 */
  } finally {
    catSaving.value = false
  }
}

async function saveCat(row) {
  if (!row.name?.trim()) return ElMessage.warning('分类名称不能为空')
  try {
    await updateCategory({
      id: row.id,
      name: row.name.trim(),
      productType: catType.value,
      sort: row.sort ?? 0,
      status: row.status
    })
    ElMessage.success('分类已保存')
    await loadCats()
    await refreshCategoryOptions()
    load()
  } catch (e) {
    /* 已提示 */
  }
}

async function removeCat(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${row.name}」？${row.productCount > 0 ? `该分类下还有 ${row.productCount} 个商品，需先移出。` : ''}`,
      '提示',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteCategory(row.id)
    ElMessage.success('分类已删除')
    await loadCats()
    await refreshCategoryOptions()
    load()
  } catch (e) {
    /* 已提示 */
  }
}

onMounted(async () => {
  try {
    categories.value = await getCategoryList()
  } catch (e) {
    categories.value = []
  }
  load()
})
</script>

<style scoped>
.admin-products { display: flex; flex-direction: column; gap: 14px; }
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.spacer { flex: 1; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.thumb-empty {
  width: 48px; height: 48px; border-radius: 8px;
  display: grid; place-items: center; font-size: 20px;
  background: var(--fill, #f4f7ff);
}
.form-row { display: flex; gap: 16px; }
.form-row .el-form-item { flex: 1; }
.upload-row { display: flex; align-items: center; gap: 12px; width: 100%; }
.cover-preview { width: 76px; height: 76px; border-radius: 8px; flex-shrink: 0; }
.cover-uploader {
  width: 76px; height: 76px; border-radius: 8px; flex-shrink: 0;
  display: grid; place-items: center; font-size: 12px; color: var(--muted);
  border: 1px dashed var(--line); cursor: pointer; background: var(--fill, #f4f7ff);
}
.cover-uploader:hover { color: var(--accent); border-color: var(--accent); }
.sku-head { display: flex; align-items: center; justify-content: space-between; margin: 8px 0; }
.sku-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.tip { font-size: 12px; color: var(--muted); margin-left: 8px; }

/* 分类管理弹层 */
.cat-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.cat-add {
  flex: 1;
  min-width: 220px;
  display: flex;
  gap: 8px;
}

.cat-used {
  color: var(--accent-deep);
  font-weight: 700;
}

.cat-tip {
  margin: 10px 0 0;
  line-height: 1.6;
}
</style>
