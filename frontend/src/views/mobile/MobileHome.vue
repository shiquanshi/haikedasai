<template>
  <div class="mobile-home-container">
    <!-- 头部区域 -->
    <div class="mobile-header">
      <h1 class="app-title">知识闪卡</h1>
      <div class="header-actions">
        <el-button 
          v-if="!userStore.isLoggedIn"
          type="primary" 
          @click="$router.push('/mobile/login')"
          size="normal"
          round
        >
          登录
        </el-button>
        <div class="user-info" v-if="userStore.isLoggedIn">
          <span class="username">{{ userStore.userInfo?.username }}</span>
          <el-button size="small" @click="handleLogout" type="danger" round>退出</el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="mobile-content">
      <!-- 创建闪卡表单 -->
      <div class="create-card-form" v-if="!showCards">
        <!-- 主题输入 -->
        <el-input
          v-model="topic"
          placeholder="学习主题"
          size="large"
          clearable
          class="form-item"
          :prefix-icon="Search"
        />
        
        <!-- 场景输入 -->
        <el-input
          v-model="scenario"
          placeholder="学习场景（可选）"
          size="large"
          clearable
          class="form-item"
          :prefix-icon="VideoPlay"
        />
        
        <!-- 其他选项 -->
        <div class="form-options">
          <!-- 闪卡数量 -->
          <el-select 
            v-model="cardCount"
            placeholder="闪卡数量"
            size="large"
            class="form-select"
          >
            <el-option label="1张" value="1"></el-option>
            <el-option label="2张" value="2"></el-option>
            <el-option label="5张" value="5"></el-option>
            <el-option label="10张" value="10"></el-option>
            <el-option label="20张" value="20"></el-option>
          </el-select>
          
          <!-- 难度级别 -->
          <el-select 
            v-model="difficulty"
            placeholder="难度级别"
            size="large"
            class="form-select"
          >
            <el-option label="初级" value="easy"></el-option>
            <el-option label="中级" value="medium"></el-option>
            <el-option label="高级" value="hard"></el-option>
          </el-select>
          
          <!-- 语言选择 -->
          <el-select 
            v-model="language"
            placeholder="语言"
            size="large"
            class="form-select"
          >
            <el-option label="中文" value="中文"></el-option>
            <el-option label="英文" value="英文"></el-option>
            <el-option label="日语" value="日语"></el-option>
            <el-option label="韩语" value="韩语"></el-option>
            <el-option label="法语" value="法语"></el-option>
            <el-option label="德语" value="德语"></el-option>
            <el-option label="西班牙语" value="西班牙语"></el-option>
            <el-option label="俄语" value="俄语"></el-option>
          </el-select>
          
          <!-- 配图选项 -->
          <el-checkbox 
            v-model="withImages"
            size="large"
            class="form-checkbox"
          >
            生成配图描述
          </el-checkbox>
        </div>
        
        <!-- 生成闪卡按钮 -->
          <el-button 
            type="primary" 
            size="large" 
            @click="generateCards"
            class="generate-button"
            :disabled="!topic || isGenerating"
            :loading="isGenerating"
          >
            <el-icon v-if="!isGenerating"><Star /></el-icon> 
            {{ isGenerating ? '生成中...' : '生成闪卡' }}
          </el-button>
      </div>

      <!-- 题库功能 -->
      <div class="question-bank" v-if="!showCards">
        <div class="bank-header">
          <h2>题库</h2>
          <el-input
            v-model="bankSearchText"
            placeholder="搜索题库"
            size="small"
            clearable
            class="bank-search"
            :prefix-icon="Search"
          />
        </div>
        
        <div class="bank-list">
          <el-divider content-position="left">推荐题库</el-divider>
          <el-loading v-loading="isLoadingBanks" element-loading-text="加载中...">
            <el-card
              v-for="bank in systemBanks"
              :key="bank.id"
              class="bank-card"
              @click="loadBankCards(bank.id)"
            >
              <template #header>
                <div class="card-header">
                  <span>{{ bank.name }}</span>
                  <el-tag size="small">{{ bank.cardCount }}张卡片</el-tag>
                </div>
              </template>
              <div class="card-content">
                <p>{{ bank.description }}</p>
              </div>
            </el-card>
          </el-loading>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import { Search, VideoPlay, Star } from '@element-plus/icons-vue'
