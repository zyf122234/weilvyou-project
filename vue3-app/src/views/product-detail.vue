<template>
  <div class="page">
    <AppHeader />

    <main class="main">
      <el-button class="back-btn" :icon="ArrowLeft" text @click="router.push('/lvyou')">返回酒店列表</el-button>

      <el-skeleton v-if="loading" :rows="8" animated />

      <el-empty v-else-if="!product" description="商品不存在或已下架">
        <el-button type="primary" @click="router.push('/lvyou')">返回推荐</el-button>
      </el-empty>

      <template v-else>
        <section class="detail-layout">
          <div class="media-panel">
            <img v-if="product.coverUrl" :src="product.coverUrl" :alt="product.name" />
            <div v-else class="cover-fallback">{{ product.city || 'TravelGo' }}</div>
          </div>

          <aside class="booking-panel">
            <div class="booking-header">
              <span>{{ product.city || product.destination || '城市待定' }}</span>
              <h1>{{ product.name }}</h1>
              <p>{{ product.description || '暂无商品介绍' }}</p>
            </div>

            <div class="price-box">
              <span>当前单价</span>
              <strong>￥{{ unitPrice }}</strong>
              <small>/晚</small>
            </div>

            <el-form label-position="top">
              <el-form-item label="入住日期">
                <el-date-picker
                  v-model="order.dateRange"
                  type="daterange"
                  start-placeholder="入住"
                  end-placeholder="离店"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disablePastDate"
                />
              </el-form-item>
              <el-form-item label="购买数量（入住天数）">
                <el-input class="stay-nights-input" :model-value="stayNightsLabel" readonly />
              </el-form-item>
              <div class="form-grid">
                <el-form-item label="联系人">
                  <el-input v-model="order.contactName" placeholder="请输入联系人姓名" />
                </el-form-item>
                <el-form-item label="联系电话">
                  <el-input v-model="order.phone" placeholder="请输入联系电话" />
                </el-form-item>
              </div>
            </el-form>

            <div class="total-row">
              <span>合计金额</span>
              <strong>￥{{ totalPrice }}</strong>
            </div>
            <el-button class="buy-btn" type="primary" size="large" :loading="submitting" @click="submitOrder">
              立即购买
            </el-button>
          </aside>
        </section>

        <section class="info-panel">
          <div class="info-card">
            <span>商家</span>
            <strong>{{ product.merchantName || '商家未知' }}</strong>
          </div>
          <div class="info-card">
            <span>目的地</span>
            <strong>{{ product.destination || product.city || '-' }}</strong>
          </div>
          <div class="info-card">
            <span>品牌</span>
            <strong>{{ product.brand || '品牌未知' }}</strong>
          </div>
          <div class="info-card">
            <span>星级</span>
            <strong>{{ product.starName || '暂无星级' }}</strong>
          </div>
          <div class="info-card">
            <span>商品编号</span>
            <strong>{{ product.id }}</strong>
          </div>
          <div class="info-card">
            <span>状态</span>
            <strong>可购买</strong>
          </div>
        </section>
      </template>
    </main>

    <el-dialog
      v-model="orderDialogVisible"
      title="订单确认"
      width="460px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :before-close="preventDialogClose"
    >
      <div class="order-confirm">
        <strong>{{ pendingOrder?.productName }}</strong>
        <p>订单号：{{ pendingOrder?.orderNo || pendingOrder?.id }}</p>
        <p>入住日期：{{ pendingOrder?.dateRange?.join(' 至 ') }}</p>
        <p>联系人：{{ pendingOrder?.contactName }}，合计 ￥{{ pendingOrder?.totalPrice }}</p>
        <p>状态：{{ pendingOrder?.statusText || '待支付' }}</p>
      </div>
      <template #footer>
        <el-button :disabled="paying" @click="leaveWithPendingPayment">继续退出</el-button>
        <el-button type="primary" :loading="paying" @click="payOrder">立即付款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getPublishedProduct } from '@/api/product'
import { createOrder, payOrder as payOrderApi } from '@/api/order'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const paying = ref(false)
const orderDialogVisible = ref(false)
const pendingOrder = ref(null)
const product = ref(null)

const order = reactive({
  dateRange: [],
  contactName: '',
  phone: ''
})

const unitPrice = computed(() => Number(product.value?.price || 0).toFixed(2))
const stayNights = computed(() => {
  const [start, end] = order.dateRange || []
  if (!start || !end) return 0

  const startTime = parseDateValue(start)
  const endTime = parseDateValue(end)
  const days = Math.round((endTime - startTime) / 86400000)
  return days > 0 ? days : 0
})
const stayNightsLabel = computed(() => (stayNights.value > 0 ? `${stayNights.value} 晚` : '请选择入住日期'))
const totalPrice = computed(() => (Number(product.value?.price || 0) * stayNights.value).toFixed(2))

onMounted(fetchProduct)

async function fetchProduct() {
  loading.value = true
  try {
    const res = await getPublishedProduct(route.params.id)
    product.value = res.data || res
  } catch (error) {
    product.value = null
  } finally {
    loading.value = false
  }
}

function disablePastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function parseDateValue(value) {
  if (value instanceof Date) {
    const date = new Date(value)
    date.setHours(0, 0, 0, 0)
    return date.getTime()
  }

  return new Date(`${value}T00:00:00`).getTime()
}

