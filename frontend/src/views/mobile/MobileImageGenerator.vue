<template>
  <div class="mobile-image-generator-container">
    <div class="mobile-header">
      <h1 class="app-title">AI 图片生成器</h1>
      <p class="subtitle">使用火山引擎 AI 技术生成精美图片</p>
    </div>

    <div class="mobile-content">
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
              style="width: 100%; height: 120px"
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
import { imageApi } from '../../api/image'

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
      ElMessage.error('图片生成失败')
    }
  } catch (error) {
    ElMessage.error('图片生成失败，请重试')
  } finally {
    isGenerating.value = false
  }
}

const copyImageUrl = () => {
  if (!generatedImageUrl.value) return
  
  navigator.clipboard.writeText(generatedImageUrl.value)
    .then(() => {
      ElMessage.success('链接已复制')
    })
    .catch(() => {
      ElMessage.error('复制失败')
    })
}

const downloadImage = () => {
  if (!generatedImageUrl.value) return
  
  const link = document.createElement('a')
  link.href = generatedImageUrl.value
  link.download = `ai-generated-${Date.now()}.jpg`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  ElMessage.success('图片下载成功')
}

const viewHistoryImage = (item: HistoryItem) => {
  generatedImageUrl.value = item.url
  prompt.value = item.prompt
}
</script>

<style scoped>
.mobile-image-generator-container {
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
  font-size: 24px;
  margin-bottom: 10px;
  font-weight: bold;
}

.subtitle {
  font-size: 14px;
  opacity: 0.9;
}

.mobile-content {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.generator-card {
  margin-bottom: 30px;
}

.el-form {
  width: 100%;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input {
  border-radius: 8px;
}

.el-select {
  border-radius: 8px;
}

.el-button {
  border-radius: 8px;
  padding: 12px 0;
  font-size: 16px;
}

.result-card {
  margin-bottom: 30px;
  text-align: center;
}

.result-card h3 {
  margin-bottom: 20px;
  font-size: 18px;
  color: #333;
}

.image-container {
  margin-bottom: 20px;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 15px;
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-error {
  text-align: center;
  color: #999;
}

.image-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.history-section {
  margin-top: 30px;
}

.history-section h3 {
  margin-bottom: 15px;
  font-size: 18px;
  color: #333;
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.history-item {
  cursor: pointer;
  background: #f5f5f5;
  border-radius: 8px;
  padding: 10px;
  transition: transform 0.2s;
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.history-prompt {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>