import { questionBankApi } from '../../api/questionBank'
import { ElMessage } from 'element-plus'

// 用户状态
const userStore = useUserStore()

// 表单数据
const topic = ref('')
const scenario = ref('')
const cardCount = ref('5')
const difficulty = ref('medium')
const language = ref('中文')
const withImages = ref(false)

// 状态管理
const isGenerating = ref(false)
const showCards = ref(false)
const bankSearchText = ref('')
const isLoadingBanks = ref(false)
const systemBanks = ref([
  {
    id: 1,
    name: '计算机基础知识',
    description: '包含计算机基础概念、操作系统、网络等知识点',
    cardCount: 20
  },
  {
    id: 2,
    name: '英语词汇1000',
    description: '精选1000个常用英语词汇',
    cardCount: 50
  }
])

// 退出登录
const handleLogout = async () => {
  try {
    await userStore.logout()
    ElMessage.success('退出成功')
    window.location.href = '/mobile/login'
  } catch (error) {
    ElMessage.error('退出失败')
  }
}

// 生成闪卡
const generateCards = async () => {
  if (!topic.value.trim()) {
    ElMessage.warning('请输入学习主题')
    return
  }

  isGenerating.value = true
  try {
    // 使用流式API生成闪卡
    questionBankApi.generateAIBankStream(
      {
        topic: topic.value,
        scenario: scenario.value,
        cardCount: parseInt(cardCount.value),
        difficulty: difficulty.value,
        language: language.value,
        withImages: withImages.value
      },
      (content) => {
        // 处理收到的内容
        try {
          const parsedContent = JSON.parse(content)
          // 可以在这里处理不同类型的消息
        } catch (e) {
          // 处理普通文本内容
        }
      },
      (error) => {
        ElMessage.error(error)
        isGenerating.value = false
      },
      () => {
        // 生成完成
        ElMessage.success('闪卡生成成功')
        isGenerating.value = false
        showCards.value = true
      }
    )
  } catch (error) {
    ElMessage.error('生成失败，请重试')
    isGenerating.value = false
  }
}

// 加载题库卡片
const loadBankCards = (bankId: number) => {
  ElMessage.info(`加载题库 ${bankId}`)
  showCards.value = true
  // 实际项目中这里应该调用API获取题库详情
}
</script>

<style scoped>
.mobile-home-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  box-sizing: border-box;
}

.mobile-header {
  text-align: center;
  margin-bottom: 30px;
  color: white;
}

.app-title {
  font-size: 28px;
  margin-bottom: 15px;
  font-weight: bold;
}

.mobile-content {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.form-item {
  margin-bottom: 20px;
}

.form-options {
  margin-bottom: 25px;
}

.form-select {
  margin-bottom: 15px;
  width: 100%;
}

.form-checkbox {
  margin-top: 15px;
  display: block;
}

.generate-button {
  width: 100%;
  padding: 16px 0;
  font-size: 18px;
  font-weight: bold;
}

.question-bank {
  margin-top: 30px;
}

.bank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.bank-header h2 {
  margin: 0;
  font-size: 20px;
}

.bank-search {
  width: 150px;
}

.bank-card {
  margin-bottom: 15px;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
}

.bank-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 15px 0 15px;
}

.card-content {
  margin-top: 10px;
  color: #666;
  padding: 0 15px 15px 15px;
  font-size: 16px;
  line-height: 1.6;
}

/* 增大卡片内文本内容区域 */
.el-card__body {
  padding: 0 !important;
}

/* 调整卡片标题样式 */
.card-header span {
  font-size: 18px;
  font-weight: bold;
}

/* 调整标签样式 */
.card-header .el-tag {
  padding: 4px 12px;
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  color: white;
  font-size: 16px;
}
</style>