async function submitOrder() {
  if (userStore.isAdmin) {
    ElMessage.warning('超级管理员不能购买商品')
    return
  }

  if (!order.dateRange?.length || order.dateRange.length < 2) {
    ElMessage.warning('请选择入住日期')
    return
  }
  if (stayNights.value < 1) {
    ElMessage.warning('离店日期需晚于入住日期')
    return
  }
  if (!order.contactName.trim()) {
    ElMessage.warning('请输入联系人')
    return
  }
  if (!order.phone.trim()) {
    ElMessage.warning('请输入联系电话')
    return
  }

  submitting.value = true
  try {
    const localOrder = createLocalOrder()
    const response = await createOrder(buildOrderPayload(localOrder))
    const savedOrder = response.data || response
    pendingOrder.value = mergeSavedOrder(localOrder, savedOrder, '待支付')
    saveOrder(pendingOrder.value)
    orderDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '订单创建失败')
  } finally {
    submitting.value = false
  }
}

function createLocalOrder() {
  const now = new Date().toISOString()
  const localId = `T${Date.now()}`
  return {
    id: localId,
    localId,
    productId: product.value.id,
    productName: product.value.name,
    dateRange: [...order.dateRange],
    quantity: stayNights.value,
    contactName: order.contactName.trim(),
    phone: order.phone.trim(),
    unitPrice: unitPrice.value,
    totalPrice: totalPrice.value,
    status: 0,
    statusText: '待支付',
    createTime: now,
    updateTime: now
  }
}

function preventDialogClose() {}

function saveOrder(nextOrder) {
  const orders = JSON.parse(localStorage.getItem('travel_orders') || '[]')
  const orderIndex = orders.findIndex((item) => item.localId === nextOrder.localId || item.id === nextOrder.id)

  if (orderIndex >= 0) {
    orders.splice(orderIndex, 1, nextOrder)
  } else {
    orders.unshift(nextOrder)
  }

  localStorage.setItem('travel_orders', JSON.stringify(orders.slice(0, 20)))
}

function buildOrderPayload(sourceOrder) {
  return {
    productId: sourceOrder.productId,
    dateStart: sourceOrder.dateRange?.[0] || null,
    dateEnd: sourceOrder.dateRange?.[1] || null,
    quantity: sourceOrder.quantity,
    totalPrice: sourceOrder.totalPrice,
    contactName: sourceOrder.contactName,
    phone: sourceOrder.phone
  }
}

function mergeSavedOrder(localOrder, savedOrder, fallbackStatusText) {
  return {
    ...localOrder,
    ...savedOrder,
    localId: localOrder.localId,
    id: savedOrder?.id || localOrder.id,
    orderNo: savedOrder?.orderNo || localOrder.orderNo,
    dateRange: localOrder.dateRange,
    statusText: savedOrder?.statusText || fallbackStatusText,
    totalPrice: String(savedOrder?.totalPrice ?? localOrder.totalPrice),
    updateTime: savedOrder?.updateTime || new Date().toISOString()
  }
}

function leaveWithPendingPayment() {
  if (!pendingOrder.value) return

  const nextOrder = {
    ...pendingOrder.value,
    status: 0,
    statusText: '待支付',
    updateTime: new Date().toISOString()
  }
  saveOrder(nextOrder)
  pendingOrder.value = nextOrder
  orderDialogVisible.value = false
  ElMessage.info('订单已保留为待支付')
}

async function payOrder() {
  if (!pendingOrder.value) return

  paying.value = true
  try {
    const response = await payOrderApi(pendingOrder.value.id)
    const savedOrder = response.data || response
    pendingOrder.value = mergeSavedOrder(pendingOrder.value, savedOrder, '已支付')
    saveOrder(pendingOrder.value)
    orderDialogVisible.value = false
    await router.push('/lvyou')
    ElMessage.success('订单支付成功')
  } catch (error) {
    ElMessage.error(error.message || '订单支付失败')
  } finally {
    paying.value = false
  }
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
  padding: 24px 28px 72px;
}

.back-btn {
  margin-bottom: 16px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 18px;
  align-items: start;
}

.media-panel,
.booking-panel,
.info-panel {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.media-panel {
  min-height: 520px;
  overflow: hidden;
}

.media-panel img,
.cover-fallback {
  width: 100%;
  min-height: 520px;
}

.media-panel img {
  display: block;
  object-fit: cover;
}

.cover-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #e2e8f0, #f8fafc);
  color: var(--text-muted);
  font-size: 36px;
  font-weight: 800;
}

.booking-panel {
  position: sticky;
  top: 88px;
  padding: 22px;
}

.booking-header span {
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.booking-header h1 {
  margin: 8px 0 10px;
  font-size: 26px;
  line-height: 1.3;
}

.booking-header p {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.price-box {
  margin: 20px 0;
  padding: 16px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  border-radius: var(--radius-md);
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.price-box span,
.price-box small {
  color: #9a3412;
  font-size: 13px;
  font-weight: 650;
}

.price-box strong {
  color: #ea580c;
  font-size: 30px;
  line-height: 1;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.booking-panel :deep(.el-date-editor) {
  width: 100%;
}

.stay-nights-input :deep(.el-input__wrapper) {
  background: var(--bg-subtle);
}

.stay-nights-input :deep(.el-input__inner) {
  color: var(--text-primary);
  font-weight: 700;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
  color: var(--text-secondary);
}

.total-row strong {
  color: #ea580c;
  font-size: 28px;
}

.buy-btn {
  width: 100%;
  height: 44px;
}

.info-panel {
  margin-top: 18px;
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.info-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-subtle);
  display: grid;
  gap: 8px;
}

.info-card span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.info-card strong {
  overflow-wrap: anywhere;
  font-size: 14px;
}

.order-confirm {
  display: grid;
  gap: 10px;
}

.order-confirm strong {
  font-size: 18px;
}

.order-confirm p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 980px) {
  .main {
    padding: 18px 16px 56px;
  }

  .detail-layout,
  .info-panel {
    grid-template-columns: 1fr;
  }

  .booking-panel {
    position: static;
  }

  .media-panel,
  .media-panel img,
  .cover-fallback {
    min-height: 300px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
