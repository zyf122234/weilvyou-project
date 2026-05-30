<template>
  <div class="page">
    <AppHeader />

    <main class="main">
      <section class="toolbar">
        <div>
          <span class="toolbar__eyebrow">Admin Console</span>
          <h1>管理后台</h1>
          <p>集中处理用户状态、商户入驻申请和平台订单。</p>
        </div>
      </section>

      <section v-if="!isAdmin" class="panel empty-panel">
        <el-empty description="当前账号没有超级管理员权限" />
      </section>

      <template v-else>
        <el-tabs v-model="activeTab" class="admin-tabs">
          <el-tab-pane label="用户管理" name="users">
            <section class="panel">
              <div class="panel-filter">
                <el-input v-model="keyword" placeholder="搜索用户名" clearable @keyup.enter="fetchUsers" />
                <el-select v-model="role" clearable placeholder="全部角色" style="width: 150px">
                  <el-option label="普通用户" value="ROLE_USER" />
                  <el-option label="商家" value="ROLE_MERCHANT" />
                </el-select>
                <el-select v-model="status" clearable placeholder="全部状态" style="width: 140px">
                  <el-option label="正常" :value="1" />
                  <el-option label="禁用" :value="0" />
                </el-select>
                <el-button :loading="loading" @click="fetchUsers">
                  <el-icon><Search /></el-icon>
                  查询
                </el-button>
              </div>

              <div class="panel-summary">
                <span>{{ roleSummary }}</span>
                <strong>{{ total }}</strong>
                <span>个账号</span>
              </div>

              <el-table :data="users" v-loading="loading" row-key="id">
                <el-table-column prop="id" label="ID" width="90" />
                <el-table-column prop="username" label="用户名" min-width="140" />
                <el-table-column prop="email" label="邮箱" min-width="180">
                  <template #default="{ row }">{{ row.email || '未设置' }}</template>
                </el-table-column>
                <el-table-column label="角色" min-width="180">
                  <template #default="{ row }">
                    <el-tag v-for="item in row.roles || []" :key="item" class="role-tag" type="info">
                      {{ roleLabel(item) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="账号状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                      {{ row.status === 1 ? '正常' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right">
                  <template #default="{ row }">
                    <button
                      type="button"
                      class="status-action"
                      :class="row.status === 1 ? 'status-action--disable' : 'status-action--enable'"
                      @click="toggleUserStatus(row)"
                    >
                      <el-icon>
                        <Lock v-if="row.status === 1" />
                        <Unlock v-else />
                      </el-icon>
                      {{ row.status === 1 ? '禁用' : '启用' }}
                    </button>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </el-tab-pane>

          <el-tab-pane label="商户申请" name="applications">
            <section class="panel">
              <div class="panel-filter application-filter">
                <el-select v-model="applicationStatus" clearable placeholder="全部申请状态" style="width: 160px">
                  <el-option label="待审核" :value="0" />
                  <el-option label="已同意" :value="1" />
                  <el-option label="已拒绝" :value="2" />
                </el-select>
                <el-button type="primary" :loading="applicationLoading" @click="fetchApplications">
                  <el-icon><Search /></el-icon>
                  查询申请
                </el-button>
              </div>

              <div class="panel-summary application-summary">
                <span>商户申请</span>
                <strong>{{ applicationTotal }}</strong>
                <span>条</span>
              </div>

              <el-table :data="applications" v-loading="applicationLoading" row-key="id">
                <el-table-column prop="id" label="ID" width="90" />
                <el-table-column prop="username" label="申请用户" min-width="140" />
                <el-table-column prop="email" label="邮箱" min-width="180">
                  <template #default="{ row }">{{ row.email || '未设置' }}</template>
                </el-table-column>
                <el-table-column prop="createTime" label="申请时间" min-width="170" />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="applicationTagType(row.status)">
                      {{ applicationStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="reason" label="反馈消息" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.reason || '-' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="170" fixed="right">
                  <template #default="{ row }">
                    <div v-if="row.status === 0" class="application-actions">
                      <button type="button" class="application-action application-action--approve" @click="approveApplication(row)">同意</button>
                      <button type="button" class="application-action application-action--reject" @click="rejectApplication(row)">不同意</button>
                    </div>
                    <span v-else class="application-done">已处理</span>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </el-tab-pane>

          <el-tab-pane label="所有订单" name="orders">
            <section class="panel">
              <div class="panel-filter order-filter">
                <el-input v-model="orderKeyword" placeholder="搜索订单号、商品、商家、联系人或电话" clearable @keyup.enter="fetchOrders" />
                <el-select v-model="orderStatus" clearable placeholder="全部状态" style="width: 140px">
                  <el-option label="待支付" :value="0" />
                  <el-option label="已支付" :value="1" />
                  <el-option label="已取消" :value="2" />
                </el-select>
                <el-button type="primary" :loading="orderLoading" @click="fetchOrders">
                  <el-icon><Search /></el-icon>
                  查询订单
                </el-button>
              </div>

              <div class="panel-summary order-summary">
                <span>订单清单</span>
                <strong>{{ orderTotal }}</strong>
                <span>条</span>
              </div>

              <el-table :data="orders" v-loading="orderLoading" row-key="id">
                <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
                <el-table-column label="购买用户" min-width="150">
                  <template #default="{ row }">
                    <div class="buyer-cell">
                      <strong>{{ row.userNickname || row.username || `用户${row.userId}` }}</strong>
                      <span>ID：{{ row.userId }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="商品详情" min-width="260">
                  <template #default="{ row }">
                    <div class="order-product">
                      <img v-if="row.productCoverUrl" :src="row.productCoverUrl" :alt="row.productName" />
                      <div v-else class="order-product__fallback">{{ orderCoverText(row) }}</div>
                      <div>
                        <strong>{{ row.productName || '旅游商品' }}</strong>
                        <span>商品ID：{{ row.productId }}</span>
                        <span>商家：{{ row.merchantName || '-' }}</span>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="清单" min-width="180">
                  <template #default="{ row }">
                    <div class="order-detail">
                      <span>日期：{{ orderDateRange(row) }}</span>
                      <span>数量：{{ row.quantity || 1 }}</span>
                      <span>单价：￥{{ formatPrice(row.unitPrice) }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="联系人" min-width="150">
                  <template #default="{ row }">
                    <div class="order-detail">
                      <span>{{ row.contactName || '-' }}</span>
                      <span>{{ row.phone || '-' }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="金额" width="120">
                  <template #default="{ row }">
                    <strong class="order-price">￥{{ formatPrice(row.totalPrice) }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="orderTagType(row.status)">
                      {{ row.statusText || orderStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="下单时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
                </el-table-column>
                <el-table-column label="支付时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
                </el-table-column>
              </el-table>
            </section>
          </el-tab-pane>
        </el-tabs>
      </template>
    </main>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, Search, Unlock } from '@element-plus/icons-vue'
import { listAllOrders, listMerchantApplications, listUsers, reviewMerchantApplication, updateUserStatus } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const tabNames = ['users', 'applications', 'orders']
const activeTab = ref(normalizeTab(route.query.tab))
const loading = ref(false)
const keyword = ref('')
const role = ref('')
const status = ref()
const users = ref([])
const total = ref(0)
const applicationLoading = ref(false)
const applicationStatus = ref(0)
const applications = ref([])
const applicationTotal = ref(0)
const orderLoading = ref(false)
const orderKeyword = ref('')
const orderStatus = ref()
const orders = ref([])
const orderTotal = ref(0)
const isAdmin = computed(() => userStore.isAdmin)

watch(activeTab, (nextTab) => {
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab: nextTab
    }
  })
})

const roleSummary = computed(() => {
  if (role.value === 'ROLE_USER') {
    return '普通用户'
  }
  if (role.value === 'ROLE_MERCHANT') {
    return '商家'
  }
  return '全部用户'
})

// 规范化后端返回的商品数据结构，适配前端展示
async function fetchFullPage(api, params = {}) {
  const firstRes = await api({
    ...params,
    current: 1,
    size: 1
  })
  const firstPage = firstRes.data || firstRes
  const pageTotal = Number(firstPage?.total || 0)
  if (pageTotal <= 1) {
    return firstPage
  }
  const fullRes = await api({
    ...params,
    current: 1,
    size: pageTotal
  })
  return fullRes.data || fullRes
}

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (isAdmin.value) {
    await Promise.allSettled([fetchUsers(), fetchApplications(), fetchOrders()])
  }
})

async function fetchUsers() {
  loading.value = true
  try {
    const page = await fetchFullPage(listUsers, {
      keyword: keyword.value || undefined,
      status: status.value,
      role: role.value || undefined
    })
    users.value = page?.records || []
    total.value = page?.total || users.value.length
  } catch (e) {
    ElMessage.error(e.message || '用户数据加载失败')
  } finally {
    loading.value = false
  }
}

//拉取申请列表
async function fetchApplications() {
  applicationLoading.value = true
  try {
    const page = await fetchFullPage(listMerchantApplications, {
      status: applicationStatus.value
    })
    applications.value = page?.records || []
    applicationTotal.value = page?.total || applications.value.length
  } catch (e) {
    ElMessage.error(e.message || '商户申请加载失败')
  } finally {
    applicationLoading.value = false
  }
}

async function fetchOrders() {
  orderLoading.value = true
  try {
    const page = await fetchFullPage(listAllOrders, {
      status: orderStatus.value,
      keyword: orderKeyword.value || undefined
    })
    orders.value = page?.records || []
    orderTotal.value = page?.total || orders.value.length
  } catch (e) {
    ElMessage.error(e.message || '订单数据加载失败')
  } finally {
    orderLoading.value = false
  }
}

async function approveApplication(row) {
  await ElMessageBox.confirm(`确认同意 ${row.username} 成为商户吗？`, '商户申请审核', { type: 'warning' })
  await reviewMerchantApplication(row.id, { status: 1 })
  ElMessage.success('已同意申请，用户角色已更新')
  await Promise.all([fetchApplications(), fetchUsers()])
}

async function rejectApplication(row) {
  const { value } = await ElMessageBox.prompt('请输入不同意的原因，用户将在个人中心看到该消息。', '商户申请审核', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：资料不完整，不满足商户入驻要求',
    inputValue: '不满足商户入驻要求',
    inputValidator: (value) => Boolean(`${value || ''}`.trim()),
    inputErrorMessage: '请填写原因'
  })
  await reviewMerchantApplication(row.id, { status: 2, reason: value })
  ElMessage.success('已拒绝申请，并返回提示给用户')
  await fetchApplications()
}

async function toggleUserStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '禁用'
  const message = nextStatus === 1
    ? `确认启用账号「${row.username}」吗？启用后该账号可以正常登录。`
    : `确认禁用账号「${row.username}」吗？禁用后该账号将无法登录，但仍会保留在管理列表中。`
  await ElMessageBox.confirm(message, `${action}确认`, { type: 'warning' })
  await updateUserStatus(row.id, nextStatus)
  ElMessage.success(nextStatus === 1 ? '账号已启用，可以正常登录' : '账号已禁用，用户将无法登录')
  await fetchUsers()
}

function roleLabel(role) {
  const map = {
    ROLE_ADMIN: '超级管理员',
    ROLE_USER: '普通用户',
    ROLE_MERCHANT: '商家'
  }
  return map[role] || role
}

function applicationStatusLabel(status) {
  const map = {
    0: '待审核',
    1: '已同意',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

function applicationTagType(status) {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'info'
}

function orderStatusLabel(status) {
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

function orderDateRange(row) {
  if (row.dateStart && row.dateEnd) {
    return `${row.dateStart} 至 ${row.dateEnd}`
  }
  return '-'
}

function orderCoverText(row) {
  return `${row.productName || '旅'}`.slice(0, 1)
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function formatDateTime(value) {
  if (!value) return '-'
  return `${value}`.replace('T', ' ').split('.')[0]
}

function normalizeTab(tab) {
  const value = Array.isArray(tab) ? tab[0] : tab
  return tabNames.includes(value) ? value : 'users'
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--bg-body);
  color: var(--text-primary);
}

.main {
  max-width: var(--layout-max);
  margin: 0 auto;
  padding: 28px 28px 72px;
}

.toolbar {
  margin-bottom: 18px;
}

.toolbar__eyebrow {
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

h1 {
  margin: 6px 0 8px;
  font-size: 28px;
  line-height: 1.25;
}

p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.admin-tabs {
  --el-color-primary: var(--primary);
}

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--border-light);
}

.admin-tabs :deep(.el-tabs__active-bar) {
  transition: none;
}

.panel {
  padding: 18px;
  border-radius: var(--radius-md);
  background: #fff;
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
}

.empty-panel {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto auto auto;
  gap: 10px;
  max-width: 760px;
  margin-bottom: 18px;
}

.application-filter {
  grid-template-columns: 180px auto;
  max-width: 420px;
}

.order-filter {
  grid-template-columns: minmax(280px, 1fr) 150px auto;
  max-width: 820px;
}

.panel-summary {
  min-height: 38px;
  margin-bottom: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: var(--radius-md);
  background: rgba(37, 99, 235, 0.07);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 650;
}

.panel-summary strong {
  color: var(--primary);
  font-size: 16px;
}

.application-summary {
  background: rgba(254, 243, 199, 0.72);
}

.application-summary strong {
  color: #b45309;
}

.order-summary {
  background: rgba(220, 252, 231, 0.72);
}

.order-summary strong {
  color: #047857;
}

.role-tag {
  margin-right: 6px;
}

.status-action {
  min-width: 82px;
  height: 34px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
}

.status-action:hover {
  filter: brightness(0.98);
}

.status-action:active {
  filter: none;
}

.status-action--disable {
  color: #b91c1c;
  border-color: rgba(252, 165, 165, 0.72);
  background: #fff1f2;
}

.status-action--disable:hover {
  color: #991b1b;
  border-color: rgba(248, 113, 113, 0.86);
  background: #fee2e2;
}

.status-action--enable {
  color: #047857;
  border-color: rgba(110, 231, 183, 0.76);
  background: #ecfdf5;
}

.status-action--enable:hover {
  color: #065f46;
  border-color: rgba(52, 211, 153, 0.9);
  background: #d1fae5;
}

.application-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.application-action {
  height: 32px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background var(--transition-fast), color var(--transition-fast), border-color var(--transition-fast);
}

.application-action:hover {
  filter: brightness(0.98);
}

.application-action--approve {
  color: #047857;
  border-color: rgba(110, 231, 183, 0.76);
  background: #ecfdf5;
}

.application-action--approve:hover {
  color: #fff;
  border-color: #10b981;
  background: #10b981;
}

.application-action--reject {
  color: #b91c1c;
  border-color: rgba(252, 165, 165, 0.72);
  background: #fff1f2;
}

.application-action--reject:hover {
  color: #fff;
  border-color: #ef4444;
  background: #ef4444;
}

.application-done {
  color: #64748b;
  font-size: 13px;
  font-weight: 650;
}

.buyer-cell,
.order-detail,
.order-product > div {
  display: grid;
  gap: 4px;
}

.buyer-cell strong,
.order-product strong {
  color: #0f172a;
  font-size: 14px;
}

.buyer-cell span,
.order-detail span,
.order-product span {
  color: #64748b;
  font-size: 12px;
}

.order-product {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.order-product img,
.order-product__fallback {
  width: 58px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: 8px;
}

.order-product img {
  object-fit: cover;
}

.order-product__fallback {
  display: grid;
  place-items: center;
  color: #0f766e;
  background: #eef2f7;
  font-weight: 800;
}

.order-price {
  color: #f97316;
}

@media (max-width: 800px) {
  .main {
    padding: 22px 16px 56px;
  }

  .panel-filter {
    max-width: none;
    grid-template-columns: 1fr;
  }
}
</style>
