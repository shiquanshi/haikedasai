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
          <!-- 推荐题库 -->
          <h3>推荐题库</h3>
          <div v-if="isLoadingSystemBanks" class="loading-state">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <template v-else>
            <div
              v-for="bank in systemBanks"
              :key="bank.id"
              class="bank-card"
              @click="handleSelectBank(bank.id, bank.name, bank.type)"
            >
              <div class="bank-name">{{ bank.name }}</div>
              <div class="bank-description" v-if="bank.description">{{ bank.description }}</div>
              <div class="bank-info">
                <span class="bank-count">
                  <el-icon><Document /></el-icon>
                  {{ bank.cardCount || 0 }}张
                </span>
              </div>
              <div class="bank-meta">
                <el-tag v-if="bank.difficulty" size="small" type="warning">{{ bank.difficulty }}</el-tag>
                <el-tag v-if="bank.language" size="small" type="info">{{ bank.language }}</el-tag>
                <span v-if="bank.topic" class="bank-topic">{{ bank.topic }}</span>
              </div>
              <div class="bank-actions" @click.stop>
                <el-button
                  type="warning"
                  size="small"
                  :icon="Edit"
                  @click="showEditBankDialogFunc(bank)"
                  circle
                  title="编辑题库"
                />
                <el-button
                  type="primary"
                  size="small"
                  :icon="Download"
                  @click="exportBank(bank.id, bank.name)"
                  circle
                  title="导出Excel"
                />
              </div>
            </div>
            
            <div v-if="systemBanks.length === 0" class="empty-state">
              <el-icon><Box /></el-icon>
              <p>暂无推荐题库</p>
            </div>
          </template>
          
          <!-- 我的题库 -->
          <template v-if="userStore.isLoggedIn">
            <h3>我的题库</h3>
            <div v-if="isLoadingCustomBanks" class="loading-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <template v-else>
              <div
                v-for="bank in customBanks"
                :key="bank.id"
                class="bank-card"
                @click="loadBankCards(bank.id, bank.name, bank.type)"
              >
                <div class="bank-name">{{ bank.name }}</div>
                <div class="bank-description" v-if="bank.description">{{ bank.description }}</div>
                <div class="bank-info">
                  <span class="bank-count">
                    <el-icon><Document /></el-icon>
                    {{ bank.cardCount || 0 }}张
                  </span>
                </div>
                <div class="bank-meta">
                  <el-tag v-if="bank.difficulty" size="small" type="warning">{{ bank.difficulty }}</el-tag>
                  <el-tag v-if="bank.language" size="small" type="info">{{ bank.language }}</el-tag>
                  <span v-if="bank.topic" class="bank-topic">{{ bank.topic }}</span>
                </div>
                <div class="bank-actions" @click.stop>
                  <el-button
                    type="warning"
                    size="small"
                    :icon="Edit"
                    @click="showEditBankDialogFunc(bank)"
                    circle
                    title="编辑题库"
                  />
                  <el-button
                    type="primary"
                    size="small"
                    :icon="Download"
                    @click="exportBank(bank.id, bank.name)"
                    circle
                    title="导出Excel"
                  />
                  <el-button
                    type="success"
                    size="small"
                    :icon="Share"
                    @click="handleShareBank(bank.id)"
                    circle
                    title="分享题库"
                  />
                  <el-button
                    type="danger"
                    size="small"
                    :icon="Delete"
                    @click="handleDeleteBank(bank.id)"
                    circle
                    title="删除题库"
                  />
                </div>
              </div>

              <div v-if="customBanks.length === 0" class="empty-state">
                <el-icon><Box /></el-icon>
                <p>暂无自定义题库</p>
              </div>
            </template>
          </template>
          
          <!-- 我的分享 -->
          <template v-if="userStore.isLoggedIn">
            <h3>我的分享</h3>
            <template v-if="isLoadingSharedBanks">
              <div class="loading-state">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载中...</span>
              </div>
            </template>
            <template v-else>
              <div
                v-for="bank in sharedBanks"
                :key="bank.id"
                class="bank-card"
                @click="loadBankCards(bank.id, bank.name, bank.type)"
              >
                <div class="bank-name">{{ bank.name }}</div>
                <div class="bank-info">
                  <span class="bank-count">
                    <el-icon><Document /></el-icon>
                    {{ bank.cardCount || 0 }}张
                  </span>
                </div>
                <div class="bank-meta">
                  <div style="display: flex; gap: 6px; align-items: center; flex-wrap: nowrap; flex-shrink: 1; overflow: hidden;">
                    <el-tag v-if="bank.difficulty" size="small" type="warning">{{ bank.difficulty }}</el-tag>
                    <el-tag v-if="bank.language" size="small" type="info">{{ bank.language }}</el-tag>
                    <span class="share-code-tag">
                      <el-icon><Star /></el-icon>
                      分享码: {{ bank.shareCode }}
                    </span>
                  </div>
                  <span class="bank-time">
                    <el-icon><Clock /></el-icon>
                    {{ formatDate(new Date(bank.createdAt || bank.createTime), 'MM-DD HH:mm') }}
                  </span>
                </div>
                <div class="bank-actions" @click.stop>
                  <el-button
                    type="primary"
                    size="small"
                    :icon="Download"
                    @click="exportBank(bank.id, bank.name)"
                    circle
                    title="导出Excel"
                  />
                  <el-button
                    type="success"
                    size="small"
                    :icon="DocumentCopy"
                    @click="copyShareCodeDirect(bank.shareCode)"
                    circle
                    title="复制分享码"
                  />
                  <el-button
                    type="danger"
                    size="small"
                    :icon="Delete"
                    @click="handleDeleteBank(bank.id)"
                    circle
                    title="删除题库"
                  />
                </div>
              </div>

              <div v-if="sharedBanks.length === 0" class="empty-state">
                <el-icon><Box /></el-icon>
                <p>暂无分享记录</p>
              </div>
            </template>
          </template>
          
          <!-- 历史生成记录 -->
          <template v-if="userStore.isLoggedIn">
            <h3>历史生成记录</h3>
            <div v-if="isLoadingHistory" class="loading-state">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载中...</span>
            </div>
            <template v-else>
              <div
                v-for="record in historyRecords"
                :key="record.id"
                class="bank-card"
                @click="loadBankCards(record.id, record.name, record.type)"
              >
                <div class="bank-name">{{ record.name }}</div>
                <div class="bank-info">
                  <span class="bank-count">
                    <el-icon><Document /></el-icon>
                    {{ record.cardCount || 0 }}张
                  </span>
                </div>
                <div class="bank-meta">
                  <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                    <el-tag v-if="record.difficulty" size="small" type="warning">{{ record.difficulty }}</el-tag>
                    <el-tag v-if="record.language" size="small" type="info">{{ record.language }}</el-tag>
                  </div>
                  <div style="display: flex; flex-direction: column; align-items: flex-end; gap: 8px;">
                    <span class="bank-time">
                      <el-icon><Clock /></el-icon>
                      {{ formatDate(new Date(record.createdAt || record.createTime), 'MM-DD HH:mm') }}
                    </span>
                    <div class="bank-actions" @click.stop>
                      <el-button
                        type="primary"
                        size="small"
                        :icon="Download"
                        @click="exportBank(record.id, record.name)"
                        circle
                        title="导出Excel"
                      />
                      <el-button
                        type="success"
                        size="small"
                        :icon="Share"
                        @click="handleShareBank(record.id)"
                        circle
                        title="分享题库"
                      />
                      <el-button
                        type="danger"
                        size="small"
                        :icon="Delete"
                        @click="handleDeleteBank(record.id)"
                        circle
                        title="删除题库"
                      />
                    </div>
                  </div>
                </div>
              </div>
              
              <div v-if="historyRecords.length === 0" class="empty-state">
                <el-icon><Box /></el-icon>
                <p>暂无历史生成记录</p>
              </div>
            </template>
          </template>
          
          <!-- 功能按钮 -->
          <div class="bank-functions" v-if="userStore.isLoggedIn">
            <el-button 
              type="primary" 
              size="large"
              @click="showCreateBankDialog = true"
              class="function-button"
              :icon="Plus"
            >
              创建新题库
            </el-button>
            <el-button 
              type="success" 
              size="large"
              @click="handleShowImportDialog"
              class="function-button"
              :icon="Upload"
            >
              导入Excel题库
            </el-button>
            <el-button 
              type="warning" 
              size="large"
              @click="showAccessDialog = true"
              class="function-button"
              :icon="Search"
            >
              访问分享题库
            </el-button>
          </div>
        </div>
      </div>

      <!-- 卡片展示区域 -->
      <div v-if="showCards" class="cards-section">
        <!-- 卡片头部：标题、进度和操作按钮 -->
        <div class="cards-header">
          <div class="header-left">
            <h2>{{ currentBankName || '我的闪卡' }}</h2>
            <div class="card-progress">
              进度 {{ currentCardIndex + 1 }}/{{ totalCards }}
            </div>
          </div>
          <div class="header-actions">
            <el-button 
              type="primary"
              size="small"
              @click="handleAddNewCard"
              v-if="currentBankType === 'custom'"
              class="header-operation-button"
            >
              <el-icon><Plus /></el-icon>
            </el-button>
            <el-button 
              type="warning"
              size="small"
              @click="handleEditCurrentCard"
              v-if="currentBankType === 'custom' || currentBankType === 'ai'"
              :disabled="!currentCard"
              class="header-operation-button"
            >
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button 
              type="danger"
              size="small"
              @click="handleDeleteCurrentCard"
              v-if="currentBankType === 'custom'"
              :disabled="!currentCard || totalCards <= 1"
              class="header-operation-button"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button 
              @click="backToForm"
              class="back-button"
            >
              放回
            </el-button>
          </div>
        </div>

        <!-- 卡片容器 -->
        <div class="card-container">
          <div 
            v-if="currentCard" 
            class="flip-card" 
            :class="{ 'flipped': isFlipped }"
            @click="flipCard"
          >
            <div class="flip-card-inner">
              <!-- 问题面 -->
              <div class="flip-card-front">
                <div class="card-badge">问题</div>
                <div class="card-content">
                  <div class="card-text question-text">{{ currentCard.question }}</div>
                </div>
                <div class="card-footer">
                  <div class="tap-hint">点击翻转查看答案</div>
                  <el-button 
                    size="small" 
                    class="voice-button"
                    @click.stop="playQuestionVoice"
                  >
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                      <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/>
                    </svg>
                    {{ isPlayingQuestion ? '播放中' : '语音' }}
                  </el-button>
                </div>
              </div>

              <!-- 答案面 -->
              <div class="flip-card-back">
                <div class="card-badge">答案</div>
                <div class="card-content">
                  <div class="card-text">{{ currentCard.answer }}</div>
                </div>
                <div class="card-footer">
                  <div class="tap-hint">点击翻转查看问题</div>
                  <el-button 
                    size="small" 
                    class="voice-button"
                    @click.stop="playAnswerVoice"
                  >
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                      <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/>
                    </svg>
                    {{ isPlayingAnswer ? '播放中' : '语音' }}
                  </el-button>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 没有卡片时的提示 -->
          <div v-else class="no-cards">
            <el-empty description="暂无卡片内容" />
          </div>
        </div>

        <!-- 卡片进度指示器 -->
        <div class="card-progress-bar" v-if="totalCards > 0">
          <div class="progress-dots">
            <span 
              v-for="(card, index) in cards" 
              :key="index"
              class="progress-dot"
              :class="{ 'active': index === currentCardIndex }"
              @click="goToCard(index)"
            ></span>
          </div>
        </div>

        <!-- 卡片导航按钮 -->
        <div class="card-actions">
          <el-button 
            @click="prevCard"
            :disabled="currentCardIndex === 0"
            class="nav-button"
          >
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" style="margin-right: 4px;">
              <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"/>
            </svg>
            上一张
          </el-button>
          <el-button 
            @click="nextCard"
            :disabled="currentCardIndex === totalCards - 1"
            class="nav-button"
          >
            下一张
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" style="margin-left: 4px;">
              <path d="M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/>
            </svg>
          </el-button>
        </div>


      </div>
    </div>

    <!-- 分享题库对话框 -->
    <el-dialog
      v-model="showShareDialog"
      title="分享题库"
      width="90%"
      :close-on-click-modal="false"
    >
      <div v-if="!shareCode">
        <el-form label-width="80px">
          <el-form-item label="有效期">
            <el-select v-model="shareExpireHours" placeholder="永久有效" style="width: 100%">
              <el-option label="24小时" :value="24" />
              <el-option label="3天" :value="72" />
              <el-option label="7天" :value="168" />
              <el-option label="30天" :value="720" />
              <el-option label="永久有效" :value="null" />
            </el-select>
          </el-form-item>
        </el-form>
        <div style="text-align: center; margin-top: 20px">
          <el-button type="primary" @click="confirmGenerateShare" size="large">生成分享码</el-button>
          <el-button @click="showShareDialog = false" size="large">取消</el-button>
        </div>
      </div>
      <div v-else class="share-content">
        <p style="margin-bottom: 15px; font-size: 14px">分享码已生成，请复制分享码给其他用户：</p>
        <el-input
          v-model="shareCode"
          readonly
          size="large"
        >
          <template #append>
            <el-button @click="copyShareCode" type="primary">复制</el-button>
          </template>
        </el-input>
        <p style="margin-top: 15px; color: #909399; font-size: 13px">
          其他用户可以通过此分享码访问您的题库
        </p>
        <p v-if="shareExpireHours" style="margin-top: 10px; color: #e6a23c; font-size: 13px">
          ⏰ 有效期：{{ shareExpireHours }}小时
        </p>
      </div>
    </el-dialog>

    <!-- 创建题库对话框 -->
    <el-dialog
      v-model="showCreateBankDialog"
      title="创建自定义题库"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form :model="createBankForm" label-width="80px">
        <el-form-item label="题库名称" required>
          <el-input v-model="createBankForm.name" placeholder="请输入题库名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="主题" required>
          <el-input v-model="createBankForm.topic" placeholder="请输入主题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="createBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入题库描述（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="createBankForm.difficulty" placeholder="请选择难度" style="width: 100%">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="createBankForm.language" placeholder="请选择语言" style="width: 100%">
            <el-option label="中文" value="中文" />
            <el-option label="英文" value="英文" />
            <el-option label="日语" value="日语" />
            <el-option label="韩语" value="韩语" />
            <el-option label="法语" value="法语" />
            <el-option label="德语" value="德语" />
            <el-option label="西班牙语" value="西班牙语" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="text-align: center">
          <el-button @click="showCreateBankDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleCreateBank" size="large">创建</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入题库对话框 -->
    <el-dialog
      v-model="showImportDialog"
      title="从Excel导入题库"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form :model="importForm" label-width="100px" class="import-form">
        <el-form-item label="导入方式" required>
          <el-radio-group v-model="importForm.importMode">
            <el-radio label="new">创建新题库</el-radio>
            <el-radio label="existing">导入到已有题库</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 选择已有题库 -->
        <el-form-item v-if="importForm.importMode === 'existing'" label="选择题库" required>
          <el-select 
            v-model="importForm.targetBankId" 
            placeholder="请选择要导入的题库" 
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="bank in userBanks"
              :key="bank.id"
              :label="bank.name"
              :value="bank.id"
            >
              <span>{{ bank.name }}</span>
              <span style="float: right; color: var(--el-text-color-secondary); font-size: 13px">
                {{ bank.cardCount }} 张卡片
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <!-- 新建题库信息 -->
        <template v-if="importForm.importMode === 'new'">
          <el-form-item label="题库名称">
            <el-input v-model="importForm.bankName" placeholder="可选,不填则使用文件名" />
          </el-form-item>
          <el-form-item label="题库描述">
            <el-input v-model="importForm.description" type="textarea" :rows="3" placeholder="可选,请输入题库描述" />
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="importForm.difficulty" placeholder="选择难度" style="width: 100%">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>
          <el-form-item label="语言">
            <el-select v-model="importForm.language" placeholder="选择语言" style="width: 100%">
              <el-option label="中文" value="中文" />
              <el-option label="英文" value="英文" />
              <el-option label="日语" value="日语" />
              <el-option label="韩语" value="韩语" />
              <el-option label="法语" value="法语" />
              <el-option label="德语" value="德语" />
              <el-option label="西班牙语" value="西班牙语" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx"
            :on-change="handleFileChange"
            :file-list="fileList"
          >
            <el-button type="primary">选择Excel文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                仅支持.xlsx格式的Excel文件<br/>
                文件格式:第一列-问题,第二列-答案,第三列-图片URL(可选)
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showImportDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleImport" :loading="isImporting" size="large">导入</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑题库对话框 -->
    <el-dialog
      v-model="showEditBankDialog"
      title="编辑题库"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form :model="editBankForm" label-width="80px">
        <el-form-item label="题库名称" required>
          <el-input
            v-model="editBankForm.name"
            placeholder="请输入题库名称"
            maxlength="50"
            size="large"
          />
        </el-form-item>
        <el-form-item label="题库描述">
          <el-input
            v-model="editBankForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入题库描述"
            maxlength="200"
            size="large"
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editBankForm.difficulty" placeholder="请选择难度" style="width: 100%" size="large">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="editBankForm.language" placeholder="请选择语言" style="width: 100%" size="large">
            <el-option label="中文" value="中文" />
            <el-option label="English" value="English" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditBankDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleUpdateBank" size="large">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增卡片对话框 -->
    <el-dialog
      v-model="showAddCardDialog"
      title="新增卡片"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form :model="addCardForm" label-width="80px">
        <el-form-item label="问题" required>
          <el-input
            v-model="addCardForm.question"
            type="textarea"
            :rows="3"
            placeholder="请输入问题内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input
            v-model="addCardForm.answer"
            type="textarea"
            :rows="3"
            placeholder="请输入答案内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="问题图片">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleQuestionImageChange"
            :file-list="questionImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="答案图片">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleAnswerImageChange"
            :file-list="answerImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddCardDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleSubmitNewCard" size="large">创建</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑卡片对话框 -->
    <el-dialog
      v-model="showEditCardDialog"
      title="编辑卡片"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form :model="editCardForm" label-width="80px">
        <el-form-item label="问题" required>
          <el-input
            v-model="editCardForm.question"
            type="textarea"
            :rows="3"
            placeholder="请输入问题内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input
            v-model="editCardForm.answer"
            type="textarea"
            :rows="3"
            placeholder="请输入答案内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="问题图片">
          <div v-if="editCardForm.questionImage" class="current-image-preview">
            <el-image :src="editCardForm.questionImage" fit="contain" style="max-width: 100%; max-height: 200px" />
            <el-button @click="editCardForm.questionImage = ''" type="danger" size="small" style="margin-top: 8px">删除图片</el-button>
          </div>
          <el-upload
            v-else
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleEditQuestionImageChange"
            :file-list="editQuestionImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="答案图片">
          <div v-if="editCardForm.answerImage" class="current-image-preview">
            <el-image :src="editCardForm.answerImage" fit="contain" style="max-width: 100%; max-height: 200px" />
            <el-button @click="editCardForm.answerImage = ''" type="danger" size="small" style="margin-top: 8px">删除图片</el-button>
          </div>
          <el-upload
            v-else
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleEditAnswerImageChange"
            :file-list="editAnswerImageFileList"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">
                支持jpg/png/gif格式,大小不超过2MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditCardDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="handleSubmitEditCard" size="large">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 访问分享题库对话框 -->
    <el-dialog
      v-model="showAccessDialog"
      title="访问分享题库"
      width="90%"
      :close-on-click-modal="false"
    >
      <el-form label-width="80px">
        <el-form-item label="分享码">
          <el-input
            v-model="accessShareCode"
            placeholder="请输入6位分享码"
            maxlength="6"
            size="large"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAccessDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="accessSharedBank" size="large">访问</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useUserStore } from '../../stores/user'
