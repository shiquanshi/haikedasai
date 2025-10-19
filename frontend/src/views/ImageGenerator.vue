<template>
  <div class="image-generator-container">
    <div class="wave-decoration"></div>
    
    <div class="main-content">
      <div class="header">
        <div class="logo-icon">🎨</div>
        <h1>AI 图片生成器</h1>
        <p class="subtitle">使用火山引擎 AI 技术生成精美图片</p>
      </div>

      <div class="generator-card">
        <el-form @submit.prevent="generateImage" label-position="top">
          <el-form-item label="图片描述">
            <el-input
              v-model="prompt"
              type="textarea"
              :rows="4"
              placeholder="请输入图片描述，例如：一只可爱的小猫在花园里玩耍，阳光明媚，高清摄影"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="图片数量">
            <el-select v-model="imageCount" style="width: 100%">
              <el-option label="1张" :value="1" />
              <el-option label="2张" :value="2" />
              <el-option label="4张" :value="4" />
            </el-select>
          </el-form-item>

          <el-form-item label="图片尺寸">
            <el-select v-model="imageSize" style="width: 100%">
              <el-option label="1024 x 1024" value="1024x1024" />
              <el-option label="512 x 512" value="512x512" />
              <el-option label="256 x 256" value="256x256" />
            </el-select>
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            :loading="isGenerating"
            :disabled="!prompt.trim()"
            @click="generateImage"
            style="width: 100%"
          >
            <span v-if="!isGenerating">🚀 开始生成</span>
            <span v-else>生成中...</span>
          </el-button>
        </el-form>
      </div>

      <!-- 生成结果展示 -->
      <div v-if="generatedImageUrl" class="result-card">
        <h3>生成结果</h3>
        <div class="image-container">
          <el-image
            :src="generatedImageUrl"
            fit="contain"
            :preview-src-list="[generatedImageUrl]"
          >
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <span>图片加载失败</span>
              </div>
            </template>
          </el-image>
        </div>
        <div class="image-actions">
          <el-button @click="copyImageUrl">复制链接</el-button>
          <el-button @click="downloadImage">下载图片</el-button>
        </div>
      </div>

      <!-- 历史记录 -->
      <div v-if="history.length > 0" class="history-section">
        <h3>生成历史</h3>
        <div class="history-grid">
          <div
            v-for="(item, index) in history"
            :key="index"
            class="history-item"
            @click="viewHistoryImage(item)"
          >
            <el-image
              :src="item.url"
              fit="cover"
              style="width: 100%; height: 150px"
            />
            <div class="history-prompt">{{ item.prompt }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { imageApi } from '../api/image'

const prompt = ref('')
const imageCount = ref(1)
const imageSize = ref('1024x1024')
const isGenerating = ref(false)
const generatedImageUrl = ref('')

interface HistoryItem {
  url: string
  prompt: string
  timestamp: number
}

const history = ref<HistoryItem[]>([])

const generateImage = async () => {
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入图片描述')
    return
  }

  isGenerating.value = true
  try {
    const response = await imageApi.generateImage({
      prompt: prompt.value,
      n: imageCount.value,
      size: imageSize.value
    })

    if (response.code === 200 && response.data) {
      generatedImageUrl.value = response.data
      ElMessage.success('图片生成成功！')
      
      // 添加到历史记录
      history.value.unshift({
        url: response.data,
        prompt: prompt.value,
        timestamp: Date.now()
      })
      
      // 限制历史记录数量
      if (history.value.length > 10) {
        history.value = history.value.slice(0, 10)
      }
    } else {
      ElMessage.error(response.message || '图片生成失败')
    }
  } catch (error: any) {
    console.error('生成图片失败:', error)
    ElMessage.error(error.message || '生成图片时发生错误')
  } finally {
    isGenerating.value = false
  }
}

const copyImageUrl = () => {
  if (generatedImageUrl.value) {
    navigator.clipboard.writeText(generatedImageUrl.value)
    ElMessage.success('链接已复制到剪贴板')
  }
}

const downloadImage = () => {
  if (generatedImageUrl.value) {
    const link = document.createElement('a')
    link.href = generatedImageUrl.value
    link.download = `generated-image-${Date.now()}.png`
    link.click()
  }
}

const viewHistoryImage = (item: HistoryItem) => {
  generatedImageUrl.value = item.url
  prompt.value = item.prompt
}
</script>

<style scoped>
.image-generator-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  position: relative;
  overflow: hidden;
}

.wave-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  clip-path: ellipse(150% 100% at 50% 0%);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.header {
  text-align: center;
  color: white;
  margin-bottom: 40px;
}

.logo-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.header h1 {
  font-size: 48px;
  margin-bottom: 16px;
  font-weight: 700;
}

.subtitle {
  font-size: 18px;
  opacity: 0.9;
}

.generator-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  margin-bottom: 30px;
}

.result-card {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  margin-bottom: 30px;
}

.result-card h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 24px;
}

.image-container {
  margin-bottom: 20px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: #999;
}

.image-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.history-section {
  background: white;
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.history-section h3 {
  margin-bottom: 20px;
  color: #333;
  font-size: 24px;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.history-item {
  cursor: pointer;
  border-radius: 10px;
  overflow: hidden;
  transition: transform 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.history-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.history-prompt {
  padding: 10px;
  font-size: 14px;
  color: #666;
  background: #f9f9f9;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}
</style>