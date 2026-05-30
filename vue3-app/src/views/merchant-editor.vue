<template>
  <div class="page">
    <AppHeader />

    <main class="main">
      <section class="toolbar">
        <div>
          <span class="toolbar__eyebrow">Merchant Console</span>
          <h1>商家编辑</h1>
          <p>{{ canManageProducts ? '管理自己创建的旅游商品，上架后会进入推荐列表。' : '普通用户可以浏览推荐商品，但不能新增或管理商品。' }}</p>
        </div>
        <el-button v-if="canManageProducts" type="primary" @click="openDialog()">新增商品</el-button>
      </section>

      <section v-if="!canManageProducts" class="panel empty-panel">
        <el-empty description="当前账号没有商品管理权限">
          <el-button type="primary" @click="router.push('/lvyou')">返回推荐</el-button>
        </el-empty>
      </section>

      <section v-else class="panel">
        <div class="panel-filter">
          <el-input v-model="keyword" placeholder="搜索商品名称、目的地、标签或商户名" clearable @keyup.enter="searchProducts" />
          <el-button :loading="loading" @click="searchProducts">查询</el-button>
        </div>

        <el-table :data="products" v-loading="loading" row-key="id">
          <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="商户名" min-width="120">
            <template #default="{ row }">
              <el-tag class="merchant-tag" effect="plain">{{ row.merchantName || '商户未知' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="city" label="城市" min-width="100">
            <template #default="{ row }">{{ row.city || row.destination || '-' }}</template>
          </el-table-column>
          <el-table-column prop="brand" label="品牌" min-width="120">
            <template #default="{ row }">{{ row.brand || splitTag(row.tag)[0] || '-' }}</template>
          </el-table-column>
          <el-table-column prop="starName" label="星级" min-width="100">
            <template #default="{ row }">{{ row.starName || splitTag(row.tag)[1] || '-' }}</template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="120">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已上架' : '已下架' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button class="action-button edit-button" :icon="Edit" @click="openDialog(row)">编辑</el-button>
                <el-button
                  class="action-button"
                  :class="row.status === 1 ? 'offline-button' : 'online-button'"
                  :icon="row.status === 1 ? Download : Upload"
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 1 ? '\u4e0b\u67b6' : '\u4e0a\u67b6' }}
                </el-button>
                <el-button class="action-button delete-button" :icon="Delete" @click="removeProduct(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <div class="pagination-info">
            共 {{ total }} 条数据，第 {{ currentPage }} / {{ totalPages }} 页
          </div>
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20, 50]"
            background
            layout="sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="560px">
      <el-form :model="form" label-position="top">
        <el-form-item label="商品名称" required>
          <el-input v-model="form.name" placeholder="例如：云南大理四日游" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述商品亮点" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="城市" required>
            <el-select
              v-model="form.city"
              filterable
              clearable
              placeholder="请选择城市名称"
              style="width: 100%"
            >
              <el-option
                v-for="city in cityOptions"
                :key="city"
                :label="city"
                :value="city"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="品牌">
            <el-input v-model="form.brand" placeholder="例如：7天酒店" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="星级">
            <el-select v-model="form.starName" placeholder="请选择星级" clearable style="width: 100%">
              <el-option v-for="option in starOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>
          <el-form-item label="价格" required>
            <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="商品封面">
          <ImageUpload
            v-model="form.coverUrl"
            :uploader="uploadProductCover"
            title="上传封面"
            placeholder-text="+"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Upload } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { createProduct, deleteProduct, listAdminProducts, listMerchantProducts, updateProduct, updateProductStatus, uploadProductCover } from '@/api/product'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const products = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pages = ref(1)
const canManageProducts = computed(() => userStore.canManageProducts)
const totalPages = computed(() => Math.max(1, pages.value || Math.ceil(total.value / pageSize.value) || 1))
const starOptions = ['一钻', '二钻', '三钻', '四钻', '五钻', '一星级', '二星级', '三星级', '四星级', '五星级']
const cityOptions = [
  '北京', '上海', '广州', '深圳', '杭州', '南京', '苏州', '成都', '重庆', '武汉',
  '西安', '长沙', '厦门', '青岛', '大连', '天津', '郑州', '济南', '合肥', '福州',
  '南昌', '昆明', '贵阳', '南宁', '海口', '三亚', '哈尔滨', '长春', '沈阳', '呼和浩特',
  '太原', '石家庄', '兰州', '银川', '西宁', '乌鲁木齐', '拉萨', '宁波', '无锡', '常州',
  '佛山', '东莞', '珠海', '惠州', '泉州', '温州', '嘉兴', '绍兴', '扬州', '桂林',
  '丽江', '大理', '张家界', '黄山', '洛阳', '秦皇岛'
]
const form = reactive({
  id: null,
  name: '',
  description: '',
  destination: '',
  city: '',
  coverUrl: '',
  tag: '',
  brand: '',
  starName: '',
  price: 0
})

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (canManageProducts.value) {
    await fetchProducts()
  }
})