import { Search, VideoPlay, Star, Edit, Share, Delete, Upload, Plus, Document, Box, Loading, Clock, Download, DocumentCopy } from '@element-plus/icons-vue'
import { questionBankApi } from '../../api/questionBank'
import { ElMessage, ElMessageBox } from 'element-plus'

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
const isLoadingCustomBanks = ref(false)
const isLoadingHistory = ref(false)
const isLoadingSharedBanks = ref(false)
const showCreateBankDialog = ref(false)
const showAccessDialog = ref(false)
const showShareDialog = ref(false)
const shareCode = ref('')
const shareExpireHours = ref<number | null>(null)
const currentSharingBankId = ref<number | null>(null)
const accessShareCode = ref('')

// 创建题库相关
const createBankForm = ref({
  name: '',
  topic: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})

// 导入题库相关
const showImportDialog = ref(false)
const importForm = ref({
  importMode: 'new',
  targetBankId: null as number | null,
  bankName: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})
const fileList = ref<any[]>([])
const isImporting = ref(false)
const uploadRef = ref()
const userBanks = ref<any[]>([])

// 编辑题库相关
const showEditBankDialog = ref(false)
const editBankForm = ref({
  id: null as number | null,
  name: '',
  description: '',
  difficulty: 'medium',
  language: '中文'
})

