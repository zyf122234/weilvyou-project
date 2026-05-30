<template>
  <div class="page">
    <AppHeader />

    <main class="main">
      <div class="profile-container">
        <div class="profile-header">
          <div class="profile-bg">
            <div class="profile-pattern">
              <svg viewBox="0 0 400 120" fill="none">
                <path d="M0 80 Q50 40 100 80 Q150 40 200 80 Q250 40 300 80 Q350 40 400 80 L400 0 L0 0Z" fill="rgba(255,255,255,0.05)"/>
                <circle cx="350" cy="30" r="20" fill="rgba(255,255,255,0.08)"/>
                <circle cx="50" cy="20" r="15" fill="rgba(255,255,255,0.06)"/>
              </svg>
            </div>
          </div>
          <div class="profile-info">
            <ImageUpload
              :model-value="hasToken ? userStore.userInfo?.avatarUrl || '' : ''"
              :uploader="uploadAvatar"
              :placeholder-text="avatarText"
              :disabled="!hasToken"
              variant="avatar"
              title="更换头像"
            />
            <div class="profile-text">
              <h1 class="profile-name">{{ profileName }}</h1>
              <button
                v-if="userStore.isMerchant"
                class="profile-email profile-email--editable"
                type="button"
                @click="openEditDialog('nickname')"
              >
                {{ merchantDisplayName }}
              </button>
              <p v-else class="profile-email">{{ profileEmail }}</p>
            </div>
            <div v-if="hasProfile" class="profile-balance-card">
              <span class="profile-balance-card__eyebrow">账户余额</span>
              <strong class="profile-balance-card__value">￥{{ userBalance }}</strong>
              <span class="profile-balance-card__hint">当前可用余额</span>
              <el-button class="profile-balance-card__action" type="warning" plain :loading="rechargeLoading" @click="openRechargeDialog">
                充值
              </el-button>
            </div>
          </div>
        </div>

        <div class="profile-body" v-if="hasProfile">
          <div class="info-section">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              个人信息
            </h3>
            <div class="info-grid">
              <button class="info-item info-item--editable" type="button" @click="openEditDialog('username')">
                <span class="info-label">用户名</span>
                <span class="info-value">{{ userStore.userInfo.username }}</span>
                <span class="info-action">点击修改</span>
              </button>
              <button class="info-item info-item--editable" type="button" @click="openEditDialog('email')">
                <span class="info-label">邮箱</span>
                <span class="info-value">{{ userStore.userInfo.email || '未设置' }}</span>
                <span class="info-action">点击修改</span>
              </button>
              <div class="info-item">
                <span class="info-label">注册时间</span>
                <span class="info-value">{{ formattedCreateTime }}</span>
                <span class="info-action info-action--placeholder">占位</span>
              </div>
            </div>
          </div>

          <div class="merchant-section" :class="{ 'merchant-section--active': userStore.canManageProducts, 'merchant-section--pending': merchantApplication?.status === 0, 'merchant-section--rejected': merchantApplication?.status === 2 }">
            <div class="merchant-copy">
              <div class="merchant-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 9l1-5h16l1 5"/>
                  <path d="M5 9v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V9"/>
                  <path d="M9 20v-6h6v6"/>
                  <path d="M3 9h18"/>
                </svg>
              </div>
              <div>
                <h3 class="merchant-title">{{ merchantCardTitle }}</h3>
                <p class="merchant-desc">{{ merchantCardDesc }}</p>
                <p v-if="merchantApplication?.status === 2 && merchantApplication.reason" class="merchant-reason">
                  拒绝原因：{{ merchantApplication.reason }}
                </p>
              </div>
            </div>
            <div class="merchant-actions">
              <span class="merchant-badge" :class="{ 'merchant-badge--active': userStore.canManageProducts, 'merchant-badge--pending': merchantApplication?.status === 0, 'merchant-badge--rejected': merchantApplication?.status === 2 }">
                {{ merchantBadgeText }}
              </span>
              <el-button
                v-if="canSubmitMerchantApplication"
                type="primary"
                class="merchant-apply-btn"
                :loading="merchantApplying"
                @click="handleApplyMerchant"
              >
                {{ merchantApplication?.status === 2 ? '重新申请' : '立即申请' }}
              </el-button>
              <el-button v-else class="merchant-manage-btn" @click="router.push('/merchant')">
                管理商品
              </el-button>
            </div>
          </div>

          <div v-if="!userStore.isAdmin" class="stats-section">
            <h3 class="section-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
                <path d="M22 12A10 10 0 0 0 12 2v10z"/>
              </svg>
              我的数据
            </h3>
            <div class="stats-grid">
              <div class="stat-card stat-card--favorites">
                <div class="stat-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                  </svg>
                </div>
                <div class="stat-info">
                  <span class="stat-num">5</span>
                  <span class="stat-label">我的收藏</span>
                </div>
              </div>
              <div class="stat-card stat-card--reviews">
                <div class="stat-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                  </svg>
                </div>
                <div class="stat-info">
                  <span class="stat-num">12</span>
                  <span class="stat-label">我的评价</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="!userStore.isAdmin" class="orders-section">
            <div class="orders-title-row">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <path d="M6 2 3 6v16h18V6l-3-4Z"/>
                  <path d="M3 6h18"/>
                  <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
                我的订单
              </h3>
              <el-tooltip content="刷新订单" placement="top">
                <el-button class="refresh-btn" circle :icon="Refresh" :loading="ordersLoading" @click="fetchOrders" />
              </el-tooltip>
            </div>
            <div v-loading="ordersLoading" class="orders-content" element-loading-text="正在加载">
              <el-empty
                v-if="orders.length === 0"
                class="stable-empty"
                :class="{ 'stable-empty--hidden': ordersLoading }"
                description="暂无订单"
              />
              <div v-else class="order-list">
                <div v-for="item in orders" :key="item.id" class="order-item">
                  <div class="order-cover">
                    <img v-if="item.productCoverUrl" :src="item.productCoverUrl" :alt="item.productName" />
                    <span v-else>{{ orderCoverText(item) }}</span>
                  </div>
                  <div class="order-main">
                    <div class="order-top">
                      <strong>{{ item.productName || '旅游商品' }}</strong>
                      <el-tag :type="orderTagType(item.status)" size="small">
                        {{ item.statusText || orderStatusText(item.status) }}
                      </el-tag>
                    </div>
                    <div class="order-meta">
                      <span>订单号：{{ item.orderNo || item.id }}</span>
                      <span>日期：{{ orderDateRange(item) }}</span>
                      <span>数量：{{ item.quantity || 1 }}</span>
                    </div>
                    <div class="order-bottom">
                      <span>￥{{ formatPrice(item.totalPrice) }}</span>
                      <small>{{ formatDateTime(item.createTime) }}</small>
                    </div>
                  </div>
                  <div v-if="Number(item.status) === 0" class="order-actions">
                    <el-button
                      type="primary"
                      :loading="payingOrderId === item.id"
                      :disabled="cancelingOrderId === item.id"
                      @click="handlePayOrder(item)"
                    >
                      立即付款
                    </el-button>
                    <el-button
                      type="danger"
                      plain
                      :loading="cancelingOrderId === item.id"
                      :disabled="payingOrderId === item.id"
                      @click="handleCancelOrder(item)"
                    >
                      取消订单
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="userStore.isMerchant" class="orders-section">
            <div class="orders-title-row">
              <h3 class="section-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                  <path d="M3 9l1-5h16l1 5"/>
                  <path d="M5 9v10a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V9"/>
                  <path d="M3 9h18"/>
                  <path d="M9 20v-6h6v6"/>
                </svg>
                本店铺订单
              </h3>
              <el-tooltip content="刷新本店铺订单" placement="top">
                <el-button class="refresh-btn" circle :icon="Refresh" :loading="merchantOrdersLoading" @click="fetchMerchantOrders" />
              </el-tooltip>
            </div>
            <div class="merchant-order-filter">
              <el-input class="merchant-order-search" v-model="merchantOrderKeyword" placeholder="搜索订单号、商品、联系人或电话" clearable @keyup.enter="fetchMerchantOrders" />
              <el-select class="merchant-order-status" v-model="merchantOrderStatus" clearable placeholder="全部状态">
                <el-option label="待支付" :value="0" />
                <el-option label="已支付" :value="1" />
                <el-option label="已取消" :value="2" />
              </el-select>
              <el-button class="merchant-order-query" type="primary" @click="fetchMerchantOrders">查询</el-button>
            </div>
            <div v-loading="merchantOrdersLoading" class="orders-content" element-loading-text="正在加载">
              <el-empty
                v-if="merchantOrders.length === 0"
                class="stable-empty"
                :class="{ 'stable-empty--hidden': merchantOrdersLoading }"
                description="暂无本店铺订单"
              />
              <div v-else class="order-list">
                <div v-for="item in merchantOrders" :key="item.id" class="order-item merchant-order-item">
                  <div class="order-cover">
                    <img v-if="item.productCoverUrl" :src="item.productCoverUrl" :alt="item.productName" />
                    <span v-else>{{ orderCoverText(item) }}</span>
                  </div>
                  <div class="order-main">
                    <div class="order-top">
                      <strong>{{ item.productName || '旅游商品' }}</strong>
                      <el-tag :type="orderTagType(item.status)" size="small">
                        {{ item.statusText || orderStatusText(item.status) }}
                      </el-tag>
                    </div>
                    <div class="order-meta">
                      <span>订单号：{{ item.orderNo || item.id }}</span>
                      <span>购买用户：{{ buyerName(item) }}</span>
                      <span>日期：{{ orderDateRange(item) }}</span>
                      <span>数量：{{ item.quantity || 1 }}</span>
                    </div>
                    <div class="order-meta">
                      <span>联系人：{{ item.contactName || '-' }}</span>
                      <span>电话：{{ item.phone || '-' }}</span>
                    </div>
                    <div class="order-bottom">
                      <span>￥{{ formatPrice(item.totalPrice) }}</span>
                      <small>{{ formatDateTime(item.createTime) }}</small>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="actions-section">
            <el-button type="danger" size="large" class="logout-btn" @click="handleLogout">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                <polyline points="16 17 21 12 16 7"/>
                <line x1="21" y1="12" x2="9" y2="12"/>
              </svg>
              <span>退出登录</span>
            </el-button>
          </div>
        </div>

        <div class="profile-body" v-else-if="hasToken">
          <div class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" color="#cbd5e1">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <h3>加载中</h3>
            <p>正在获取个人信息</p>
          </div>
        </div>

        <div class="profile-body" v-else>
          <div class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" color="#cbd5e1">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
            <h3>请先登录</h3>
            <p>登录后查看个人信息</p>
            <el-button type="primary" @click="router.push('/login')">去登录</el-button>
          </div>
        </div>
      </div>
    </main>

    <el-dialog
      v-model="editDialogVisible"
      :title="editDialogTitle"
      width="420px"
      class="profile-edit-dialog"
      align-center
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-position="top">
        <el-form-item :label="editFieldLabel" prop="value">
          <el-input
            v-model="editForm.value"
            :placeholder="editInputPlaceholder"
            clearable
            @keyup.enter="submitEdit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="editLoading" @click="submitEdit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="rechargeDialogVisible"
      title="账户充值"
      width="420px"
      class="profile-edit-dialog"
      align-center
    >
      <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeRules" label-position="top">
        <el-form-item label="充值金额" prop="amount">
          <el-input
            v-model="rechargeForm.amount"
            placeholder="请输入充值金额"
            clearable
            @keyup.enter="submitRecharge"
          >
            <template #prefix>￥</template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rechargeDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="rechargeLoading" @click="submitRecharge">确认充值</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { cancelOrder as cancelOrderApi, listMerchantOrders, listMyOrders, payOrder as payOrderApi } from '@/api/order'

