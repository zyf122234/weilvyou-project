<template>
  <div class="auth-page">
    <main class="auth-shell">
      <section class="auth-brand">
        <button class="auth-logo" type="button" @click="router.push('/login')">
          <img src="/travelgo-icon-clean.png" alt="TravelGo" />
          <span>TravelGo</span>
        </button>
        <div class="auth-intro">
          <span class="auth-kicker">开启旅程</span>
          <h1>发现下一段理想出行</h1>
          <p>山海、城市与人间烟火，都在下一次出发里慢慢展开。</p>
        </div>
        <div class="auth-metrics">
          <div>
            <strong>灵感</strong>
            <span>探索目的地</span>
          </div>
          <div>
            <strong>住宿</strong>
            <span>精选休憩地</span>
          </div>
          <div>
            <strong>出发</strong>
            <span>轻松安排行程</span>
          </div>
        </div>
      </section>

      <section class="auth-card animate-fade-in-up">
        <div class="auth-card__header">
          <span class="auth-card__eyebrow">账号登录</span>
          <h2>登录控制台</h2>
          <p>请输入账号信息完成登录</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="auth-submit" @click="handleLogin">
            登录
          </el-button>
        </el-form>

        <div class="auth-card__footer">
          <span>还没有账号？</span>
          <router-link to="/register">创建账号</router-link>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const form = ref({ username: '', password: '' })
const loading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form.value)
    await userStore.fetchUserInfo()
    ElMessage.success('登录成功')
    router.push(resolveLoginRedirect())
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function resolveLoginRedirect() {
  const redirect = route.query.redirect
  if (userStore.isAdmin) {
    return isAdminForbiddenRedirect(redirect) ? '/admin' : (redirect || '/admin')
  }
  return redirect || '/lvyou'
}

function isAdminForbiddenRedirect(redirect) {
  if (!redirect || typeof redirect !== 'string') {
    return false
  }
  return redirect === '/lvyou'
    || redirect.startsWith('/lvyou/')
    || redirect === '/springai'
    || redirect.startsWith('/springai/')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 36px 20px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), transparent 34%),
    linear-gradient(315deg, rgba(15, 118, 110, 0.08), transparent 32%),
    var(--bg-body);
}

.auth-shell {
  width: min(1040px, 100%);
  min-height: 590px;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) 430px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: #ffffff;
  box-shadow: var(--shadow-lg);
}

.auth-brand {
  min-width: 0;
  padding: 44px;
  display: grid;
  align-content: space-between;
  gap: 36px;
  background:
    linear-gradient(135deg, rgba(23, 32, 51, 0.96), rgba(30, 41, 59, 0.94)),
    radial-gradient(circle at 82% 18%, rgba(96, 165, 250, 0.32), transparent 28%),
    radial-gradient(circle at 18% 78%, rgba(20, 184, 166, 0.28), transparent 30%);
  color: #ffffff;
}

.auth-logo {
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: 800;
  font-size: 20px;
}

.auth-logo img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.auth-kicker {
  display: inline-flex;
  height: 28px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.86);
  font-size: 13px;
  font-weight: 700;
}

.auth-intro h1 {
  max-width: 540px;
  margin: 18px 0 14px;
  font-size: 38px;
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: 0;
}

.auth-intro p {
  max-width: 520px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
  line-height: 1.8;
}

.auth-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.auth-metrics div {
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
}

.auth-metrics strong,
.auth-metrics span {
  display: block;
}

.auth-metrics strong {
  margin-bottom: 6px;
  font-size: 20px;
}

.auth-metrics span {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
}

.auth-card {
  min-width: 0;
  padding: 54px 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-card__header {
  margin-bottom: 28px;
}

.auth-card__eyebrow {
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.auth-card__header h2 {
  margin: 8px 0;
  color: var(--text-primary);
  font-size: 28px;
  line-height: 1.2;
}

.auth-card__header p {
  color: var(--text-muted);
  font-size: 14px;
}

.auth-submit {
  width: 100%;
  height: 44px;
  margin-top: 4px;
}

.auth-card__footer {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 14px;
}

.auth-card__footer a {
  color: var(--primary);
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-brand {
    padding: 30px;
  }

  .auth-intro h1 {
    font-size: 28px;
  }

  .auth-metrics {
    grid-template-columns: 1fr;
  }

  .auth-card {
    padding: 34px 24px;
  }
}
</style>