// 题库数据
const systemBanks = ref<any[]>([])
const customBanks = ref<any[]>([])
const historyRecords = ref<any[]>([])
const sharedBanks = ref<any[]>([])

// 分页状态
const systemPage = ref(1)
const systemPageSize = ref(20)
const systemTotal = ref(0)
const customPage = ref(1)
const customPageSize = ref(20)
const customTotal = ref(0)

// 卡片相关状态
const currentBankId = ref<number | null>(null)
const currentBankName = ref('')
const currentBankType = ref<string>('')
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

// 新增卡片相关
const showAddCardDialog = ref(false)
const addCardForm = ref({
  question: '',
  answer: '',
  questionImage: null as File | null,
  answerImage: null as File | null
})
const questionImageFileList = ref<any[]>([])
const answerImageFileList = ref<any[]>([])

// 编辑卡片相关
const showEditCardDialog = ref(false)
const editCardForm = ref({
  id: null as number | null,
  question: '',
  answer: '',
  questionImage: '',
  answerImage: ''
})
const editQuestionImageFileList = ref<any[]>([])
const editAnswerImageFileList = ref<any[]>([])
const newEditQuestionImage = ref<File | null>(null)
const newEditAnswerImage = ref<File | null>(null)

// 计算属性
const currentCard = computed(() => {
  return cards.value[currentCardIndex.value]
})

