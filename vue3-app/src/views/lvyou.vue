<template>
  <div class="page">
    <AppHeader />

    <main class="main">
      <section class="page-head">
        <div>
          <span class="page-head__eyebrow">Hotel Inventory</span>
          <h1>酒店商品推荐</h1>
          <p>按城市、星级、品牌和价格筛选可预订酒店商品。</p>
        </div>
        <div class="page-head__stats">
          <span>当前结果</span>
          <strong>{{ total }}</strong>
          <small>家酒店</small>
        </div>
      </section>

      <section class="query-panel">
        <div class="query-row">
          <el-input
            v-model="searchText"
            placeholder="搜索酒店名称、城市、品牌或星级"
            size="large"
            :prefix-icon="Search"
            clearable
            @keyup.enter="fetchHotels"
          />
          <el-button type="primary" size="large" :loading="loading" @click="fetchHotels">
            查询
          </el-button>
        </div>

        <div v-if="initialized" class="facet-grid">
          <div v-for="row in facetRows" :key="row.key" class="facet-row">
            <span class="facet-label">{{ row.label }}</span>
            <div class="facet-options">
              <button
                type="button"
                class="facet-option"
                :class="{ active: !filters[row.key] }"
                @click="selectFacet(row.key, '')"
              >
                全部
              </button>
              <button
                v-for="option in row.options"
                :key="option"
                type="button"
                class="facet-option"
                :class="{ active: filters[row.key] === option }"
                @click="selectFacet(row.key, option)"
              >
                {{ option }}
              </button>
            </div>
          </div>
        </div>
        <el-skeleton v-else :rows="4" animated class="facet-skeleton" />
      </section>

      <section v-if="initialized" class="content-panel">
        <div class="section-toolbar">
          <div>
            <h2>酒店列表</h2>
            <p>共 {{ total }} 条数据，展示当前筛选条件下的全部商品。</p>
          </div>
          <el-button :icon="Refresh" :loading="loading" @click="fetchHotels">刷新</el-button>
        </div>

        <el-empty v-if="!loading && destinations.length === 0" description="暂无匹配的酒店商品" />

        <div v-else v-loading="loading" class="hotel-list" element-loading-text="正在加载">
          <article
            v-for="item in destinations"
            :key="item.id"
            class="hotel-card"
            @click="goToDetail(item)"
          >
            <div class="hotel-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.name" />
              <span v-else>{{ item.city || 'Hotel' }}</span>
            </div>

            <div class="hotel-main">
              <div class="hotel-title-row">
                <div>
                  <h3>{{ item.name }}</h3>
                  <p>{{ item.desc }}</p>
                </div>
                <el-tag size="small" effect="plain">{{ item.starName || '暂无星级' }}</el-tag>
              </div>

              <div class="hotel-meta">
                <span><el-icon><Location /></el-icon>{{ item.city || '城市待定' }}</span>
                <span><el-icon><OfficeBuilding /></el-icon>{{ item.business || '商圈待定' }}</span>
                <span><el-icon><CollectionTag /></el-icon>{{ item.brand || '品牌未知' }}</span>
              </div>
            </div>

            <div class="hotel-action">
              <div class="hotel-price">
                <span>起价</span>
                <strong>¥{{ item.price }}</strong>
                <small>/晚</small>
              </div>
              <el-button type="primary" plain>查看详情</el-button>
            </div>
          </article>
        </div>
      </section>
      <section v-else class="content-panel">
        <el-skeleton :rows="6" animated />
      </section>
    </main>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CollectionTag, Location, OfficeBuilding, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { searchHotels } from '@/api/product'

const userStore = useUserStore()
const router = useRouter()
const searchText = ref('')
const loading = ref(false)
const initialized = ref(false)
const total = ref(0)
const facets = reactive({
  cities: [],
  starNames: [],
  brands: [],
  priceRanges: ['100元以下', '100-300元', '300-600元', '600-1500元', '1500元以上']
})
const filters = reactive({
  city: '',
  starName: '',
  brand: '',
  priceRange: ''
})
const destinations = ref([])

const facetRows = computed(() => [
  { key: 'city', label: '城市', options: facets.cities },
  { key: 'starName', label: '星级', options: facets.starNames },
  { key: 'brand', label: '品牌', options: facets.brands },
  { key: 'priceRange', label: '价格', options: facets.priceRanges }
])

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  await fetchHotels()
})

