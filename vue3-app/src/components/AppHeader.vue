<template>
  <header class="app-header">
    <div class="app-header__inner">
      <button class="app-header__brand" type="button" @click="goHome">
        <img class="app-header__logo-img" src="/travelgo-icon-clean.png" alt="TravelGo" />
        <span class="app-header__brand-text">TravelGo</span>
      </button>

      <nav class="app-header__nav" aria-label="主导航">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          class="app-header__link"
          :class="{ 'is-active': isActive(item.path) }"
          :to="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <div class="app-header__user">
        <el-dropdown trigger="click">
          <button class="app-header__account" type="button">
            <span class="app-header__avatar">
              <img v-if="userStore.userInfo?.avatarUrl" :src="userStore.userInfo.avatarUrl" alt="用户头像" />
              <span v-else>{{ avatarText }}</span>
            </span>
            <span class="app-header__account-text">
              <strong>{{ userStore.userInfo?.username || '用户' }}</strong>
              <small>{{ roleText }}</small>
            </span>
            <el-icon class="app-header__chevron"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="User" @click="router.push('/user')">个人中心</el-dropdown-item>
              <el-dropdown-item divided :icon="SwitchButton" @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ChatLineRound,
  Goods,
  House,
  Management,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const navItems = computed(() => [
  ...(!userStore.isAdmin ? [
    { label: '酒店推荐', path: '/lvyou', icon: House },
    { label: '智能客服', path: '/springai', icon: ChatLineRound }
  ] : []),
  ...(userStore.canManageProducts ? [{ label: '商品管理', path: '/merchant', icon: Goods }] : []),
  ...(userStore.isAdmin ? [{ label: '管理后台', path: '/admin', icon: Management }] : [])
])

const avatarText = computed(() => userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U')
const roleText = computed(() => {
  if (userStore.isAdmin) return '超级管理员'
  if (userStore.isMerchant) return '商家账号'
  return '普通用户'
})

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }
})

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

function goHome() {
  router.push(userStore.isAdmin ? '/admin' : '/lvyou')
}

function isActive(path) {
  if (path === '/lvyou') {
    return route.path === '/lvyou' || route.path.startsWith('/lvyou/')
  }
  return route.path === path
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid var(--border-light);
  backdrop-filter: blur(14px);
}

.app-header__inner {
  width: 100%;
  max-width: var(--layout-max);
  min-height: 64px;
  margin: 0 auto;
  padding: 0 28px;
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr) 230px;
  align-items: center;
  gap: 20px;
}

.app-header__brand {
  justify-self: start;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
}

.app-header__logo-img {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  object-fit: cover;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.12);
}

.app-header__brand-text {
  font-size: 19px;
  font-weight: 800;
  letter-spacing: 0;
}

.app-header__nav {
  justify-self: center;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #f8fafc;
}

.app-header__link {
  height: 36px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  transition: background var(--transition-fast), color var(--transition-fast), box-shadow var(--transition-fast);
}

.app-header__link:hover,
.app-header__link.is-active {
  color: var(--primary);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.app-header__user {
  justify-self: end;
  min-width: 0;
}

.app-header__account {
  max-width: 230px;
  height: 42px;
  padding: 4px 10px 4px 5px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  color: var(--text-primary);
  display: inline-grid;
  grid-template-columns: 32px minmax(0, 1fr) 14px;
  align-items: center;
  gap: 9px;
  cursor: pointer;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.app-header__account:hover {
  border-color: var(--border-medium);
  box-shadow: var(--shadow-sm);
}

.app-header__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  overflow: hidden;
}

.app-header__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-header__account-text {
  min-width: 0;
  display: grid;
  gap: 1px;
  text-align: left;
}

.app-header__account-text strong,
.app-header__account-text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-header__account-text strong {
  font-size: 13px;
  line-height: 1.1;
}

.app-header__account-text small {
  color: var(--text-muted);
  font-size: 12px;
}

.app-header__chevron {
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .app-header__inner {
    min-height: auto;
    padding: 12px 16px;
    grid-template-columns: 1fr auto;
    row-gap: 10px;
  }

  .app-header__nav {
    grid-column: 1 / -1;
    justify-self: stretch;
    justify-content: flex-start;
    overflow-x: auto;
  }

  .app-header__link {
    flex: 0 0 auto;
  }

  .app-header__account {
    width: 42px;
    grid-template-columns: 32px;
    padding: 4px;
  }

  .app-header__account-text,
  .app-header__chevron {
    display: none;
  }
}
</style>
