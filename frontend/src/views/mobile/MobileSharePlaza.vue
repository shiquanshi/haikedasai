<template>
  <div class="mobile-share-plaza">
    <!-- 头部 -->
    <div class="plaza-header">
      <button class="back-btn" @click="goBack">
        <span class="icon">←</span>
      </button>
      <h1 class="title">分享大厅</h1>
      <div class="placeholder"></div>
    </div>

    <!-- 搜索和筛选 -->
    <div class="search-section">
      <div class="search-box">
        <input 
          v-model="searchParams.keyword" 
          type="text" 
          placeholder="搜索题库..."
          @input="handleSearch"
        />
      </div>
      
      <div class="filter-chips">
        <div class="chip-group">
          <button 
            v-for="topic in topics" 
            :key="topic.value"
            :class="['chip', { active: searchParams.topic === topic.value }]"
            @click="selectTopic(topic.value)"
          >
            {{ topic.label }}
          </button>
        </div>
      </div>

      <div class="filter-row">
        <select v-model="searchParams.difficulty" @change="handleSearch">
          <option value="">全部难度</option>
          <option value="easy">简单</option>
          <option value="medium">中等</option>
          <option value="hard">困难</option>
        </select>
        
        <select v-model="searchParams.orderBy" @change="handleSearch">
          <option value="latest">最新发布</option>
          <option value="popular">最受欢迎</option>
          <option value="views">浏览最多</option>
        </select>
      </div>
    </div>

    <!-- 题库列表 -->
    <div class="plaza-content" v-loading="loading">
      <div v-if="plazaList.length === 0" class="empty-state">
        <p>暂无分享题库</p>
      </div>
      
      <div v-else class="bank-list">
        <div 
          v-for="bank in plazaList" 
          :key="bank.id"
          class="bank-card"
          @click="viewBankDetail(bank)"
        >
          <div class="card-header">
            <h3 class="bank-title">{{ bank.shareTitle || bank.bankName }}</h3>
            <span :class="['difficulty-tag', bank.difficulty]">{{ getDifficultyLabel(bank.difficulty) }}</span>
          </div>
          
          <p class="bank-description">{{ bank.shareDescription || bank.bankDescription || '暂无描述' }}</p>
          
          <div class="bank-meta">
            <span class="topic-tag">{{ bank.topic }}</span>
            <span class="card-count">{{ bank.cardCount || 0 }} 张卡片</span>
          </div>
          
          <div class="bank-stats">
            <span class="stat-item">
              <span class="icon">👁</span>
              {{ bank.viewCount || 0 }}
            </span>
            <span class="stat-item">
              <span class="icon">📋</span>
              {{ bank.copyCount || 0 }}
            </span>
            <span class="stat-item">
              <span class="icon">⭐</span>
              {{ bank.favoriteCount || 0 }}
            </span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination">
        <button 
          :disabled="pagination.currentPage === 1"
          @click="handlePageChange(pagination.currentPage - 1)"
        >
          上一页
        </button>
        <span class="page-info">
          {{ pagination.currentPage }} / {{ pagination.totalPages }}
        </span>
        <button 
          :disabled="pagination.currentPage >= pagination.totalPages"
          @click="handlePageChange(pagination.currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPlaza } from '@/api/share'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const plazaList = ref<any[]>([])

const topics = [
  { label: '全部', value: '' },
  { label: '编程', value: 'programming' },
  { label: '数学', value: 'math' },
  { label: '英语', value: 'english' },
  { label: '历史', value: 'history' },
  { label: '科学', value: 'science' },
  { label: '其他', value: 'other' }
]

const searchParams = reactive({
  topic: '',
  difficulty: '',
  keyword: '',
  orderBy: 'latest'
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
})

const goBack = () => {
  router.back()
}

const selectTopic = (topic: string) => {
  searchParams.topic = topic
  handleSearch()
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadPlaza()
}

const handlePageChange = (page: number) => {
  pagination.currentPage = page
  loadPlaza()
}

const loadPlaza = async () => {
  loading.value = true
  try {
    const response = await getPlaza({
      topic: searchParams.topic,
      difficulty: searchParams.difficulty,
      keyword: searchParams.keyword,
      orderBy: searchParams.orderBy,
      page: pagination.currentPage,
      pageSize: pagination.pageSize
    })
    
    plazaList.value = response.data.list || []
    pagination.total = response.data.total || 0
    pagination.totalPages = response.data.totalPages || 0
  } catch (error: any) {
    console.error('加载分享大厅失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const viewBankDetail = (bank: any) => {
  // 跳转到题库详情页面，传递分享码
  router.push({
    path: '/share-detail',
    query: { shareCode: bank.shareCode }
  })
}

const getDifficultyLabel = (difficulty: string) => {
  const map: any = {
    easy: '简单',
    medium: '中等',
    hard: '困难'
  }
  return map[difficulty] || '未知'
}

onMounted(() => {
  loadPlaza()
})
</script>

<style scoped>
.mobile-share-plaza {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding-bottom: 20px;
}

.plaza-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.back-btn {
  background: none;
  border: none;
  color: white;
  font-size: 24px;
  cursor: pointer;
  padding: 5px;
}

.title {
  color: white;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.placeholder {
  width: 34px;
}

.search-section {
  padding: 15px 20px;
}

.search-box {
  margin-bottom: 15px;
}

.search-box input {
  width: 100%;
  padding: 12px 15px;
  border: none;
  border-radius: 25px;
  background: white;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.filter-chips {
  margin-bottom: 15px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.chip-group {
  display: flex;
  gap: 10px;
  padding: 5px 0;
}

.chip {
  flex-shrink: 0;
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.chip.active {
  background: white;
  color: #667eea;
  font-weight: 600;
}

.filter-row {
  display: flex;
  gap: 10px;
}

.filter-row select {
  flex: 1;
  padding: 10px 12px;
  border: none;
  border-radius: 12px;
  background: white;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.plaza-content {
  padding: 0 20px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: white;
  font-size: 16px;
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.bank-card {
  background: white;
  border-radius: 15px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s;
}

.bank-card:active {
  transform: scale(0.98);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.bank-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
  margin-right: 10px;
}

.difficulty-tag {
  flex-shrink: 0;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.difficulty-tag.easy {
  background: #d4edda;
  color: #155724;
}

.difficulty-tag.medium {
  background: #fff3cd;
  color: #856404;
}

.difficulty-tag.hard {
  background: #f8d7da;
  color: #721c24;
}

.bank-description {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin: 10px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bank-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0;
}

.topic-tag {
  padding: 4px 12px;
  background: #f0f0f0;
  color: #666;
  border-radius: 12px;
  font-size: 12px;
}

.card-count {
  color: #999;
  font-size: 13px;
}

.bank-stats {
  display: flex;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #999;
  font-size: 13px;
}

.stat-item .icon {
  font-size: 16px;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 30px;
  padding: 20px 0;
}

.pagination button {
  padding: 10px 20px;
  border: none;
  border-radius: 12px;
  background: white;
  color: #667eea;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination button:not(:disabled):active {
  transform: scale(0.95);
}

.page-info {
  color: white;
  font-size: 14px;
  font-weight: 600;
}
</style>