async function fetchHotels() {
  loading.value = true
  try {
    const res = await searchHotels({
      current: 1,
      all: true,
      keyword: searchText.value || undefined,
      city: filters.city || undefined,
      starName: filters.starName || undefined,
      brand: filters.brand || undefined,
      priceRange: filters.priceRange || undefined
    })
    const data = res.data || res
    const page = data.page || {}
    const nextFacets = data.facets || {}
    const records = (page.records || []).map(normalizeHotel)
    total.value = page.total || 0
    facets.cities = resolveFacetOptions(nextFacets.cities, records, 'city', 20)
    facets.starNames = resolveFacetOptions(nextFacets.starNames, records, 'starName', 20)
    facets.brands = resolveFacetOptions(nextFacets.brands, records, 'brand', 30)
    facets.priceRanges = nextFacets.priceRanges?.length ? nextFacets.priceRanges : facets.priceRanges
    destinations.value = records
  } catch (error) {
    total.value = 0
    destinations.value = []
  } finally {
    initialized.value = true
    loading.value = false
  }
}

function resolveFacetOptions(options, records, field, limit) {
  if (options?.length) {
    return options
  }
  const values = []
  const seen = new Set()
  for (const item of records) {
    const value = item?.[field]
    if (value && !seen.has(value)) {
      seen.add(value)
      values.push(value)
      if (values.length >= limit) {
        break
      }
    }
  }
  return values
}

function normalizeHotel(item) {
  return {
    ...item,
    desc: item.address || '酒店暂无地址信息',
    price: item.price || 0,
    coverUrl: item.pic
  }
}

function selectFacet(key, value) {
  filters[key] = filters[key] === value ? '' : value
  fetchHotels()
}

function goToDetail(item) {
  if (!item?.id) return
  router.push({ name: 'ProductDetail', params: { id: item.id } })
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

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 18px;
}

.page-head__eyebrow {
  color: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

.page-head h1 {
  margin: 6px 0 8px;
  font-size: 28px;
  line-height: 1.25;
}

.page-head p {
  color: var(--text-secondary);
  font-size: 14px;
}

.page-head__stats {
  min-width: 150px;
  padding: 14px 16px;
  display: grid;
  gap: 4px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.page-head__stats span,
.page-head__stats small {
  color: var(--text-muted);
  font-size: 12px;
}

.page-head__stats strong {
  color: var(--text-primary);
  font-size: 28px;
  line-height: 1;
}

.query-panel,
.content-panel {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}

.query-panel {
  padding: 18px;
  margin-bottom: 18px;
}

.query-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 96px;
  gap: 10px;
  margin-bottom: 16px;
}

.facet-grid {
  display: grid;
  gap: 1px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--border-light);
}

.facet-skeleton {
  min-height: 180px;
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
}

.facet-row {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  min-height: 44px;
  padding: 9px 12px;
  background: #ffffff;
}

.facet-label {
  align-self: start;
  padding-top: 5px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.facet-options {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.facet-option {
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 650;
}

.facet-option:hover,
.facet-option.active {
  color: var(--primary);
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.07);
}

.content-panel {
  padding: 18px;
}

.section-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.section-toolbar h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.section-toolbar p {
  color: var(--text-muted);
  font-size: 13px;
}

.hotel-list {
  min-height: 220px;
  display: grid;
  gap: 12px;
}

.hotel-card {
  display: grid;
  grid-template-columns: 188px minmax(0, 1fr) 152px;
  gap: 18px;
  padding: 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: #ffffff;
  cursor: pointer;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
}

.hotel-card:hover {
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.hotel-cover {
  width: 188px;
  height: 128px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #e2e8f0, #f8fafc);
  color: var(--text-muted);
  font-weight: 800;
}

.hotel-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hotel-main {
  min-width: 0;
  display: grid;
  align-content: space-between;
  gap: 18px;
}

.hotel-title-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.hotel-title-row h3 {
  margin: 0 0 8px;
  font-size: 18px;
  line-height: 1.35;
}

.hotel-title-row p {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.hotel-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: var(--text-muted);
  font-size: 13px;
}

.hotel-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 220px;
}

.hotel-action {
  min-width: 0;
  display: grid;
  align-content: space-between;
  justify-items: end;
  border-left: 1px solid var(--border-light);
  padding-left: 18px;
}

.hotel-price {
  display: grid;
  justify-items: end;
  gap: 3px;
}

.hotel-price span,
.hotel-price small {
  color: var(--text-muted);
  font-size: 12px;
}

.hotel-price strong {
  color: #ea580c;
  font-size: 26px;
  line-height: 1;
}

@media (max-width: 920px) {
  .main {
    padding: 22px 16px 56px;
  }

  .page-head,
  .section-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .query-row,
  .facet-row,
  .hotel-card {
    grid-template-columns: 1fr;
  }

  .hotel-cover {
    width: 100%;
  }

  .hotel-action {
    align-content: start;
    justify-items: stretch;
    gap: 12px;
    border-left: 0;
    border-top: 1px solid var(--border-light);
    padding-left: 0;
    padding-top: 14px;
  }

  .hotel-price {
    justify-items: start;
  }
}
</style>
