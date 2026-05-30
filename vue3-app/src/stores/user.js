import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo, uploadAvatar as uploadAvatarApi, updateUserInfo as updateUserInfoApi, rechargeBalance as rechargeBalanceApi, applyForMerchant as applyForMerchantApi, getMerchantApplication as getMerchantApplicationApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 初始化 token 和用户信息
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const merchantApplication = ref(null)
  const roles = computed(() => userInfo.value?.roles || [])
  const isMerchant = computed(() => roles.value.includes('ROLE_MERCHANT'))
  const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'))
  const canManageProducts = computed(() => isMerchant.value || isAdmin.value)

  // 登录方法
  async function login(credentials) {
    const res = await loginApi(credentials)
    const loginData = res.data || res
    token.value = loginData.token || ''
    localStorage.setItem('token', token.value)
    return res
  }

  
  // 获取用户信息方法
  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data || res
    return userInfo.value
  }

  // 头像上传方法
  async function uploadAvatar(file) {
    const res = await uploadAvatarApi(file)
    const avatarUrl = res.data || res
    // 更新用户信息中的头像URL
    userInfo.value = {
      ...(userInfo.value || {}),
      avatarUrl
    }
    return avatarUrl
  }

  async function updateUserInfo(data) {
    const res = await updateUserInfoApi(data)
    userInfo.value = res.data || res
    return userInfo.value
  }

  async function rechargeBalance(amount) {
    const res = await rechargeBalanceApi({ amount })
    userInfo.value = res.data || res
    return userInfo.value
  }

  async function applyForMerchant() {
    const res = await applyForMerchantApi()
    merchantApplication.value = res.data || res
    return merchantApplication.value
  }

  async function fetchMerchantApplication() {
    const res = await getMerchantApplicationApi()
    merchantApplication.value = res.data || res || null
    if (merchantApplication.value?.token) {
      token.value = merchantApplication.value.token
      localStorage.setItem('token', token.value)
      await fetchUserInfo()
    }
    return merchantApplication.value
  }

  // 清除用户状态方法
  function clearUserState() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    sessionStorage.removeItem('aiConversationId')
  }

  // 退出登录方法
  async function logout() {
    try {
      if (token.value) {
        await invalidateCurrentAiConversation(token.value).catch(() => {})
        await logoutApi()
      }
    } catch (e) {
      console.warn('退出登录接口调用失败，已清理本地登录状态')
    } finally {
      clearUserState()
    }
  }

  return {
    token,
    userInfo,
    merchantApplication,
    roles,
    isMerchant,
    isAdmin,
    canManageProducts,
    login,
    fetchUserInfo,
    uploadAvatar,
    updateUserInfo,
    rechargeBalance,
    applyForMerchant,
    fetchMerchantApplication,
    logout,
    clearUserState
  }
})

function invalidateCurrentAiConversation(token) {
  return fetch('/api/ai/customer-service/session/invalidate-current', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`
    }
  })
}