const totalCards = computed(() => {
  return cards.value.length
})

// 日期格式化函数
const formatDate = (date: Date | string | number, format: string = 'YYYY-MM-DD HH:mm'): string => {
  if (!date) return ''
  
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  
  return format
    .replace('YYYY', year.toString())
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
}

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
        // 生成成功后刷新历史记录
        loadHistoryRecords()
      }
    )
  } catch (error) {
    ElMessage.error('生成失败，请重试')
    isGenerating.value = false
  }
}

// 处理选择题库
const handleSelectBank = async (bankId: number, bankName?: string, bankType?: string) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后查看题库内容')
    return
  }
  await loadBankCards(bankId, bankName, bankType)
}

// 加载题库卡片
const loadBankCards = async (bankId: number, bankName?: string, bankType?: string) => {
  try {
    // 添加严格的类型检查，确保bankId是有效的数字
    if (!bankId || typeof bankId !== 'number' || isNaN(bankId) || bankId <= 0) {
      console.error('无效的题库ID:', bankId)
      ElMessage.error('无效的题库ID')
      return
    }
    
    const response = await questionBankApi.getBankCards(bankId)
    
    if (response.code === 200 && response.data && response.data.length > 0) {
      cards.value = response.data.map((card: any) => ({
        id: card.id,
        question: card.question,
        answer: card.answer,
        questionImage: card.questionImage,
        answerImage: card.answerImage
      }))
      
      // 保存当前题库ID
      currentBankId.value = bankId
      
      // 保存题库名称
      if (bankName) {
        currentBankName.value = bankName
      }
      
      // 保存题库类型
      if (bankType) {
        currentBankType.value = bankType
      }
      
      showCards.value = true
      currentCardIndex.value = 0
      isFlipped.value = false
      ElMessage.success(`加载了 ${cards.value.length} 张卡片`)
    } else {
      ElMessage.warning('该题库暂无卡片')
    }
  } catch (error) {
    console.error('加载卡片失败:', error)
    ElMessage.error('加载卡片失败，请重试')
  }
}

