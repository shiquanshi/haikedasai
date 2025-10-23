<template>
  <div class="share-plaza">
    <div class="plaza-header">
      <h1>题库分享大厅</h1>
      <p class="subtitle">发现和学习社区分享的优质题库</p>
    </div>

    <!-- 筛选和搜索 -->
    <div class="filter-bar">
      <el-input
        v-model="searchParams.keyword"
        placeholder="搜索题库名称或描述"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
        v-model="searchParams.topic"
        placeholder="选择主题"
        clearable
        @change="handleSearch"
      >
        <el-option label="全部主题" value="" />
        <el-option label="编程" value="编程" />
        <el-option label="数学" value="数学" />
        <el-option label="英语" value="英语" />
        <el-option label="历史" value="历史" />
        <el-option label="地理" value="地理" />
        <el-option label="其他" value="其他" />
      </el-select>

      <el-select
        v-model="searchParams.difficulty"
        placeholder="选择难度"
        clearable
        @change="handleSearch"
      >
        <el-option label="全部难度" value="" />
        <el-option label="简单" value="简单" />
        <el-option label="中等" value="中等" />
        <el-option label="困难" value="困难" />
      </el-select>

      <el-select
        v-model="searchParams.orderBy"
        placeholder="排序方式"
        @change="handleSearch"
      >
        <el-option label="最新发布" value="created_at" />
        <el-option label="浏览最多" value="view_count" />
        <el-option label="收藏最多" value="favorite_count" />
        <el-option label="复制最多" value="copy_count" />
      </el-select>

      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
    </div>

    <!-- 题库列表 -->
    <div v-loading="loading" class="plaza-content">
      <div v-if="plazaList.length === 0" class="empty-state">
        <el-empty description="暂无分享题库" />
      </div>

      <div v-else class="bank-grid">
        <div
          v-for="item in plazaList"
          :key="item.id"
          class="bank-card"
          @click="viewBankDetail(item)"
        >
          <div class="card-header">
            <h3 class="bank-title">{{ item.shareTitle || item.bankName }}</h3>
            <el-tag :type="getDifficultyType(item.difficulty)" size="small">
              {{ item.difficulty || '未知' }}
            </el-tag>
          </div>

          <div class="card-description">
            {{ item.shareDescription || item.bankDescription || '暂无描述' }}
          </div>

          <div class="card-meta">
            <el-tag size="small" effect="plain">{{ item.topic || '其他' }}</el-tag>
            <span class="card-count">{{ item.cardCount }} 张卡片</span>
          </div>

          <div class="card-stats">
            <span class="stat-item">
              <el-icon><View /></el-icon>
              {{ item.viewCount }}
            </span>
            <span class="stat-item">
              <el-icon><User /></el-icon>
              {{ item.uniqueViewCount }}
            </span>
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ item.favoriteCount }}
            </span>
            <span class="stat-item">
              <el-icon><DocumentCopy /></el-icon>
              {{ item.copyCount }}
            </span>
          </div>

          <div class="card-footer">
            <div class="creator-info">
              <el-avatar :size="24" :src="item.creatorAvatar">
                {{ item.creatorName?.charAt(0) }}
              </el-avatar>
              <span class="creator-name">{{ item.creatorName }}</span>
            </div>
            <span class="share-time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, User, Star, DocumentCopy } from '@element-plus/icons-vue'
import { getPlaza, type PlazaItem } from '@/api/share'

const router = useRouter()
const loading = ref(false)
const plazaList = ref<PlazaItem[]>([])

const searchParams = reactive({
  keyword: '',
  topic: '',
  difficulty: '',
  orderBy: 'created_at'
})

const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0,
  totalPages: 0
})

// 加载分享大厅数据
const loadPlaza = async () => {
  loading.value = true
  try {
    const res = await getPlaza({
      ...searchParams,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    if (res.code === 200 && res.data) {
      plazaList.value = res.data.list
      pagination.total = res.data.total
      pagination.totalPages = res.data.totalPages
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadPlaza()
}

// 分页改变
const handlePageChange = (page: number) => {
  pagination.page = page
  loadPlaza()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.page = 1
  loadPlaza()
}

// 查看题库详情
const viewBankDetail = (item: PlazaItem) => {
  router.push({
    path: '/share-detail',
    query: { code: item.shareCode }
  })
}

// 获取难度标签类型
const getDifficultyType = (difficulty?: string) => {
  switch (difficulty) {
    case '简单':
      return 'success'
    case '中等':
      return 'warning'
    case '困难':
      return 'danger'
    default:
      return 'info'
  }
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

onMounted(() => {
  loadPlaza()
})
</script>

<style scoped lang="scss">
.share-plaza {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 20px;

  .plaza-header {
    text-align: center;
    margin-bottom: 40px;

    h1 {
      font-size: 36px;
      font-weight: 700;
      color: #303133;
      margin-bottom: 12px;
    }

    .subtitle {
      font-size: 16px;
      color: #909399;
    }
  }

  .filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 32px;
    flex-wrap: wrap;

    .search-input {
      flex: 1;
      min-width: 200px;
    }

    .el-select {
      width: 150px;
    }
  }

  .plaza-content {
    min-height: 400px;

    .empty-state {
      padding: 80px 0;
    }
  }

  .bank-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
    margin-bottom: 32px;
  }

  .bank-card {
    background: white;
    border-radius: 12px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;
      gap: 12px;

      .bank-title {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        line-height: 1.4;
      }
    }

    .card-description {
      font-size: 14px;
      color: #606266;
      line-height: 1.6;
      margin-bottom: 16px;
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      min-height: 44px;
    }

    .card-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 1px solid #ebeef5;

      .card-count {
        font-size: 13px;
        color: #909399;
      }
    }

    .card-stats {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;
      padding-bottom: 16px;
      border-bottom: 1px solid #ebeef5;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 13px;
        color: #909399;

        .el-icon {
          font-size: 14px;
        }
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .creator-info {
        display: flex;
        align-items: center;
        gap: 8px;

        .creator-name {
          font-size: 13px;
          color: #606266;
        }
      }

      .share-time {
        font-size: 12px;
        color: #c0c4cc;
      }
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: 32px;
  }
}

@media (max-width: 768px) {
  .share-plaza {
    padding: 20px 12px;

    .plaza-header h1 {
      font-size: 24px;
    }

    .filter-bar {
      .search-input,
      .el-select {
        width: 100%;
      }
    }

    .bank-grid {
      grid-template-columns: 1fr;
      gap: 16px;
    }
  }
}
</style>