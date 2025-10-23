<template>
  <div class="share-detail">
    <div v-loading="loading" class="detail-container">
      <div v-if="shareInfo" class="detail-content">
        <!-- 返回按钮 -->
        <el-button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回大厅
        </el-button>

        <!-- 题库信息 -->
        <div class="bank-info">
          <div class="info-header">
            <h1 class="bank-title">{{ shareInfo.shareTitle || bankInfo?.name }}</h1>
            <div class="header-actions">
              <el-button type="primary" @click="handleImport">
                <el-icon><DocumentCopy /></el-icon>
                导入到我的题库
              </el-button>
              <el-button @click="handleFavorite">
                <el-icon><Star /></el-icon>
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>

          <p class="bank-description">
            {{ shareInfo.shareDescription || bankInfo?.description || '暂无描述' }}
          </p>

          <div class="bank-meta">
            <el-tag :type="getDifficultyType(bankInfo?.difficulty)">
              {{ bankInfo?.difficulty || '未知' }}
            </el-tag>
            <el-tag effect="plain">{{ bankInfo?.topic || '其他' }}</el-tag>
            <el-tag effect="plain">{{ bankInfo?.language || '中文' }}</el-tag>
            <span class="card-count">共 {{ bankInfo?.cardCount || 0 }} 张卡片</span>
          </div>

          <div class="bank-stats">
            <div class="stat-item">
              <span class="stat-label">浏览量(PV)</span>
              <span class="stat-value">{{ shareInfo.viewCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">访客数(UV)</span>
              <span class="stat-value">{{ shareInfo.uniqueViewCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">收藏数</span>
              <span class="stat-value">{{ shareInfo.favoriteCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">导入次数</span>
              <span class="stat-value">{{ shareInfo.copyCount }}</span>
            </div>
          </div>

          <div class="creator-info">
            <el-avatar :size="40" :src="creatorInfo?.avatar">
              {{ creatorInfo?.username?.charAt(0) }}
            </el-avatar>
            <div class="creator-text">
              <span class="creator-name">{{ creatorInfo?.username }}</span>
              <span class="share-time">分享于 {{ formatTime(shareInfo.createdAt) }}</span>
            </div>
          </div>
        </div>

        <!-- 卡片预览 -->
        <div class="cards-preview">
          <h2 class="section-title">卡片预览</h2>
          <div v-if="cards.length === 0" class="empty-cards">
            <el-empty description="暂无卡片" />
          </div>
          <div v-else class="cards-list">
            <div 
              v-for="(card, index) in cards" 
              :key="card.id" 
              class="card-item clickable"
              @click="goToCard(card.id)"
            >
              <div class="card-number">{{ index + 1 }}</div>
              <div class="card-content">
                <div class="card-question">
                  <strong>问题：</strong>
                  <span>{{ card.question }}</span>
                </div>
                <div class="card-answer">
                  <strong>答案：</strong>
                  <span>{{ card.answer }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="error-state">
        <el-result icon="warning" title="分享不存在" sub-title="该分享可能已被删除或已过期">
          <template #extra>
            <el-button type="primary" @click="goBack">返回大厅</el-button>
          </template>
        </el-result>
      </div>
    </div>

    <!-- 选择目标题库对话框 -->
    <el-dialog
      v-model="showBankDialog"
      title="选择目标题库"
      width="600px"
      @close="selectedTargetBankId = null"
    >
      <div v-loading="isLoadingDialogBanks" class="bank-selection-dialog">
        <!-- 搜索框 -->
        <el-input
          v-model="bankSearchText"
          placeholder="搜索题库名称或主题"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />

        <!-- 题库列表 -->
        <div class="bank-list">
          <!-- 系统推荐题库 -->
          <div v-if="filteredDialogSystemBanks.length > 0" class="bank-category">
            <h4>系统推荐</h4>
            <el-radio-group v-model="selectedTargetBankId" class="bank-radio-group">
              <el-radio
                v-for="bank in filteredDialogSystemBanks"
                :key="bank.id"
                :value="bank.id"
                class="bank-radio-item"
              >
                <div class="bank-item-content">
                  <div class="bank-name">{{ bank.name }}</div>
                  <div class="bank-meta">
                    <el-tag size="small" effect="plain">{{ bank.topic }}</el-tag>
                    <span class="card-count">{{ bank.cardCount }} 张卡片</span>
                  </div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>

          <!-- 自定义题库 -->
          <div v-if="filteredDialogCustomBanks.length > 0" class="bank-category">
            <h4>我的题库</h4>
            <el-radio-group v-model="selectedTargetBankId" class="bank-radio-group">
              <el-radio
                v-for="bank in filteredDialogCustomBanks"
                :key="bank.id"
                :value="bank.id"
                class="bank-radio-item"
              >
                <div class="bank-item-content">
                  <div class="bank-name">{{ bank.name }}</div>
                  <div class="bank-meta">
                    <el-tag size="small" effect="plain">{{ bank.topic }}</el-tag>
                    <span class="card-count">{{ bank.cardCount }} 张卡片</span>
                  </div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>

          <!-- 无结果提示 -->
          <el-empty
            v-if="filteredDialogSystemBanks.length === 0 && filteredDialogCustomBanks.length === 0"
            description="没有找到匹配的题库"
          />
        </div>

        <!-- 创建新题库按钮 -->
        <div class="create-bank-section">
          <el-button type="primary" :icon="Plus" @click="showCreateBankDialog = true">
            创建新题库
          </el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="showBankDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmImport">确认导入</el-button>
      </template>
    </el-dialog>

    <!-- 创建题库对话框 -->
    <el-dialog
      v-model="showCreateBankDialog"
      title="创建自定义题库"
      width="500px"
    >
      <el-form :model="createBankForm" label-width="80px">
        <el-form-item label="题库名称" required>
          <el-input v-model="createBankForm.name" placeholder="请输入题库名称" />
        </el-form-item>
        <el-form-item label="主题" required>
          <el-input v-model="createBankForm.topic" placeholder="请输入主题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述（可选）"
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="createBankForm.difficulty" placeholder="选择难度">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="createBankForm.language" placeholder="选择语言">
            <el-option label="中文" value="中文" />
            <el-option label="英文" value="英文" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateBankDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateBank">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, DocumentCopy, Star, Plus, Search, Loading } from '@element-plus/icons-vue'
import { getShareDetail, incrementCopyCount, type SharedBank } from '@/api/share'
import { questionBankApi } from '@/api/questionBank'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const shareInfo = ref<SharedBank | null>(null)
const bankInfo = ref<any>(null)
const cards = ref<any[]>([])
const creatorInfo = ref<any>(null)
const isFavorited = ref(false)

// 选择题库弹窗相关
const showBankDialog = ref(false)
const showCreateBankDialog = ref(false)
const selectedTargetBankId = ref<number | null>(null)
const dialogSystemBanks = ref<any[]>([])
const dialogCustomBanks = ref<any[]>([])
const isLoadingDialogBanks = ref(false)
const bankSearchText = ref('')

// 创建题库表单
const createBankForm = ref({
  name: '',
  topic: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})

// 过滤后的题库列表
const filteredDialogSystemBanks = computed(() => {
  if (!bankSearchText.value) return dialogSystemBanks.value
  const keyword = bankSearchText.value.toLowerCase()
  return dialogSystemBanks.value.filter(bank => 
    bank.name.toLowerCase().includes(keyword) || 
    bank.topic?.toLowerCase().includes(keyword)
  )
})

const filteredDialogCustomBanks = computed(() => {
  if (!bankSearchText.value) return dialogCustomBanks.value
  const keyword = bankSearchText.value.toLowerCase()
  return dialogCustomBanks.value.filter(bank => 
    bank.name.toLowerCase().includes(keyword) || 
    bank.topic?.toLowerCase().includes(keyword)
  )
})

const shareCode = computed(() => route.query.code as string)

// 加载分享详情
const loadShareDetail = async () => {
  if (!shareCode.value) {
    ElMessage.error('缺少分享码')
    return
  }

  loading.value = true
  try {
    // 直接通过分享码获取题库完整信息
    const res = await questionBankApi.getByShareCode(shareCode.value)
    if (res.code === 200 && res.data) {
      // res.data 就是 QuestionBankDTO，包含所有需要的字段
      bankInfo.value = res.data
      cards.value = res.data.cards || []
      
      // 如果需要创建者信息，可以从用户信息中获取
      if (res.data.userId) {
        creatorInfo.value = {
          username: res.data.username || '未知用户',
          avatar: res.data.avatar
        }
      }
      
      // 同时获取分享统计信息（浏览量等）
      const shareRes = await getShareDetail(shareCode.value)
      if (shareRes.code === 200 && shareRes.data) {
        shareInfo.value = shareRes.data
      }
      
      // 检查收藏状态
      try {
        const favoriteRes = await questionBankApi.checkFavorite(res.data.id)
        if (favoriteRes.code === 200) {
          isFavorited.value = favoriteRes.data
        }
      } catch (error) {
        console.error('检查收藏状态失败:', error)
      }
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 返回大厅
const goBack = () => {
  router.push('/share-plaza')
}

// 导入题库 - 打开选择题库弹窗
const handleImport = async () => {
  if (!cards.value || cards.value.length === 0) {
    ElMessage.warning('该题库暂无卡片')
    return
  }
  
  // 打开对话框前加载所有题库
  try {
    isLoadingDialogBanks.value = true
    // 加载所有系统题库
    const systemResponse = await questionBankApi.getSystemBanks('', 1, 1000)
    dialogSystemBanks.value = systemResponse.data.data || []
    // 加载所有用户自定义题库
    const customResponse = await questionBankApi.getUserCustomBanks(1, 1000)
    dialogCustomBanks.value = customResponse.data.data || []
  } catch (error) {
    ElMessage.error('加载题库列表失败')
    return
  } finally {
    isLoadingDialogBanks.value = false
  }
  showBankDialog.value = true
}

// 确认导入到选中的题库
const confirmImport = async () => {
  if (!selectedTargetBankId.value) {
    ElMessage.warning('请选择目标题库')
    return
  }
  
  try {
    const cardIds = cards.value.map(card => card.id)
    
    await questionBankApi.addCardsToBank({
      targetBankId: selectedTargetBankId.value,
      cardIds: cardIds,
      sourceBankId: shareInfo.value?.bankId
    })
    
    ElMessage.success(`成功导入 ${cardIds.length} 张卡片到题库`)
    
    // 重新加载分享详情以更新统计数据
    await loadShareDetail()
    showBankDialog.value = false
    selectedTargetBankId.value = null
    
    // 可选：跳转到首页查看导入的题库
    router.push('/')
  } catch (error: any) {
    ElMessage.error(error.message || '导入失败，请重试')
  }
}

// 创建自定义题库
const handleCreateBank = async () => {
  if (!createBankForm.value.name || !createBankForm.value.topic) {
    ElMessage.warning('请填写题库名称和主题')
    return
  }
  
  try {
    const response = await questionBankApi.createCustomBank({
      name: createBankForm.value.name,
      topic: createBankForm.value.topic,
      description: createBankForm.value.description,
      difficulty: createBankForm.value.difficulty,
      language: createBankForm.value.language
    })
    
    // 先关闭对话框
    showCreateBankDialog.value = false
    
    // 重置表单
    createBankForm.value = {
      name: '',
      topic: '',
      description: '',
      difficulty: 'medium',
      language: '中文'
    }
    
    // 刷新对话框的题库列表
    try {
      const [systemResponse, customResponse] = await Promise.all([
        questionBankApi.getSystemBanks('', 1, 1000),
        questionBankApi.getUserCustomBanks(1, 1000)
      ])
      
      dialogSystemBanks.value = systemResponse.data.data || []
      dialogCustomBanks.value = customResponse.data.data || []
    } catch (error) {
      console.error('刷新题库列表失败:', error)
    }
    
    // 自动选中新创建的题库
    if (response.data && response.data.id) {
      selectedTargetBankId.value = response.data.id
    }
    
    ElMessage.success('题库创建成功！')
  } catch (error) {
    ElMessage.error('创建题库失败，请重试')
  }
}

// 收藏/取消收藏
const handleFavorite = async () => {
  if (!bankInfo.value?.id) return
  
  try {
    if (isFavorited.value) {
      await questionBankApi.removeFavorite(bankInfo.value.id)
      isFavorited.value = false
      ElMessage.success('取消收藏成功')
      // 更新收藏数
      if (shareInfo.value) {
        shareInfo.value.favoriteCount = Math.max(0, shareInfo.value.favoriteCount - 1)
      }
    } else {
      await questionBankApi.addFavorite(bankInfo.value.id)
      isFavorited.value = true
      ElMessage.success('收藏成功')
      // 更新收藏数
      if (shareInfo.value) {
        shareInfo.value.favoriteCount += 1
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 跳转到卡片详情
const goToCard = (cardId: number) => {
  if (!bankInfo.value?.id) return
  // 跳转到对应题库的卡片页面
  router.push({
    path: '/mobile',
    query: {
      bankId: bankInfo.value.id,
      cardId: cardId
    }
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
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadShareDetail()
})
</script>

<style scoped lang="scss">
.share-detail {
  min-height: 100vh;
  background: #f5f7fa;

  .detail-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 20px;
  }

  .back-btn {
    margin-bottom: 20px;
  }

  .bank-info {
    background: white;
    border-radius: 12px;
    padding: 32px;
    margin-bottom: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

    .info-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
      gap: 20px;

      .bank-title {
        font-size: 28px;
        font-weight: 700;
        color: #303133;
        flex: 1;
      }

      .header-actions {
        display: flex;
        gap: 12px;
        flex-shrink: 0;
      }
    }

    .bank-description {
      font-size: 16px;
      color: #606266;
      line-height: 1.8;
      margin-bottom: 20px;
    }

    .bank-meta {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 24px;
      flex-wrap: wrap;

      .card-count {
        font-size: 14px;
        color: #909399;
        margin-left: 8px;
      }
    }

    .bank-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 20px;
      margin-bottom: 24px;
      padding: 20px 0;
      border-top: 1px solid #ebeef5;
      border-bottom: 1px solid #ebeef5;

      .stat-item {
        text-align: center;

        .stat-label {
          display: block;
          font-size: 13px;
          color: #909399;
          margin-bottom: 8px;
        }

        .stat-value {
          display: block;
          font-size: 24px;
          font-weight: 600;
          color: #409eff;
        }
      }
    }

    .creator-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .creator-text {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .creator-name {
          font-size: 15px;
          font-weight: 500;
          color: #303133;
        }

        .share-time {
          font-size: 13px;
          color: #909399;
        }
      }
    }
  }

  .cards-preview {
    background: white;
    border-radius: 12px;
    padding: 32px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);

    .section-title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 24px;
    }

    .empty-cards {
      padding: 40px 0;
    }

    .cards-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .card-item {
      display: flex;
      gap: 16px;
      padding: 20px;
      background: #f8f9fa;
      border-radius: 8px;
      transition: all 0.3s;

      &.clickable {
        cursor: pointer;

        &:hover {
          background: #e8f4ff;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
        }
      }

      .card-number {
        flex-shrink: 0;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #409eff;
        color: white;
        border-radius: 50%;
        font-weight: 600;
        font-size: 14px;
      }

      .card-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 12px;

        .card-question,
        .card-answer {
          font-size: 14px;
          line-height: 1.6;

          strong {
            color: #606266;
            margin-right: 8px;
          }

          span {
            color: #303133;
          }
        }
      }
    }
  }

  .error-state {
    padding: 80px 0;
  }
}

@media (max-width: 768px) {
  .share-detail {
    .detail-container {
      padding: 20px 12px;
    }

    .bank-info {
      padding: 20px;

      .info-header {
        flex-direction: column;

        .bank-title {
          font-size: 22px;
        }

        .header-actions {
          width: 100%;

          .el-button {
            flex: 1;
          }
        }
      }

      .bank-stats {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  }
}

/* 题库选择对话框样式 */
.bank-selection-dialog {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
}

.search-input {
  margin-bottom: 20px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  margin-bottom: 20px;
}

.bank-category {
  display: flex;
  flex-direction: column;
  gap: 12px;

  h4 {
    font-size: 14px;
    font-weight: 600;
    color: #606266;
    padding: 8px 12px;
    background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
    border-radius: 8px;
    margin: 0;
  }
}

.bank-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bank-radio-item {
  width: 100%;
  margin: 0;
  height: auto;

  :deep(.el-radio__label) {
    width: 100%;
    padding: 0;
  }
}

.bank-item-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .bank-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .bank-meta {
    display: flex;
    gap: 12px;
    align-items: center;
    font-size: 13px;
    color: #909399;

    .card-count {
      margin-left: 0;
    }
  }
}

.create-bank-section {
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}
</style>