const userStore = useUserStore()
const router = useRouter()
const editFormRef = ref()
const editDialogVisible = ref(false)
const editLoading = ref(false)
const rechargeFormRef = ref()
const rechargeDialogVisible = ref(false)
const rechargeLoading = ref(false)
const merchantApplying = ref(false)
const editField = ref('username')
const editForm = ref({ value: '' })
const rechargeForm = ref({ amount: '' })
const orders = ref([])
const ordersTotal = ref(0)
const ordersLoading = ref(false)
const payingOrderId = ref(null)
const cancelingOrderId = ref(null)
const merchantOrders = ref([])
const merchantOrdersTotal = ref(0)
const merchantOrdersLoading = ref(false)
const merchantOrderKeyword = ref('')
const merchantOrderStatus = ref()

const hasToken = computed(() => Boolean(userStore.token))
const hasProfile = computed(() => hasToken.value && Boolean(userStore.userInfo))
const profileName = computed(() => {
  if (hasToken.value) return userStore.userInfo?.username || '用户'
  return '未登录'
})
const profileEmail = computed(() => {
  if (hasToken.value) return userStore.userInfo?.email || '未设置邮箱'
  return '请先登录'
})
const merchantApplication = computed(() => userStore.merchantApplication)
const canSubmitMerchantApplication = computed(() => {
  return hasProfile.value
    && !userStore.canManageProducts
    && merchantApplication.value?.status !== 0
})
const merchantCardTitle = computed(() => {
  if (userStore.canManageProducts) return '商户权限已开通'
  if (merchantApplication.value?.status === 0) return '商户申请待审核'
  if (merchantApplication.value?.status === 2) return '商户申请未通过'
  return '申请成为商户'
})
const merchantCardDesc = computed(() => {
  if (userStore.canManageProducts) return '你可以发布、编辑和上下架自己的旅游商品。'
  if (merchantApplication.value?.status === 0) return '申请已通过 RabbitMQ 投递给超级管理员，请等待审核结果。'
  if (merchantApplication.value?.status === 2) return '当前申请未满足入驻要求，调整信息后可以重新提交。'
  return '提交申请后将进入超级管理员审核列表，通过后即可管理自己的旅游商品。'
})
const merchantBadgeText = computed(() => {
  if (userStore.canManageProducts) return '已开通'
  if (merchantApplication.value?.status === 0) return '待审核'
  if (merchantApplication.value?.status === 2) return '未通过'
  return '普通用户'
})
const avatarText = computed(() => hasToken.value ? (userStore.userInfo?.username?.charAt(0) || 'U') : 'U')
const formattedCreateTime = computed(() => formatDateTime(userStore.userInfo?.createTime))
const userBalance = computed(() => formatPrice(userStore.userInfo?.balance))
const merchantDisplayName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '未设置商户名称')
const editDialogTitle = computed(() => {
  if (editField.value === 'username') return '修改用户名'
  if (editField.value === 'nickname') return '修改商户名称'
  return '修改邮箱'
})
const editFieldLabel = computed(() => {
  if (editField.value === 'username') return '用户名'
  if (editField.value === 'nickname') return '商户名称'
  return '邮箱'
})
const editInputPlaceholder = computed(() => {
  if (editField.value === 'username') return '请输入新的用户名'
  if (editField.value === 'nickname') return '请输入商户名称'
  return '请输入新的邮箱'
})