// 编辑题库
const showEditBankDialogFunc = (bank: any) => {
  editBankForm.value = {
    id: bank.id,
    name: bank.name,
    description: bank.description || '',
    difficulty: bank.difficulty || 'medium',
    language: bank.language || '中文'
  }
  showEditBankDialog.value = true
}

// 分享题库
const handleShareBank = (bankId: number) => {
  currentSharingBankId.value = bankId
  shareCode.value = ''
  shareExpireHours.value = null
  showShareDialog.value = true
}

// 生成分享码
const confirmGenerateShare = async () => {
  if (!currentSharingBankId.value) {
    ElMessage.error('题库ID无效')
    return
  }

  try {
    const response = await questionBankApi.generateShareCode(
      currentSharingBankId.value,
      shareExpireHours.value || undefined
    )
    shareCode.value = response.data.shareCode
    ElMessage.success('分享码生成成功！')
  } catch (error: any) {
    console.error('生成分享码失败:', error)
    ElMessage.error(error.response?.data?.message || '生成分享码失败')
  }
}

// 创建自定义题库
const handleCreateBank = async () => {
  if (!createBankForm.value.name || !createBankForm.value.topic) {
    ElMessage.warning('请填写题库名称和主题')
    return
  }
  
  try {
    await questionBankApi.createCustomBank({
      name: createBankForm.value.name,
      topic: createBankForm.value.topic,
      description: createBankForm.value.description,
      difficulty: createBankForm.value.difficulty,
      language: createBankForm.value.language
    })
    
    ElMessage.success('题库创建成功！')
    showCreateBankDialog.value = false
    
    // 重置表单
    createBankForm.value = {
      name: '',
      topic: '',
      description: '',
      difficulty: 'medium',
      language: '中文'
    }
    
    // 刷新题库列表
    await loadCustomBanks()
  } catch (error) {
    ElMessage.error('创建题库失败，请重试')
  }
}

// 更新题库
const handleUpdateBank = async () => {
  if (!editBankForm.value.name.trim()) {
    ElMessage.warning('请输入题库名称')
    return
  }

  if (!editBankForm.value.id) {
    ElMessage.error('题库ID无效')
    return
  }

  try {
    await questionBankApi.updateBank(editBankForm.value.id, {
      name: editBankForm.value.name.trim(),
      description: editBankForm.value.description.trim(),
      difficulty: editBankForm.value.difficulty,
      language: editBankForm.value.language
    })
    
    ElMessage.success('更新成功')
    showEditBankDialog.value = false
    
    // 刷新题库列表
    loadSystemBanks()
    loadCustomBanks()
  } catch (error: any) {
    console.error('更新题库失败:', error)
    ElMessage.error(error.response?.data?.message || '更新失败')
  }
}

// 删除题库
const handleDeleteBank = async (bankId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个题库吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 实际项目中这里应该调用删除API
    ElMessage.success('删除成功')
    // 删除成功后刷新题库列表
    loadCustomBanks()
  } catch (error) {
    // 用户取消删除
  }
}

// 加载用户题库列表(用于导入时选择)
const loadUserBanks = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return
  
  try {
    const response = await questionBankApi.getUserBanks(userStore.userInfo.id)
    if (response.code === 200) {
      userBanks.value = response.data
    }
  } catch (error) {
    console.error('加载用户题库列表失败:', error)
  }
}

// 显示导入对话框
const handleShowImportDialog = async () => {
  // 加载用户题库列表
  await loadUserBanks()
  showImportDialog.value = true
}

// 文件选择变化处理
const handleFileChange = (file: any) => {
  fileList.value = [file]
}