async function fetchProducts() {
  if (!canManageProducts.value) {
    products.value = []
    total.value = 0
    pages.value = 1
    return
  }
  loading.value = true
  try {
    const listProducts = userStore.isAdmin ? listAdminProducts : listMerchantProducts
    const res = await listProducts({
      current: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value || undefined
    })
    const page = res.data || res
    products.value = page?.records || []
    total.value = page?.total || 0
    pages.value = page?.pages || Math.max(1, Math.ceil(total.value / pageSize.value))
  } finally {
    loading.value = false
  }
}

function searchProducts() {
  currentPage.value = 1
  fetchProducts()
}

function handlePageChange(page) {
  currentPage.value = page
  fetchProducts()
}

function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchProducts()
}

function openDialog(row) {
  if (!canManageProducts.value) {
    ElMessage.warning('当前账号没有商品管理权限')
    return
  }
  Object.assign(form, row ? normalizeProductForm(row) : {
    id: null,
    name: '',
    description: '',
    destination: '',
    city: '',
    coverUrl: '',
    tag: '',
    brand: '',
    starName: '',
    price: 0
  })
  dialogVisible.value = true
}

async function submit() {
  if (!form.name) {
    ElMessage.warning('请填写商品名称')
    return
  }
  if (!form.city) {
    ElMessage.warning('请填写城市')
    return
  }
  if (!cityOptions.includes(form.city)) {
    ElMessage.warning('请选择正确的城市名称')
    return
  }
  if (!form.brand) {
    ElMessage.warning('请填写品牌')
    return
  }
  if (!form.starName) {
    ElMessage.warning('请选择星级')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      description: form.description,
      destination: form.city,
      city: form.city,
      coverUrl: form.coverUrl,
      tag: [form.brand, form.starName].filter(Boolean).join('/'),
      brand: form.brand,
      starName: form.starName,
      price: form.price
    }
    if (form.id) {
      await updateProduct(form.id, payload)
    } else {
      await createProduct(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await fetchProducts()
  } finally {
    saving.value = false
  }
}

function normalizeProductForm(row) {
  const [brand, starName] = splitTag(row.tag)
  return {
    ...row,
    city: row.city || row.destination || '',
    brand: row.brand || brand || '',
    starName: row.starName || starName || ''
  }
}

function splitTag(tag) {
  if (!tag) {
    return []
  }
  return String(tag).split('/').map(item => item.trim())
}

async function toggleStatus(row) {
  await updateProductStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  await fetchProducts()
}

async function removeProduct(row) {
  await ElMessageBox.confirm(`确认删除商品「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  if (products.value.length === 1 && currentPage.value > 1) {
    currentPage.value -= 1
  }
  await fetchProducts()
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
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
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
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 10px;
  max-width: 680px;
  margin-bottom: 18px;
}

.merchant-tag {
  border-color: rgba(14, 165, 233, 0.24);
  background: rgba(14, 165, 233, 0.08);
  color: #0369a1;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-buttons .action-button {
  height: 30px;
  margin-left: 0;
  padding: 0 10px;
  border-radius: var(--radius-sm);
  font-weight: 600;
}

.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.pagination-info {
  flex: 0 0 auto;
  color: var(--text-secondary);
  font-size: 13px;
}

.edit-button {
  border-color: rgba(37, 99, 235, 0.26);
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
}

.edit-button:hover,
.edit-button:focus {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}

.online-button {
  border-color: rgba(22, 163, 74, 0.26);
  background: rgba(22, 163, 74, 0.08);
  color: #15803d;
}

.online-button:hover,
.online-button:focus {
  border-color: #16a34a;
  background: #16a34a;
  color: #fff;
}

.offline-button {
  border-color: rgba(217, 119, 6, 0.3);
  background: rgba(217, 119, 6, 0.1);
  color: #b45309;
}

.offline-button:hover,
.offline-button:focus {
  border-color: #d97706;
  background: #d97706;
  color: #fff;
}

.delete-button {
  border-color: rgba(220, 38, 38, 0.24);
  background: rgba(220, 38, 38, 0.08);
  color: #b91c1c;
}

.delete-button:hover,
.delete-button:focus {
  border-color: #dc2626;
  background: #dc2626;
  color: #fff;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 800px) {
  .main {
    padding: 22px 16px 56px;
  }

  .toolbar,
  .panel-filter,
  .pagination-wrap,
  .form-grid {
    display: flex;
    flex-direction: column;
  }

  .pagination-wrap {
    align-items: flex-start;
  }
}
</style>
