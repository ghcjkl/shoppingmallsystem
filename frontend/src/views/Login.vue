<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="brand">
        <div class="brand-mark">🧋</div>
        <div class="brand-text">
          <strong>食光铺子</strong>
          <span>零食 · 奶茶 · 咖啡</span>
        </div>
      </div>

      <p class="subtitle">{{ mode === 'login' ? '管理员与顾客统一登录' : '新用户注册' }}</p>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onSubmit">
        <el-form-item v-if="mode === 'register'" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" clearable>
            <template #prefix>📱</template>
          </el-input>
        </el-form-item>
        <el-form-item v-else prop="username">
          <el-input v-model="form.username" placeholder="用户名 / 手机号" clearable>
            <template #prefix>👤</template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（可选）" clearable>
            <template #prefix>🙂</template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password>
            <template #prefix>🔒</template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="mode === 'register'" prop="confirm">
          <el-input v-model="form.confirm" type="password" placeholder="确认密码" show-password>
            <template #prefix>🔒</template>
          </el-input>
        </el-form-item>
        <el-button class="submit" type="primary" size="large" :loading="loading" @click="onSubmit">
          {{ mode === 'login' ? '登 录' : '注 册' }}
        </el-button>
      </el-form>

      <div class="hint" v-if="mode === 'login'">
        <div>管理员：admin / 123456（进入管理台）</div>
        <div>顾客：13800000000 / 123456（进入商城）</div>
      </div>
      <div class="switch">
        <template v-if="mode === 'login'">
          还没有账号？<button type="button" class="switch-btn" @click="switchMode('register')">去注册</button>
        </template>
        <template v-else>
          已有账号？<button type="button" class="switch-btn" @click="switchMode('login')">去登录</button>
        </template>
      </div>
      <button class="back" type="button" @click="router.push('/home')">← 先逛逛商城</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const mode = ref('login') // 'login' | 'register'
const form = reactive({ username: '', phone: '', nickname: '', password: '', confirm: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名或手机号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const switchMode = (m) => {
  mode.value = m
  form.username = ''
  form.phone = ''
  form.nickname = ''
  form.password = ''
  form.confirm = ''
  formRef.value?.clearValidate()
}

const onSubmit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    let data
    if (mode.value === 'login') {
      data = await auth.login(form.username, form.password)
      ElMessage.success('登录成功')
      const target = data.role === 1 ? '/queue' : '/home'
      router.push(route.query.redirect || target)
    } else {
      data = await auth.register({ phone: form.phone, password: form.password, nickname: form.nickname || undefined })
      ElMessage.success('注册成功，欢迎加入')
      // 注册的顾客账号直接进入商城首页
      router.push('/home')
    }
  } catch (e) {
    /* 错误信息已由拦截器提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(60% 50% at 12% 8%, rgba(47, 107, 255, 0.18), transparent 60%),
    radial-gradient(55% 48% at 100% 100%, rgba(120, 80, 40, 0.14), transparent 60%),
    var(--app-bg);
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: 34px 30px 26px;
  border-radius: 24px;
  background: var(--surface);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 56px rgba(26, 43, 74, 0.14);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  margin-bottom: 20px;
}

.brand-mark {
  width: 46px;
  height: 46px;
  border-radius: 15px;
  display: grid;
  place-items: center;
  font-size: 24px;
  background: var(--accent);
  box-shadow: 0 8px 20px rgba(47, 107, 255, 0.32);
}

.brand-text strong {
  display: block;
  font-size: 19px;
  color: var(--ink);
}

.brand-text span {
  font-size: 12px;
  color: var(--muted);
}

.subtitle {
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 20px;
}

.submit {
  width: 100%;
  border-radius: 12px;
  font-size: 15px;
  letter-spacing: 4px;
}

.hint {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: var(--muted);
}

.switch {
  margin-top: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--muted);
}

.switch-btn {
  border: none;
  background: transparent;
  color: var(--accent-deep);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.back {
  display: block;
  margin: 14px auto 0;
  border: none;
  background: transparent;
  color: var(--accent-deep);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}
</style>