// 导入题库
const handleImport = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的Excel文件')
    return
  }
  
  if (importForm.value.importMode === 'existing' && !importForm.value.targetBankId) {
    ElMessage.warning('请选择要导入的题库')
    return
  }
  
  isImporting.value = true
  try {
    const file = fileList.value[0].raw
    const formData = new FormData()
    formData.append('file', file)
    
    if (importForm.value.importMode === 'existing') {
      // 导入到已有题库
      formData.append('targetBankId', importForm.value.targetBankId!.toString())
    } else {
      // 创建新题库
      if (importForm.value.bankName) {
        formData.append('bankName', importForm.value.bankName)
      }
      if (importForm.value.description) {
        formData.append('description', importForm.value.description)
      }
      formData.append('difficulty', importForm.value.difficulty)
      formData.append('language', importForm.value.language)
    }

    const response = await questionBankApi.importBankFromExcel(formData)
    
    if (response.code === 200) {
      ElMessage({
        message: '导入成功！',
        type: 'success',
        showClose: false,
        duration: 2000
      })
      showImportDialog.value = false
      // 重置表单
      importForm.value = {
        importMode: 'new',
        targetBankId: null,
        bankName: '',
        description: '',
        difficulty: 'medium',
        language: '中文'
      }
      fileList.value = []
      // 刷新题库列表
      await loadCustomBanks()
    } else {
      ElMessage.error(response.message || '导入失败')
    }
  } catch (error: any) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败，请重试')
  } finally {
    isImporting.value = false
  }
}

// 访问分享题库
const accessSharedBank = async () => {
  const code = accessShareCode.value.trim()
  
  if (!code) {
    ElMessage.warning('请输入分享码')
    return
  }
  
  // 验证分享码格式
  if (code.length < 6) {
    ElMessage.warning('分享码格式不正确')
    return
  }
  
  try {
    const response = await questionBankApi.getByShareCode(code)
    const bank = response.data
    
    ElMessage.success('访问成功！')
    showAccessDialog.value = false
    accessShareCode.value = ''
    
    // 加载题库卡片
    await loadBankCards(bank.id)
  } catch (error: any) {
    console.error('访问分享题库失败:', error)
    ElMessage.error(error.response?.data?.message || '分享码无效或已过期')
  }
}

