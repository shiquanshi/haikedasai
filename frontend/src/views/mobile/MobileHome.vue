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

      <!-- 卡片展示区域 -->
      <div v-if="showCards" class="cards-section">
        <!-- 卡片头部：标题、进度和放回按钮 -->
        <div class="cards-header">
          <div class="header-left">
            <h2>{{ currentBankName || '我的闪卡' }}</h2>
            <div class="card-progress">
              进度 {{ currentCardIndex + 1 }}/{{ totalCards }}
            </div>
          </div>
          <el-button 
            @click="backToForm"
            class="back-button"
          >
            放回
          </el-button>
        </div>

        <!-- 卡片容器 -->
        <div class="card-container">
          <div 
            v-if="currentCard" 
            class="flip-card" 
            @click="flipCard"
          >
            <div class="flip-card-inner" :class="{ 'flipped': isFlipped }">
              <!-- 问题面 -->
              <div class="flip-card-front">
                <div class="card-content">
                  <div class="card-label">问题</div>
                  <div class="card-text question-text">{{ currentCard.question }}</div>
                  
                  <!-- 语音播放区域 -->
                  <div class="voice-section">
                    <el-button 
                      size="small" 
                      class="voice-button"
                      @click.stop="playQuestionVoice"
                    >
                      <el-icon><Volume /></el-icon>
                      {{ isPlayingQuestion ? '播放中...' : '播放语音' }}
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- 答案面 -->
              <div class="flip-card-back">
                <div class="card-content">
                  <div class="card-label">答案</div>
                  <div class="card-text">{{ currentCard.answer }}</div>
                  
                  <!-- 语音播放区域 -->
                  <div class="voice-section">
                    <el-button 
                      size="small" 
                      class="voice-button"
                      @click.stop="playAnswerVoice"
                    >
                      <el-icon><Volume /></el-icon>
                      {{ isPlayingAnswer ? '播放中...' : '播放语音' }}
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 没有卡片时的提示 -->
          <div v-else class="no-cards">
            <el-empty description="暂无卡片内容" />
          </div>
        </div>

        <!-- 卡片导航按钮 -->
        <div class="card-actions">
          <el-button 
            @click="prevCard"
            :disabled="currentCardIndex === 0"
            class="nav-button"
          >
            上一张
          </el-button>
          <el-button 
            @click="nextCard"
            :disabled="currentCardIndex === totalCards - 1"
            class="nav-button"
          >
            下一张
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import { Search, VideoPlay, Star, Volume } from '@element-plus/icons-vue'
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

// 卡片相关状态
const currentBankName = ref('')
const cards = ref<any[]>([
  {
    question: '什么是HTML？',
    answer: 'HTML是超文本标记语言（HyperText Markup Language）的缩写，是用于创建网页的标准标记语言。'
  },
  {
    question: 'CSS的主要作用是什么？',
    answer: 'CSS（层叠样式表）用于描述HTML或XML文档的呈现方式，控制网页的布局和外观。'
  },
  {
    question: 'JavaScript是什么类型的语言？',
    answer: 'JavaScript是一种高级的、解释执行的编程语言，主要用于网页交互和动态内容。'
  }
])
const currentCardIndex = ref(0)
const isFlipped = ref(false)
const isPlayingQuestion = ref(false)
const isPlayingAnswer = ref(false)

// 计算属性
const currentCard = computed(() => {
  return cards.value[currentCardIndex.value]
})

const totalCards = computed(() => {
  return cards.value.length
})

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
  // 重置卡片状态
  currentCardIndex.value = 0
  isFlipped.value = false
  // 实际项目中这里应该调用API获取题库详情
}

// 返回表单
const backToForm = () => {
  showCards.value = false
  isFlipped.value = false
}

// 翻转卡片
const flipCard = () => {
  isFlipped.value = !isFlipped.value
}

// 上一张卡片
const prevCard = () => {
  if (currentCardIndex.value > 0) {
    currentCardIndex.value--
    isFlipped.value = false
  }
}

// 下一张卡片
const nextCard = () => {
  if (currentCardIndex.value < totalCards.value - 1) {
    currentCardIndex.value++
    isFlipped.value = false
  }
}

// 播放问题语音
const playQuestionVoice = () => {
  isPlayingQuestion.value = true
  // 模拟语音播放
  setTimeout(() => {
    isPlayingQuestion.value = false
  }, 2000)
}

// 播放答案语音
const playAnswerVoice = () => {
  isPlayingAnswer.value = true
  // 模拟语音播放
  setTimeout(() => {
    isPlayingAnswer.value = false
  }, 2000)
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

/* 卡片展示区域样式 */
.cards-section {
  width: 100%;
}

/* 卡片头部样式 - 左上角布局 */
.cards-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  color: white;
  width: 100%;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 5px;
  align-items: flex-start;
}

.cards-header h2 {
  margin: 0;
  font-size: 22px;
  font-weight: bold;
}

.card-progress {
  background: rgba(255, 255, 255, 0.2);
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(10px);
}

.back-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  backdrop-filter: blur(10px);
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 20px;
  color: white;
  transition: all 0.3s ease;
}

.back-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 卡片容器样式 */
.card-container {
  width: 100%;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  margin-bottom: 30px;
}

/* 翻转卡片样式 */
.flip-card {
  width: 100%;
  height: 400px;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  text-align: center;
  transition: transform 0.8s cubic-bezier(0.2, 0.85, 0.4, 1.275);
  transform-style: preserve-3d;
}

.flip-card.flipped .flip-card-inner {
  transform: rotateY(180deg);
}

.flip-card-front,
.flip-card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

/* 突出卡片大小 */
.flip-card-front {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.flip-card-back {
  background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  color: #333;
  transform: rotateY(180deg);
}

/* 卡片内容样式 */
.card-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  box-sizing: border-box;
}

.card-label {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px;
  opacity: 0.9;
  font-weight: 600;
}

/* 问题文本放大 */
.card-text {
  font-size: 16px;
  line-height: 1.6;
  margin: 10px 0;
  font-weight: 500;
  overflow-y: auto;
  padding: 10px;
  max-height: 200px;
  word-wrap: break-word;
  width: 100%;
}

/* 问题区域特别放大 */
.question-text {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.8;
}

/* 语音区域缩小 */
.voice-section {
  width: 100%;
  display: flex;
  justify-content: center;
}

.voice-button {
  background: rgba(255, 255, 255, 0.2) !important;
  border: none !important;
  color: inherit !important;
  font-size: 12px !important;
  padding: 6px 12px !important;
  border-radius: 20px !important;
}

.flip-card-back .voice-button {
  background: rgba(0, 0, 0, 0.1) !important;
}

/* 底部按钮区域 */
.card-actions {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 20px;
}

.nav-button {
  flex: 1;
  padding: 10px 0 !important;
  font-size: 14px !important;
  height: auto !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
}

/* 无卡片提示 */
.no-cards {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
  background: rgba(240, 245, 255, 0.6);
  border-radius: 15px;
}
</style>