function formatDateTime(value) {
  if (!value) return '未知'

  const normalized = `${value}`.replace('T', ' ')
  const date = new Date(normalized.replace(/-/g, '/'))
  if (Number.isNaN(date.getTime())) {
    return normalized.split('.')[0]
  }

  const pad = (num) => `${num}`.padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const editRules = {
  value: [
    {
      validator: (rule, value, callback) => {
        const text = `${value || ''}`.trim()
        if (editField.value === 'username') {
          if (!text) {
            callback(new Error('请输入用户名'))
            return
          }
          if (text.length < 3 || text.length > 20) {
            callback(new Error('用户名长度必须在 3 到 20 位之间'))
            return
          }
        }
        if (editField.value === 'nickname' && text.length > 30) {
          callback(new Error('商户名称不能超过 30 个字符'))
          return
        }
        if (editField.value === 'email' && text && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(text)) {
          callback(new Error('邮箱格式不正确'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const rechargeRules = {
  amount: [
    {
      validator: (rule, value, callback) => {
        const text = `${value || ''}`.trim()
        if (!text) {
          callback(new Error('请输入充值金额'))
          return
        }
        if (!/^\d+(\.\d{1,2})?$/.test(text)) {
          callback(new Error('充值金额最多保留两位小数'))
          return
        }
        if (Number(text) <= 0) {
          callback(new Error('充值金额必须大于 0'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      console.error('获取用户信息失败')
    }
  }
  if (userStore.token) {
    try {
      await userStore.fetchMerchantApplication()
    } catch (e) {
      console.error('获取商户申请状态失败')
    }
    await Promise.allSettled([fetchOrders(), fetchMerchantOrders()])
  }
})

async function fetchOrders() {
  if (!userStore.token || userStore.isAdmin) return

  ordersLoading.value = true
  const minimumLoading = wait(3000)
  try {
    const res = await listMyOrders({ current: 1, size: 5 })
    await minimumLoading
    const data = res.data || res || {}
    orders.value = data.records || data.list || []
    ordersTotal.value = Number(data.total || orders.value.length || 0)
  } catch (e) {
    await minimumLoading
    console.error('获取订单列表失败')
    ElMessage.error(e.message || '获取订单列表失败')
  } finally {
    ordersLoading.value = false
  }
}

async function fetchMerchantOrders() {
  if (!userStore.token || !userStore.isMerchant) return

  merchantOrdersLoading.value = true
  const minimumLoading = wait(3000)
  try {
    const res = await listMerchantOrders({
      current: 1,
      size: 20,
      status: merchantOrderStatus.value,
      keyword: merchantOrderKeyword.value || undefined
    })
    await minimumLoading
    const data = res.data || res || {}
    merchantOrders.value = data.records || data.list || []
    merchantOrdersTotal.value = Number(data.total || merchantOrders.value.length || 0)
  } catch (e) {
    await minimumLoading
    console.error('获取本店铺订单失败')
    ElMessage.error(e.message || '获取本店铺订单失败')
  } finally {
    merchantOrdersLoading.value = false
  }
}

function wait(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function handlePayOrder(orderItem) {
  if (!orderItem?.id) return

  payingOrderId.value = orderItem.id
  try {
    const res = await payOrderApi(orderItem.id)
    const savedOrder = res.data || res
    orders.value = orders.value.map((item) => {
      if (item.id !== orderItem.id) return item
      return {
        ...item,
        ...savedOrder,
        statusText: savedOrder?.statusText || orderStatusText(savedOrder?.status)
      }
    })
    syncLocalOrder(savedOrder)
    ElMessage.success('订单支付成功')
  } catch (e) {
    ElMessage.error(e.message || '订单支付失败')
  } finally {
    payingOrderId.value = null
  }
}

async function handleCancelOrder(orderItem) {
  if (!orderItem?.id) return

  try {
    await ElMessageBox.confirm('确认取消该订单吗？取消后不能再继续付款。', '取消订单', { type: 'warning' })
  } catch (e) {
    return
  }

  cancelingOrderId.value = orderItem.id
  try {
    const res = await cancelOrderApi(orderItem.id)
    const savedOrder = res.data || res
    orders.value = orders.value.map((item) => {
      if (item.id !== orderItem.id) return item
      return {
        ...item,
        ...savedOrder,
        statusText: savedOrder?.statusText || orderStatusText(savedOrder?.status)
      }
    })
    syncLocalOrder(savedOrder)
    ElMessage.success('订单已取消')
  } catch (e) {
    ElMessage.error(e.message || '取消订单失败')
  } finally {
    cancelingOrderId.value = null
  }
}

function syncLocalOrder(savedOrder) {
  if (!savedOrder?.id && !savedOrder?.orderNo) return

  const localOrders = JSON.parse(localStorage.getItem('travel_orders') || '[]')
  const orderIndex = localOrders.findIndex((item) => {
    return item.id === savedOrder.id || item.orderNo === savedOrder.orderNo
  })

  if (orderIndex < 0) return

  localOrders.splice(orderIndex, 1, {
    ...localOrders[orderIndex],
    ...savedOrder,
    statusText: savedOrder.statusText || orderStatusText(savedOrder.status)
  })
  localStorage.setItem('travel_orders', JSON.stringify(localOrders.slice(0, 20)))
}

function orderStatusText(status) {
  const map = {
    0: '待支付',
    1: '已支付',
    2: '已取消'
  }
  return map[Number(status)] || '未知'
}

function orderTagType(status) {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return map[Number(status)] || 'info'
}

function orderDateRange(orderItem) {
  if (orderItem.dateStart && orderItem.dateEnd) {
    return `${orderItem.dateStart} 至 ${orderItem.dateEnd}`
  }
  return '-'
}

function orderCoverText(orderItem) {
  return `${orderItem.productName || '旅游'}`.slice(0, 1)
}

function buyerName(orderItem) {
  return orderItem.userNickname || orderItem.username || `用户${orderItem.userId || '-'}`
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function uploadAvatar(file) {
  return userStore.uploadAvatar(file)
}

function openEditDialog(field) {
  if (!hasProfile.value) return
  editField.value = field
  if (field === 'username') {
    editForm.value.value = userStore.userInfo.username || ''
  } else if (field === 'nickname') {
    editForm.value.value = userStore.userInfo.nickname || userStore.userInfo.username || ''
  } else {
    editForm.value.value = userStore.userInfo.email || ''
  }
  editDialogVisible.value = true
}

async function submitEdit() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const nextValue = editForm.value.value.trim()
  const oldValue = currentEditValue()
  if (nextValue === oldValue) {
    editDialogVisible.value = false
    return
  }

  editLoading.value = true
  try {
    await userStore.updateUserInfo({ [editField.value]: nextValue })
    ElMessage.success(editSuccessMessage())
    editDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '修改失败')
  } finally {
    editLoading.value = false
  }
}

function openRechargeDialog() {
  rechargeForm.value.amount = ''
  rechargeDialogVisible.value = true
}

async function submitRecharge() {
  const valid = await rechargeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  rechargeLoading.value = true
  try {
    await userStore.rechargeBalance(rechargeForm.value.amount)
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '充值失败')
  } finally {
    rechargeLoading.value = false
  }
}

function currentEditValue() {
  if (editField.value === 'username') {
    return userStore.userInfo.username || ''
  }
  if (editField.value === 'nickname') {
    return userStore.userInfo.nickname || userStore.userInfo.username || ''
  }
  return userStore.userInfo.email || ''
}

function editSuccessMessage() {
  if (editField.value === 'username') return '用户名修改成功'
  if (editField.value === 'nickname') return '商户名称修改成功'
  return '邮箱修改成功'
}

async function handleApplyMerchant() {
  if (!canSubmitMerchantApplication.value) return

  merchantApplying.value = true
  try {
    await userStore.applyForMerchant()
    ElMessage.success('申请已提交，请等待超级管理员审核')
  } catch (e) {
    ElMessage.error(e.message || '申请失败，请稍后重试')
  } finally {
    merchantApplying.value = false
  }
}

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

function goHome() {
  router.push(userStore.isAdmin ? '/admin' : '/lvyou')
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--bg-body);
  color: var(--text-primary);
}

.main {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 28px 72px;
}

.profile-container {
  position: relative;
  background: #ffffff;
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  animation: fadeInUp 0.28s ease forwards;
}

.profile-container::before {
  content: none;
}

.profile-header {
  position: relative;
  padding: 32px;
  border-bottom: 1px solid var(--border-light);
}

.profile-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100px;
  background: #eef2f7;
}

.profile-pattern {
  display: none;
}

.profile-pattern svg {
  display: none;
}

.profile-info {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) 220px;
  align-items: center;
  gap: 20px;
  margin-top: 44px;
}

.profile-text {
  min-width: 0;
  padding: 16px 18px;
  flex: 1;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.profile-balance-card {
  min-width: 0;
  padding: 18px 20px;
  display: grid;
  gap: 8px;
  align-self: stretch;
  align-content: center;
  background: var(--primary);
  border: 1px solid var(--primary);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  color: #ffffff;
}

.profile-balance-card__eyebrow {
  font-size: 12px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.8);
}

.profile-balance-card__value {
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
  color: #ffffff;
}

.profile-balance-card__hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.74);
}

.profile-balance-card__action {
  width: 100%;
  margin-top: 6px;
  height: 36px;
  font-weight: 700;
  color: #b45309;
  background: rgba(255, 255, 255, 0.96);
  border-color: rgba(255, 255, 255, 0.9);
}

.profile-balance-card__action:hover,
.profile-balance-card__action:focus {
  color: #92400e;
  background: #fff7ed;
  border-color: #fde68a;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
  cursor: pointer;
}

.avatar {
  width: 104px;
  height: 104px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  font-weight: 700;
  border: 5px solid rgba(255, 255, 255, 0.95);
  overflow: hidden;
  position: relative;
  box-shadow: var(--shadow-md);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.avatar:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar--image {
  background: var(--bg-subtle);
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(15, 23, 42, 0.58);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.avatar:hover .avatar-mask {
  opacity: 1;
}

.avatar-input {
  display: none;
}

.profile-name {
  display: block;
  max-width: 100%;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 760;
  color: var(--text-primary);
  margin-bottom: 10px;
  overflow-wrap: anywhere;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.7);
}

.profile-email {
  border: 1px solid rgba(37, 99, 235, 0.18);
  font-size: 14px;
  color: var(--primary);
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 7px 12px;
  background: rgba(37, 99, 235, 0.07);
  border-radius: var(--radius-sm);
  max-width: 100%;
  overflow-wrap: anywhere;
}

.profile-email--editable {
  font: inherit;
  cursor: pointer;
  transition: color var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.profile-email--editable:hover,
.profile-email--editable:focus {
  color: #ffffff;
  background: #0f766e;
  border-color: #0f766e;
  transform: translateY(-1px);
}

.profile-body {
  padding: 22px 32px 32px;
}

.info-section,
.stats-section,
.orders-section {
  margin-bottom: 18px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.merchant-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
  padding: 18px;
  background: #f8fafc;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.merchant-section--active {
  background: #f0fdf4;
  border-color: rgba(52, 211, 153, 0.48);
}

.merchant-section--pending {
  background: #fffbeb;
  border-color: rgba(251, 191, 36, 0.52);
}

.merchant-section--rejected {
  background: #fff1f2;
  border-color: rgba(251, 113, 133, 0.5);
}

.merchant-copy {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.merchant-icon {
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0284c7;
  background: rgba(224, 242, 254, 0.95);
  border: 1px solid rgba(125, 211, 252, 0.48);
  border-radius: 16px;
}

.merchant-section--active .merchant-icon {
  color: #059669;
  background: rgba(209, 250, 229, 0.92);
  border-color: rgba(110, 231, 183, 0.62);
}

.merchant-section--pending .merchant-icon {
  color: #b45309;
  background: rgba(254, 243, 199, 0.92);
  border-color: rgba(251, 191, 36, 0.62);
}

.merchant-section--rejected .merchant-icon {
  color: #be123c;
  background: rgba(255, 228, 230, 0.92);
  border-color: rgba(251, 113, 133, 0.58);
}

.merchant-icon svg {
  width: 26px;
  height: 26px;
}

.merchant-title {
  margin: 0 0 7px;
  font-size: 18px;
  line-height: 1.25;
  color: var(--text-primary);
}

.merchant-desc {
  margin: 0;
  max-width: 520px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.65;
}

.merchant-reason {
  margin: 8px 0 0;
  color: #be123c;
  font-size: 13px;
  font-weight: 650;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.merchant-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 0 0 auto;
}

.merchant-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  height: 30px;
  padding: 0 12px;
  color: #0369a1;
  background: rgba(224, 242, 254, 0.9);
  border: 1px solid rgba(125, 211, 252, 0.58);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.merchant-badge--active {
  color: #047857;
  background: rgba(209, 250, 229, 0.92);
  border-color: rgba(110, 231, 183, 0.68);
}

.merchant-badge--pending {
  color: #92400e;
  background: rgba(254, 243, 199, 0.92);
  border-color: rgba(251, 191, 36, 0.7);
}

.merchant-badge--rejected {
  color: #be123c;
  background: rgba(255, 228, 230, 0.92);
  border-color: rgba(251, 113, 133, 0.7);
}

.merchant-apply-btn,
.merchant-manage-btn {
  height: 40px;
  min-width: 108px;
  border-radius: var(--radius-sm) !important;
  font-weight: 700;
}

.merchant-manage-btn {
  color: #047857;
  background: rgba(236, 253, 245, 0.82);
  border-color: rgba(52, 211, 153, 0.52);
}

.merchant-manage-btn:hover {
  color: #fff;
  background: #10b981;
  border-color: #10b981;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.86);
}

.section-title svg {
  color: var(--primary);
  padding: 6px;
  width: 30px;
  height: 30px;
  background: rgba(37, 99, 235, 0.08);
  border-radius: var(--radius-sm);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
}

.info-item {
  min-width: 0;
  min-height: 112px;
  padding: 18px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  align-items: start;
  gap: 10px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.78);
  border-radius: var(--radius-md);
  color: inherit;
  font: inherit;
  text-align: left;
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.info-item--editable {
  cursor: pointer;
}

.info-item--editable:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: var(--shadow-sm);
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  letter-spacing: 0;
}

.info-value {
  max-width: 100%;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  overflow-wrap: anywhere;
  align-self: center;
}

.info-action {
  min-height: 18px;
  justify-self: end;
  color: #0284c7;
  font-size: 12px;
  font-weight: 700;
}

.info-action--placeholder {
  visibility: hidden;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-start;
  gap: 14px;
  min-height: 94px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: var(--radius-md);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast), border-color var(--transition-fast);
  border: 1px solid rgba(226, 232, 240, 0.84);
}

.stat-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
  border-color: rgba(37, 99, 235, 0.24);
}

.stat-card--favorites .stat-icon { color: #e11d48; background: rgba(255, 241, 242, 0.95); }
.stat-card--reviews .stat-icon { color: #ea580c; background: rgba(255, 247, 237, 0.95); }

.stat-icon {
  width: 50px;
  height: 50px;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  text-align: left;
}

.stat-num {
  display: block;
  font-size: 25px;
  line-height: 1;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.stat-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.orders-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.orders-title-row .section-title {
  flex: 1;
}

.refresh-btn {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  color: #0f766e;
  background: rgba(240, 253, 250, 0.88);
  border-color: rgba(153, 246, 228, 0.86);
}

.refresh-btn:hover,
.refresh-btn:focus {
  color: #0369a1;
  background: rgba(224, 242, 254, 0.92);
  border-color: rgba(125, 211, 252, 0.9);
}

.orders-content {
  min-height: 168px;
}

.stable-empty {
  min-height: 168px;
}

.stable-empty--hidden {
  visibility: hidden;
}

.merchant-order-filter {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 140px 76px;
  gap: 10px;
  max-width: 680px;
  margin-bottom: 14px;
  align-items: center;
}

.merchant-order-filter :deep(.el-input),
.merchant-order-filter :deep(.el-select),
.merchant-order-filter :deep(.el-button) {
  width: 100%;
}

.merchant-order-filter :deep(.el-input__wrapper),
.merchant-order-filter :deep(.el-select__wrapper),
.merchant-order-query {
  height: 34px;
  min-height: 34px;
}

.merchant-order-status,
.merchant-order-query {
  width: 100%;
  min-width: 0;
}

.order-list {
  display: grid;
  gap: 12px;
}

.order-item {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.84);
  border-radius: var(--radius-md);
}

.order-actions {
  display: grid;
  gap: 8px;
  min-width: 92px;
}

.order-actions :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.merchant-order-item {
  grid-template-columns: 76px minmax(0, 1fr);
}

.order-cover {
  width: 76px;
  height: 62px;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: #0f766e;
  background: #eef2f7;
  border-radius: var(--radius-sm);
  font-size: 24px;
  font-weight: 780;
}

.order-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-main {
  min-width: 0;
}

.order-top,
.order-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-top strong {
  min-width: 0;
  color: var(--text-primary);
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin: 8px 0;
  color: var(--text-muted);
  font-size: 12px;
}

.order-meta span {
  overflow-wrap: anywhere;
}

.order-bottom span {
  color: #f97316;
  font-size: 18px;
  font-weight: 760;
}

.order-bottom small {
  color: var(--text-muted);
}

/* Actions */
.actions-section {
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}

.logout-btn {
  width: auto;
  min-width: 190px;
  height: 50px;
  font-size: 15px;
  border-radius: var(--radius-sm) !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 58px 0;
  background: var(--bg-subtle);
  border-radius: var(--radius-md);
}

.empty-icon {
  margin-bottom: 16px;
  color: #7dd3fc;
}

.empty-state h3 {
  font-size: 19px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.profile-edit-dialog) {
  border-radius: var(--radius-md);
  overflow: hidden;
}

:deep(.profile-edit-dialog .el-dialog__header) {
  padding: 22px 24px 8px;
}

:deep(.profile-edit-dialog .el-dialog__title) {
  font-weight: 700;
  color: var(--text-primary);
}

:deep(.profile-edit-dialog .el-dialog__body) {
  padding: 18px 24px;
}

:deep(.profile-edit-dialog .el-dialog__footer) {
  padding: 8px 24px 22px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .main {
    padding: 22px 16px 56px;
  }

  .profile-header {
    padding: 28px 20px 22px;
  }

  .profile-bg {
    height: 108px;
  }

  .profile-info {
    align-items: center;
    grid-template-columns: 1fr;
    gap: 16px;
    margin-top: 50px;
    text-align: center;
  }

  .avatar {
    width: 86px;
    height: 86px;
    font-size: 30px;
  }

  .profile-name {
    font-size: 23px;
  }

  .profile-text {
    width: 100%;
    padding: 16px;
  }

  .profile-name {
    max-width: 100%;
  }

  .profile-email {
    max-width: 100%;
    overflow-wrap: anywhere;
  }

  .profile-balance-card {
    width: 100%;
    justify-items: center;
    text-align: center;
  }

  .profile-body {
    padding: 0 20px 26px;
  }

  .info-section,
  .stats-section,
  .orders-section {
    padding: 18px;
  }

  .merchant-section {
    align-items: stretch;
    flex-direction: column;
    padding: 18px;
  }

  .merchant-copy {
    align-items: flex-start;
  }

  .merchant-actions {
    justify-content: space-between;
  }

  .merchant-apply-btn,
  .merchant-manage-btn {
    flex: 1;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .actions-section {
    justify-content: stretch;
  }

  .logout-btn {
    width: 100%;
  }

  .stat-card {
    min-height: 82px;
    padding: 16px;
  }

  .orders-title-row,
  .order-top,
  .order-bottom {
    align-items: stretch;
    flex-direction: column;
  }

  .order-item {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .order-actions {
    grid-column: 1 / -1;
    width: 100%;
  }

  .order-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-cover {
    width: 64px;
    height: 58px;
  }

  .merchant-order-filter {
    max-width: none;
    grid-template-columns: 1fr;
  }
}
</style>