// 导出题库为Excel
const exportBank = async (bankId: number, bankName: string) => {
  try {
    const token = localStorage.getItem('token')?.trim()
    if (!token) {
      ElMessage.error('请先登录')
      return
    }
    
    // 使用相对路径,自动适配当前域名和协议
    const url = `/api/question-bank/${bankId}/export`
    
    // 使用fetch下载文件
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
    
    if (!response.ok) {
      const errorText = await response.text()
      console.error('导出失败:', response.status, errorText)
      throw new Error(`导出失败: ${response.status}`)
    }
    
    // 创建blob并触发下载
    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = `${bankName}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败,请稍后重试')
  }
}

// 加载推荐题库
const loadSystemBanks = async () => {
  isLoadingBanks.value = true
  try {
    const response = await questionBankApi.getSystemBanks(
      bankSearchText.value,
      systemPage.value,
      systemPageSize.value
    )
    systemBanks.value = response.data?.data || []
    systemTotal.value = response.data?.total || 0
  } catch (error) {
    console.error('加载推荐题库失败:', error)
    ElMessage.error('加载推荐题库失败')
  } finally {
    isLoadingBanks.value = false
  }
}

// 加载自定义题库
const loadCustomBanks = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return
  
  isLoadingCustomBanks.value = true
  try {
    const response = await questionBankApi.getUserCustomBanks(
      customPage.value,
      customPageSize.value
    )
    customBanks.value = response.data?.data || []
    customTotal.value = response.data?.total || 0
  } catch (error) {
    console.error('加载我的题库失败:', error)
    ElMessage.error('加载我的题库失败')
  } finally {
    isLoadingCustomBanks.value = false
  }
}

// 加载历史生成记录
const loadHistoryRecords = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo) return
  
  isLoadingHistory.value = true
  try {
    const response = await questionBankApi.searchBanks({
      page: 1,
      pageSize: 20,
      sortBy: 'created_at',
      sortOrder: 'desc',
      userId: userStore.userInfo.id,
      type: 'ai'
    })
    historyRecords.value = response.data?.data || []
    console.log('历史记录数据:', historyRecords.value)
  } catch (error) {
    console.error('加载历史记录失败:', error)
    ElMessage.error('加载历史记录失败')
  } finally {
    isLoadingHistory.value = false
  }
}

// 加载分享记录
const loadSharedBanks = async () => {
  if (!userStore.isLoggedIn) return
  
  isLoadingSharedBanks.value = true
  try {
    const response = await questionBankApi.getSharedRecords()
    sharedBanks.value = response.data || []
    console.log('分享记录数据:', sharedBanks.value)
  } catch (error) {
    console.error('加载分享记录失败:', error)
    ElMessage.error('加载分享记录失败')
  } finally {
    isLoadingSharedBanks.value = false
  }
}

// 直接复制分享码
const copyShareCodeDirect = async (code: string) => {
  try {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(code)
    } else {
      const textArea = document.createElement('textarea')
      textArea.value = code
      textArea.style.position = 'fixed'
      textArea.style.top = '0'
      textArea.style.left = '0'
      textArea.style.opacity = '0'
      document.body.appendChild(textArea)
      textArea.focus()
      textArea.select()
      document.execCommand('copy')
      document.body.removeChild(textArea)
    }
    ElMessage.success('分享码已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
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

// 跳转到指定卡片
const goToCard = (index: number) => {
  if (index >= 0 && index < totalCards.value) {
    currentCardIndex.value = index
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

// 新增卡片
const handleAddNewCard = () => {
  if (currentBankType.value !== 'custom') {
    ElMessage.warning('只有自定义题库支持添加卡片')
    return
  }
  
  // 重置表单
  addCardForm.value = {
    question: '',
    answer: ''
  }
  questionImageFileList.value = []
  answerImageFileList.value = []
  showAddCardDialog.value = true
}

// 处理问题图片变化
const handleQuestionImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e: any) => {
    addCardForm.value.questionImage = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 处理答案图片变化
const handleAnswerImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e: any) => {
    addCardForm.value.answerImage = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 提交新卡片
const handleSubmitNewCard = async () => {
  if (!addCardForm.value.question.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }
  if (!addCardForm.value.answer.trim()) {
    ElMessage.warning('请输入答案内容')
    return
  }
  
  try {
    const cardData: any = {
      bankId: currentBankId.value,
      question: addCardForm.value.question,
      answer: addCardForm.value.answer
    }
    
    if (addCardForm.value.questionImage) {
      cardData.questionImage = addCardForm.value.questionImage
    }
    if (addCardForm.value.answerImage) {
      cardData.answerImage = addCardForm.value.answerImage
    }
    
    const response = await questionBankApi.addCard(cardData)
    if (response.code === 200) {
      ElMessage.success('卡片添加成功')
      showAddCardDialog.value = false
      
      // 重新加载卡片列表
      await loadBankCards(currentBankId.value, currentBankName.value)
    }
  } catch (error) {
    console.error('添加卡片失败:', error)
    ElMessage.error('添加卡片失败，请重试')
  }
}

// 编辑当前卡片
const handleEditCurrentCard = () => {
  if (!currentCard.value) {
    ElMessage.warning('没有可编辑的卡片')
    return
  }
  
  if (currentBankType.value !== 'custom' && currentBankType.value !== 'ai') {
    ElMessage.warning('该题库不支持编辑卡片')
    return
  }
  
  // 填充表单数据
  editCardForm.value = {
    question: currentCard.value.question,
    answer: currentCard.value.answer,
    questionImage: currentCard.value.questionImage,
    answerImage: currentCard.value.answerImage
  }
  
  // 设置图片文件列表
  editQuestionImageFileList.value = currentCard.value.questionImage ? [{
    name: 'question-image',
    url: currentCard.value.questionImage
  }] : []
  
  editAnswerImageFileList.value = currentCard.value.answerImage ? [{
    name: 'answer-image',
    url: currentCard.value.answerImage
  }] : []
  
  showEditCardDialog.value = true
}

// 处理编辑问题图片变化
const handleEditQuestionImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e: any) => {
    editCardForm.value.questionImage = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 移除编辑问题图片
const handleRemoveEditQuestionImage = () => {
  editCardForm.value.questionImage = ''
  editQuestionImageFileList.value = []
}

// 处理编辑答案图片变化
const handleEditAnswerImageChange = (file: any) => {
  const reader = new FileReader()
  reader.onload = (e: any) => {
    editCardForm.value.answerImage = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 移除编辑答案图片
const handleRemoveEditAnswerImage = () => {
  editCardForm.value.answerImage = ''
  editAnswerImageFileList.value = []
}

// 提交编辑卡片
const handleSubmitEditCard = async () => {
  if (!editCardForm.value.question.trim()) {
    ElMessage.warning('请输入问题内容')
    return
  }
  if (!editCardForm.value.answer.trim()) {
    ElMessage.warning('请输入答案内容')
    return
  }
  
  try {
    const cardData: any = {
      id: currentCard.value.id,
      bankId: currentBankId.value,
      question: editCardForm.value.question,
      answer: editCardForm.value.answer,
      questionImage: editCardForm.value.questionImage || '',
      answerImage: editCardForm.value.answerImage || ''
    }
    
    const response = await questionBankApi.updateCard(cardData)
    if (response.code === 200) {
      ElMessage.success('卡片更新成功')
      showEditCardDialog.value = false
      
      // 更新当前卡片数据
      cards.value[currentCardIndex.value] = {
        ...cards.value[currentCardIndex.value],
        question: editCardForm.value.question,
        answer: editCardForm.value.answer,
        questionImage: editCardForm.value.questionImage,
        answerImage: editCardForm.value.answerImage
      }
    }
  } catch (error) {
    console.error('更新卡片失败:', error)
    ElMessage.error('更新卡片失败，请重试')
  }
}

// 删除当前卡片
const handleDeleteCurrentCard = async () => {
  if (!currentCard.value) {
    ElMessage.warning('没有可删除的卡片')
    return
  }
  
  if (currentBankType.value !== 'custom') {
    ElMessage.warning('只有自定义题库支持删除卡片')
    return
  }
  
  if (cards.value.length <= 1) {
    ElMessage.warning('题库至少需要保留一张卡片')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '确定要删除这张卡片吗？',
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await questionBankApi.deleteCard(currentCard.value.id)
    if (response.code === 200) {
      ElMessage.success('卡片删除成功')
      
      // 从列表中移除当前卡片
      cards.value.splice(currentCardIndex.value, 1)
      
      // 调整当前索引
      if (currentCardIndex.value >= cards.value.length) {
        currentCardIndex.value = cards.value.length - 1
      }
      
      // 如果没有卡片了，返回主页
      if (cards.value.length === 0) {
        backToForm()
      }
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除卡片失败:', error)
      ElMessage.error('删除卡片失败，请重试')
    }
  }
}

// 页面加载时执行
const initPage = async () => {
  await loadSystemBanks()
  if (userStore.isLoggedIn) {
    await Promise.all([
      loadCustomBanks(),
      loadHistoryRecords(),
      loadSharedBanks()
    ])
  }
}

// 复制分享码到剪贴板
const copyShareCode = async () => {
  if (!shareCode.value) {
    ElMessage.warning('请先生成分享码')
    return
  }
  
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(shareCode.value)
      ElMessage.success('分享码已复制到剪贴板')
    } else {
      // 降级方案：使用传统方法
      const textArea = document.createElement('textarea')
      textArea.value = shareCode.value
      textArea.style.position = 'fixed'
      textArea.style.left = '-999999px'
      document.body.appendChild(textArea)
      textArea.select()
      try {
        document.execCommand('copy')
        ElMessage.success('分享码已复制到剪贴板')
      } catch (err) {
        ElMessage.error('复制失败，请手动复制')
      }
      document.body.removeChild(textArea)
    }
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

// 初始化页面
initPage()
</script>

<style scoped>
.mobile-home-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 16px;
  padding-bottom: 40px;
  box-sizing: border-box;
  overflow-x: hidden;
}

.mobile-header {
  text-align: center;
  margin-bottom: 24px;
  color: white;
  padding: 8px 0;
}

.app-title {
  font-size: 24px;
  margin-bottom: 12px;
  font-weight: bold;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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

.mobile-content {
  background: white;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  margin-bottom: 20px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.form-options {
  margin-bottom: 20px;
}

.form-select {
  margin-bottom: 12px;
  width: 100%;
}

.form-select :deep(.el-select) {
  width: 100%;
}

.form-select :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.form-checkbox {
  margin-top: 12px;
  display: block;
}

.generate-button {
  width: 100%;
  padding: 14px 0;
  font-size: 16px;
  font-weight: bold;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 题库模块样式 */
.question-bank {
  margin-top: 20px;
}

.bank-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 4px;
}

.bank-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

/* 搜索框样式 */
.bank-search {
  margin: 16px 0;
  display: flex;
  align-items: center;
}

.bank-search :deep(.el-input__wrapper) {
  border-radius: 20px;
  padding: 0 16px;
  height: 40px;
}

.bank-search :deep(.el-input__inner) {
  font-size: 14px;
}

/* 题库列表样式 */
.bank-list {
  margin-top: 16px;
}

.bank-list h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
  padding: 0 4px;
}

/* 题库卡片样式 */
.bank-card {
  position: relative;
  background: #fff;
  border-radius: 12px;
  padding: 14px 110px 14px 14px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid #f0f0f0;
}

.bank-card:active {
  transform: scale(0.98);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.bank-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #333;
  line-height: 1.4;
}

.bank-description {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bank-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.bank-meta {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 8px;
  gap: 8px;
  flex-wrap: wrap;
}

.bank-count {
  display: flex;
  align-items: center;
  gap: 4px;
}

.bank-count svg {
  width: 14px;
  height: 14px;
}

.bank-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.bank-time .el-icon {
  width: 14px;
  height: 14px;
}

.bank-topic {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  padding: 2px 8px;
  background: #ecf5ff;
  border-radius: 10px;
  white-space: nowrap;
}

.share-code-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #e6a23c;
  background: #fdf6ec;
  padding: 4px 8px;
  border-radius: 10px;
}

.share-code-tag .el-icon {
  width: 14px;
  height: 14px;
}

.bank-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 6px;
}

.bank-actions .el-button {
  width: 32px;
  height: 32px;
  padding: 0;
}

/* 功能按钮区域 */
.bank-functions {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}

.function-button {
  flex: 1;
  height: 44px;
  font-size: 14px;
}

.feature-buttons {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}

.feature-btn {
  flex: 1;
  height: 44px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
  color: #606266;
}

.feature-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.feature-btn svg {
  margin-bottom: 4px;
  font-size: 16px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: #666;
  font-size: 14px;
  gap: 8px;
}

.loading-state .el-icon {
  font-size: 20px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
  font-size: 14px;
}

.empty-state .el-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 13px;
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
  color: #333;
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
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card-progress {
  background: rgba(255, 255, 255, 0.9);
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.header-operation-button {
  min-width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.header-operation-button:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.back-button {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 20px;
  color: #333;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.back-button:hover {
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

/* 卡片容器样式 */
.card-container {
  width: 100%;
  min-height: 450px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  margin-bottom: 30px;
  perspective: 1000px;
}

/* 翻转卡片样式 */
.flip-card {
  width: 100%;
  height: 450px;
  cursor: pointer;
  position: relative;
  transition: transform 0.2s ease;
}

.flip-card:active {
  transform: scale(0.98);
}

.flip-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  text-align: center;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
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
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 问题面 - 深蓝色渐变 */
.flip-card-front {
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
  color: white;
}

/* 答案面 - 绿色渐变 */
.flip-card-back {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
  color: white;
  transform: rotateY(180deg);
}

/* 卡片徽章 */
.card-badge {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 卡片内容样式 */
.card-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 30px 30px;
  overflow-y: auto;
}

/* 卡片文本 */
.card-text {
  font-size: 18px;
  line-height: 1.8;
  font-weight: 500;
  word-wrap: break-word;
  text-align: center;
  max-width: 100%;
}

/* 问题文本特别强调 */
.question-text {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.9;
}

/* 卡片底部 */
.card-footer {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

/* 提示文本 */
.tap-hint {
  font-size: 12px;
  opacity: 0.8;
  display: flex;
  align-items: center;
  gap: 4px;
}

.tap-hint::before {
  content: '👆';
  font-size: 14px;
}

/* 语音按钮 */
.voice-button {
  background: rgba(255, 255, 255, 0.3) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4) !important;
  color: white !important;
  font-size: 11px !important;
  padding: 6px 14px !important;
  border-radius: 20px !important;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
}

.voice-button:hover {
  background: rgba(255, 255, 255, 0.4) !important;
  transform: translateY(-1px);
}

.voice-button:active {
  transform: translateY(0);
}

/* 卡片进度指示器 */
.card-progress-bar {
  display: flex;
  justify-content: center;
  margin: 16px 0;
}

.progress-dots {
  display: flex;
  gap: 8px;
  align-items: center;
}

.progress-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e0e0e0;
  border: 1px solid rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.progress-dot.active {
  width: 24px;
  border-radius: 4px;
  background: #409eff;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
}

.progress-dot:hover:not(.active) {
  background: #c0c0c0;
  transform: scale(1.2);
}

/* 底部按钮区域 */
.card-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
}

.nav-button {
  flex: 1;
  padding: 14px 0 !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 16px !important;
  border: none !important;
  color: white !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  height: auto !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
}

.nav-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2) !important;
}

.nav-button:active {
  transform: translateY(0);
}

.nav-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
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