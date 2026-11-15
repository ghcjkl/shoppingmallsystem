<template>
  <div class="admin-store" v-loading="loading">
    <!-- 当前门店预览 -->
    <div class="preview">
      <div class="pv-mark">🏪</div>
      <div class="pv-main">
        <div class="pv-name">
          {{ preview.name || '未命名门店' }}
          <el-tag size="small" :type="preview.status === 1 ? 'success' : 'danger'" round>
            {{ preview.status === 1 ? '营业中' : '暂停营业' }}
          </el-tag>
        </div>
        <div class="pv-line">📍 {{ preview.address || '未填写地址' }}</div>
        <div class="pv-line">📞 {{ preview.phone || '未填写电话' }}</div>
        <div class="pv-line">🕒 {{ preview.businessHours || '未设置营业时间' }}</div>
      </div>
      <div class="pv-side">
        <span class="pv-label">顾客端展示</span>
        <span class="pv-tip">首页横幅取这里的名称与状态</span>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-head">
          <b>门店信息</b>
          <span class="tip">修改后保存即生效，顾客端刷新可见</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="store-form">
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="如 食光铺子·旗舰店" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="32" placeholder="如 0571-88886666" />
        </el-form-item>
        <el-form-item label="门店地址" prop="address">
          <el-input v-model="form.address" maxlength="255" placeholder="如 杭州市西湖区文三路 100 号 1 层" />
        </el-form-item>
        <el-form-item label="营业时间">
          <div class="hours">
            <el-time-select
              v-model="form.startTime"
              start="06:00"
              step="00:30"
              end="23:30"
              placeholder="开始"
              style="width: 130px"
            />
            <span class="dash">至</span>
            <el-time-select
              v-model="form.endTime"
              start="06:00"
              step="00:30"
              end="23:30"
              placeholder="结束"
              style="width: 130px"
            />
          </div>
        </el-form-item>
        <el-form-item label="营业状态">
          <div class="status-row">
            <el-switch
              v-model="form.open"
              inline-prompt
              active-text="营业"
              inactive-text="停业"
              style="--el-switch-on-color: #3f9d63; --el-switch-off-color: #d5485f"
            />
            <span class="status-tip" :class="{ off: !form.open }">
              {{ form.open ? '正常接单：顾客可以下单' : '暂停营业：顾客下单会被拒绝' }}
            </span>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">保存设置</el-button>
          <el-button @click="reset">还原</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getStore, updateStore } from '../api/store'
import { useAppStore } from '../stores/app'

const app = useAppStore()

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const store = ref(null)

const form = reactive({
  name: '',
  phone: '',
  address: '',
  startTime: '09:00',
  endTime: '22:00',
  open: true
})

const preview = computed(() => store.value || {})

const rules = {
  name: [{ required: true, message: '请填写门店名称', trigger: 'blur' }]
}

// "09:00-22:00" -> 起止时间
const parseHours = (hours) => {
  const m = /^\s*(\d{1,2}:\d{2})\s*[-~至]\s*(\d{1,2}:\d{2})\s*$/.exec(hours || '')
  if (m) return { startTime: m[1].padStart(5, '0'), endTime: m[2].padStart(5, '0') }
  return { startTime: '09:00', endTime: '22:00' }
}

const fill = (s) => {
  const h = parseHours(s?.businessHours)
  form.name = s?.name || ''
  form.phone = s?.phone || ''
  form.address = s?.address || ''
  form.startTime = h.startTime
  form.endTime = h.endTime
  form.open = (s?.status ?? 1) === 1
}

const load = async () => {
  loading.value = true
  try {
    store.value = await getStore()
    fill(store.value)
  } catch (e) {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

const reset = () => {
  fill(store.value)
  formRef.value?.clearValidate()
  ElMessage.info('已还原为当前保存的门店信息')
}

const submit = async () => {
  await formRef.value.validate()
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请选择完整的营业时间')
    return
  }
  if (form.startTime >= form.endTime) {
    ElMessage.warning('营业结束时间需要晚于开始时间')
    return
  }
  saving.value = true
  try {
    const saved = await updateStore({
      name: form.name.trim(),
      phone: form.phone.trim(),
      address: form.address.trim(),
      businessHours: `${form.startTime}-${form.endTime}`,
      status: form.open ? 1 : 0
    })
    // 老版本后端只返回空 data，兜底再拉一次
    store.value = saved && saved.id ? saved : await getStore()
    fill(store.value)
    // 顶栏门店名、首页横幅同步刷新
    await app.loadStore()
    ElMessage.success('门店设置已保存')
  } catch (e) {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-store {
  max-width: 860px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.preview {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px 24px;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(120deg, rgba(47, 107, 255, 0.92) 0%, rgba(30, 79, 214, 0.95) 60%, rgba(91, 127, 255, 0.9) 100%);
  box-shadow: 0 14px 32px rgba(47, 107, 255, 0.28);
}

.pv-mark {
  font-size: 42px;
  line-height: 1;
}

.pv-main {
  flex: 1;
  min-width: 0;
}

.pv-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 19px;
  font-weight: 700;
  margin-bottom: 6px;
}

.pv-line {
  font-size: 13px;
  opacity: 0.92;
  margin-top: 2px;
}

.pv-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  text-align: right;
}

.pv-label {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
}

.pv-tip {
  font-size: 11.5px;
  opacity: 0.8;
}

.card-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.tip {
  font-size: 12px;
  color: var(--muted);
}

.store-form {
  max-width: 620px;
}

.hours {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dash {
  font-size: 13px;
  color: var(--muted);
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-tip {
  font-size: 12.5px;
  color: #3f9d63;
}

.status-tip.off {
  color: #d5485f;
}
</style>
