<template>
  <div class="auth-page">
    <main class="auth-shell">
      <section class="auth-brand">
        <button class="auth-logo" type="button" @click="router.push('/login')">
          <img src="/travelgo-icon-clean.png" alt="TravelGo" />
          <span>TravelGo</span>
        </button>
        <div class="auth-intro">
          <span class="auth-kicker">加入旅程</span>
          <h1>把期待收藏进每一次出发</h1>
          <p>创建账号后，记录偏好、查看行程，让旅行从容开始。</p>
        </div>
        <div class="auth-steps">
          <div>
            <strong>1</strong>
            <span>创建账号</span>
          </div>
          <div>
            <strong>2</strong>
            <span>选择目的地</span>
          </div>
          <div>
            <strong>3</strong>
            <span>准备出发</span>
          </div>
        </div>
      </section>

      <section class="auth-card animate-fade-in-up">
        <div class="auth-card__header">
          <span class="auth-card__eyebrow">账号注册</span>
          <h2>创建新账号</h2>
          <p>请填写基础账号信息完成注册</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="handleRegister">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" :prefix-icon="Message" clearable />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password />
          </el-form-item>
          <el-button type="primary" :loading="loading" class="auth-submit" @click="handleRegister">
            注册
          </el-button>
        </el-form>

        <div class="auth-card__footer">
          <span>已有账号？</span>
          <router-link to="/login">返回登录</router-link>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Message, User } from '@element-plus/icons-vue'
import { register } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const form = ref({ username: '', email: '', password: '', confirmPassword: '' })
const loading = ref(false)

const validateConfirm = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register({
      username: form.value.username,
      email: form.value.email,
      password: form.value.password
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 36px 20px;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.08), transparent 34%),
    linear-gradient(315deg, rgba(37, 99, 235, 0.08), transparent 32%),
    var(--bg-body);
}

.auth-shell {
  width: min(1040px, 100%);
  min-height: 640px;
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
    linear-gradient(135deg, rgba(15, 23, 42, 0.94), rgba(17, 94, 89, 0.9)),
    radial-gradient(circle at 80% 20%, rgba(45, 212, 191, 0.3), transparent 28%),
    radial-gradient(circle at 18% 78%, rgba(96, 165, 250, 0.24), transparent 30%);
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
}

.auth-intro p {
  max-width: 520px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
  line-height: 1.8;
}

.auth-steps {
  display: grid;
  gap: 12px;
}

.auth-steps div {
  display: grid;
  grid-template-columns: 34px 1fr;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.82);
}

.auth-steps strong {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

.auth-card {
  min-width: 0;
  padding: 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-card__header {
  margin-bottom: 24px;
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
  margin-top: 22px;
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

  .auth-card {
    padding: 34px 24px;
  }
}
</style>
