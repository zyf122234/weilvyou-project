import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/lvyou',
    name: 'Lvyou',
    component: () => import('@/views/lvyou.vue'),
    meta: { title: '推荐', requiresAuth: true, forbiddenRoles: ['ROLE_ADMIN'] }
  },
  {
    path: '/springai',
    name: 'SpringAi',
    component: () => import('@/views/springai.vue'),
    meta: { title: '智能客服', requiresAuth: true, forbiddenRoles: ['ROLE_ADMIN'] }
  },
  {
    path: '/lvyou/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product-detail.vue'),
    meta: { title: '商品详情', requiresAuth: true, forbiddenRoles: ['ROLE_ADMIN'] }
  },
  {
    path: '/merchant',
    name: 'MerchantEditor',
    component: () => import('@/views/merchant-editor.vue'),
    meta: { title: '商家编辑', requiresAuth: true, requiredRoles: ['ROLE_MERCHANT', 'ROLE_ADMIN'] }
  },
  {
    path: '/admin',
    name: 'AdminManage',
    component: () => import('@/views/admin-manage.vue'),
    meta: { title: '超级管理员', requiresAuth: true, requiredRoles: ['ROLE_ADMIN'] }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('@/views/user.vue'),
    meta: { title: '用户中心', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title || '旅游平台'
  const userStore = useUserStore()

  // 如果路由需要认证但用户没有 token，直接跳转到登录页
  if (to.meta.requiresAuth && !userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 如果路由需要认证但用户信息未加载，尝试获取用户信息
  if (to.meta.requiresAuth && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      userStore.clearUserState()
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 如果路由禁止某些角色访问但用户有这些角色，显示提示并跳转到合适页面
  const forbiddenRoles = to.meta.forbiddenRoles || []
  if (forbiddenRoles.length > 0) {
    const isForbidden = forbiddenRoles.some(role => userStore.roles.includes(role))
    if (isForbidden) {
      ElMessage.warning('超级管理员不能访问该页面')
      next({ path: '/admin' })
      return
    }
  }

  // 如果路由需要特定角色但用户没有，显示提示并跳转到合适页面
  const requiredRoles = to.meta.requiredRoles || []
  if (requiredRoles.length > 0) {
    const hasPermission = requiredRoles.some(role => userStore.roles.includes(role))
    if (!hasPermission) {
      ElMessage.warning('当前账号没有访问该页面的权限')
      next({ path: userStore.isAdmin ? '/admin' : '/lvyou' })
      return
    }
  }

  next()
})

export default router
